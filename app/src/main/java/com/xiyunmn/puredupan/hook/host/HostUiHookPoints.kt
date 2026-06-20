package com.xiyunmn.puredupan.hook.host

internal data class HostUiHookPoints(
    val mainActivityClassName: String? = null,
    val homeActivityClassName: String? = null,
    val aboutMeActivityClassName: String? = null,
    val newAboutMeActivityClassName: String? = null,
    val mainActivityPresenterClassName: String? = null,
    val newHomeFabFragmentClassName: String? = null,
    val popupResponseClassName: String? = null,
    val settingsImageResultHostActivityClassNames: List<String> = emptyList(),
    val skinConfigClassName: String? = null,
    val homeCustomize: HomeCustomizeHookPoints = HomeCustomizeHookPoints(),
)
