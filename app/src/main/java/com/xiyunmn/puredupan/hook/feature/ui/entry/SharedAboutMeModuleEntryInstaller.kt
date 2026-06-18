package com.xiyunmn.puredupan.hook.feature.ui.entry

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.StableBaiduPanHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat

internal object SharedAboutMeModuleEntryInstaller {
    private const val SCAN_ICON_ID_NAME = "self_qrcode_scan_icon"

    private val hookStates = linkedMapOf<String, HookState>()

    fun hook(cl: ClassLoader, tag: String) {
        val mod = XposedCompat.module ?: return
        val hookState = hookStates.getOrPut(tag) { HookState() }
        if (!hookState.markInstalled()) return

        try {
            val activityClass = XposedCompat.findClassOrNull(
                StableBaiduPanHookPoints.ABOUT_ME_ACTIVITY,
                cl,
            ) ?: run {
                hookState.reset()
                XposedCompat.log("[$tag] AboutMeActivity class NOT FOUND")
                return
            }

            val onCreateMethod = XposedCompat.findMethodOrNull(
                activityClass,
                "onCreate",
                Bundle::class.java,
            ) ?: run {
                hookState.reset()
                XposedCompat.log("[$tag] AboutMeActivity.onCreate NOT FOUND")
                return
            }

            mod.hook(onCreateMethod).intercept { chain ->
                val result = chain.proceed()
                bindScanIconLongPress(chain.thisObject as? Activity, tag)
                result
            }
            XposedCompat.log("[$tag] hook INSTALLED: ${StableBaiduPanHookPoints.ABOUT_ME_ACTIVITY}.onCreate")
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[$tag] install FAILED: ${e.message}")
            XposedCompat.log(e)
        }
    }

    private fun bindScanIconLongPress(activity: Activity?, tag: String) {
        if (activity == null) return
        val resId = activity.resources.getIdentifier(
            SCAN_ICON_ID_NAME,
            "id",
            activity.packageName,
        )
        if (resId == 0) {
            XposedCompat.logD("[$tag] scan icon resId not found: $SCAN_ICON_ID_NAME")
            return
        }

        val scanIconView = activity.findViewById<View>(resId)
        if (scanIconView == null) {
            XposedCompat.logD("[$tag] scan icon view not found in hierarchy (resId=$resId)")
            return
        }

        ModuleEntryBindingSupport.bindLongPressToSettings(
            view = scanIconView,
            classLoader = activity.classLoader,
            tag = tag,
            entryName = SCAN_ICON_ID_NAME,
        )
    }
}
