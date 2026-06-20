package com.xiyunmn.puredupan.hook.ui.settings

import android.content.SharedPreferences
import com.xiyunmn.puredupan.hook.settings.registry.SettingsUserState
import com.xiyunmn.puredupan.hook.ui.UiText

internal enum class BottomBarCustomizeSettingsSection {
    DIRECT,
    TAB,
}

internal data class BottomBarCustomizeSwitchSpec(
    val key: String,
    val label: String,
    val description: String?,
    val section: BottomBarCustomizeSettingsSection,
)

internal object BottomBarCustomizeSettingsRegistry {
    val specs: List<BottomBarCustomizeSwitchSpec> = listOf(
        BottomBarCustomizeSwitchSpec(
            SettingsUserState.KEY_REPLACE_BOTTOM_AI,
            UiText.Settings.REPLACE_BOTTOM_AI_LABEL,
            UiText.Settings.REPLACE_BOTTOM_AI_DESC,
            BottomBarCustomizeSettingsSection.DIRECT,
        ),
        BottomBarCustomizeSwitchSpec(
            SettingsUserState.KEY_BLOCK_BOTTOM_BADGE,
            UiText.Settings.BLOCK_BOTTOM_BADGE_LABEL,
            UiText.Settings.BLOCK_BOTTOM_BADGE_DESC,
            BottomBarCustomizeSettingsSection.DIRECT,
        ),
        BottomBarCustomizeSwitchSpec(
            SettingsUserState.KEY_HIDE_TAB_HOME,
            UiText.Settings.BOTTOM_BAR_HIDE_TAB_HOME_LABEL,
            null,
            BottomBarCustomizeSettingsSection.TAB,
        ),
        BottomBarCustomizeSwitchSpec(
            SettingsUserState.KEY_HIDE_TAB_FILE,
            UiText.Settings.BOTTOM_BAR_HIDE_TAB_FILE_LABEL,
            null,
            BottomBarCustomizeSettingsSection.TAB,
        ),
        BottomBarCustomizeSwitchSpec(
            SettingsUserState.KEY_HIDE_TAB_AIGC,
            UiText.Settings.BOTTOM_BAR_HIDE_TAB_AIGC_LABEL,
            null,
            BottomBarCustomizeSettingsSection.TAB,
        ),
        BottomBarCustomizeSwitchSpec(
            SettingsUserState.KEY_HIDE_TAB_SHARE,
            UiText.Settings.BOTTOM_BAR_HIDE_TAB_SHARE_LABEL,
            null,
            BottomBarCustomizeSettingsSection.TAB,
        ),
        BottomBarCustomizeSwitchSpec(
            SettingsUserState.KEY_HIDE_TAB_VIP,
            UiText.Settings.BOTTOM_BAR_HIDE_TAB_VIP_LABEL,
            null,
            BottomBarCustomizeSettingsSection.TAB,
        ),
        BottomBarCustomizeSwitchSpec(
            SettingsUserState.KEY_HIDE_TAB_MINE,
            UiText.Settings.BOTTOM_BAR_HIDE_TAB_MINE_LABEL,
            null,
            BottomBarCustomizeSettingsSection.TAB,
        ),
    )

    fun specsIn(section: BottomBarCustomizeSettingsSection): List<BottomBarCustomizeSwitchSpec> {
        return specs.filter { it.section == section }
    }

    fun readTabSelection(prefs: SharedPreferences): SettingsUserState.BottomBarTabSelection {
        return SettingsUserState.BottomBarTabSelection(
            hideFile = prefs.getBoolean(SettingsUserState.KEY_HIDE_TAB_FILE, false),
            hideShare = prefs.getBoolean(SettingsUserState.KEY_HIDE_TAB_SHARE, false),
            hideVip = prefs.getBoolean(SettingsUserState.KEY_HIDE_TAB_VIP, false),
            hideAigc = prefs.getBoolean(SettingsUserState.KEY_HIDE_TAB_AIGC, false),
            hideHome = prefs.getBoolean(SettingsUserState.KEY_HIDE_TAB_HOME, false),
            hideMine = prefs.getBoolean(SettingsUserState.KEY_HIDE_TAB_MINE, false),
        )
    }

    fun tabSelectionFor(valueFor: (String) -> Boolean): SettingsUserState.BottomBarTabSelection {
        return SettingsUserState.BottomBarTabSelection(
            hideFile = valueFor(SettingsUserState.KEY_HIDE_TAB_FILE),
            hideShare = valueFor(SettingsUserState.KEY_HIDE_TAB_SHARE),
            hideVip = valueFor(SettingsUserState.KEY_HIDE_TAB_VIP),
            hideAigc = valueFor(SettingsUserState.KEY_HIDE_TAB_AIGC),
            hideHome = valueFor(SettingsUserState.KEY_HIDE_TAB_HOME),
            hideMine = valueFor(SettingsUserState.KEY_HIDE_TAB_MINE),
        )
    }

    fun tabValue(selection: SettingsUserState.BottomBarTabSelection, key: String): Boolean {
        return when (key) {
            SettingsUserState.KEY_HIDE_TAB_FILE -> selection.hideFile
            SettingsUserState.KEY_HIDE_TAB_SHARE -> selection.hideShare
            SettingsUserState.KEY_HIDE_TAB_VIP -> selection.hideVip
            SettingsUserState.KEY_HIDE_TAB_AIGC -> selection.hideAigc
            SettingsUserState.KEY_HIDE_TAB_HOME -> selection.hideHome
            SettingsUserState.KEY_HIDE_TAB_MINE -> selection.hideMine
            else -> false
        }
    }

    fun tabValues(selection: SettingsUserState.BottomBarTabSelection): Map<String, Boolean> {
        return linkedMapOf(
            SettingsUserState.KEY_HIDE_TAB_HOME to selection.hideHome,
            SettingsUserState.KEY_HIDE_TAB_FILE to selection.hideFile,
            SettingsUserState.KEY_HIDE_TAB_AIGC to selection.hideAigc,
            SettingsUserState.KEY_HIDE_TAB_SHARE to selection.hideShare,
            SettingsUserState.KEY_HIDE_TAB_VIP to selection.hideVip,
            SettingsUserState.KEY_HIDE_TAB_MINE to selection.hideMine,
        )
    }

    fun hasHiddenTab(selection: SettingsUserState.BottomBarTabSelection): Boolean {
        return specsIn(BottomBarCustomizeSettingsSection.TAB).any { spec ->
            tabValue(selection, spec.key)
        }
    }
}
