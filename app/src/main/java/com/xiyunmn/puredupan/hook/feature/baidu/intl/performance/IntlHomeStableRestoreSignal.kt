package com.xiyunmn.puredupan.hook.feature.baidu.intl.performance

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.feature.baidu.shared.runtime.BaiduFeatureRuntime

internal object IntlHomeStableRestoreSignal {
    private const val HOME_STABLE_REASON = "home_stable"
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    fun hook(cl: ClassLoader, tag: String, onHomeStable: () -> Unit): Boolean {
        val mod = XposedCompat.module ?: return false
        val mainActivityClassName = BaiduFeatureRuntime.currentMainActivityClassName() ?: run {
            XposedCompat.log("[$tag] MainActivity host capability missing")
            return false
        }
        val mainActivityClass = XposedCompat.findClassOrNull(mainActivityClassName, cl) ?: run {
            XposedCompat.log("[$tag] MainActivity class NOT FOUND")
            return false
        }
        val focusMethod = XposedCompat.findMethodOrNull(
            mainActivityClass,
            "onWindowFocusChanged",
            Boolean::class.javaPrimitiveType!!,
        ) ?: run {
            XposedCompat.log("[$tag] MainActivity.onWindowFocusChanged NOT FOUND")
            return false
        }

        mod.hook(focusMethod).intercept { chain ->
            val result = chain.proceed()
            val activity = chain.thisObject as? Activity
            val hasFocus = chain.args.firstOrNull() as? Boolean ?: false
            if (hasFocus && activity?.javaClass?.name == mainActivityClassName) {
                onHomeStable()
            }
            result
        }
        return true
    }

    fun scheduleDelayedRestore(
        tag: String,
        delayMs: Long,
        tryMarkScheduled: () -> Boolean,
        clearScheduled: () -> Unit,
        restore: (String) -> Unit,
    ) {
        if (!tryMarkScheduled()) return
        mainHandler.postDelayed({
            clearScheduled()
            restore(HOME_STABLE_REASON)
        }, delayMs)
        XposedCompat.logD("[$tag] home stable restore scheduled")
    }
}
