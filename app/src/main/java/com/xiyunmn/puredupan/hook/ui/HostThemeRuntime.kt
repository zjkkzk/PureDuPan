package com.xiyunmn.puredupan.hook.ui

import android.content.Context
import com.xiyunmn.puredupan.hook.host.HostRuntimeState

internal object HostThemeRuntime {
    fun skinConfigClassName(context: Context): String? {
        return HostRuntimeState.skinConfigClassNameFor(context)
    }
}
