package com.xiyunmn.puredupan.hook.feature.performance

import com.xiyunmn.puredupan.hook.config.ConfigManager
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.StableBaiduPanHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat


object AudioCircleViewAutostartBlockHook {
    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!isEnabled()) {
            XposedCompat.log("[AudioCircleViewAutostartBlock] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val installed = hookFloatViewStartupTask(cl)
            if (installed == 0) {
                hookState.reset()
                XposedCompat.log("[AudioCircleViewAutostartBlock] No hooks installed")
            } else {
                XposedCompat.log("[AudioCircleViewAutostartBlock] hook INSTALLED")
            }
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[AudioCircleViewAutostartBlock] Installation error: ${e.message}")
            XposedCompat.log(e)
        }
    }

   
    private fun hookFloatViewStartupTask(cl: ClassLoader): Int {
        val mod = XposedCompat.module ?: return 0
        val clazz = XposedCompat.findClassOrNull(
            StableBaiduPanHookPoints.FLOAT_VIEW_STARTUP_TASK,
            cl,
        )
        if (clazz == null) {
            XposedCompat.log("[AudioCircleViewAutostartBlock] FloatViewStartupTask class NOT FOUND")
            return 0
        }

        val method = XposedCompat.findMethodOrNull(
            clazz,
            StableBaiduPanHookPoints.FLOAT_VIEW_STARTUP_TASK_INIT_AUDIO_CIRCLE_VIEW_METHOD,
        )
        if (method == null) {
            XposedCompat.log("[AudioCircleViewAutostartBlock] initAudioCircleView method NOT FOUND")
            return 0
        }

        mod.hook(method).intercept { chain ->
            if (isEnabled()) {
                XposedCompat.logD("[AudioCircleViewAutostartBlock] initAudioCircleView blocked")
                null
            } else {
                chain.proceed()
            }
        }

        XposedCompat.log("[AudioCircleViewAutostartBlock] FloatViewStartupTask.initAudioCircleView hooked")
        return 1
    }

    private fun isEnabled(): Boolean =
        ConfigManager.isPerformanceOptimizeEnabled && ConfigManager.isMediaBrowserServiceAutostartDisabled
}
