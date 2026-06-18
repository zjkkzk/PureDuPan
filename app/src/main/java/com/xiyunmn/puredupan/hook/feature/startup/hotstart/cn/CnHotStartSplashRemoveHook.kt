package com.xiyunmn.puredupan.hook.feature.startup.hotstart.cn

import android.app.Activity
import com.xiyunmn.puredupan.hook.config.ConfigManager
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.StableBaiduPanHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat

internal object CnHotStartSplashRemoveHook {
    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!ConfigManager.isSplashInterstitialBlockEnabled) return
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val hotStartClass = XposedCompat.findClassOrNull(
                StableBaiduPanHookPoints.ADVERTISE_HOT_START_MANAGER,
                cl,
            )
            val method = hotStartClass?.let {
                XposedCompat.findMethodOrNull(it, "onResume", Activity::class.java)
            }
            if (method == null) {
                hookState.reset()
                XposedCompat.log("[CnHotStartSplashRemoveHook] onResume hook target NOT FOUND")
                return
            }

            mod.hook(method).intercept {
                if (ConfigManager.isSplashInterstitialBlockEnabled) false else it.proceed()
            }
            XposedCompat.log("[CnHotStartSplashRemoveHook] AdvertiseHotStartManager.onResume hooked")
        } catch (t: Throwable) {
            hookState.reset()
            XposedCompat.log("[CnHotStartSplashRemoveHook] install FAILED: ${t.message}")
            XposedCompat.log(t)
        }
    }
}
