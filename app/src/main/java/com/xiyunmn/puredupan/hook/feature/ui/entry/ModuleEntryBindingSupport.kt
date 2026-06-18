package com.xiyunmn.puredupan.hook.feature.ui.entry

import android.content.Context
import android.view.View
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.ui.SettingsMenuHook

internal object ModuleEntryBindingSupport {
    fun bindLongPressToSettings(
        view: View?,
        classLoader: ClassLoader?,
        tag: String,
        entryName: String,
    ) {
        if (view == null || classLoader == null) return
        view.setOnLongClickListener {
            showSettings(it.context, classLoader, tag, entryName)
            true
        }
        XposedCompat.log("[$tag] long-press listener bound to $entryName")
    }

    fun findViewByEntryName(root: View, entryName: String): View? {
        val context = root.context ?: return null
        val resId = context.resources.getIdentifier(entryName, "id", context.packageName)
        if (resId == 0) return null
        return root.findViewById(resId)
    }

    private fun showSettings(
        context: Context,
        classLoader: ClassLoader,
        tag: String,
        entryName: String,
    ) {
        try {
            SettingsMenuHook.showModuleSettingsDialog(context, classLoader)
        } catch (t: Throwable) {
            XposedCompat.logW("[$tag] show settings dialog failed for $entryName: ${t.message}")
        }
    }
}
