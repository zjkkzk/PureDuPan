package com.xiyunmn.puredupan.hook.feature.ui.entry.intl

import com.xiyunmn.puredupan.hook.feature.ui.entry.SharedHomeTitleBarModuleEntryInstaller

internal object IntlHomeTitleBarModuleEntryHook {
    private const val TAG = "IntlHomeTitleBarModuleEntryHook"
    private const val FRAGMENT_CLASS_NAME =
        "com.baidu.netdisk.newfeedhome.feedhome.ui.view.fragment.FHTitleBarFragment"

    fun hook(cl: ClassLoader) {
        SharedHomeTitleBarModuleEntryInstaller.hook(cl, TAG, FRAGMENT_CLASS_NAME)
    }
}
