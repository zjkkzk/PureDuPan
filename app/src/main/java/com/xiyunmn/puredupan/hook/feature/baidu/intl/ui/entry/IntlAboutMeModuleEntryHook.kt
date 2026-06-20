package com.xiyunmn.puredupan.hook.feature.baidu.intl.ui.entry

import com.xiyunmn.puredupan.hook.feature.baidu.shared.ui.entry.SharedAboutMeModuleEntryInstaller

internal object IntlAboutMeModuleEntryHook {
    private const val TAG = "IntlAboutMeModuleEntryHook"

    fun hook(cl: ClassLoader) {
        SharedAboutMeModuleEntryInstaller.hook(cl, TAG)
    }
}
