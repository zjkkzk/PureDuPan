package com.xiyunmn.puredupan.hook.feature.startup.hotstart

import com.xiyunmn.puredupan.hook.feature.startup.hotstart.cn.CnHotStartSplashRemoveHook
import com.xiyunmn.puredupan.hook.feature.startup.hotstart.intl.IntlHotStartSplashRemoveHook
import com.xiyunmn.puredupan.hook.host.HostFlavor
import com.xiyunmn.puredupan.hook.host.HostRegistry

object HotStartSplashRemoveHook {
    internal fun hook(cl: ClassLoader) {
        when (resolveHostFlavor()) {
            HostFlavor.BAIDU_CN -> CnHotStartSplashRemoveHook.hook(cl)
            HostFlavor.BAIDU_INTL -> IntlHotStartSplashRemoveHook.hook(cl)
        }
    }

    private fun resolveHostFlavor(): HostFlavor {
        val packageName = com.xiyunmn.puredupan.hook.core.XposedCompat.currentPackageName()
        return packageName?.let { HostRegistry.resolveByPackageName(it)?.flavor }
            ?: HostFlavor.BAIDU_CN
    }
}
