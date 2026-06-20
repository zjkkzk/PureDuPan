package com.xiyunmn.puredupan.hook.plan

internal data class HookInstallEntry(
    val id: String,
    val install: (ClassLoader) -> Unit,
)

internal data class HookInstallPlan(
    val processName: String,
    val phase: String,
    val entries: List<HookInstallEntry>,
) {
    fun isEmpty(): Boolean = entries.isEmpty()
}
