package com.xiyunmn.puredupan.hook.feature.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.xiyunmn.puredupan.hook.config.ConfigManager
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.StableBaiduPanHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat

object BottomBarSimplifyFeature {
    private const val TAB_CONTAINER_ID = "rg_tabs"
    private const val TAB_AIGC_HI_LOTTIE_ID = "aigc_hi_lottie"
    private const val TAB_DISCOVERY_ID = "rb_findresoure"

    private val hookState = HookState()

    private data class TabTarget(
        val idName: String,
        val label: String,
        val isHidden: () -> Boolean,
    )

    private val tabTargets = listOf(
        TabTarget("rb_home", "首页") { ConfigManager.isBottomBarTabHomeHidden },
        TabTarget("rb_filelist", "文件") { ConfigManager.isBottomBarTabFileHidden },
        TabTarget("aigc_cloud", "AIGC") { ConfigManager.isBottomBarTabAigcHidden },
        TabTarget("rb_share", "共享") { ConfigManager.isBottomBarTabShareHidden },
        TabTarget("rb_findresoure", "会员") { ConfigManager.isBottomBarTabVipHidden },
        TabTarget("rb_about_me", "我的") { ConfigManager.isBottomBarTabMineHidden },
    )

    internal fun hook(cl: ClassLoader) {
        if (!ConfigManager.isBottomBarCustomEnabled) {
            XposedCompat.log("[BottomBarSimplifyFeature] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val activityClass = XposedCompat.findClassOrNull(
                StableBaiduPanHookPoints.MAIN_ACTIVITY,
                cl,
            ) ?: run {
                XposedCompat.log(
                    "[BottomBarSimplifyFeature] MainActivity class NOT FOUND: " +
                        StableBaiduPanHookPoints.MAIN_ACTIVITY,
                )
                return
            }

            val onCreateMethod = XposedCompat.findMethodOrNull(
                activityClass,
                "onCreate",
                Bundle::class.java,
            ) ?: run {
                XposedCompat.log("[BottomBarSimplifyFeature] MainActivity.onCreate NOT FOUND")
                return
            }

            mod.hook(onCreateMethod).intercept { chain ->
                val result = chain.proceed()
                try {
                    applyTabVisibility(chain.thisObject as? Activity)
                } catch (e: Exception) {
                    XposedCompat.logD {
                        "[BottomBarSimplifyFeature] applyTabVisibility failed (non-fatal): ${e.message}"
                    }
                }
                result
            }

            val onWindowFocusChangedMethod = XposedCompat.findMethodOrNull(
                activityClass,
                "onWindowFocusChanged",
                Boolean::class.javaPrimitiveType!!,
            )
            if (onWindowFocusChangedMethod != null) {
                mod.hook(onWindowFocusChangedMethod).intercept { chain ->
                    val result = chain.proceed()
                    try {
                        applyTabVisibility(chain.thisObject as? Activity)
                    } catch (e: Exception) {
                        XposedCompat.logD {
                            "[BottomBarSimplifyFeature] focus reapply failed (non-fatal): ${e.message}"
                        }
                    }
                    result
                }
            }

            XposedCompat.log(
                "[BottomBarSimplifyFeature] hook INSTALLED: ${StableBaiduPanHookPoints.MAIN_ACTIVITY}.onCreate",
            )
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[BottomBarSimplifyFeature] install FAILED: ${e.message}")
        }
    }

    private fun applyTabVisibility(activity: Activity?) {
        if (activity == null) return
        if (!ConfigManager.isBottomBarCustomEnabled) return

        if (ConfigManager.isBottomBarTabAigcHidden) {
            hideAigcFeatureSlot(activity)
        }

        var hiddenCount = 0
        for (tab in tabTargets) {
            try {
                if (!tab.isHidden()) continue

                val resId = activity.resources.getIdentifier(
                    tab.idName,
                    "id",
                    activity.packageName,
                )
                if (resId == 0) {
                    XposedCompat.logD(
                        "[BottomBarSimplifyFeature] ${tab.label} tab resId not found: ${tab.idName}",
                    )
                    continue
                }

                val view = activity.findViewById<View>(resId)
                if (view != null) {
                    hideTabView(view)
                    hiddenCount++
                    XposedCompat.logD(
                        "[BottomBarSimplifyFeature] ${tab.label} tab hidden (${tab.idName})",
                    )
                } else {
                    XposedCompat.logD(
                        "[BottomBarSimplifyFeature] ${tab.label} tab view not in hierarchy (resId=$resId)",
                    )
                }
            } catch (e: Exception) {
                XposedCompat.logD(
                    "[BottomBarSimplifyFeature] ${tab.label} tab hide failed: ${e.message}",
                )
            }
        }

        if (hiddenCount > 0) {
            XposedCompat.log("[BottomBarSimplifyFeature] applied: $hiddenCount tab(s) hidden")
        }
    }

    private fun hideAigcFeatureSlot(activity: Activity) {
        runCatching {
            val aigcTab = findViewByEntryName(activity, "aigc_cloud")
            hideTabView(aigcTab)
            val resoureTab = findViewByEntryName(activity, TAB_DISCOVERY_ID)
            hideTabView(resoureTab)
            val aigcHiLottie = findViewByEntryName(activity, TAB_AIGC_HI_LOTTIE_ID)
            hideTabView(aigcHiLottie)

            val tabContainer = findViewByEntryName(activity, TAB_CONTAINER_ID) as? LinearLayout
            if (tabContainer != null) {
                reflowLinearLayoutChildren(tabContainer)
            }
        }.onFailure {
            XposedCompat.logD("[BottomBarSimplifyFeature] hideAigcFeatureSlot failed: ${it.message}")
        }
    }

    private fun findViewByEntryName(activity: Activity, idName: String): View? {
        val resId = activity.resources.getIdentifier(idName, "id", activity.packageName)
        if (resId == 0) return null
        return activity.findViewById(resId)
    }

    private fun hideTabView(view: View?) {
        if (view == null) return
        view.visibility = View.GONE
        val params = view.layoutParams
        if (params is LinearLayout.LayoutParams) {
            params.width = 0
            params.weight = 0f
            view.layoutParams = params
        }
        (view.parent as? ViewGroup)?.requestLayout()
    }

    private fun reflowLinearLayoutChildren(container: LinearLayout) {
        for (index in 0 until container.childCount) {
            val child = container.getChildAt(index)
            val params = child.layoutParams as? LinearLayout.LayoutParams ?: continue
            if (child.visibility == View.VISIBLE && child.id != View.NO_ID) {
                val idName = runCatching { child.resources.getResourceEntryName(child.id) }.getOrNull()
                if (idName != "aigc_cloud" && params.width == 0 && params.weight == 0f) {
                    params.weight = 1f
                    child.layoutParams = params
                }
            }
        }
        container.requestLayout()
    }
}
