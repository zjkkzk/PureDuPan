package com.xiyunmn.puredupan.hook.feature.baidu.shared.runtime

import android.content.Context
import com.xiyunmn.puredupan.hook.host.HostRuntimeState

internal object BaiduFeatureRuntime {
    fun currentMainActivityClassName(): String? =
        HostRuntimeState.currentMainActivityClassName()

    fun currentMainActivityPresenterClassName(): String? =
        HostRuntimeState.currentMainActivityPresenterClassName()

    fun currentAboutMeActivityClassName(): String? =
        HostRuntimeState.currentAboutMeActivityClassName()

    fun currentPopupResponseClassName(): String? =
        HostRuntimeState.currentPopupResponseClassName()

    fun currentNewHomeFabFragmentClassName(): String? =
        HostRuntimeState.currentNewHomeFabFragmentClassName()

    fun currentSettingsImageResultHostActivityClassNames(): List<String> =
        HostRuntimeState.currentSettingsImageResultHostActivityClassNames()

    fun currentHomeCustomizeHookPoints() =
        HostRuntimeState.currentHomeCustomizeHookPoints()

    fun skinConfigClassNameFor(context: Context): String? =
        HostRuntimeState.skinConfigClassNameFor(context)
}
