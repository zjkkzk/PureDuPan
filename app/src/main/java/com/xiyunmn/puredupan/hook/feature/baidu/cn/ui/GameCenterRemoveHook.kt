package com.xiyunmn.puredupan.hook.feature.baidu.cn.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.symbols.baidu.cn.BaiduCnHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.core.HookState

/**
 * 游戏中心移除 Hook。
 *
 * 受 [HookSettings.isGameCenterRemoved] 控制，默认开启。
 */
object GameCenterRemoveHook {
    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!isEnabled()) {
            XposedCompat.log("[GameCenterRemoveHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val clazz = XposedCompat.findClassOrNull(
                BaiduCnHookPoints.ABOUT_ME_GAME_CENTER_FRAGMENT, cl
            ) ?: run {
                XposedCompat.log("[GameCenterRemoveHook] AboutMeGameCenterFragment class NOT FOUND")
                return
            }
            val method = XposedCompat.findMethodOrNull(
                clazz, "onCreateView",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Bundle::class.java
            ) ?: run {
                XposedCompat.log("[GameCenterRemoveHook] onCreateView NOT FOUND")
                return
            }
            mod.hook(method).intercept {
                if (isEnabled()) null else it.proceed()
            }
            XposedCompat.log("[GameCenterRemoveHook] hook INSTALLED")
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[GameCenterRemoveHook] FAILED: ${e.message}")
        }
    }

    private fun isEnabled(): Boolean =
        HookSettings.isMyPageCustomizeEnabled && HookSettings.isGameCenterRemoved

}
