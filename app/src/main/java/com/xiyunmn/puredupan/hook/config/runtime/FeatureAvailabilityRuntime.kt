package com.xiyunmn.puredupan.hook.config.runtime

import com.xiyunmn.puredupan.hook.config.model.FeatureAvailabilityStatus

internal object FeatureAvailabilityRuntime {
    @Volatile
    private var availability: Map<String, Boolean> = emptyMap()

    fun isAvailable(featureKey: String): Boolean {
        return availability[featureKey] == true
    }

    fun apply(featureStatusMap: Map<String, FeatureAvailabilityStatus>) {
        availability = featureStatusMap.mapValues { (_, status) ->
            status.isSupported()
        }
    }
}
