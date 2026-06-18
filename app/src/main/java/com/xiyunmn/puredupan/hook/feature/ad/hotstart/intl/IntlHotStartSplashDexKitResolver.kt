package com.xiyunmn.puredupan.hook.feature.ad.hotstart.intl

import com.xiyunmn.puredupan.hook.config.ConfigManager
import com.xiyunmn.puredupan.hook.core.XposedCompat
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.matchers.MethodMatcher

internal object IntlHotStartSplashDexKitResolver {
    data class ResolveResult(
        val className: String,
        val methodName: String,
    )

    private val signatureHints = listOf(
        "hot_start_filter_ad",
        "onResume startActivity",
        "onResume disable show activity",
        "returnToYun.isAdFiltered",
        "SplashManager().limitCountAdShow()",
        "onPauseByScreenLock",
        "com.baidu.netdisk.advertise.ui.SplashAdActivity",
    )

    fun resolve(cl: ClassLoader): ResolveResult? {
        if (!ConfigManager.isExperimentalDexKitEnabled) {
            XposedCompat.logD("[IntlHotStartSplashDexKitResolver] skipped: config disabled")
            return null
        }

        return runCatching {
            DexKitBridge.create(cl, false).use { bridge ->
                bridge.setThreadNum(1)

                val methods = bridge.findMethod(
                    FindMethod.create()
                        .searchPackages("f6")
                        .matcher(
                            MethodMatcher.create()
                                .returnType(Boolean::class.javaPrimitiveType!!)
                                .paramTypes(android.app.Activity::class.java)
                                .usingEqStrings(signatureHints),
                        ),
                )

                val best = methods
                    .filter {
                        !it.isConstructor &&
                            it.returnTypeName == "boolean" &&
                            it.paramTypeNames == listOf("android.app.Activity")
                    }
                    .sortedWith(
                        compareByDescending<org.luckypray.dexkit.result.MethodData> { score(it) }
                            .thenBy { it.className }
                            .thenBy { it.name },
                    )
                    .firstOrNull()

                if (best == null) {
                    XposedCompat.log("[IntlHotStartSplashDexKitResolver] no candidate matched")
                    null
                } else {
                    XposedCompat.log(
                        "[IntlHotStartSplashDexKitResolver] resolved ${best.className}.${best.name} score=${score(best)}",
                    )
                    ResolveResult(
                        className = best.className,
                        methodName = best.name,
                    )
                }
            }
        }.onFailure {
            XposedCompat.log("[IntlHotStartSplashDexKitResolver] resolve FAILED: ${it.message}")
            XposedCompat.log(it)
        }.getOrNull()
    }

    private fun score(method: org.luckypray.dexkit.result.MethodData): Int {
        val usingStrings = method.usingStrings.toSet()
        var score = 0
        signatureHints.forEachIndexed { index, hint ->
            if (hint in usingStrings) {
                score += 100 - index
            }
        }
        if (method.className == "f6.e") score += 500
        if (method.name == "q") score += 200
        return score
    }
}
