package com.xiyunmn.puredupan.hook.host

internal data class HostProfile(
    val flavor: HostFlavor,
    val packageName: String,
    val applicationClassName: String?,
    val handledProcessNames: Set<String>,
    val attachHookProcessNames: Set<String>,
    val capabilities: HostCapabilities,
) {
    val mainProcessName: String = packageName

    fun handlesPackage(name: String): Boolean = packageName == name

    fun isMainProcess(processName: String): Boolean = processName == mainProcessName

    fun isPushServiceProcess(processName: String): Boolean =
        processName == "$packageName:pushservice"

    fun shouldHandleProcess(processName: String): Boolean =
        handledProcessNames.contains(processName)

    fun shouldInstallAttachHook(processName: String): Boolean =
        attachHookProcessNames.contains(processName)
}
