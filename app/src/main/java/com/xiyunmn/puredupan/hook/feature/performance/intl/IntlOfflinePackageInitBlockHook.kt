package com.xiyunmn.puredupan.hook.feature.performance.intl

import com.xiyunmn.puredupan.hook.config.ConfigManager
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.XposedCompat
import java.lang.reflect.Method

internal object IntlOfflinePackageInitBlockHook {
    private const val DYNAMIC_CONTEXT_CLASS_NAME =
        "rubik.generate.context.bd_netdisk_com_baidu_netdisk_dynamic.DynamicContext"
    private const val FAST_WEB_VIEW_CLIENT_CLASS_NAME = "com.baidu.netdisk.webview.FastWebViewClient"
    private const val OFFLINE_H5_PACKAGE_ACTIVITY_CLASS_NAME =
        "com.baidu.netdisk.ui.webview.OfflineH5PackageActivity"
    private const val START_SYNC_METHOD_NAME = "startSyncOfflinePackages"

    private val hookState = HookState()
    private val lock = Any()

    @Volatile private var startSyncMethod: Method? = null
    @Volatile private var skipped = false
    @Volatile private var restored = false
    @Volatile private var restoring = false
    @Volatile private var skipCount = 0
    @Volatile private var restoreCount = 0

    internal fun hook(cl: ClassLoader) {
        if (!isEnabled()) {
            XposedCompat.log("[IntlOfflinePackageInitBlockHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val syncMethod = resolveStartSyncMethod(cl)
            if (syncMethod == null) {
                hookState.reset()
                XposedCompat.log("[IntlOfflinePackageInitBlockHook] start sync method NOT FOUND")
                return
            }
            startSyncMethod = syncMethod

            mod.hook(syncMethod).intercept { chain ->
                if (!shouldBlockStartupInit()) {
                    return@intercept chain.proceed()
                }

                synchronized(lock) {
                    skipped = true
                    skipCount++
                }
                XposedCompat.log(
                    "[IntlOfflinePackageInitBlockHook] blocked startup H5 offline package init: " +
                        "${syncMethod.declaringClass.name}.${syncMethod.name}, skipCount=$skipCount",
                )
                null
            }

            val h5HookCount = hookH5RestoreSignals(cl)
            XposedCompat.log(
                "[IntlOfflinePackageInitBlockHook] hooks INSTALLED: " +
                    "sync=${syncMethod.declaringClass.name}.${syncMethod.name}, h5Signals=$h5HookCount",
            )
        } catch (t: Throwable) {
            hookState.reset()
            XposedCompat.log("[IntlOfflinePackageInitBlockHook] install FAILED: ${t.message}")
            XposedCompat.log(t)
        }
    }

    private fun resolveStartSyncMethod(cl: ClassLoader): Method? {
        // Rubik generated context keeps a semantic class and method name; the provider
        // implementation behind it is obfuscated and should not be hooked directly.
        val contextClass = XposedCompat.findClassOrNull(DYNAMIC_CONTEXT_CLASS_NAME, cl)
        return contextClass?.let {
            XposedCompat.findMethodOrNull(it, START_SYNC_METHOD_NAME)
        }
    }

    private fun shouldBlockStartupInit(): Boolean {
        if (!isEnabled()) return false
        if (restoring || restored) return false
        return true
    }

    private fun hookH5RestoreSignals(cl: ClassLoader): Int {
        val mod = XposedCompat.module ?: return 0
        var installed = 0

        XposedCompat.findClassOrNull(FAST_WEB_VIEW_CLIENT_CLASS_NAME, cl)?.let { fastClientClass ->
            for (constructor in fastClientClass.declaredConstructors) {
                if (constructor.parameterTypes.size < 3) continue
                constructor.isAccessible = true
                mod.hook(constructor).intercept { chain ->
                    restoreForH5Entry("fast_webview_client:${constructor.parameterTypes.joinToString { it.simpleName }}")
                    chain.proceed()
                }
                installed++
            }
        } ?: XposedCompat.log("[IntlOfflinePackageInitBlockHook] FastWebViewClient class NOT FOUND")

        XposedCompat.findClassOrNull(OFFLINE_H5_PACKAGE_ACTIVITY_CLASS_NAME, cl)?.let { activityClass ->
            listOf("initFragment", "onResume").forEach { methodName ->
                XposedCompat.findMethodOrNull(activityClass, methodName)?.let { method ->
                    mod.hook(method).intercept { chain ->
                        restoreForH5Entry("offline_h5_activity:$methodName")
                        chain.proceed()
                    }
                    installed++
                }
            }
        } ?: XposedCompat.log("[IntlOfflinePackageInitBlockHook] OfflineH5PackageActivity class NOT FOUND")

        return installed
    }

    private fun restoreForH5Entry(reason: String) {
        if (!isEnabled()) return
        val method = startSyncMethod ?: run {
            XposedCompat.logW("[IntlOfflinePackageInitBlockHook] restore skipped: startSyncMethod missing, reason=$reason")
            return
        }

        val shouldInvoke = synchronized(lock) {
            if (restored) return
            restored = true
            if (!skipped) {
                XposedCompat.log(
                    "[IntlOfflinePackageInitBlockHook] H5 entry reached before startup block: " +
                        "future init allowed, reason=$reason",
                )
                return
            }
            restoreCount++
            true
        }
        if (!shouldInvoke) return

        try {
            restoring = true
            method.invoke(null)
            XposedCompat.log(
                "[IntlOfflinePackageInitBlockHook] restored H5 offline package init: " +
                    "reason=$reason, restoreCount=$restoreCount",
            )
        } catch (t: Throwable) {
            synchronized(lock) {
                restored = false
                restoreCount--
            }
            XposedCompat.logW(
                "[IntlOfflinePackageInitBlockHook] restore FAILED: reason=$reason, msg=${t.message}",
            )
            XposedCompat.log(t)
        } finally {
            restoring = false
        }
    }

    private fun isEnabled(): Boolean =
        ConfigManager.isPerformanceOptimizeEnabled && ConfigManager.isIntlOfflinePackageInitBlocked
}
