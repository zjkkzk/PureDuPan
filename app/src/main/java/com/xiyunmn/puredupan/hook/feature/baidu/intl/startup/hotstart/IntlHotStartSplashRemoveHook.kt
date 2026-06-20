package com.xiyunmn.puredupan.hook.feature.baidu.intl.startup.hotstart

import android.app.Activity
import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.symbols.baidu.intl.BaiduIntlHookPoints

internal object IntlHotStartSplashRemoveHook {
    private const val SPLASH_AD_ACTIVITY_CLASS_NAME = BaiduIntlHookPoints.SPLASH_AD_ACTIVITY

    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!HookSettings.isHotStartSplashRemoveEnabled) return
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            hookSplashAdActivity(cl)

            val resolvedMethod = resolveWithDexKit(cl)

            if (resolvedMethod == null) {
                XposedCompat.log("[IntlHotStartSplashRemoveHook] hot start entry NOT FOUND, fallback only")
                return
            }

            mod.hook(resolvedMethod).intercept {
                if (HookSettings.isHotStartSplashRemoveEnabled) false else it.proceed()
            }
            XposedCompat.log(
                "[IntlHotStartSplashRemoveHook] ${resolvedMethod.declaringClass.name}.${resolvedMethod.name} hooked",
            )
        } catch (t: Throwable) {
            hookState.reset()
            XposedCompat.log("[IntlHotStartSplashRemoveHook] install FAILED: ${t.message}")
            XposedCompat.log(t)
        }
    }

    private fun resolveWithDexKit(cl: ClassLoader): java.lang.reflect.Method? {
        val result = IntlHotStartSplashDexKitResolver.resolve(cl) ?: return null
        val clazz = XposedCompat.findClassOrNull(result.className, cl) ?: return null
        return XposedCompat.findMethodOrNull(clazz, result.methodName, Activity::class.java)
    }

    private fun hookSplashAdActivity(cl: ClassLoader) {
        val mod = XposedCompat.module ?: return
        val splashActivityClass = XposedCompat.findClassOrNull(SPLASH_AD_ACTIVITY_CLASS_NAME, cl) ?: run {
            XposedCompat.logD("[IntlHotStartSplashRemoveHook] SplashAdActivity class not found for fallback")
            return
        }

        val onCreate = XposedCompat.findMethodOrNull(
            splashActivityClass,
            "onCreate",
            android.os.Bundle::class.java,
        ) ?: run {
            XposedCompat.logD("[IntlHotStartSplashRemoveHook] SplashAdActivity.onCreate not found for fallback")
            return
        }

        mod.hook(onCreate).intercept { chain ->
            val result = chain.proceed()
            if (HookSettings.isHotStartSplashRemoveEnabled) {
                (chain.thisObject as? Activity)?.let { activity ->
                    XposedCompat.log("[IntlHotStartSplashRemoveHook] finishing SplashAdActivity fallback")
                    activity.finish()
                    activity.overridePendingTransition(0, 0)
                }
            }
            result
        }
    }
}
