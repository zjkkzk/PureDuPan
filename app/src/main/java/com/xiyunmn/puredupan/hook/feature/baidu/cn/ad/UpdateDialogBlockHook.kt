package com.xiyunmn.puredupan.hook.feature.baidu.cn.ad

import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.symbols.baidu.cn.BaiduCnHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.core.HookUtils

/**
 * Blocks the in-app software update dialog.
 *
 * The narrow business entry is VersionUpdateHelper.showLCVersionDialog(...). Blocking this
 * avoids touching BaseDialogBuilder, which is shared by many unrelated host dialogs.
 */
object UpdateDialogBlockHook {
    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!HookSettings.isUpdateDialogBlocked) {
            XposedCompat.log("[UpdateDialogBlockHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val clazz = XposedCompat.findClassOrNull(
                BaiduCnHookPoints.VERSION_UPDATE_HELPER,
                cl,
            ) ?: run {
                XposedCompat.log("[UpdateDialogBlockHook] VersionUpdateHelper class NOT FOUND")
                return
            }

            var installed = 0
            for (method in clazz.declaredMethods) {
                if (method.name != BaiduCnHookPoints.VERSION_UPDATE_HELPER_SHOW_LC_VERSION_DIALOG_METHOD) {
                    continue
                }
                method.isAccessible = true
                mod.hook(method).intercept { chain ->
                    if (HookSettings.isUpdateDialogBlocked) {
                        HookUtils.getDefaultReturnValue(method.returnType)
                    } else {
                        chain.proceed()
                    }
                }
                installed++
            }

            if (installed == 0) {
                hookState.reset()
                XposedCompat.log("[UpdateDialogBlockHook] showLCVersionDialog NOT FOUND")
                return
            }

            XposedCompat.log("[UpdateDialogBlockHook] hooks INSTALLED: count=$installed")
        } catch (e: ReflectiveOperationException) {
            hookState.reset()
            XposedCompat.log("[UpdateDialogBlockHook] FAILED (reflection): ${e.javaClass.simpleName}: ${e.message}")
            XposedCompat.log(e)
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[UpdateDialogBlockHook] FAILED: ${e.message}")
            XposedCompat.log(e)
        }
    }
}
