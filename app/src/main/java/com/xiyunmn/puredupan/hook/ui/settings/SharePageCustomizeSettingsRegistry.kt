package com.xiyunmn.puredupan.hook.ui.settings

import com.xiyunmn.puredupan.hook.settings.registry.SettingsUserState
import com.xiyunmn.puredupan.hook.ui.UiText

internal data class SharePageCustomizeSwitchSpec(
    val key: String,
    val label: String,
    val description: String,
)

internal object SharePageCustomizeSettingsRegistry {
    val specs: List<SharePageCustomizeSwitchSpec> = listOf(
        SharePageCustomizeSwitchSpec(
            SettingsUserState.KEY_REMOVE_HOME_FAB,
            UiText.Settings.REMOVE_HOME_FAB_LABEL,
            UiText.Settings.REMOVE_HOME_FAB_DESC,
        ),
    )
}
