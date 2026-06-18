package com.xiyunmn.puredupan.hook.feature.ad

import com.xiyunmn.puredupan.hook.config.ConfigManager
import com.xiyunmn.puredupan.hook.core.StableBaiduPanHookPoints
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.core.HookUtils

/**
 * Blocks the full-screen SVIP exclusive icon guide shown from AboutMeActivity.
 */
object SvipIconGuideBlockHook {
    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!ConfigManager.isFullScreenBackupBlocked) {
            XposedCompat.log("[SvipIconGuideBlockHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val clazz = XposedCompat.findClassOrNull(
                StableBaiduPanHookPoints.SVIP_ICON_GUIDE,
                cl,
            ) ?: run {
                XposedCompat.log("[SvipIconGuideBlockHook] SvipIconGuide class NOT FOUND")
                return
            }

            var installed = 0
            for (method in clazz.declaredMethods) {
                if (method.name != StableBaiduPanHookPoints.SVIP_ICON_GUIDE_SHOW_GUIDE_METHOD) {
                    continue
                }
                method.isAccessible = true
                mod.hook(method).intercept { chain ->
                    if (ConfigManager.isFullScreenBackupBlocked) {
                        HookUtils.getDefaultReturnValue(method.returnType)
                    } else {
                        chain.proceed()
                    }
                }
                installed++
            }

            if (installed == 0) {
                hookState.reset()
                XposedCompat.log("[SvipIconGuideBlockHook] showGuide NOT FOUND")
                return
            }

            XposedCompat.log("[SvipIconGuideBlockHook] hooks INSTALLED: count=$installed")
        } catch (e: ReflectiveOperationException) {
            hookState.reset()
            XposedCompat.log("[SvipIconGuideBlockHook] FAILED (reflection): ${e.javaClass.simpleName}: ${e.message}")
            XposedCompat.log(e)
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[SvipIconGuideBlockHook] FAILED: ${e.message}")
            XposedCompat.log(e)
        }
    }
}
