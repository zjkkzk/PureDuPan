package com.xiyunmn.puredupan.hook.plan

internal interface HookCatalog {
    fun postAttachSpecs(): List<HookSpec>
}
