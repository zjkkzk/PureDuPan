package com.xiyunmn.puredupan.hook.feature.baidu.cn.performance

import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.symbols.baidu.cn.BaiduCnHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.core.HookState
import java.lang.reflect.Method

/**
 * Blocks only AIGC widget background refresh and startup resource unzip routes.
 */
object AigcBackgroundComponentBlockHook {
    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!isEnabled()) {
            XposedCompat.log("[AigcBackgroundComponentBlockHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val clazz = XposedCompat.findClassOrNull(
                BaiduCnHookPoints.AIGC_CLOUD_CONTEXT_COMPANION,
                cl,
            ) ?: run {
                XposedCompat.log("[AigcBackgroundComponentBlockHook] AigcCloudContext Companion class NOT FOUND")
                hookState.reset()
                return
            }

            val methods = listOfNotNull(
                findNoArgMethod(clazz, BaiduCnHookPoints.AIGC_UPDATE_WIDGET_FROM_CACHE_METHOD),
                findStringArgMethod(clazz, BaiduCnHookPoints.AIGC_UPDATE_WIDGET_BY_DATA_METHOD),
                findNoArgMethod(clazz, BaiduCnHookPoints.AIGC_UNZIP_CLOUD_ZIP_METHOD),
            )

            if (methods.isEmpty()) {
                XposedCompat.log("[AigcBackgroundComponentBlockHook] no AIGC background methods found")
                hookState.reset()
                return
            }

            for (method in methods) {
                mod.hook(method).intercept { chain ->
                    if (isEnabled()) {
                        XposedCompat.logD(
                            "[AigcBackgroundComponentBlockHook] ${method.name} blocked",
                        )
                        null
                    } else {
                        chain.proceed()
                    }
                }
            }

            XposedCompat.log("[AigcBackgroundComponentBlockHook] hooks INSTALLED: count=${methods.size}")
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[AigcBackgroundComponentBlockHook] FAILED: ${e.message}")
            XposedCompat.log(e)
        }
    }

    private fun findNoArgMethod(clazz: Class<*>, methodName: String): Method? {
        return XposedCompat.findMethodOrNull(clazz, methodName).also { method ->
            if (method == null) {
                XposedCompat.log("[AigcBackgroundComponentBlockHook] $methodName NOT FOUND")
            }
        }
    }

    private fun findStringArgMethod(clazz: Class<*>, methodName: String): Method? {
        return XposedCompat.findMethodOrNull(clazz, methodName, String::class.java).also { method ->
            if (method == null) {
                XposedCompat.log("[AigcBackgroundComponentBlockHook] $methodName(String) NOT FOUND")
            }
        }
    }



    private fun isEnabled(): Boolean =
        HookSettings.isPerformanceOptimizeEnabled && HookSettings.isAigcBackgroundComponentDisabled
}
