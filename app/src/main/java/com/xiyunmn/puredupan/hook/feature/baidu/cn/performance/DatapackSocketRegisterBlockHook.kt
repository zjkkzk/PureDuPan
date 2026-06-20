package com.xiyunmn.puredupan.hook.feature.baidu.cn.performance

import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.symbols.baidu.cn.BaiduCnHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.core.HookState

/**
 * Blocks only the DataPack module socket registration.
 */
object DatapackSocketRegisterBlockHook {
    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!isEnabled()) {
            XposedCompat.log("[DatapackSocketRegisterBlockHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val clazz = XposedCompat.findClassOrNull(
                BaiduCnHookPoints.DATAPACK_CONTEXT_COMPANION,
                cl,
            ) ?: run {
                XposedCompat.log("[DatapackSocketRegisterBlockHook] DatapackContext Companion class NOT FOUND")
                hookState.reset()
                return
            }

            val method = XposedCompat.findMethodOrNull(
                clazz,
                BaiduCnHookPoints.DATAPACK_REGISTER_SOCKET_METHOD,
            ) ?: run {
                XposedCompat.log("[DatapackSocketRegisterBlockHook] registerSocket NOT FOUND")
                hookState.reset()
                return
            }

            mod.hook(method).intercept { chain ->
                if (isEnabled()) {
                    XposedCompat.logD("[DatapackSocketRegisterBlockHook] registerSocket blocked")
                    null
                } else {
                    chain.proceed()
                }
            }

            XposedCompat.log("[DatapackSocketRegisterBlockHook] hook INSTALLED")
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[DatapackSocketRegisterBlockHook] FAILED: ${e.message}")
            XposedCompat.log(e)
        }
    }



    private fun isEnabled(): Boolean =
        HookSettings.isPerformanceOptimizeEnabled && HookSettings.isDatapackSocketRegisterDisabled
}
