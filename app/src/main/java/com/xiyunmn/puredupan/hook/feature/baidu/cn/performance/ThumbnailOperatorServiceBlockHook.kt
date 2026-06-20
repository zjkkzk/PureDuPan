package com.xiyunmn.puredupan.hook.feature.baidu.cn.performance

import android.content.Context
import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.symbols.baidu.cn.BaiduCnHookPoints
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.core.HookState

/**
 * Blocks the client-compute thumbnail operator before it starts the operator process.
 */
object ThumbnailOperatorServiceBlockHook {
    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!isEnabled()) {
            XposedCompat.log("[ThumbnailOperatorServiceBlockHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            var installedCount = 0
            val managerClass = XposedCompat.findClassOrNull(
                BaiduCnHookPoints.CLIENT_COMPUTE_MANAGER,
                cl,
            ) ?: run {
                XposedCompat.log("[ThumbnailOperatorServiceBlockHook] ClientComputeManager NOT FOUND")
                hookState.reset()
                return
            }

            XposedCompat.findMethodOrNull(
                managerClass,
                BaiduCnHookPoints.CLIENT_COMPUTE_MANAGER_INIT_METHOD,
                Context::class.java,
            )?.let { method ->
                mod.hook(method).intercept { chain ->
                    if (isEnabled()) {
                        XposedCompat.logD(
                            "[ThumbnailOperatorServiceBlockHook] ClientComputeManager.init blocked",
                        )
                        false
                    } else {
                        chain.proceed()
                    }
                }
                installedCount += 1
            } ?: XposedCompat.log("[ThumbnailOperatorServiceBlockHook] ClientComputeManager.init NOT FOUND")

            installedCount += hookAddJob(cl)

            if (installedCount == 0) {
                XposedCompat.log("[ThumbnailOperatorServiceBlockHook] no hooks installed")
                hookState.reset()
                return
            }

            XposedCompat.log("[ThumbnailOperatorServiceBlockHook] hooks INSTALLED: count=$installedCount")
        } catch (e: Exception) {
            hookState.reset()
            XposedCompat.log("[ThumbnailOperatorServiceBlockHook] FAILED: ${e.message}")
            XposedCompat.log(e)
        }
    }

    private fun hookAddJob(cl: ClassLoader): Int {
        val mod = XposedCompat.module ?: return 0
        val utilClass = XposedCompat.findClassOrNull(
            BaiduCnHookPoints.THUMBNAIL_OPERATOR_UTIL,
            cl,
        ) ?: run {
            XposedCompat.log("[ThumbnailOperatorServiceBlockHook] ThumbnailOperatorUtil NOT FOUND")
            return 0
        }
        val compressBeanClass = XposedCompat.findClassOrNull(
            BaiduCnHookPoints.TERMINALCALC_COMPRESS_BEAN,
            cl,
        ) ?: run {
            XposedCompat.log("[ThumbnailOperatorServiceBlockHook] CompressBean NOT FOUND")
            return 0
        }
        val configCompressImageClass = XposedCompat.findClassOrNull(
            BaiduCnHookPoints.CONFIG_COMPRESS_IMAGE,
            cl,
        ) ?: run {
            XposedCompat.log("[ThumbnailOperatorServiceBlockHook] ConfigCompressImage NOT FOUND")
            return 0
        }

        val method = XposedCompat.findMethodOrNull(
            utilClass,
            BaiduCnHookPoints.THUMBNAIL_OPERATOR_UTIL_ADD_JOB_METHOD,
            Context::class.java,
            compressBeanClass,
            configCompressImageClass,
            String::class.java,
        )
        if (method == null) {
            XposedCompat.log("[ThumbnailOperatorServiceBlockHook] ThumbnailOperatorUtil.addJob NOT FOUND")
            return 0
        }

        mod.hook(method).intercept { chain ->
            if (isEnabled()) {
                XposedCompat.logD("[ThumbnailOperatorServiceBlockHook] ThumbnailOperatorUtil.addJob blocked")
                null
            } else {
                chain.proceed()
            }
        }
        return 1
    }



    private fun isEnabled(): Boolean =
        HookSettings.isPerformanceOptimizeEnabled && HookSettings.isThumbnailOperatorServiceDisabled
}
