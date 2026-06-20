package com.xiyunmn.puredupan.hook.host

internal data class HostDexKitRuntimeInfo(
    val hostId: String,
    val mainProcessName: String,
    val targetRegistryId: String?,
    val availableFeatureKeys: Set<String>,
    val stableActivityClassNames: List<String>,
) {
    companion object {
        fun from(host: HostProfile): HostDexKitRuntimeInfo {
            val dexKitCapabilities = host.capabilities.dexKit
            return HostDexKitRuntimeInfo(
                hostId = host.id,
                mainProcessName = host.mainProcessName,
                targetRegistryId = dexKitCapabilities.targetRegistryId,
                availableFeatureKeys = host.capabilities.features.availableKeys,
                stableActivityClassNames = dexKitCapabilities.stableActivityClassNames,
            )
        }
    }
}
