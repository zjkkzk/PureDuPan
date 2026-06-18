package com.xiyunmn.puredupan.hook.feature.ui.entry.cn

import com.xiyunmn.puredupan.hook.feature.ui.entry.SharedHomeTitleBarModuleEntryInstaller

internal object CnHomeTitleBarModuleEntryHook {
    private const val TAG = "CnHomeTitleBarModuleEntryHook"
    private const val FRAGMENT_CLASS_NAME = "com.baidu.netdisk.home25ai.fragment.HomeSearchboxFragment"

    fun hook(cl: ClassLoader) {
        SharedHomeTitleBarModuleEntryInstaller.hook(cl, TAG, FRAGMENT_CLASS_NAME)
    }
}
