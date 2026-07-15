package com.xiyunmn.puredupan.hook.feature.baidu.intl.ui

import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.HookState
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.dexkit.DexKitCompat
import com.xiyunmn.puredupan.hook.feature.baidu.shared.resolver.KotlinMetadataUtils
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.matchers.AnnotationElementMatcher
import org.luckypray.dexkit.query.matchers.AnnotationEncodeArrayMatcher
import org.luckypray.dexkit.query.matchers.AnnotationMatcher
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher

internal object IntlBottomAiTabHideHook {
    private val hookState = HookState()

    internal fun hook(cl: ClassLoader) {
        if (!isEnabled()) {
            XposedCompat.log("[IntlBottomAiTabHideHook] skipped: config disabled")
            return
        }
        val mod = XposedCompat.module ?: return
        if (!hookState.markInstalled()) return

        try {
            val method = IntlBottomAiTabModeDexKitResolver.resolve(cl) ?: run {
                XposedCompat.log("[IntlBottomAiTabHideHook] getAiCloudTabMode equivalent NOT FOUND")
                hookState.reset()
                return
            }
            mod.hook(method).intercept {
                if (isEnabled()) 0L else it.proceed()
            }
            XposedCompat.log(
                "[IntlBottomAiTabHideHook] hook INSTALLED: ${method.declaringClass.name}.${method.name}",
            )
        } catch (t: Throwable) {
            hookState.reset()
            XposedCompat.log("[IntlBottomAiTabHideHook] install FAILED: ${t.message}")
            XposedCompat.log(t)
        }
    }

    private fun isEnabled(): Boolean =
        HookSettings.isBottomBarCustomEnabled && HookSettings.isBottomBarTabAigcHidden
}

internal object IntlBottomAiTabModeDexKitResolver {
    const val CACHE_ID = "intl_bottom_ai_tab_mode_v1"

    private const val TAG = "IntlBottomAiTabModeDexKitResolver"
    private const val KOTLIN_METADATA = "kotlin.Metadata"
    private const val STABLE_CLASS_NAME = "pz._"
    private const val STABLE_METHOD_NAME = "_"

    private val metadataTokens = listOf(
        "AI_CLOUD_TAB_NODE",
        "AMIS_AI_CLOUD_MODE_DEFAULT",
        "AMIS_AI_CLOUD_MODE",
        "AMIS_VIP_MODE",
        "AMIS_YIKE_MODE",
        "aiCloudTabMode",
        "getAiCloudTabMode",
        "()J",
    )

    private data class DexMethodCandidate(
        val className: String,
        val methodName: String,
        val returnTypeName: String,
        val paramTypeNames: List<String>,
        val isConstructor: Boolean,
        val modifiers: Int,
    ) {
        fun memberName(): String = "$className.$methodName"
    }

    fun warmUpDexKitCache(cl: ClassLoader): Boolean {
        return resolve(cl) != null
    }

    fun resolve(cl: ClassLoader): Method? {
        when (val cached = DexKitCompat.getCachedMethod(TAG, CACHE_ID) { ref ->
            validateRef(cl, ref)
        }) {
            is DexKitCompat.CachedResult.Found -> return cached.value
            DexKitCompat.CachedResult.NotFound -> return resolveStableFallback(cl)
            DexKitCompat.CachedResult.Miss -> Unit
        }

        val candidates = DexKitCompat.withBridge(TAG, cl, resolverId = CACHE_ID) { bridge ->
            bridge.setThreadNum(1)
            bridge.findClass(
                FindClass.create()
                    .matcher(aiCloudTabOwnerMatcher()),
            ).flatMap { classData ->
                classData.findMethod(
                    FindMethod.create()
                        .matcher(tabModeGetterMatcher()),
                )
            }.map { methodData ->
                DexMethodCandidate(
                    className = methodData.className,
                    methodName = methodData.name,
                    returnTypeName = methodData.returnTypeName,
                    paramTypeNames = methodData.paramTypeNames,
                    isConstructor = methodData.isConstructor,
                    modifiers = methodData.modifiers,
                )
            }
        } ?: return resolveStableFallback(cl)

        val rejected = mutableListOf<String>()
        val matches = candidates.mapNotNull { candidate ->
            if (!candidate.isTabModeGetterShape()) return@mapNotNull null
            val method = validateCandidate(cl, candidate, rejected) ?: return@mapNotNull null
            candidate to method
        }.sortedWith(
            compareByDescending<Pair<DexMethodCandidate, Method>> {
                if (it.first.className == STABLE_CLASS_NAME) 1 else 0
            }.thenBy { it.first.className }.thenBy { it.first.methodName },
        )

        val best = matches.firstOrNull()
        if (best == null) {
            val diagnostic = buildDiagnostic(candidates, matches, rejected)
            XposedCompat.logW("[$TAG] getAiCloudTabMode unresolved: $diagnostic")
            DexKitCompat.markTargetError(TAG, CACHE_ID, diagnostic)
            DexKitCompat.putCachedMethod(TAG, CACHE_ID, null)
            return resolveStableFallback(cl)
        }

        val method = best.second
        DexKitCompat.putCachedMethod(
            TAG,
            CACHE_ID,
            DexKitCompat.MethodRef(method.declaringClass.name, method.name),
        )
        XposedCompat.log("[$TAG] resolved getAiCloudTabMode: ${method.declaringClass.name}.${method.name}")
        return method
    }

    private fun resolveStableFallback(cl: ClassLoader): Method? {
        return validateRef(
            cl,
            DexKitCompat.MethodRef(STABLE_CLASS_NAME, STABLE_METHOD_NAME),
        )?.also { method ->
            DexKitCompat.markTargetSuccess(
                TAG,
                CACHE_ID,
                "fallback:${method.declaringClass.name}.${method.name}",
            )
        }
    }

    private fun validateCandidate(
        cl: ClassLoader,
        candidate: DexMethodCandidate,
        rejected: MutableList<String>,
    ): Method? {
        val method = validateRef(
            cl,
            DexKitCompat.MethodRef(candidate.className, candidate.methodName),
        )
        if (method == null) {
            rejected += "${candidate.memberName()} rejected: metadata/signature mismatch"
        }
        return method
    }

    private fun validateRef(cl: ClassLoader, ref: DexKitCompat.MethodRef): Method? {
        val clazz = XposedCompat.findClassOrNull(ref.className, cl) ?: return null
        if (!KotlinMetadataUtils.metadataContainsAll(clazz, metadataTokens)) return null
        return clazz.declaredMethods.firstOrNull { method ->
            method.name == ref.methodName && isTabModeGetter(method)
        }?.apply { isAccessible = true }
    }

    private fun aiCloudTabOwnerMatcher(): ClassMatcher {
        return ClassMatcher.create()
            .addAnnotation(
                AnnotationMatcher.create()
                    .type(KOTLIN_METADATA)
                    .addElement(
                        AnnotationElementMatcher.create()
                            .name("d2")
                            .arrayValue(
                                AnnotationEncodeArrayMatcher.create().apply {
                                    metadataTokens.forEach(::addString)
                                },
                            ),
                    ),
            )
            .addMethod(tabModeGetterMatcher())
    }

    private fun tabModeGetterMatcher(): MethodMatcher {
        return MethodMatcher.create()
            .modifiers(Modifier.STATIC)
            .returnType(Long::class.javaPrimitiveType!!)
            .paramTypes()
    }

    private fun DexMethodCandidate.isTabModeGetterShape(): Boolean =
        !isConstructor &&
            Modifier.isStatic(modifiers) &&
            returnTypeName == "long" &&
            paramTypeNames.isEmpty()

    private fun isTabModeGetter(method: Method): Boolean =
        Modifier.isStatic(method.modifiers) &&
            method.returnType == Long::class.javaPrimitiveType &&
            method.parameterTypes.isEmpty()

    private fun buildDiagnostic(
        candidates: List<DexMethodCandidate>,
        matches: List<Pair<DexMethodCandidate, Method>>,
        rejected: List<String>,
    ): String {
        val topCandidates = candidates.take(5)
            .joinToString("\n") { candidate ->
                "${candidate.memberName()} ${candidate.returnTypeName}(${candidate.paramTypeNames.joinToString()})"
            }
            .ifBlank { "-" }
        val topMatches = matches.take(5)
            .joinToString("\n") { (candidate, method) ->
                "${candidate.memberName()} -> ${method.declaringClass.name}.${method.name}"
            }
            .ifBlank { "-" }
        val rejectedText = rejected.take(5).joinToString("\n").ifBlank { "-" }
        return buildString {
            append("candidateCount=").append(candidates.size).append('\n')
            append("matchCount=").append(matches.size).append('\n')
            append("topCandidates=\n").append(topCandidates).append('\n')
            append("topMatches=\n").append(topMatches).append('\n')
            append("rejected=\n").append(rejectedText)
        }
    }
}
