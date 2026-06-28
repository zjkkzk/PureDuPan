package com.xiyunmn.puredupan.hook.feature.baidu.cn.ui

import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.symbols.baidu.cn.BaiduCnHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.feature.baidu.shared.resolver.KotlinMetadataUtils
import java.lang.reflect.Method
import java.lang.reflect.Modifier

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
            val method = resolveAiCloudTabModeMethod(cl) ?: run {
                XposedCompat.log("[BottomAiTabReplaceHook] getAiCloudTabMode equivalent NOT FOUND")
                hookState.reset()
                return
            }
            mod.hook(method).intercept {
                if (isEnabled()) 0L else it.proceed()
            }
            XposedCompat.log(
                "[BottomAiTabReplaceHook] hook INSTALLED: " +
                    "${method.declaringClass.name}.${method.name}",
            )
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[BottomAiTabReplaceHook] FAILED: ${e.message}")
        }
    }

    private fun resolveAiCloudTabModeMethod(cl: ClassLoader): Method? {
        XposedCompat.findClassOrNull(BaiduCnHookPoints.AI_CLOUD_TAB_AMIS_KT, cl)
            ?.let { clazz ->
                XposedCompat.findMethodOrNull(clazz, "getAiCloudTabMode")?.let { return it }
            }

        val clazz = XposedCompat.findClassOrNull(BaiduCnHookPoints.AI_CLOUD_TAB_AMIS_KT_13_27_8, cl)
            ?: return null
        if (!KotlinMetadataUtils.metadataContainsAll(
                clazz,
                listOf("AI_CLOUD_TAB_NODE", "getAiCloudTabMode", "aiCloudTabMode"),
            )
        ) {
            XposedCompat.logD("[BottomAiTabReplaceHook] kotlin.cd1 metadata mismatch")
            return null
        }
        return clazz.declaredMethods.firstOrNull { method ->
            Modifier.isStatic(method.modifiers) &&
                method.parameterTypes.isEmpty() &&
                method.returnType == Long::class.javaPrimitiveType
        }?.apply { isAccessible = true }
    }

    private fun isEnabled(): Boolean =
        HookSettings.isBottomBarCustomEnabled && HookSettings.isBottomAiReplaced
}
