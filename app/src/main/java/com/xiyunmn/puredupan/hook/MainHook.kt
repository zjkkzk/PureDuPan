package com.xiyunmn.puredupan.hook

import android.content.Context
import com.xiyunmn.puredupan.hook.config.runtime.HookSettings
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.plan.HookInstaller
import com.xiyunmn.puredupan.hook.runtime.HostLoadRuntime
import com.xiyunmn.puredupan.hook.runtime.HostLoadSession
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import com.xiyunmn.puredupan.hook.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class MainHook : XposedModule() {

    private val sAppContext = AtomicReference<Context?>(null)
    private val sAttachHookInstalled = AtomicBoolean(false)
    private val sStaticHooksInstalled = AtomicBoolean(false)
    private val sPostAttachStaticHooksInstalled = AtomicBoolean(false)
    private var processName: String = ""

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        super.onModuleLoaded(param)
        XposedCompat.module = this
        processName = param.processName
        XposedCompat.setProcessName(param.processName)
        XposedCompat.log("[MainHook] onModuleLoaded: process=${param.processName}")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)
        XposedCompat.log("[MainHook] onPackageLoaded: pkg=${param.packageName}")

        HostLoadRuntime.resolve(param.packageName) ?: return
        XposedCompat.setCurrentPackageName(param.packageName)
    }

    override fun onPackageReady(param: PackageReadyParam) {
        super.onPackageReady(param)
        XposedCompat.log("[MainHook] onPackageReady: pkg=${param.packageName}, process=$processName")

        val hostSession = HostLoadRuntime.resolve(param.packageName)
        if (hostSession == null) {
            XposedCompat.log("[MainHook] onPackageReady: SKIP - non-target package (${param.packageName})")
            return
        }
        XposedCompat.setCurrentPackageName(param.packageName)
        if (!hostSession.shouldHandleProcess(processName)) {
            XposedCompat.log("[MainHook] onPackageReady: SKIP - non-target process ($processName)")
            return
        }
        val cl = param.classLoader
        XposedCompat.log("[MainHook] onPackageReady: using app classloader=$cl")

        handleLoadPackage(hostSession, cl)
    }

    private fun handleLoadPackage(hostSession: HostLoadSession, cl: ClassLoader) {
        XposedCompat.log(
            "[MainHook] handleLoadPackage: pkg=${hostSession.packageName}, cl=$cl, host=${hostSession.hostId}",
        )
        val staticPlan = hostSession.staticPlan(processName)
        if (staticPlan.isEmpty()) {
            XposedCompat.logD("[MainHook] static hook plan empty for process=$processName, skip")
        } else if (sStaticHooksInstalled.compareAndSet(false, true)) {
            try {
                XposedCompat.log("[MainHook] initialized. version=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")
                HookInstaller.install(staticPlan, cl)
                XposedCompat.log("[MainHook] All static hooks dispatched.")
            } catch (e: Exception) {
                sStaticHooksInstalled.set(false)
                XposedCompat.log("[MainHook] static hook install FAILED: ${e.message}")
                XposedCompat.log(e)
            } catch (e: Error) {
                sStaticHooksInstalled.set(false)
                XposedCompat.logE("[MainHook] static hook install FATAL ERROR: ${e.message}")
                throw e
            }
        } else {
            XposedCompat.log("[MainHook] static hooks already installed, skip duplicate install")
        }

        if (!hostSession.shouldInstallAttachHook(processName)) {
            XposedCompat.logD("[MainHook] Application.attach hook skipped for process=$processName")
            return
        }

        if (!sAttachHookInstalled.compareAndSet(false, true)) {
            XposedCompat.log("[MainHook] Application.attach hook already installed, skip")
            return
        }

        try {
            val attachMethod = android.app.Application::class.java
                .getDeclaredMethod("attach", Context::class.java)
            attachMethod.isAccessible = true
            XposedCompat.log("[MainHook] Application.attach method found, installing hook...")

            hook(attachMethod).intercept { chain ->
                XposedCompat.log("[MainHook] > Application.attach INTERCEPTED, thisObj=${chain.thisObject?.javaClass?.name}")
                val result = chain.proceed()
                XposedCompat.log("[MainHook] > Application.attach proceed() returned")

                if (sAppContext.get() == null) {
                    val app = chain.thisObject as? android.app.Application
                    if (app != null) {
                        sAppContext.set(app)
                        HookSettings.initialize(app)
                        XposedCompat.log("[MainHook] > settings initialized, app=${app.packageName}")
                    }
                }

                if (sPostAttachStaticHooksInstalled.compareAndSet(false, true)) {
                    val settings = HookSettings.settingsSnapshot()
                    HookInstaller.install(
                        hostSession.postAttachPlan(processName, settings),
                        cl,
                    )
                    hostSession.startDexKitWarmUp(
                        processName = processName,
                        settings = settings,
                        classLoader = cl,
                    )
                }

                result
            }
            XposedCompat.log("[MainHook] Application.attach hook INSTALLED")
        } catch (e: Exception) {
            sAttachHookInstalled.set(false)
            XposedCompat.log("[MainHook] FAILED to hook Application.attach: ${e.message}")
            XposedCompat.log(e)
        } catch (e: Error) {
            sAttachHookInstalled.set(false)
            XposedCompat.logE("[MainHook] FATAL ERROR in Application.attach hook: ${e.message}")
            throw e
        }
    }

    // Removed: markAttachHookInstalled, markStaticHooksInstalled, markPostAttachStaticHooksInstalled
    // These are now replaced by AtomicBoolean.compareAndSet() inline

}
