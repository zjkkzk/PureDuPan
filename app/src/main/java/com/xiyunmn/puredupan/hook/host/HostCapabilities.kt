package com.xiyunmn.puredupan.hook.host

import com.xiyunmn.puredupan.hook.config.model.MemberCardLayoutMode

internal data class HostCapabilities(
    val features: HostFeatureCapabilities = HostFeatureCapabilities(),
    val hooks: HostHookCapabilities = HostHookCapabilities(),
    val uiHookPoints: HostUiHookPoints = HostUiHookPoints(),
    val startupHookPoints: HostStartupHookPoints = HostStartupHookPoints(),
    val settings: HostSettingsCapabilities = HostSettingsCapabilities(),
    val dexKit: HostDexKitCapabilities = HostDexKitCapabilities(),
    val diagnostics: HostDiagnosticsCapabilities = HostDiagnosticsCapabilities(),
)

internal data class HostFeatureCapabilities(
    val availableKeys: Set<String> = emptySet(),
)

internal data class HostHookCapabilities(
    val catalogId: String? = null,
    val supportsOemPushHook: Boolean = false,
)

internal data class HostStartupHookPoints(
    val hotStartSplashLifecycleManagerClassName: String? = null,
    val hotStartSplashBackgroundResumeAdStartMethodName: String? = null,
)

internal data class HostSettingsCapabilities(
    val primarySplashAdFeatureKey: String? = null,
    val memberCardLayoutMode: MemberCardLayoutMode = MemberCardLayoutMode.STANDARD,
)

internal data class HostDexKitCapabilities(
    val targetRegistryId: String? = null,
    val showStatusInSettings: Boolean = false,
    val stableActivityClassNames: List<String> = emptyList(),
)

internal data class HostDiagnosticsCapabilities(
    val deviceFingerprintCollector: HostDeviceFingerprintCollector? = null,
)
