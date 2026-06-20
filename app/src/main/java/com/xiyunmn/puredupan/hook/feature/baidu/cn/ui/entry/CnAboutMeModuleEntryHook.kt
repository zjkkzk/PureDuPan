package com.xiyunmn.puredupan.hook.feature.baidu.cn.ui.entry

import com.xiyunmn.puredupan.hook.feature.baidu.shared.ui.entry.SharedAboutMeModuleEntryInstaller

internal object CnAboutMeModuleEntryHook {
    private const val TAG = "CnAboutMeModuleEntryHook"

    fun hook(cl: ClassLoader) {
        SharedAboutMeModuleEntryInstaller.hook(cl, TAG)
    }
}
