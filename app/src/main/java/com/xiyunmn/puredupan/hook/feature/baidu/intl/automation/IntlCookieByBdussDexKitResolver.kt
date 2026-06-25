package com.xiyunmn.puredupan.hook.feature.baidu.intl.automation

import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.dexkit.DexKitCompat
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.matchers.MethodMatcher

internal object IntlCookieByBdussDexKitResolver {
    const val CACHE_ID = "intl_cookie_by_bduss"

    private const val TAG = "IntlCookieByBdussDexKitResolver"
    private const val PROBE_BDUSS = "__puredupan_cookie_probe__"
    private const val COOKIE_UTILS_TOKEN = "CookiesUtils"

    private data class DexMethodCandidate(
        val className: String,
        val methodName: String,
        val returnTypeName: String,
        val paramTypeNames: List<String>,
        val isConstructor: Boolean,
        val modifiers: Int,
        val usingStrings: Set<String>,
    )

    fun cookieFor(cl: ClassLoader, bduss: String): String {
        val method = resolve(cl) ?: return ""
        return runCatching { method.invoke(null, bduss) as? String }
            .getOrElse { t ->
                XposedCompat.logD("[$TAG] invoke cookie helper failed: ${t.message}")
                null
            }
            .orEmpty()
    }

    fun warmUpDexKitCache(cl: ClassLoader): Boolean {
        return resolve(cl) != null
    }

    private fun resolve(cl: ClassLoader): Method? {
        if (!HookSettings.isExperimentalDexKitEnabled) {
            XposedCompat.logD("[$TAG] skipped: config disabled")
            return null
        }

        when (val cached = DexKitCompat.getCachedMethod(TAG, CACHE_ID) { ref ->
            validateCachedResult(cl, ref)
        }) {
            is DexKitCompat.CachedResult.Found -> return cached.value
            DexKitCompat.CachedResult.NotFound -> return null
            DexKitCompat.CachedResult.Miss -> Unit
        }

        val methods = DexKitCompat.withBridge(TAG, cl) { bridge ->
            bridge.setThreadNum(1)
            bridge.findMethod(
                FindMethod.create()
                    .matcher(
                        MethodMatcher.create()
                            .modifiers(Modifier.STATIC)
                            .returnType(String::class.java)
                            .paramTypes(String::class.java),
                    ),
            ).map { methodData ->
                DexMethodCandidate(
                    className = methodData.className,
                    methodName = methodData.name,
                    returnTypeName = methodData.returnTypeName,
                    paramTypeNames = methodData.paramTypeNames,
                    isConstructor = methodData.isConstructor,
                    modifiers = methodData.modifiers,
                    usingStrings = methodData.usingStrings.toSet(),
                )
            }
        } ?: return null

        val best = methods.mapNotNull { candidate ->
            if (!candidate.isCookieHelperShape()) return@mapNotNull null
            val method = validateCachedResult(
                cl,
                DexKitCompat.MethodRef(candidate.className, candidate.methodName),
            ) ?: return@mapNotNull null
            candidate to method
        }.sortedWith(
            compareByDescending<Pair<DexMethodCandidate, Method>> { score(it.first) }
                .thenBy { it.first.className }
                .thenBy { it.first.methodName },
        ).firstOrNull()

        if (best == null) {
            XposedCompat.log("[$TAG] no cookie helper candidate matched")
            DexKitCompat.putCachedMethod(TAG, CACHE_ID, null)
            return null
        }

        val method = best.second
        XposedCompat.log(
            "[$TAG] resolved cookie helper by DexKit: " +
                "${method.declaringClass.name}.${method.name}, score=${score(best.first)}",
        )
        DexKitCompat.putCachedMethod(
            TAG,
            CACHE_ID,
            DexKitCompat.MethodRef(method.declaringClass.name, method.name),
        )
        return method
    }

    private fun validateCachedResult(cl: ClassLoader, ref: DexKitCompat.MethodRef): Method? {
        val clazz = XposedCompat.findClassOrNull(ref.className, cl) ?: return null
        if (COOKIE_UTILS_TOKEN !in staticStringConstants(clazz)) return null
        val method = XposedCompat.findMethodOrNull(clazz, ref.methodName, String::class.java)
            ?: return null
        if (!Modifier.isStatic(method.modifiers)) return null
        if (method.returnType != String::class.java) return null
        if (!probeLooksLikeBdussCookie(method)) return null
        return method
    }

    private fun DexMethodCandidate.isCookieHelperShape(): Boolean =
        !isConstructor &&
            Modifier.isStatic(modifiers) &&
            returnTypeName == "java.lang.String" &&
            paramTypeNames == listOf("java.lang.String")

    private fun score(candidate: DexMethodCandidate): Int {
        var score = 0
        if (COOKIE_UTILS_TOKEN in candidate.usingStrings) score += 200
        if ("BDUSS" in candidate.usingStrings || "BDUSS=" in candidate.usingStrings) score += 100
        if ("STOKEN" in candidate.usingStrings) score += 60
        if ("PANPSC" in candidate.usingStrings) score += 40
        return score
    }

    private fun probeLooksLikeBdussCookie(method: Method): Boolean {
        val value = runCatching { method.invoke(null, PROBE_BDUSS) as? String }.getOrNull()
            ?: return false
        return value.contains(PROBE_BDUSS) &&
            (value.contains("BDUSS=$PROBE_BDUSS") || value.contains("Cookie: BDUSS=$PROBE_BDUSS"))
    }

    private fun staticStringConstants(clazz: Class<*>): Set<String> {
        return clazz.declaredFields.mapNotNull { field ->
            if (!Modifier.isStatic(field.modifiers) || field.type != String::class.java) return@mapNotNull null
            runCatching {
                field.isAccessible = true
                field.get(null) as? String
            }.getOrNull()
        }.toSet()
    }
}
