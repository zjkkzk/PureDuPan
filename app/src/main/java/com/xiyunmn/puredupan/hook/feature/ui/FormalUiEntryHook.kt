package com.xiyunmn.puredupan.hook.feature.ui

import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.feature.ui.entry.cn.CnAboutMeModuleEntryHook
import com.xiyunmn.puredupan.hook.feature.ui.entry.intl.IntlAboutMeModuleEntryHook
import com.xiyunmn.puredupan.hook.host.HostFlavor
import com.xiyunmn.puredupan.hook.host.HostRegistry

/**
 * “我的”页模块入口。
 *
 * 保持统一的功能入口名，但将具体宿主实现拆到独立目录，
 * 方便国际版后续继续偏离时单独维护。
 */
object FormalUiEntryHook {
    internal fun hook(cl: ClassLoader) {
        when (resolveHostFlavor()) {
            HostFlavor.BAIDU_INTL -> IntlAboutMeModuleEntryHook.hook(cl)
            HostFlavor.BAIDU_CN -> CnAboutMeModuleEntryHook.hook(cl)
        }
    }

    private fun resolveHostFlavor(): HostFlavor {
        val packageName = XposedCompat.currentPackageName()
        return HostRegistry.resolveByPackageName(packageName.orEmpty())?.flavor
            ?: HostFlavor.BAIDU_CN
    }
}
