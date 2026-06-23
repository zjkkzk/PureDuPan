package com.xiyunmn.puredupan.hook.feature.baidu.shared.ad

import android.content.Context
import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.HookUtils
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.symbols.baidu.shared.BaiduTransferHookPoints
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

internal object NonWifiDownloadDialogBlockHook {
    private val hookState = HookState()

    private data class RestartSchedulersInvoker(
        val target: Any,
        val method: Method,
    )

    internal fun hook(cl: ClassLoader) {
        if (!HookSettings.isNonWifiDownloadDialogBlocked) {
            XposedCompat.log("[NonWifiDownloadDialogBlockHook] skipped: config disabled")
            return
        }
        if (!hookState.markInstalled()) return

        try {
            val listenerClass = XposedCompat.findClassOrNull(
                BaiduTransferHookPoints.DIALOG_CTR_LISTENER,
                cl,
            ) ?: run {
                hookState.reset()
                XposedCompat.log("[NonWifiDownloadDialogBlockHook] DialogCtrListener class NOT FOUND")
                return
            }
            val wifiOnlyConfigMethod = findSetWifiOnlyConfigMethod(cl)
            val restartSchedulersInvoker = findRestartSchedulersInvoker(cl)

            var installed = 0
            installed += hookDialogMethods(
                cl = cl,
                listenerClass = listenerClass,
                wifiOnlyConfigMethod = wifiOnlyConfigMethod,
                restartSchedulersInvoker = restartSchedulersInvoker,
                className = BaiduTransferHookPoints.TRANSFER_CONTEXT_COMPANION,
                tagPrefix = "TransferContext.Companion",
            )
            installed += hookDialogMethods(
                cl = cl,
                listenerClass = listenerClass,
                wifiOnlyConfigMethod = wifiOnlyConfigMethod,
                restartSchedulersInvoker = restartSchedulersInvoker,
                className = BaiduTransferHookPoints.TRANSFER_APIS,
                tagPrefix = "TransferApis",
            )
            installed += hookDialogMethods(
                cl = cl,
                listenerClass = listenerClass,
                wifiOnlyConfigMethod = wifiOnlyConfigMethod,
                restartSchedulersInvoker = restartSchedulersInvoker,
                className = BaiduTransferHookPoints.FLOW_ALERT_DIALOG_MANAGER,
                tagPrefix = "FlowAlertDialogManager",
            )

            if (installed == 0) {
                hookState.reset()
                XposedCompat.log("[NonWifiDownloadDialogBlockHook] target methods NOT FOUND")
                return
            }

            XposedCompat.log("[NonWifiDownloadDialogBlockHook] hooks INSTALLED: count=$installed")
        } catch (e: ReflectiveOperationException) {
            hookState.reset()
            XposedCompat.log(
                "[NonWifiDownloadDialogBlockHook] FAILED (reflection): " +
                    "${e.javaClass.simpleName}: ${e.message}",
            )
            XposedCompat.log(e)
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[NonWifiDownloadDialogBlockHook] FAILED: ${e.message}")
            XposedCompat.log(e)
        }
    }

    private fun hookDialogMethods(
        cl: ClassLoader,
        listenerClass: Class<*>,
        wifiOnlyConfigMethod: Method?,
        restartSchedulersInvoker: RestartSchedulersInvoker?,
        className: String,
        tagPrefix: String,
    ): Int {
        var installed = 0
        installed += hookDialogMethod(
            cl = cl,
            listenerClass = listenerClass,
            wifiOnlyConfigMethod = wifiOnlyConfigMethod,
            restartSchedulersInvoker = restartSchedulersInvoker,
            className = className,
            methodName = BaiduTransferHookPoints.SHOW_NON_WIFI_ALERT_DOWNLOAD_DIALOG_METHOD,
            tag = "$tagPrefix.showNonWiFiAlertDownloadDialog",
        )
        installed += hookDialogMethod(
            cl = cl,
            listenerClass = listenerClass,
            wifiOnlyConfigMethod = wifiOnlyConfigMethod,
            restartSchedulersInvoker = restartSchedulersInvoker,
            className = className,
            methodName = BaiduTransferHookPoints.SHOW_NON_WIFI_ALERT_DOWNLOAD_BOTTOM_DIALOG_METHOD,
            tag = "$tagPrefix.showNonWiFiAlertDownloadBottomDialog",
        )
        return installed
    }

    private fun hookDialogMethod(
        cl: ClassLoader,
        listenerClass: Class<*>,
        wifiOnlyConfigMethod: Method?,
        restartSchedulersInvoker: RestartSchedulersInvoker?,
        className: String,
        methodName: String,
        tag: String,
    ): Int {
        val clazz = XposedCompat.findClassOrNull(className, cl) ?: run {
            XposedCompat.logD("[NonWifiDownloadDialogBlockHook] class not found: $className")
            return 0
        }
        val method = XposedCompat.findMethodOrNull(clazz, methodName, listenerClass) ?: run {
            XposedCompat.logD("[NonWifiDownloadDialogBlockHook] method not found: $tag")
            return 0
        }
        val mod = XposedCompat.module ?: return 0
        method.isAccessible = true
        mod.hook(method).intercept { chain ->
            if (!HookSettings.isNonWifiDownloadDialogBlocked) {
                return@intercept chain.proceed()
            }

            val listener = chain.args.firstOrNull()
            if (confirmDownload(listener, wifiOnlyConfigMethod, restartSchedulersInvoker, chain.thisObject, tag)) {
                HookUtils.getDefaultReturnValue(method.returnType)
            } else {
                chain.proceed()
            }
        }
        return 1
    }

    private fun confirmDownload(
        listener: Any?,
        wifiOnlyConfigMethod: Method?,
        restartSchedulersInvoker: RestartSchedulersInvoker?,
        managerObject: Any?,
        tag: String,
    ): Boolean {
        if (listener == null) {
            allowMobileDataDownload(wifiOnlyConfigMethod, tag)
            restartTransferSchedulers(restartSchedulersInvoker, managerObject, null, tag)
            XposedCompat.logD("[NonWifiDownloadDialogBlockHook] $tag skipped with null listener")
            return true
        }

        val onOkMethod = findNoArgMethodInHierarchy(
            listener.javaClass,
            BaiduTransferHookPoints.DIALOG_CTR_LISTENER_ON_OK_METHOD,
        ) ?: run {
            XposedCompat.logW("[NonWifiDownloadDialogBlockHook] onOkBtnClick not found for $tag")
            return false
        }

        return try {
            allowMobileDataDownload(wifiOnlyConfigMethod, tag)
            onOkMethod.invoke(listener)
            restartTransferSchedulers(restartSchedulersInvoker, managerObject, listener, tag)
            XposedCompat.logD("[NonWifiDownloadDialogBlockHook] $tag confirmed without dialog")
            true
        } catch (e: InvocationTargetException) {
            restartTransferSchedulers(restartSchedulersInvoker, managerObject, listener, tag)
            XposedCompat.logE(
                "[NonWifiDownloadDialogBlockHook] onOkBtnClick threw in $tag: " +
                    "${e.targetException?.javaClass?.simpleName}: ${e.targetException?.message}",
            )
            XposedCompat.log(e.targetException ?: e)
            true
        } catch (e: ReflectiveOperationException) {
            XposedCompat.logW(
                "[NonWifiDownloadDialogBlockHook] onOkBtnClick invoke failed in $tag: " +
                    "${e.javaClass.simpleName}: ${e.message}",
            )
            false
        }
    }

    private fun findSetWifiOnlyConfigMethod(cl: ClassLoader): Method? {
        val clazz = XposedCompat.findClassOrNull(BaiduTransferHookPoints.NET_CONFIG_UTIL, cl)
            ?: run {
                XposedCompat.logD("[NonWifiDownloadDialogBlockHook] NetConfigUtil class not found")
                return null
            }
        return XposedCompat.findMethodOrNull(
            clazz,
            BaiduTransferHookPoints.SET_WIFI_ONLY_CHECKED_CONFIG_METHOD,
            Boolean::class.javaPrimitiveType!!,
        )?.apply { isAccessible = true } ?: run {
            XposedCompat.logW("[NonWifiDownloadDialogBlockHook] setWiFiOnlyCheckedConfig not found")
            null
        }
    }

    private fun findRestartSchedulersInvoker(cl: ClassLoader): RestartSchedulersInvoker? {
        val clazz = XposedCompat.findClassOrNull(BaiduTransferHookPoints.MAIN_CREATE_OBJECT_API, cl)
            ?: run {
                XposedCompat.logD("[NonWifiDownloadDialogBlockHook] MCreateObjectApi class not found")
                return null
            }
        val method = XposedCompat.findMethodOrNull(
            clazz,
            BaiduTransferHookPoints.RESTART_SCHEDULERS_METHOD,
            Context::class.java,
        )?.apply { isAccessible = true } ?: run {
            XposedCompat.logW("[NonWifiDownloadDialogBlockHook] restartSchedulers(Context) not found")
            return null
        }

        return try {
            val constructor = clazz.getDeclaredConstructor().apply { isAccessible = true }
            RestartSchedulersInvoker(constructor.newInstance(), method)
        } catch (e: ReflectiveOperationException) {
            XposedCompat.logW(
                "[NonWifiDownloadDialogBlockHook] create MCreateObjectApi failed: " +
                    "${e.javaClass.simpleName}: ${e.message}",
            )
            null
        }
    }

    private fun allowMobileDataDownload(method: Method?, tag: String) {
        if (method == null) return
        try {
            method.invoke(null, false)
            XposedCompat.logD("[NonWifiDownloadDialogBlockHook] $tag disabled wifi-only before confirm")
        } catch (e: InvocationTargetException) {
            XposedCompat.logW(
                "[NonWifiDownloadDialogBlockHook] setWiFiOnlyCheckedConfig threw in $tag: " +
                    "${e.targetException?.javaClass?.simpleName}: ${e.targetException?.message}",
            )
        } catch (e: ReflectiveOperationException) {
            XposedCompat.logW(
                "[NonWifiDownloadDialogBlockHook] setWiFiOnlyCheckedConfig failed in $tag: " +
                    "${e.javaClass.simpleName}: ${e.message}",
            )
        }
    }

    private fun restartTransferSchedulers(
        invoker: RestartSchedulersInvoker?,
        managerObject: Any?,
        listener: Any?,
        tag: String,
    ): Boolean {
        if (restartSchedulersByProvider(invoker, managerObject, listener, tag)) return true
        if (restartSchedulersByManager(managerObject, tag)) return true

        XposedCompat.logW("[NonWifiDownloadDialogBlockHook] restart schedulers unavailable for $tag")
        return false
    }

    private fun restartSchedulersByProvider(
        invoker: RestartSchedulersInvoker?,
        managerObject: Any?,
        listener: Any?,
        tag: String,
    ): Boolean {
        if (invoker == null) return false
        val context = resolveHostContext(managerObject, listener) ?: run {
            XposedCompat.logW("[NonWifiDownloadDialogBlockHook] host context unavailable for $tag")
            return false
        }
        return try {
            invoker.method.invoke(invoker.target, context)
            XposedCompat.logD("[NonWifiDownloadDialogBlockHook] $tag restarted transfer schedulers")
            true
        } catch (e: InvocationTargetException) {
            XposedCompat.logW(
                "[NonWifiDownloadDialogBlockHook] restartSchedulers threw in $tag: " +
                    "${e.targetException?.javaClass?.simpleName}: ${e.targetException?.message}",
            )
            false
        } catch (e: ReflectiveOperationException) {
            XposedCompat.logW(
                "[NonWifiDownloadDialogBlockHook] restartSchedulers failed in $tag: " +
                    "${e.javaClass.simpleName}: ${e.message}",
            )
            false
        }
    }

    private fun restartSchedulersByManager(managerObject: Any?, tag: String): Boolean {
        if (managerObject == null) return false
        val method = findNoArgMethodInHierarchy(
            managerObject.javaClass,
            BaiduTransferHookPoints.RESTART_SCHEDULERS_METHOD,
        ) ?: return false

        return try {
            method.invoke(managerObject)
            XposedCompat.logD("[NonWifiDownloadDialogBlockHook] $tag restarted schedulers via manager")
            true
        } catch (e: InvocationTargetException) {
            XposedCompat.logW(
                "[NonWifiDownloadDialogBlockHook] manager restart threw in $tag: " +
                    "${e.targetException?.javaClass?.simpleName}: ${e.targetException?.message}",
            )
            false
        } catch (e: ReflectiveOperationException) {
            XposedCompat.logW(
                "[NonWifiDownloadDialogBlockHook] manager restart failed in $tag: " +
                    "${e.javaClass.simpleName}: ${e.message}",
            )
            false
        }
    }

    private fun resolveHostContext(managerObject: Any?, listener: Any?): Context? {
        currentApplicationContext()?.let { return it }
        findContextField(managerObject)?.let { return it }
        return findContextField(listener)
    }

    private fun currentApplicationContext(): Context? {
        return runCatching {
            val activityThread = Class.forName("android.app.ActivityThread")
            val method = activityThread.getDeclaredMethod("currentApplication").apply { isAccessible = true }
            val context = method.invoke(null) as? Context
            context?.applicationContext ?: context
        }.getOrNull()
    }

    private fun findContextField(instance: Any?): Context? {
        if (instance == null) return null

        var current: Class<*>? = instance.javaClass
        while (current != null) {
            for (field in current.declaredFields) {
                if (!Context::class.java.isAssignableFrom(field.type)) continue
                val context = runCatching {
                    field.isAccessible = true
                    field.get(instance) as? Context
                }.getOrNull()
                if (context != null) {
                    return context.applicationContext ?: context
                }
            }
            current = current.superclass
        }
        return null
    }

    private fun findNoArgMethodInHierarchy(clazz: Class<*>, name: String): Method? {
        var current: Class<*>? = clazz
        while (current != null) {
            try {
                return current.getDeclaredMethod(name).apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
                current = current.superclass
            }
        }
        return null
    }
}
