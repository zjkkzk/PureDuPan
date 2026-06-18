package com.xiyunmn.puredupan.hook.feature.ui.entry.intl

import com.xiyunmn.puredupan.hook.feature.ui.entry.SharedAboutMeModuleEntryInstaller

internal object IntlAboutMeModuleEntryHook {
    private const val TAG = "IntlAboutMeModuleEntryHook"

    fun hook(cl: ClassLoader) {
        SharedAboutMeModuleEntryInstaller.hook(cl, TAG)
    }
}
