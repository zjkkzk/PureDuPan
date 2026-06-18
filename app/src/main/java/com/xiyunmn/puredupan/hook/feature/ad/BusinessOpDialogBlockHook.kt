package com.xiyunmn.puredupan.hook.feature.ad

import com.xiyunmn.puredupan.hook.config.ConfigManager
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.StableBaiduPanHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.core.HookUtils

/**
 * Blocks in-app business operation dialogs.
 *
 * This is intentionally separate from SplashInterstitialBlockHook. Both this hook and
 * LuckyCouponBlockHook are controlled by ConfigManager.isInAppDialogBlocked.
 */
object BusinessOpDialogBlockHook {
    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!ConfigManager.isInAppDialogBlocked) {
            XposedCompat.log("[BusinessOpDialogBlockHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val clazz = XposedCompat.findClassOrNull(
                StableBaiduPanHookPoints.BUSINESS_OP_DIALOG,
                cl,
            ) ?: run {
                XposedCompat.log("[BusinessOpDialogBlockHook] BusinessOPDialog class NOT FOUND")
                return
            }

            var installed = 0
            for (method in clazz.declaredMethods) {
                if (method.name != StableBaiduPanHookPoints.BUSINESS_OP_DIALOG_SHOW_DIALOG_METHOD) {
                    continue
                }
                method.isAccessible = true
                mod.hook(method).intercept { chain ->
                    if (ConfigManager.isInAppDialogBlocked) {
                        HookUtils.getDefaultReturnValue(method.returnType)
                    } else {
                        chain.proceed()
                    }
                }
                installed++
            }

            for (method in clazz.declaredMethods) {
                if (method.name != StableBaiduPanHookPoints.BUSINESS_OP_DIALOG_ON_CREATE_VIEW_METHOD) {
                    continue
                }
                method.isAccessible = true
                mod.hook(method).intercept { chain ->
                    if (ConfigManager.isInAppDialogBlocked) {
                        null
                    } else {
                        chain.proceed()
                    }
                }
                installed++
            }

            if (installed == 0) {
                hookState.reset()
                XposedCompat.log("[BusinessOpDialogBlockHook] BusinessOPDialog methods NOT FOUND")
                return
            }

            XposedCompat.log("[BusinessOpDialogBlockHook] hooks INSTALLED: count=$installed")
        } catch (e: ReflectiveOperationException) {
            hookState.reset()
            XposedCompat.log("[BusinessOpDialogBlockHook] FAILED (reflection): ${e.javaClass.simpleName}: ${e.message}")
            XposedCompat.log(e)
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[BusinessOpDialogBlockHook] FAILED: ${e.message}")
            XposedCompat.log(e)
        }
    }
}
