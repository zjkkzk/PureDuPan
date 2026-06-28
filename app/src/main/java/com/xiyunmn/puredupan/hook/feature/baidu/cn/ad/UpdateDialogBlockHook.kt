package com.xiyunmn.puredupan.hook.feature.baidu.cn.ad

import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.symbols.baidu.cn.BaiduCnHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.core.HookUtils
import java.lang.reflect.Method

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
            var installed = 0
            installed += hookLegacyVersionUpdateHelper(cl)
            installed += hookCompatVersionUpdateHelper(cl)

            if (installed == 0) {
                hookState.reset()
                XposedCompat.log("[UpdateDialogBlockHook] no hooks installed")
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

    private fun hookLegacyVersionUpdateHelper(cl: ClassLoader): Int {
        val clazz = XposedCompat.findClassOrNull(
            BaiduCnHookPoints.VERSION_UPDATE_HELPER,
            cl,
        ) ?: run {
            XposedCompat.logD("[UpdateDialogBlockHook] VersionUpdateHelper class not found")
            return 0
        }
        return hookMethods(
            clazz.declaredMethods.filter {
                it.name == BaiduCnHookPoints.VERSION_UPDATE_HELPER_SHOW_LC_VERSION_DIALOG_METHOD
            },
            "VersionUpdateHelper.showLCVersionDialog",
        )
    }

    private fun hookCompatVersionUpdateHelper(cl: ClassLoader): Int {
        val clazz = XposedCompat.findClassOrNull(BaiduCnHookPoints.VERSION_UPDATE_HELPER_13_27_8, cl)
            ?: run {
                XposedCompat.logD("[UpdateDialogBlockHook] kotlin.sov0 class not found")
                return 0
            }
        if (!looksLikeVersionUpdateHelper(clazz)) {
            XposedCompat.logD("[UpdateDialogBlockHook] kotlin.sov0 helper identity mismatch")
            return 0
        }
        val methods = clazz.declaredMethods.filter { method ->
            method.returnType == Void.TYPE &&
                method.parameterTypes.size == 4 &&
                method.parameterTypes[0].name == "android.app.Activity" &&
                method.parameterTypes[1].name == BaiduCnHookPoints.UPDATE_INFO &&
                method.parameterTypes[2].name == BaiduCnHookPoints.PRIORITY_DIALOG_INFO &&
                method.parameterTypes[3] == String::class.java
        }
        return hookMethods(methods, "ILCUpdateHelper.showLCVersionDialog")
    }

    private fun looksLikeVersionUpdateHelper(clazz: Class<*>): Boolean {
        val implementsHelper = clazz.interfaces.any {
            it.name == BaiduCnHookPoints.ILC_UPDATE_HELPER
        }
        val hasTag = clazz.declaredFields.any { field ->
            field.type == String::class.java &&
                runCatching {
                    field.isAccessible = true
                    field.get(null) == "VersionUpdateHelper"
                }.getOrDefault(false)
        }
        return implementsHelper && hasTag
    }

    private fun hookMethods(methods: List<Method>, logName: String): Int {
        val mod = XposedCompat.module ?: return 0
        var installed = 0
        for (method in methods) {
            method.isAccessible = true
            mod.hook(method).intercept { chain ->
                if (HookSettings.isUpdateDialogBlocked) {
                    XposedCompat.logD("[UpdateDialogBlockHook] $logName blocked")
                    HookUtils.getDefaultReturnValue(method.returnType)
                } else {
                    chain.proceed()
                }
            }
            installed++
        }
        return installed
    }
}
