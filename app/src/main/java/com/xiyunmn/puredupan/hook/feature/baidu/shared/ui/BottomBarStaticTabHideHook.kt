package com.xiyunmn.puredupan.hook.feature.baidu.shared.ui

import android.app.Activity
import android.view.View
import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.feature.baidu.shared.runtime.BaiduFeatureRuntime

/**
 * Hides static bottom tabs at MainActivity's fixed render entry.
 *
 * AIGC is intentionally excluded here; it is blocked by the mode provider hook.
 */
internal object BottomBarStaticTabHideHook {
    private const val INIT_VIEW_METHOD = "initView"

    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!isEnabled()) {
            XposedCompat.log("[BottomBarStaticTabHideHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val className = BaiduFeatureRuntime.currentMainActivityClassName()
            val clazz = className?.let { XposedCompat.findClassOrNull(it, cl) }
            val method = clazz?.declaredMethods?.firstOrNull {
                it.name == INIT_VIEW_METHOD && it.parameterTypes.isEmpty()
            }

            if (method == null) {
                hookState.reset()
                XposedCompat.log("[BottomBarStaticTabHideHook] MainActivity.initView NOT FOUND")
                return
            }

            method.isAccessible = true
            mod.hook(method).intercept { chain ->
                val result = chain.proceed()
                if (isEnabled()) {
                    applyStaticTabVisibility(chain.thisObject as Activity)
                }
                result
            }
            XposedCompat.log("[BottomBarStaticTabHideHook] hook INSTALLED: ${clazz.name}.$INIT_VIEW_METHOD")
        } catch (t: Throwable) {
            hookState.reset()
            XposedCompat.log("[BottomBarStaticTabHideHook] install FAILED: ${t.message}")
            XposedCompat.log(t)
        }
    }

    private fun applyStaticTabVisibility(activity: Activity) {
        hideTab(activity, "rb_home", HookSettings.isBottomBarTabHomeHidden)
        hideTab(activity, "rb_filelist", HookSettings.isBottomBarTabFileHidden)
        hideTab(activity, "rb_share", HookSettings.isBottomBarTabShareHidden)
        hideTab(activity, "rb_findresoure", HookSettings.isBottomBarTabVipHidden)
        hideTab(activity, "rb_about_me", HookSettings.isBottomBarTabMineHidden)
    }

    private fun hideTab(activity: Activity, idName: String, hidden: Boolean) {
        if (!hidden) return
        val id = activity.resources.getIdentifier(idName, "id", activity.packageName)
        if (id == 0) return
        activity.findViewById<View>(id)?.visibility = View.GONE
    }

    private fun isEnabled(): Boolean =
        HookSettings.isBottomBarCustomEnabled &&
            (
                HookSettings.isBottomBarTabHomeHidden ||
                    HookSettings.isBottomBarTabFileHidden ||
                    HookSettings.isBottomBarTabShareHidden ||
                    HookSettings.isBottomBarTabVipHidden ||
                    HookSettings.isBottomBarTabMineHidden
                )
}
