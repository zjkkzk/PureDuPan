package com.xiyunmn.puredupan.hook.plan

import com.xiyunmn.puredupan.hook.host.HostProfile

internal data class HookPlanHostContext(
    val hostId: String,
    val catalogId: String?,
    val isMain: Boolean,
    val isPushService: Boolean,
    private val supportsOemPushHookForHost: Boolean,
    private val availableFeatureKeys: Set<String>,
) {
    val supportsOemPushHook: Boolean =
        supportsOemPushHookForHost && (isMain || isPushService)

    fun isFeatureAvailable(featureKey: String): Boolean {
        return featureKey in availableFeatureKeys
    }

    companion object {
        fun from(host: HostProfile, processName: String): HookPlanHostContext {
            return HookPlanHostContext(
                hostId = host.id,
                catalogId = host.capabilities.hooks.catalogId,
                isMain = host.isMainProcess(processName),
                isPushService = host.isPushServiceProcess(processName),
                supportsOemPushHookForHost = host.capabilities.hooks.supportsOemPushHook,
                availableFeatureKeys = host.capabilities.features.availableKeys,
            )
        }
    }
}
