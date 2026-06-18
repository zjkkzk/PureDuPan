package com.xiyunmn.puredupan.hook.feature.ui.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.XposedCompat

internal object SharedHomeTitleBarModuleEntryInstaller {
    private const val HOME_UPLOAD_ENTRY_ID_NAME = "upload_file_entry"

    private val hookStates = linkedMapOf<String, HookState>()

    fun hook(cl: ClassLoader, tag: String, fragmentClassName: String) {
        val mod = XposedCompat.module ?: return
        val hookState = hookStates.getOrPut(tag) { HookState() }
        if (!hookState.markInstalled()) return

        try {
            val fragmentClass = XposedCompat.findClassOrNull(fragmentClassName, cl) ?: run {
                hookState.reset()
                XposedCompat.log("[$tag] title bar fragment class NOT FOUND: $fragmentClassName")
                return
            }

            val onCreateViewMethod = XposedCompat.findMethodOrNull(
                fragmentClass,
                "onCreateView",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Bundle::class.java,
            ) ?: run {
                hookState.reset()
                XposedCompat.log("[$tag] $fragmentClassName.onCreateView NOT FOUND")
                return
            }

            mod.hook(onCreateViewMethod).intercept { chain ->
                val result = chain.proceed()
                bindUploadEntryLongPress(
                    rootView = result as? View,
                    classLoader = chain.thisObject?.javaClass?.classLoader,
                    tag = tag,
                )
                result
            }
            XposedCompat.log("[$tag] hook INSTALLED: $fragmentClassName.onCreateView")
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[$tag] install FAILED: ${e.message}")
            XposedCompat.log(e)
        }
    }

    private fun bindUploadEntryLongPress(rootView: View?, classLoader: ClassLoader?, tag: String) {
        if (rootView == null) return
        val uploadEntry = ModuleEntryBindingSupport.findViewByEntryName(
            root = rootView,
            entryName = HOME_UPLOAD_ENTRY_ID_NAME,
        )
        if (uploadEntry == null) {
            XposedCompat.logD("[$tag] upload entry view not found: $HOME_UPLOAD_ENTRY_ID_NAME")
            return
        }
        ModuleEntryBindingSupport.bindLongPressToSettings(
            view = uploadEntry,
            classLoader = classLoader,
            tag = tag,
            entryName = HOME_UPLOAD_ENTRY_ID_NAME,
        )
    }
}
