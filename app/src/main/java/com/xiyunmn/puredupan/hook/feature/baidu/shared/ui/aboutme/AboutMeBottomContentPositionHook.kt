package com.xiyunmn.puredupan.hook.feature.baidu.shared.ui.aboutme

import android.os.Bundle
import android.os.Build
import android.view.View
import android.view.ViewParent
import com.xiyunmn.puredupan.hook.BuildConfig
import com.xiyunmn.puredupan.hook.config.SettingsSnapshot
import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.symbols.baidu.shared.BaiduAboutMeHookPoints
import java.util.Collections
import java.util.WeakHashMap

/** Moves the content below the member card without remeasuring the host scroll layout. */
internal object AboutMeBottomContentPositionHook {
    private const val TAG = "AboutMeBottomContentPositionHook"
    private const val SCROLL_VIEW_ID = "scroll_view"
    private const val APPLY_DELAY_MS = 1500L
    private const val CACHE_SCHEMA_VERSION = 2
    private const val MIN_OFFSET_DP = -160
    private const val MAX_OFFSET_DP = 160

    private val hookState = HookState()
    private data class PositionCache(
        val baseTranslationY: Float,
    )

    private data class ResolvedOffset(
        val signature: String,
        val offsetPx: Int,
        val fromPersistentCache: Boolean,
    )

    private val positionCache: MutableMap<View, PositionCache> =
        Collections.synchronizedMap(WeakHashMap())
    private val appliedRoots: MutableSet<View> =
        Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))
    fun hook(cl: ClassLoader) {
        val snapshot = HookSettings.settingsSnapshot()
        if (!isEnabled(snapshot)) {
            XposedCompat.logD("[$TAG] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        val fragmentClass = XposedCompat.findClassOrNull(
            BaiduAboutMeHookPoints.ABOUT_ME_BOTTOM_FRAGMENT,
            cl,
        ) ?: run {
            hookState.reset()
            XposedCompat.logW("[$TAG] AboutMeBottomFragment not found")
            return
        }
        val method = XposedCompat.findMethodOrNull(
            fragmentClass,
            "onViewCreated",
            View::class.java,
            Bundle::class.java,
        ) ?: run {
            hookState.reset()
            XposedCompat.logW("[$TAG] AboutMeBottomFragment.onViewCreated not found")
            return
        }

        mod.hook(method).intercept { chain ->
            val result = chain.proceed()
            val root = chain.args.firstOrNull() as? View
            root?.post { applyPosition(root, "cache", allowCalibration = false) }
            root?.postDelayed(
                { applyPosition(root, "calibration", allowCalibration = true) },
                APPLY_DELAY_MS,
            )
            result
        }
        XposedCompat.logD("[$TAG] hook installed: ${fragmentClass.name}.${method.name}")
    }

    private fun applyPosition(root: View?, source: String, allowCalibration: Boolean) {
        root ?: return
        val snapshot = HookSettings.settingsSnapshot()
        if (!isEnabled(snapshot)) return
        val resolved = resolveOffset(root, snapshot, allowCalibration) ?: return
        val scrollView = findScrollView(root) ?: run {
            XposedCompat.logD(
                "[$TAG] scroll view not found via $source: root=${root.javaClass.name}, " +
                    "attached=${root.isAttachedToWindow}",
            )
            return
        }
        if (!appliedRoots.add(root)) return
        val cached = positionCache.getOrPut(scrollView) {
            PositionCache(scrollView.translationY)
        }
        scrollView.translationY = cached.baseTranslationY
        val targetTranslationY = cached.baseTranslationY + resolved.offsetPx
        if (scrollView.translationY != targetTranslationY) {
            scrollView.translationY = targetTranslationY
        }

        if (!resolved.fromPersistentCache) {
            HookSettings.recordContentPositionCache(
                root.context,
                HookSettings.ContentPositionCache(resolved.signature, resolved.offsetPx),
            )
        }

        XposedCompat.logD(
            "[$TAG] content position applied via $source: offsetPx=${resolved.offsetPx}, " +
                "translationY=${cached.baseTranslationY}->$targetTranslationY",
        )
    }

    private fun resolveOffset(
        view: View,
        snapshot: SettingsSnapshot,
        allowCalibration: Boolean,
    ): ResolvedOffset? {
        val density = view.resources?.displayMetrics?.density ?: return null
        val signature = cacheSignature(view, snapshot, density)
        HookSettings.contentPositionCache(view.context, signature)?.let { cached ->
            return ResolvedOffset(signature, cached.offsetPx, fromPersistentCache = true)
        }
        if (!allowCalibration) return null
        val offsetPx: Int? = when {
            snapshot.isMyPageContentAutoFollowMemberCardEnabled -> {
                autoFollowOffsetPx(view, snapshot, density)
            }
            snapshot.isMyPageContentManualOffsetEnabled -> {
                dpToPx(snapshot.myPageContentOffsetYDp.coerceIn(MIN_OFFSET_DP, MAX_OFFSET_DP), density)
            }
            else -> 0
        }
        return offsetPx?.let { ResolvedOffset(signature, it, fromPersistentCache = false) }
    }

    private fun cacheSignature(
        view: View,
        snapshot: SettingsSnapshot,
        density: Float,
    ): String {
        val hostVersion = hostVersionCode(view)
        val packageName = view.context?.packageName.orEmpty()
        return if (snapshot.isMyPageContentAutoFollowMemberCardEnabled) {
            val defaultHeightPx = view.context?.let(HookSettings::recordedMemberCardDefaultHeightPx) ?: 0
            listOf(
                CACHE_SCHEMA_VERSION,
                packageName,
                hostVersion,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                density.toBits(),
                "auto",
                snapshot.isMemberCardSizeAdjusted,
                snapshot.memberCardHeightDp,
                defaultHeightPx,
            ).joinToString(":")
        } else {
            listOf(
                CACHE_SCHEMA_VERSION,
                packageName,
                hostVersion,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                density.toBits(),
                "manual",
                snapshot.myPageContentOffsetYDp.coerceIn(MIN_OFFSET_DP, MAX_OFFSET_DP),
            ).joinToString(":")
        }
    }

    private fun hostVersionCode(view: View): Long {
        val context = view.context ?: return 0L
        return runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        }.getOrDefault(0L)
    }

    private fun autoFollowOffsetPx(
        view: View,
        snapshot: SettingsSnapshot,
        density: Float,
    ): Int? {
        if (!snapshot.isMemberCardSizeAdjusted || snapshot.memberCardHeightDp <= 0) return 0
        val context = view.context ?: return 0
        val defaultHeightPx = HookSettings.recordedMemberCardDefaultHeightPx(context)
        if (defaultHeightPx <= 0) {
            XposedCompat.logD("[$TAG] auto-follow pending: default member-card height unavailable")
            return null
        }
        val targetHeightPx = dpToPx(snapshot.memberCardHeightDp, density)
        val minOffsetPx = dpToPx(MIN_OFFSET_DP, density)
        val maxOffsetPx = dpToPx(MAX_OFFSET_DP, density)
        return (targetHeightPx - defaultHeightPx).coerceIn(minOffsetPx, maxOffsetPx)
    }

    private fun findScrollView(root: View): View? {
        val context = root.context ?: return null
        val id = root.resources?.getIdentifier(SCROLL_VIEW_ID, "id", context.packageName) ?: 0
        if ((id != 0 && root.id == id) || root.javaClass.name.endsWith("NestedScrollView")) {
            return root
        }
        var current: ViewParent? = root.parent
        while (current is View) {
            if ((id != 0 && current.id == id) ||
                current.javaClass.name.endsWith("NestedScrollView")
            ) {
                return current
            }
            current = current.parent
        }
        return if (id != 0) (root.rootView ?: root).findViewById(id) else null
    }

    private fun isEnabled(snapshot: SettingsSnapshot): Boolean {
        return snapshot.isMyPageCustomizeEnabled &&
            (
                snapshot.isMyPageContentAutoFollowMemberCardEnabled ||
                    snapshot.isMyPageContentManualOffsetEnabled
                )
    }

    private fun dpToPx(dp: Int, density: Float): Int = (dp * density).toInt()
}
