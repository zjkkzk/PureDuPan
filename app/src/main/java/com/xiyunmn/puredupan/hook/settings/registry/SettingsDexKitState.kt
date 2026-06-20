package com.xiyunmn.puredupan.hook.settings.registry

import android.content.Context
import com.xiyunmn.puredupan.hook.config.runtime.DexKitSettingsRuntime
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.dexkit.DexKitCacheWarmUp
import com.xiyunmn.puredupan.hook.dexkit.DexKitHostContext
import com.xiyunmn.puredupan.hook.host.HostRuntimeState

internal object SettingsDexKitState {
    data class TargetStatusView(
        val id: String,
        val target: String,
        val feature: String,
        val state: String,
        val detail: String?,
    )

    fun summaryText(context: Context): String {
        val host = hostFor(context) ?: return "0/0"
        return DexKitCacheWarmUp.summaryText(host)
    }

    fun statusViews(context: Context): List<TargetStatusView> {
        val host = hostFor(context) ?: return emptyList()
        return DexKitCacheWarmUp.statusViews(host).map { status ->
            TargetStatusView(
                id = status.descriptor.id,
                target = status.descriptor.target,
                feature = status.descriptor.feature,
                state = status.state,
                detail = status.detail,
            )
        }
    }

    fun markFullScanPendingFromSettings(context: Context) {
        if (!SettingsHostState.isFeatureVisibleForContext(
                context,
                SettingsUserState.KEY_ENABLE_EXPERIMENTAL_DEXKIT,
            )
        ) {
            XposedCompat.logW("[SettingsDexKitState] full scan pending skipped: unsupported host")
            return
        }
        DexKitSettingsRuntime.markFullScanPendingFromSettings()
    }

    private fun hostFor(context: Context) =
        HostRuntimeState.dexKitRuntimeInfoForPackage(context.packageName)
            ?.let(DexKitHostContext::from)
}
