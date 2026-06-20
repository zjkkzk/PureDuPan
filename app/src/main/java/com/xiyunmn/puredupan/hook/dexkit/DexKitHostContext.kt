package com.xiyunmn.puredupan.hook.dexkit

import com.xiyunmn.puredupan.hook.host.HostDexKitRuntimeInfo

internal data class DexKitHostContext(
    val hostId: String,
    val mainProcessName: String,
    val targetRegistryId: String?,
    val availableFeatureKeys: Set<String>,
    val stableActivityClassNames: List<String>,
) {
    fun isMainProcess(processName: String): Boolean =
        processName == mainProcessName

    fun isFeatureAvailable(featureKey: String): Boolean =
        featureKey in availableFeatureKeys

    companion object {
        fun from(host: HostDexKitRuntimeInfo): DexKitHostContext {
            return DexKitHostContext(
                hostId = host.hostId,
                mainProcessName = host.mainProcessName,
                targetRegistryId = host.targetRegistryId,
                availableFeatureKeys = host.availableFeatureKeys,
                stableActivityClassNames = host.stableActivityClassNames,
            )
        }
    }
}
