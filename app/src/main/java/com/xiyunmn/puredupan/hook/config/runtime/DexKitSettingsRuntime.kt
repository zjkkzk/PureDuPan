package com.xiyunmn.puredupan.hook.config.runtime

import android.content.Context
import android.content.SharedPreferences
import com.xiyunmn.puredupan.hook.dexkit.DexKitCompat

internal object DexKitSettingsRuntime {
    fun bindRuntimeProvider(
        appContextProvider: () -> Context?,
        moduleStatePrefsProvider: (Context) -> SharedPreferences,
    ) {
        DexKitCompat.setRuntimeProvider(
            appContextProvider = appContextProvider,
            moduleStatePrefsProvider = moduleStatePrefsProvider,
        )
    }

    fun markFullScanPendingFromConfigListener() {
        DexKitCompat.markFullScanPending("dexkit switch enabled")
    }

    fun markFullScanPendingFromSettings() {
        DexKitCompat.markFullScanPending("dexkit switch enabled from settings")
    }
}
