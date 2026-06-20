package com.xiyunmn.puredupan.hook.feature.baidu.intl.ui.entry

import com.xiyunmn.puredupan.hook.feature.baidu.shared.ui.entry.SharedHomeTitleBarModuleEntryInstaller
import com.xiyunmn.puredupan.hook.symbols.baidu.intl.BaiduIntlHookPoints

internal object IntlHomeTitleBarModuleEntryHook {
    private const val TAG = "IntlHomeTitleBarModuleEntryHook"
    private const val FRAGMENT_CLASS_NAME = BaiduIntlHookPoints.NEW_FEED_HOME_TITLE_BAR_FRAGMENT

    fun hook(cl: ClassLoader) {
        SharedHomeTitleBarModuleEntryInstaller.hook(cl, TAG, FRAGMENT_CLASS_NAME)
    }
}
