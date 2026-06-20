package com.xiyunmn.puredupan.hook.feature.baidu.cn.ui

import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.symbols.baidu.cn.BaiduCnHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.core.HookState

/**
 * 底栏 AI Tab 替换为会员 Hook。
 *
 * 受 [HookSettings.isBottomAiReplaced] 控制，默认开启。
 */
object BottomAiTabReplaceHook {
    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!isEnabled()) {
            XposedCompat.log("[BottomAiTabReplaceHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val clazz = XposedCompat.findClassOrNull(
                BaiduCnHookPoints.AI_CLOUD_TAB_AMIS_KT, cl
            ) ?: run {
                XposedCompat.log("[BottomAiTabReplaceHook] AiCloudTabAmisKt class NOT FOUND")
                return
            }
            val method = XposedCompat.findMethodOrNull(clazz, "getAiCloudTabMode")
                ?: run {
                    XposedCompat.log("[BottomAiTabReplaceHook] getAiCloudTabMode NOT FOUND")
                    return
            }
            mod.hook(method).intercept {
                if (isEnabled()) 0L else it.proceed()
            }
            XposedCompat.log("[BottomAiTabReplaceHook] hook INSTALLED")
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[BottomAiTabReplaceHook] FAILED: ${e.message}")
        }
    }

    private fun isEnabled(): Boolean =
        HookSettings.isBottomBarCustomEnabled && HookSettings.isBottomAiReplaced
}
