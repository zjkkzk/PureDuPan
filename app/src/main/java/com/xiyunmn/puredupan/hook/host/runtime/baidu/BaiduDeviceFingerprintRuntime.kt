package com.xiyunmn.puredupan.hook.host.runtime.baidu

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.xiyunmn.puredupan.hook.host.HostDeviceFingerprintCollector
import com.xiyunmn.puredupan.hook.symbols.baidu.shared.BaiduDeviceFingerprintSymbols
import java.lang.reflect.Modifier

internal object BaiduDomesticDeviceFingerprintRuntime : HostDeviceFingerprintCollector {
    override fun collect(context: Context): Map<String, Any?> {
        return BaiduDeviceFingerprintCollector.collect(context, "domestic")
    }
}

internal object BaiduIntlDeviceFingerprintRuntime : HostDeviceFingerprintCollector {
    override fun collect(context: Context): Map<String, Any?> {
        return BaiduDeviceFingerprintCollector.collect(context, "intl")
    }
}

private object BaiduDeviceFingerprintCollector {
    fun collect(context: Context, profile: String): Map<String, Any?> {
        return linkedMapOf<String, Any?>().apply {
            put("collectorProfile", profile)
            putIfNotEmpty("deviceFingerprint", collectDeviceInfo(context))
            putIfNotEmpty("riskFingerprint", collectRiskFingerprint(context))
        }
    }

    @SuppressLint("HardwareIds")
    private fun collectDeviceInfo(context: Context): Map<String, Any?> {
        val fields = linkedMapOf<String, Any?>()
        fields.putIfUseful(
            "Android ID",
            runCatching {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            }.getOrNull(),
        )
        for ((outputKey, fieldName) in BaiduDeviceFingerprintSymbols.appCommonStringFields) {
            fields.putIfUseful(
                outputKey,
                readStaticField(
                    context = context,
                    className = BaiduDeviceFingerprintSymbols.APP_COMMON,
                    fieldName = fieldName,
                ).getOrNull(),
            )
        }
        fields.putIfUseful(
            "CUID",
            invokeStaticContextMethod(
                context = context,
                className = BaiduDeviceFingerprintSymbols.DEVICE_ID,
                methodName = "getCUID",
            ).getOrNull(),
        )
        fields.putIfUseful(
            "Device ID",
            invokeStaticContextMethod(
                context = context,
                className = BaiduDeviceFingerprintSymbols.DEVICE_ID,
                methodName = "getDeviceID",
            ).getOrNull(),
        )
        fields.putIfUseful(
            "CommonParam CUID",
            invokeStaticContextMethod(
                context = context,
                className = BaiduDeviceFingerprintSymbols.COMMON_PARAM,
                methodName = "getCUID",
            ).getOrNull(),
        )
        fields.putIfUseful("Swan UUID", readSwanUuid(context).getOrNull())
        return fields
    }

    private fun collectRiskFingerprint(context: Context): Map<String, Any?> {
        return linkedMapOf<String, Any?>().apply {
            putIfUseful(
                "Sofire Version",
                invokeStaticContextMethod(
                    context = context,
                    className = BaiduDeviceFingerprintSymbols.SOFIRE_FH,
                    methodName = "getVersion",
                ).recoverCatching {
                    invokeStaticNoArgMethod(
                        context = context,
                        className = BaiduDeviceFingerprintSymbols.SOFIRE_FH,
                        methodName = "getVersion",
                    ).getOrThrow()
                }.getOrNull(),
            )
            putIfUseful(
                "Sofire GD",
                invokeStaticContextMethod(
                    context = context,
                    className = BaiduDeviceFingerprintSymbols.SOFIRE_FH,
                    methodName = "gd",
                ).getOrNull(),
            )
        }
    }

    private fun MutableMap<String, Any?>.putIfNotEmpty(key: String, value: Map<String, Any?>) {
        if (value.isNotEmpty()) {
            this[key] = value
        }
    }

    private fun MutableMap<String, Any?>.putIfUseful(key: String, value: Any?) {
        val text = value?.toString()?.trim()
        if (!text.isNullOrEmpty()) {
            this[key] = value
        }
    }

    private fun readStaticField(context: Context, className: String, fieldName: String): Result<Any?> {
        return runCatching {
            val clazz = loadClass(context, className)
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            if (!Modifier.isStatic(field.modifiers)) {
                error("$className.$fieldName is not static")
            }
            field.get(null)
        }
    }

    private fun invokeStaticContextMethod(context: Context, className: String, methodName: String): Result<Any?> {
        return invokeStaticMethod(
            context = context,
            className = className,
            methodName = methodName,
            parameterTypes = arrayOf<Class<*>>(Context::class.java),
            args = arrayOf<Any>(context.applicationContext ?: context),
        )
    }

    private fun invokeStaticNoArgMethod(context: Context, className: String, methodName: String): Result<Any?> {
        return invokeStaticMethod(
            context = context,
            className = className,
            methodName = methodName,
            parameterTypes = emptyArray<Class<*>>(),
            args = emptyArray<Any>(),
        )
    }

    private fun invokeStaticMethod(
        context: Context,
        className: String,
        methodName: String,
        parameterTypes: Array<Class<*>>,
        args: Array<Any>,
    ): Result<Any?> {
        return runCatching {
            val clazz = loadClass(context, className)
            val method = clazz.getDeclaredMethod(methodName, *parameterTypes)
            method.isAccessible = true
            if (!Modifier.isStatic(method.modifiers)) {
                error("$className.$methodName is not static")
            }
            method.invoke(null, *args)
        }
    }

    private fun readSwanUuid(context: Context): Result<Any?> {
        return runCatching {
            val clazz = loadClass(context, BaiduDeviceFingerprintSymbols.SWAN_UUID)
            val ofMethod = clazz.getDeclaredMethod("of", Context::class.java)
            ofMethod.isAccessible = true
            val instance = ofMethod.invoke(null, context.applicationContext ?: context)
                ?: return@runCatching null
            val getUuidMethod = clazz.getDeclaredMethod("getUUID")
            getUuidMethod.isAccessible = true
            getUuidMethod.invoke(instance)
        }
    }

    private fun loadClass(context: Context, className: String): Class<*> {
        return Class.forName(className, false, context.classLoader)
    }
}
