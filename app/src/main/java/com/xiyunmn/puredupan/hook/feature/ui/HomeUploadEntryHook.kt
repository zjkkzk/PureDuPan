package com.xiyunmn.puredupan.hook.feature.ui

import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.feature.ui.entry.cn.CnHomeTitleBarModuleEntryHook
import com.xiyunmn.puredupan.hook.feature.ui.entry.intl.IntlHomeTitleBarModuleEntryHook
import com.xiyunmn.puredupan.hook.host.HostFlavor
import com.xiyunmn.puredupan.hook.host.HostRegistry

object HomeUploadEntryHook {
    internal fun hook(cl: ClassLoader) {
        when (resolveHostFlavor()) {
            HostFlavor.BAIDU_INTL -> IntlHomeTitleBarModuleEntryHook.hook(cl)
            HostFlavor.BAIDU_CN -> CnHomeTitleBarModuleEntryHook.hook(cl)
        }
    }

    private fun resolveHostFlavor(): HostFlavor {
        val packageName = XposedCompat.currentPackageName()
        return HostRegistry.resolveByPackageName(packageName.orEmpty())?.flavor
            ?: HostFlavor.BAIDU_CN
    }
}
