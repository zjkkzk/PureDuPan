package com.xiyunmn.puredupan.hook.feature.performance.intl

import com.xiyunmn.puredupan.hook.config.ConfigManager
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.XposedCompat

internal object IntlAigcWidgetBackgroundBlockHook {
    private const val AIGC_CLOUD_CONTEXT_CLASS_NAME =
        "rubik.generate.context.bd_netdisk_com_baidu_netdisk_aigc_cloud.AigcCloudContext"
    private const val AIGC_CLOUD_COMPANION_CLASS_NAME =
        "rubik.generate.context.bd_netdisk_com_baidu_netdisk_aigc_cloud.AigcCloudContext\$Companion"
    private const val AIGC_CLOUD_AGGREGATE_CLASS_NAME =
        "rubik.generate.aggregate.bd_netdisk_com_baidu_netdisk_aigc_cloud.AigcCloudAggregate"

    private val backgroundMethodNames = setOf(
        "updateAigcWidgetFromCache",
        "updateAigcWidgetByData",
        "unzipAigcCloudZip",
    )

    private val hookState = HookState()
    private val lock = Any()

    @Volatile private var blockedCount = 0

    internal fun hook(cl: ClassLoader) {
        if (!isEnabled()) {
            XposedCompat.log("[IntlAigcWidgetBackgroundBlockHook] skipped: config disabled")
            return
        }
        if (!hookState.markInstalled()) return

        try {
            val contextHooks = hookNamedBackgroundMethods(AIGC_CLOUD_CONTEXT_CLASS_NAME, cl)
            val companionHooks = hookNamedBackgroundMethods(AIGC_CLOUD_COMPANION_CLASS_NAME, cl)
            val aggregateHooks = hookNamedBackgroundMethods(AIGC_CLOUD_AGGREGATE_CLASS_NAME, cl)
            if (contextHooks + companionHooks + aggregateHooks == 0) {
                hookState.reset()
                XposedCompat.log("[IntlAigcWidgetBackgroundBlockHook] background methods NOT FOUND")
                return
            }

            XposedCompat.log(
                "[IntlAigcWidgetBackgroundBlockHook] hooks INSTALLED: " +
                    "context=$contextHooks, companion=$companionHooks, aggregate=$aggregateHooks",
            )
        } catch (t: Throwable) {
            hookState.reset()
            XposedCompat.log("[IntlAigcWidgetBackgroundBlockHook] install FAILED: ${t.message}")
            XposedCompat.log(t)
        }
    }

    private fun hookNamedBackgroundMethods(className: String, cl: ClassLoader): Int {
        val mod = XposedCompat.module ?: return 0
        val clazz = XposedCompat.findClassOrNull(className, cl) ?: run {
            XposedCompat.logD("[IntlAigcWidgetBackgroundBlockHook] class NOT FOUND: $className")
            return 0
        }

        var installed = 0
        for (method in clazz.declaredMethods) {
            if (method.name !in backgroundMethodNames) continue
            method.isAccessible = true
            mod.hook(method).intercept {
                if (!isEnabled()) {
                    return@intercept it.proceed()
                }

                synchronized(lock) {
                    blockedCount++
                }
                XposedCompat.log(
                    "[IntlAigcWidgetBackgroundBlockHook] blocked AIGC widget background work: " +
                        "${method.declaringClass.name}.${method.name}/${method.parameterTypes.size}, " +
                        "blockedCount=$blockedCount",
                )
                null
            }
            installed++
        }
        return installed
    }

    private fun isEnabled(): Boolean =
        ConfigManager.isPerformanceOptimizeEnabled && ConfigManager.isIntlAigcWidgetBackgroundBlocked
}
