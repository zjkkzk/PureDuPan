package com.xiyunmn.puredupan.hook.feature.baidu.samsung.ad

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.HookUtils
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.symbols.baidu.samsung.BaiduSamsungHookPoints
import java.lang.reflect.Method

internal object SamsungNotificationPromptBlockHook {
    private const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"
    private const val ACTIVITY_COMPAT = "androidx.core.app.ActivityCompat"
    private const val ANDROIDX_FRAGMENT = "androidx.fragment.app.Fragment"

    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!HookSettings.isNotificationPromptBlocked) {
            XposedCompat.log("[SamsungNotificationPromptBlockHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            var installed = 0
            installed += hookSystemPermissionRequests(cl)
            installed += hookHostPermissionRequests(cl)
            installed += hookPushGuideDialog(cl)

            if (installed == 0) {
                hookState.reset()
                XposedCompat.log("[SamsungNotificationPromptBlockHook] no hooks installed")
                return
            }

            XposedCompat.log("[SamsungNotificationPromptBlockHook] hooks INSTALLED: count=$installed")
        } catch (t: Throwable) {
            hookState.reset()
            XposedCompat.log("[SamsungNotificationPromptBlockHook] FAILED: ${t.message}")
            XposedCompat.log(t)
        }
    }

    private fun hookSystemPermissionRequests(cl: ClassLoader): Int {
        var installed = 0
        val activityRequestPermissions = Activity::class.java.getDeclaredMethod(
            "requestPermissions",
            Array<String>::class.java,
            Int::class.javaPrimitiveType!!,
        ).apply { isAccessible = true }
        installed += hookPermissionArrayMethod(
            method = activityRequestPermissions,
            permissionsArgIndex = 0,
            tag = "Activity.requestPermissions",
        )

        val activityCompatClass = XposedCompat.findClassOrNull(ACTIVITY_COMPAT, cl)
        if (activityCompatClass != null) {
            XposedCompat.findMethodOrNull(
                activityCompatClass,
                "requestPermissions",
                Activity::class.java,
                Array<String>::class.java,
                Int::class.javaPrimitiveType!!,
            )?.let { method ->
                installed += hookPermissionArrayMethod(
                    method = method,
                    permissionsArgIndex = 1,
                    tag = "ActivityCompat.requestPermissions",
                )
            }
        } else {
            XposedCompat.logD("[SamsungNotificationPromptBlockHook] ActivityCompat class not found")
        }

        val fragmentClass = XposedCompat.findClassOrNull(ANDROIDX_FRAGMENT, cl)
        if (fragmentClass != null) {
            XposedCompat.findMethodOrNull(
                fragmentClass,
                "requestPermissions",
                Array<String>::class.java,
                Int::class.javaPrimitiveType!!,
            )?.let { method ->
                installed += hookPermissionArrayMethod(
                    method = method,
                    permissionsArgIndex = 0,
                    tag = "Fragment.requestPermissions",
                )
            }
        } else {
            XposedCompat.logD("[SamsungNotificationPromptBlockHook] Fragment class not found")
        }
        return installed
    }

    private fun hookHostPermissionRequests(cl: ClassLoader): Int {
        var installed = 0
        val presenterClass = XposedCompat.findClassOrNull(
            BaiduSamsungHookPoints.PERMISSION_PRESENTER,
            cl,
        )
        if (presenterClass != null) {
            for (method in presenterClass.declaredMethods) {
                if (method.parameterTypes.firstOrNull() == Array<String>::class.java &&
                    method.name.startsWith("requestPermission")
                ) {
                    installed += hookPermissionArrayMethod(
                        method = method,
                        permissionsArgIndex = 0,
                        tag = "PermissionPresenter.${method.name}",
                    )
                }
            }
        } else {
            XposedCompat.logD("[SamsungNotificationPromptBlockHook] PermissionPresenter class not found")
        }

        val activityClass = XposedCompat.findClassOrNull(
            BaiduSamsungHookPoints.PERMISSION_DIALOG_ACTIVITY,
            cl,
        )
        if (activityClass != null) {
            XposedCompat.findMethodOrNull(
                activityClass,
                BaiduSamsungHookPoints.PERMISSION_DIALOG_ACTIVITY_ON_CREATE_METHOD,
                Bundle::class.java,
            )?.let { method ->
                method.isAccessible = true
                XposedCompat.module?.hook(method)?.intercept { chain ->
                    val activity = chain.thisObject as? Activity
                        ?: return@intercept chain.proceed()
                    val permissions = activity?.intent?.getStringArrayExtra(
                        BaiduSamsungHookPoints.PERMISSION_ARRAY_EXTRA,
                    )
                    val filter = filterNotificationPermission(permissions)
                    when {
                        !HookSettings.isNotificationPromptBlocked || filter == null -> chain.proceed()
                        filter.permissions.isEmpty() -> {
                            activity.finish()
                            XposedCompat.logD(
                                "[SamsungNotificationPromptBlockHook] blocked PermissionDialogActivity",
                            )
                            HookUtils.getDefaultReturnValue(method.returnType)
                        }
                        filter.removed -> {
                            activity.intent?.putExtra(
                                BaiduSamsungHookPoints.PERMISSION_ARRAY_EXTRA,
                                filter.permissions,
                            )
                            XposedCompat.logD(
                                "[SamsungNotificationPromptBlockHook] filtered PermissionDialogActivity",
                            )
                            chain.proceed()
                        }
                        else -> chain.proceed()
                    }
                }
                installed++
            }
        } else {
            XposedCompat.logD("[SamsungNotificationPromptBlockHook] PermissionDialogActivity class not found")
        }
        return installed
    }

    private fun hookPushGuideDialog(cl: ClassLoader): Int {
        val targetClass = XposedCompat.findClassOrNull(
            BaiduSamsungHookPoints.PUSH_GUIDE_NORMAL_DIALOG,
            cl,
        ) ?: run {
            XposedCompat.log("[SamsungNotificationPromptBlockHook] PushGuideNormalDialog class NOT FOUND")
            return 0
        }

        var installed = 0
        XposedCompat.findMethodOrNull(
            targetClass,
            BaiduSamsungHookPoints.PUSH_GUIDE_ON_CREATE_DIALOG_METHOD,
            Bundle::class.java,
        )?.let { method ->
            method.isAccessible = true
            XposedCompat.module?.hook(method)?.intercept { chain ->
                if (!HookSettings.isNotificationPromptBlocked) {
                    return@intercept chain.proceed()
                }
                createDismissedDialog(chain.thisObject) ?: chain.proceed()
            }
            installed++
        }

        for (methodName in listOf(
            BaiduSamsungHookPoints.PUSH_GUIDE_ON_START_METHOD,
            BaiduSamsungHookPoints.PUSH_GUIDE_ON_RESUME_METHOD,
        )) {
            findNoArgMethodInHierarchy(targetClass, methodName)?.let { method ->
                XposedCompat.module?.hook(method)?.intercept { chain ->
                    val result = chain.proceed()
                    if (HookSettings.isNotificationPromptBlocked) {
                        dismissDialogFragment(chain.thisObject)
                    }
                    result
                }
                installed++
            }
        }

        if (installed == 0) {
            XposedCompat.log("[SamsungNotificationPromptBlockHook] PushGuideNormalDialog methods NOT FOUND")
        }
        return installed
    }

    private fun hookPermissionArrayMethod(
        method: Method,
        permissionsArgIndex: Int,
        tag: String,
    ): Int {
        method.isAccessible = true
        val mod = XposedCompat.module ?: return 0
        mod.hook(method).intercept { chain ->
            val filter = filterNotificationPermission(chain.args.getOrNull(permissionsArgIndex) as? Array<*>)
            when {
                !HookSettings.isNotificationPromptBlocked || filter == null -> chain.proceed()
                filter.permissions.isEmpty() -> {
                    XposedCompat.logD("[SamsungNotificationPromptBlockHook] blocked $tag")
                    HookUtils.getDefaultReturnValue(method.returnType)
                }
                filter.removed -> {
                    val args = chain.args.toMutableList().toTypedArray()
                    args[permissionsArgIndex] = filter.permissions
                    XposedCompat.logD("[SamsungNotificationPromptBlockHook] filtered $tag")
                    chain.proceed(args)
                }
                else -> chain.proceed()
            }
        }
        return 1
    }

    private fun filterNotificationPermission(permissions: Array<*>?): PermissionFilter? {
        if (permissions == null) return null
        val original = permissions.filterIsInstance<String>()
        if (original.none { it == POST_NOTIFICATIONS_PERMISSION }) return null
        val filtered = original.filterNot { it == POST_NOTIFICATIONS_PERMISSION }.toTypedArray()
        return PermissionFilter(
            permissions = filtered,
            removed = filtered.size != original.size,
        )
    }

    private fun createDismissedDialog(fragment: Any?): Dialog? {
        val context = findContext(fragment) ?: return null
        return Dialog(context).apply {
            setOnShowListener { dialog -> dialog.dismiss() }
        }
    }

    private fun findContext(fragment: Any?): Context? {
        if (fragment == null) return null
        return try {
            findNoArgMethodInHierarchy(fragment.javaClass, "getActivity")?.invoke(fragment) as? Context
                ?: findNoArgMethodInHierarchy(fragment.javaClass, "getContext")?.invoke(fragment) as? Context
        } catch (t: Throwable) {
            XposedCompat.logD("[SamsungNotificationPromptBlockHook] context lookup failed: ${t.message}")
            null
        }
    }

    private fun dismissDialogFragment(fragment: Any?) {
        if (fragment == null) return
        try {
            findNoArgMethodInHierarchy(fragment.javaClass, "dismissAllowingStateLoss")
                ?.invoke(fragment)
                ?: findNoArgMethodInHierarchy(fragment.javaClass, "dismiss")?.invoke(fragment)
            XposedCompat.logD("[SamsungNotificationPromptBlockHook] PushGuideNormalDialog dismissed")
        } catch (t: Throwable) {
            XposedCompat.logD("[SamsungNotificationPromptBlockHook] dismiss ignored: ${t.message}")
        }
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

    private data class PermissionFilter(
        val permissions: Array<String>,
        val removed: Boolean,
    )
}
