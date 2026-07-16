package com.xiyunmn.puredupan.hook.feature.baidu.shared.ui

import android.content.Context
import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.symbols.baidu.shared.BaiduFilePageHookPoints

/**
 * 文件页定制 Hook。
 *
 * 底部安全提示迁移到渲染入口层：hook [BaiduFilePageHookPoints.MY_NETDISK_FRAGMENT]
 * 的 [BaiduFilePageHookPoints.INIT_SAFETY_BOTTOM_VIEW]，命中隐藏开关时整方法 no-op，
 * 不 inflate `safety_ability_layout`、不 addFooterView。此时宿主 mBottomSafety 保持
 * null，下游 updateBottomSafetyVisibility / updateSafetyBottomContent 已有 null 判空，
 * 天然安全。三端类名、方法名和签名稳定未混淆，不纳入 DexKit。
 *
 * 已删除旧 View 树路径：FileListChildFragment 根节点的 OnGlobalLayoutListener /
 * OnPreDrawListener / postDelayed 循环，以及 safe_ability_layout 资源 ID 全树递归。
 */
internal object FilePageCustomizeHook {

    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!isEnabled()) {
            XposedCompat.log("[FilePageCustomizeHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val clazz = XposedCompat.findClassOrNull(
                BaiduFilePageHookPoints.MY_NETDISK_FRAGMENT,
                cl,
            ) ?: run {
                XposedCompat.log("[FilePageCustomizeHook] MyNetdiskFragment class NOT FOUND")
                hookState.reset()
                return
            }

            val method = XposedCompat.findMethodOrNull(
                clazz,
                BaiduFilePageHookPoints.INIT_SAFETY_BOTTOM_VIEW,
                Context::class.java,
            ) ?: run {
                XposedCompat.log("[FilePageCustomizeHook] initSafetyBottomView(Context) NOT FOUND")
                hookState.reset()
                return
            }

            mod.hook(method).intercept { chain ->
                if (isEnabled()) {
                    XposedCompat.logD(
                        "[FilePageCustomizeHook] MyNetdiskFragment.initSafetyBottomView blocked",
                    )
                    null
                } else {
                    chain.proceed()
                }
            }

            XposedCompat.log("[FilePageCustomizeHook] hook INSTALLED")
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[FilePageCustomizeHook] FAILED: ${e.message}")
            XposedCompat.log(e)
        }
    }

    private fun isEnabled(): Boolean {
        return HookSettings.isFilePageCustomizeEnabled &&
            HookSettings.isFilePageBottomSafetyTipHidden
    }
}
