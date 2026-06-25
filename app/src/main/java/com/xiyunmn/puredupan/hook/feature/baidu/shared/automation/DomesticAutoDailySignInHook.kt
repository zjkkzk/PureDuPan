package com.xiyunmn.puredupan.hook.feature.baidu.shared.automation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.runtime.AutoDailySignInRuntime
import com.xiyunmn.puredupan.hook.symbols.baidu.shared.BaiduAutomationHookPoints
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

internal object DomesticAutoDailySignInHook {
    private const val TAG = "DomesticAutoDailySignInHook"
    private const val RESULT_CODE_SUCCESS = 1
    private const val RESULT_KEY = "com.mars.RESULT"
    private const val SIGN_IN_STATUS_LOOKBACK_MS = 604_800_000L
    private const val SIGN_IN_STATUS_TIMEOUT_MS = 12_000L
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val membershipSignInRunning = AtomicBoolean(false)

    fun hook(cl: ClassLoader) {
        AutoDailySignInRuntime.registerManualTrigger { context ->
            val activity = findActivity(context)
            if (activity == null) {
                XposedCompat.logW("[$TAG] manual sign-in skipped: activity unavailable")
                false
            } else {
                XposedCompat.log("[$TAG] manual sign-in requested")
                run(activity, cl, force = true)
                true
            }
        }
        AutoDailySignInScheduler.install(cl, TAG) { activity ->
            run(activity, cl)
        }
    }

    private fun run(activity: Activity, cl: ClassLoader, force: Boolean = false) {
        if (!force && !HookSettings.isAutoDailySignInEnabled) return

        val context = activity.applicationContext ?: activity
        val account = AccountAccess.resolve(cl)
        if (account == null) {
            if (AutoDailySignInStateStore.beginAttempt(context, null, TAG, force = force)) {
                AutoDailySignInStateStore.markFailed(context, null, TAG, "account state unavailable")
            }
            return
        }
        if (!account.isLogin) {
            if (force && AutoDailySignInStateStore.beginAttempt(context, account.uid, TAG, force = true)) {
                AutoDailySignInStateStore.markFailed(context, account.uid, TAG, "account not logged in")
                return
            }
            XposedCompat.logD("[$TAG] auto sign-in skipped: account not logged in")
            return
        }
        if (!AutoDailySignInStateStore.beginAttempt(context, account.uid, TAG, force = force)) return

        runMembershipSignIn(
            context = context,
            account = account,
            cookieAccess = DomesticCookieAccess.resolve(cl),
            statusAccess = SignInStatusAccess.resolve(cl),
            force = force,
        )
    }

    private fun runMembershipSignIn(
        context: Context,
        account: AccountState,
        cookieAccess: DomesticCookieAccess?,
        statusAccess: SignInStatusAccess?,
        force: Boolean,
    ) {
        val uid = account.uid
        val bduss = account.bduss
        if (bduss.isNullOrBlank()) {
            AutoDailySignInStateStore.markFailed(context, uid, TAG, "bduss unavailable")
            return
        }
        if (!membershipSignInRunning.compareAndSet(false, true)) {
            AutoDailySignInStateStore.markRetryableFailed(context, uid, TAG, "membership sign-in already running")
            return
        }

        fun startRequest() {
            Thread({
                try {
                    if (!force && !HookSettings.isAutoDailySignInEnabled) {
                        AutoDailySignInStateStore.markSkipped(context, uid, TAG, "disabled before membership request")
                        return@Thread
                    }
                    val cookie = cookieAccess?.cookieFor(bduss).takeUnless { it.isNullOrBlank() } ?: "BDUSS=$bduss"
                    when (val result = MembershipSignInClient.signIn(cookie, TAG)) {
                        is MembershipSignInResult.Success -> {
                            AutoDailySignInStateStore.markSuccess(
                                context,
                                uid,
                                TAG,
                                "membership endpoint signed in, points=${result.points ?: "unknown"}",
                            )
                        }
                        is MembershipSignInResult.AlreadySignedIn -> {
                            AutoDailySignInStateStore.markAlreadySignedIn(
                                context,
                                uid,
                                TAG,
                                "membership endpoint reports already signed: ${result.message}",
                            )
                        }
                        is MembershipSignInResult.Failed -> {
                            confirmMembershipFailure(
                                context = context,
                                account = account,
                                statusAccess = statusAccess,
                                fallbackDetail = result.detail,
                            )
                        }
                    }
                } finally {
                    membershipSignInRunning.set(false)
                }
            }, "$TAG-MembershipSignIn").start()
        }

        if (!force && statusAccess != null && !uid.isNullOrBlank()) {
            statusAccess.queryTodaySignedIn(context, bduss, uid) { signedIn ->
                if (signedIn == true) {
                    membershipSignInRunning.set(false)
                    AutoDailySignInStateStore.markAlreadySignedIn(
                        context,
                        uid,
                        TAG,
                        "signin list reports already signed before membership request",
                    )
                } else {
                    startRequest()
                }
            }
        } else {
            startRequest()
        }
    }

    private fun confirmMembershipFailure(
        context: Context,
        account: AccountState,
        statusAccess: SignInStatusAccess?,
        fallbackDetail: String,
    ) {
        val uid = account.uid
        val bduss = account.bduss
        if (statusAccess == null || uid.isNullOrBlank() || bduss.isNullOrBlank()) {
            AutoDailySignInStateStore.markRetryableFailed(context, uid, TAG, fallbackDetail)
            return
        }

        statusAccess.queryTodaySignedIn(context, bduss, uid) { signedIn ->
            if (signedIn == true) {
                AutoDailySignInStateStore.markAlreadySignedIn(
                    context,
                    uid,
                    TAG,
                    "$fallbackDetail, signin list reports already signed after membership request",
                )
            } else {
                AutoDailySignInStateStore.markRetryableFailed(context, uid, TAG, fallbackDetail)
            }
        }
    }

    private fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return current as? Activity
    }

    private data class AccountState(
        val isLogin: Boolean,
        val uid: String?,
        val bduss: String?,
    )

    private object AccountAccess {
        fun resolve(cl: ClassLoader): AccountState? {
            return runCatching {
                val clazz = XposedCompat.findClassOrNull(BaiduAutomationHookPoints.ACCOUNT_UTILS, cl) ?: return null
                val getInstance = XposedCompat.findMethodOrNull(clazz, "getInstance") ?: return null
                val instance = getInstance.invoke(null) ?: return null
                val isLogin = XposedCompat.findMethodOrNull(clazz, "isLogin")
                    ?.invoke(instance) as? Boolean ?: return null
                val uid = XposedCompat.findMethodOrNull(clazz, "getUid")
                    ?.invoke(instance) as? String
                val bduss = XposedCompat.findMethodOrNull(clazz, "getBduss")
                    ?.invoke(instance) as? String
                AccountState(isLogin = isLogin, uid = uid, bduss = bduss)
            }.getOrElse { t ->
                XposedCompat.logD("[$TAG] account state resolve failed: ${t.message}")
                null
            }
        }
    }

    private class DomesticCookieAccess(
        private val getCookieByBduss: Method,
    ) {
        fun cookieFor(bduss: String): String {
            return runCatching { getCookieByBduss.invoke(null, bduss) as? String }
                .getOrElse { t ->
                    XposedCompat.logD("[$TAG] getCookieByBduss failed: ${t.message}")
                    null
                }
                .orEmpty()
        }

        companion object {
            fun resolve(cl: ClassLoader): DomesticCookieAccess? {
                return runCatching {
                    val cookieUtilsClass = XposedCompat.findClassOrNull(
                        BaiduAutomationHookPoints.COOKIE_UTILS,
                        cl,
                    ) ?: return null
                    val getCookieByBduss = XposedCompat.findMethodOrNull(
                        cookieUtilsClass,
                        "getCookieByBduss",
                        String::class.java,
                    ) ?: return null
                    DomesticCookieAccess(getCookieByBduss)
                }.getOrElse { t ->
                    XposedCompat.logD("[$TAG] cookie access resolve failed: ${t.message}")
                    null
                }
            }
        }
    }

    private enum class SignInStatusResult {
        ALREADY_SIGNED,
        NOT_SIGNED,
        UNKNOWN,
    }

    private class SignInStatusAccess(
        private val managerConstructor: java.lang.reflect.Constructor<*>,
        private val getSigninList: Method,
        private val responseClass: Class<*>,
        private val getTodaySignined: Method,
    ) {
        fun queryTodaySignedIn(
            context: Context,
            bduss: String,
            uid: String,
            callback: (Boolean?) -> Unit,
        ) {
            val appContext = context.applicationContext ?: context
            val once = AtomicBoolean(false)

            fun finish(result: SignInStatusResult) {
                if (!once.compareAndSet(false, true)) return
                val signedIn = when (result) {
                    SignInStatusResult.ALREADY_SIGNED -> true
                    SignInStatusResult.NOT_SIGNED -> false
                    SignInStatusResult.UNKNOWN -> null
                }
                callback(signedIn)
            }

            val receiver = object : ResultReceiver(mainHandler) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    val result = parseResult(resultCode, resultData)
                    XposedCompat.logD("[$TAG] signin list status result: $result")
                    finish(result)
                }
            }

            runCatching {
                val manager = managerConstructor.newInstance()
                val (startTime, endTime) = signInDateRange()
                getSigninList.invoke(manager, appContext, receiver, bduss, uid, startTime, endTime)
                mainHandler.postDelayed({
                    if (!once.get()) {
                        XposedCompat.logD("[$TAG] signin list status query timeout")
                        finish(SignInStatusResult.UNKNOWN)
                    }
                }, SIGN_IN_STATUS_TIMEOUT_MS)
            }.onFailure { t ->
                XposedCompat.logD("[$TAG] signin list status query failed: ${t.message}")
                finish(SignInStatusResult.UNKNOWN)
            }
        }

        private fun parseResult(resultCode: Int, resultData: Bundle?): SignInStatusResult {
            if (resultCode != RESULT_CODE_SUCCESS || resultData == null) {
                return SignInStatusResult.UNKNOWN
            }
            return runCatching {
                resultData.classLoader = responseClass.classLoader
                @Suppress("DEPRECATION")
                val response = resultData.getParcelable<android.os.Parcelable>(RESULT_KEY)
                when (getTodaySignined.invoke(response) as? Boolean) {
                    true -> SignInStatusResult.ALREADY_SIGNED
                    false -> SignInStatusResult.NOT_SIGNED
                    null -> SignInStatusResult.UNKNOWN
                }
            }.getOrElse { t ->
                XposedCompat.logD("[$TAG] signin list status parse failed: ${t.message}")
                SignInStatusResult.UNKNOWN
            }
        }

        companion object {
            fun resolve(cl: ClassLoader): SignInStatusAccess? {
                return runCatching {
                    val managerClass = XposedCompat.findClassOrNull(
                        BaiduAutomationHookPoints.MY_POINT_MANAGER,
                        cl,
                    ) ?: return null
                    val responseClass = XposedCompat.findClassOrNull(
                        BaiduAutomationHookPoints.SIGNIN_LIST_RESPONSE,
                        cl,
                    ) ?: return null
                    val managerConstructor = managerClass.getDeclaredConstructor()
                    managerConstructor.isAccessible = true
                    val getSigninList = XposedCompat.findMethodOrNull(
                        managerClass,
                        "getSigninList",
                        Context::class.java,
                        ResultReceiver::class.java,
                        String::class.java,
                        String::class.java,
                        String::class.java,
                        String::class.java,
                    ) ?: return null
                    val getTodaySignined = XposedCompat.findMethodOrNull(
                        responseClass,
                        "getTodaySignined",
                    ) ?: return null
                    SignInStatusAccess(
                        managerConstructor = managerConstructor,
                        getSigninList = getSigninList,
                        responseClass = responseClass,
                        getTodaySignined = getTodaySignined,
                    )
                }.getOrElse { t ->
                    XposedCompat.logD("[$TAG] signin status access resolve failed: ${t.message}")
                    null
                }
            }
        }
    }

    private fun signInDateRange(): Pair<String, String> {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = System.currentTimeMillis()
        return formatter.format(Date(now - SIGN_IN_STATUS_LOOKBACK_MS)) to formatter.format(Date(now))
    }
}
