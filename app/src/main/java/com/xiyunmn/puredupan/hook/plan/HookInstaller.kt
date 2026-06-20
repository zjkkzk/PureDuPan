package com.xiyunmn.puredupan.hook.plan

import com.xiyunmn.puredupan.hook.core.XposedCompat

internal object HookInstaller {
    fun install(plan: HookInstallPlan, cl: ClassLoader) {
        if (plan.entries.isEmpty()) {
            XposedCompat.logD("[HookInstallPlan] ${plan.phase}: empty for process=${plan.processName}")
            return
        }
        for (entry in plan.entries) {
            try {
                XposedCompat.logD("[HookInstallPlan] Installing ${entry.id} (${plan.phase})...")
                entry.install(cl)
            } catch (e: ReflectiveOperationException) {
                XposedCompat.log(
                    "[HookInstallPlan] ${entry.id} install FAILED (${plan.phase}): " +
                        "${e.javaClass.simpleName}: ${e.message}",
                )
                XposedCompat.log(e)
            } catch (e: Exception) {
                XposedCompat.log(
                    "[HookInstallPlan] ${entry.id} install FAILED (${plan.phase}): ${e.message}",
                )
                XposedCompat.log(e)
            } catch (e: Error) {
                XposedCompat.logE(
                    "[HookInstallPlan] ${entry.id} install FATAL ERROR (${plan.phase}): " +
                        "${e.javaClass.simpleName}: ${e.message}",
                )
                XposedCompat.log(e)
            }
        }
    }
}
