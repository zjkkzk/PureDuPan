package com.xiyunmn.puredupan.hook.ui.settings

import com.xiyunmn.puredupan.hook.config.model.MemberCardLayoutMode
import com.xiyunmn.puredupan.hook.settings.registry.SettingsUserState
import com.xiyunmn.puredupan.hook.ui.UiText

internal data class MemberCardCustomizeSwitchSpec(
    val key: String,
    val label: String,
    val description: String,
    val saveOnlyWhenFeatureVisible: Boolean = false,
)

internal object MemberCardCustomizeSettingsRegistry {
    val sizeAdjustSpec = MemberCardCustomizeSwitchSpec(
        SettingsUserState.KEY_MEMBER_CARD_SIZE_ADJUST,
        UiText.Settings.MEMBER_CARD_SIZE_ADJUST_LABEL,
        UiText.Settings.MEMBER_CARD_SIZE_ADJUST_DESC,
    )
    val removeCardClickSpec = MemberCardCustomizeSwitchSpec(
        SettingsUserState.KEY_REMOVE_MEMBER_CARD_CLICK,
        UiText.Settings.REMOVE_MEMBER_CARD_CLICK_LABEL,
        UiText.Settings.REMOVE_MEMBER_CARD_CLICK_DESC,
    )
    val viewBackgroundOnClickSpec = MemberCardCustomizeSwitchSpec(
        SettingsUserState.KEY_VIEW_MEMBER_CARD_BACKGROUND_ON_CLICK,
        UiText.Settings.VIEW_MEMBER_CARD_BACKGROUND_ON_CLICK_LABEL,
        UiText.Settings.VIEW_MEMBER_CARD_BACKGROUND_ON_CLICK_DESC,
    )

    private val operationSpec = MemberCardCustomizeSwitchSpec(
        SettingsUserState.KEY_HIDE_MEMBER_CARD_OPERATION,
        UiText.Settings.HIDE_MEMBER_CARD_OPERATION_LABEL,
        UiText.Settings.HIDE_MEMBER_CARD_OPERATION_DESC,
    )
    private val benefitSpec = MemberCardCustomizeSwitchSpec(
        SettingsUserState.KEY_HIDE_MEMBER_CARD_BENEFIT,
        UiText.Settings.HIDE_MEMBER_CARD_BENEFIT_LABEL,
        UiText.Settings.HIDE_MEMBER_CARD_BENEFIT_DESC,
    )
    private val firstBenefitSpec = MemberCardCustomizeSwitchSpec(
        SettingsUserState.KEY_HIDE_MEMBER_CARD_FIRST_BENEFIT,
        UiText.Settings.HIDE_MEMBER_CARD_FIRST_BENEFIT_LABEL,
        UiText.Settings.HIDE_MEMBER_CARD_FIRST_BENEFIT_DESC,
        saveOnlyWhenFeatureVisible = true,
    )
    private val secondBenefitSpec = MemberCardCustomizeSwitchSpec(
        SettingsUserState.KEY_HIDE_MEMBER_CARD_SECOND_BENEFIT,
        UiText.Settings.HIDE_MEMBER_CARD_SECOND_BENEFIT_LABEL,
        UiText.Settings.HIDE_MEMBER_CARD_SECOND_BENEFIT_DESC,
        saveOnlyWhenFeatureVisible = true,
    )
    private val thirdBenefitSpec = MemberCardCustomizeSwitchSpec(
        SettingsUserState.KEY_HIDE_MEMBER_CARD_THIRD_BENEFIT,
        UiText.Settings.HIDE_MEMBER_CARD_THIRD_BENEFIT_LABEL,
        UiText.Settings.HIDE_MEMBER_CARD_THIRD_BENEFIT_DESC,
        saveOnlyWhenFeatureVisible = true,
    )
    private val benefitBarSpec = MemberCardCustomizeSwitchSpec(
        SettingsUserState.KEY_HIDE_MEMBER_CARD_BENEFIT_BAR,
        UiText.Settings.HIDE_MEMBER_CARD_BENEFIT_BAR_LABEL,
        UiText.Settings.HIDE_MEMBER_CARD_BENEFIT_BAR_DESC,
    )
    private val svipStatusSpec = MemberCardCustomizeSwitchSpec(
        SettingsUserState.KEY_HIDE_MEMBER_CARD_SVIP_STATUS,
        UiText.Settings.HIDE_MEMBER_CARD_SVIP_STATUS_LABEL,
        UiText.Settings.HIDE_MEMBER_CARD_SVIP_STATUS_DESC,
    )

    fun allHideSpecsForLayout(memberCardLayoutMode: MemberCardLayoutMode): List<MemberCardCustomizeSwitchSpec> {
        return listOf(
            operationSpec,
            benefitSpec,
            firstBenefitSpec,
            secondBenefitSpec,
            thirdBenefitSpec,
            benefitBarSpec,
            svipLevelSpec(memberCardLayoutMode),
            svipStatusSpec,
            renewButtonSpec(memberCardLayoutMode),
        )
    }

    fun visibleHideSpecsForLayout(memberCardLayoutMode: MemberCardLayoutMode): List<MemberCardCustomizeSwitchSpec> {
        return when (memberCardLayoutMode) {
            MemberCardLayoutMode.BENEFIT_SLOT -> listOf(
                firstBenefitSpec,
                secondBenefitSpec,
                thirdBenefitSpec,
                svipLevelSpec(MemberCardLayoutMode.BENEFIT_SLOT),
                renewButtonSpec(MemberCardLayoutMode.BENEFIT_SLOT),
            )
            MemberCardLayoutMode.STANDARD -> listOf(
                operationSpec,
                benefitSpec,
                benefitBarSpec,
                svipLevelSpec(MemberCardLayoutMode.STANDARD),
                svipStatusSpec,
                renewButtonSpec(MemberCardLayoutMode.STANDARD),
            )
        }
    }

    private fun svipLevelSpec(memberCardLayoutMode: MemberCardLayoutMode): MemberCardCustomizeSwitchSpec {
        return when (memberCardLayoutMode) {
            MemberCardLayoutMode.BENEFIT_SLOT -> MemberCardCustomizeSwitchSpec(
                SettingsUserState.KEY_HIDE_INTL_MEMBER_CARD_SVIP_LEVEL,
                UiText.Settings.HIDE_INTL_MEMBER_CARD_SVIP_LEVEL_LABEL,
                UiText.Settings.HIDE_INTL_MEMBER_CARD_SVIP_LEVEL_DESC,
            )
            MemberCardLayoutMode.STANDARD -> MemberCardCustomizeSwitchSpec(
                SettingsUserState.KEY_HIDE_MEMBER_CARD_SVIP_LEVEL,
                UiText.Settings.HIDE_MEMBER_CARD_SVIP_LEVEL_LABEL,
                UiText.Settings.HIDE_MEMBER_CARD_SVIP_LEVEL_DESC,
            )
        }
    }

    private fun renewButtonSpec(memberCardLayoutMode: MemberCardLayoutMode): MemberCardCustomizeSwitchSpec {
        return when (memberCardLayoutMode) {
            MemberCardLayoutMode.BENEFIT_SLOT -> MemberCardCustomizeSwitchSpec(
                SettingsUserState.KEY_HIDE_INTL_MEMBER_CARD_UPGRADE_BUTTON,
                UiText.Settings.HIDE_INTL_MEMBER_CARD_UPGRADE_BUTTON_LABEL,
                UiText.Settings.HIDE_INTL_MEMBER_CARD_UPGRADE_BUTTON_DESC,
            )
            MemberCardLayoutMode.STANDARD -> MemberCardCustomizeSwitchSpec(
                SettingsUserState.KEY_HIDE_MEMBER_CARD_RENEW_BUTTON,
                UiText.Settings.HIDE_MEMBER_CARD_RENEW_BUTTON_LABEL,
                UiText.Settings.HIDE_MEMBER_CARD_RENEW_BUTTON_DESC,
            )
        }
    }
}
