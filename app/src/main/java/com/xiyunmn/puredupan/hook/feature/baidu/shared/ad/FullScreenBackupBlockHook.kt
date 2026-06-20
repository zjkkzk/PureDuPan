package com.xiyunmn.puredupan.hook.feature.baidu.shared.ad

import android.app.Activity
import android.os.Bundle
import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.symbols.baidu.shared.BaiduDialogHookPoints

/**
 * Closes Baidu Netdisk's full-screen backup guide Activity after the host lifecycle is registered.
 */
internal object FullScreenBackupBlockHook {
    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!HookSettings.isFullScreenBackupBlocked) {
            XposedCompat.log("[FullScreenBackupBlockHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val clazz = XposedCompat.findClassOrNull(
                BaiduDialogHookPoints.NEW_QUICK_SETTINGS_ACTIVITY,
                cl,
            ) ?: run {
                XposedCompat.log("[FullScreenBackupBlockHook] NewQuickSettingsActivity class NOT FOUND")
                return
            }

            val method = XposedCompat.findMethodOrNull(
                clazz,
                BaiduDialogHookPoints.NEW_QUICK_SETTINGS_ACTIVITY_ON_CREATE_METHOD,
                Bundle::class.java,
            ) ?: run {
                XposedCompat.log("[FullScreenBackupBlockHook] NewQuickSettingsActivity.onCreate NOT FOUND")
                return
            }

            mod.hook(method).intercept { chain ->
                val result = chain.proceed()
                if (!HookSettings.isFullScreenBackupBlocked) {
                    return@intercept result
                }

                val activity = chain.thisObject as? Activity
                if (activity != null) {
                    hideBeforeNextFrame(activity)
                    activity.finish()
                    suppressTransition(activity)
                    XposedCompat.logD("[FullScreenBackupBlockHook] NewQuickSettingsActivity closed")
                }
                result
            }

            XposedCompat.log("[FullScreenBackupBlockHook] hook INSTALLED: NewQuickSettingsActivity.onCreate")
        } catch (e: ReflectiveOperationException) {
            hookState.reset()
            XposedCompat.log("[FullScreenBackupBlockHook] FAILED (reflection): ${e.javaClass.simpleName}: ${e.message}")
            XposedCompat.log(e)
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[FullScreenBackupBlockHook] FAILED: ${e.message}")
            XposedCompat.log(e)
        }
    }

    private fun hideBeforeNextFrame(activity: Activity) {
        try {
            activity.window?.decorView?.alpha = 0f
        } catch (t: Throwable) {
            XposedCompat.logD("[FullScreenBackupBlockHook] hide failed: ${t.message}")
        }
    }

    private fun suppressTransition(activity: Activity) {
        try {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(0, 0)
        } catch (_: Throwable) {
            // Optional visual polish only.
        }
    }
}
