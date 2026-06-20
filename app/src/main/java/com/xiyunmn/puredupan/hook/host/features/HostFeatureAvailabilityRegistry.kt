package com.xiyunmn.puredupan.hook.host.features

import com.xiyunmn.puredupan.hook.config.model.FeatureAvailabilityState
import com.xiyunmn.puredupan.hook.config.model.FeatureAvailabilityStatus
import com.xiyunmn.puredupan.hook.host.HostRegistry

internal object HostFeatureAvailabilityRegistry {
    private val fullStatus = FeatureAvailabilityStatus(
        state = FeatureAvailabilityState.FULL,
    )

    private val disabledStatus = FeatureAvailabilityStatus(
        state = FeatureAvailabilityState.DISABLED,
    )

    fun featureStatusMapFor(packageName: String): Map<String, FeatureAvailabilityStatus> {
        val availableKeys = HostRegistry.resolveByPackageName(packageName)
            ?.capabilities
            ?.features
            ?.availableKeys
            .orEmpty()
        return HostRegistry.supportedFeatureKeys.associateWith { featureKey ->
            if (featureKey in availableKeys) fullStatus else disabledStatus
        }
    }
}
