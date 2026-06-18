package com.xiyunmn.puredupan.hook.feature.ui.entry.cn

import com.xiyunmn.puredupan.hook.feature.ui.entry.SharedAboutMeModuleEntryInstaller

internal object CnAboutMeModuleEntryHook {
    private const val TAG = "CnAboutMeModuleEntryHook"

    fun hook(cl: ClassLoader) {
        SharedAboutMeModuleEntryInstaller.hook(cl, TAG)
    }
}
