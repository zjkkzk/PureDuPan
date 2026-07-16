package com.xiyunmn.puredupan.hook.symbols.baidu.shared

internal object BaiduFilePageHookPoints {
    /**
     * 文件页底部安全提示的渲染入口宿主。
     *
     * 安全提示由 [MY_NETDISK_FRAGMENT].[INIT_SAFETY_BOTTOM_VIEW] inflate
     * `safety_ability_layout` 后 addFooterView，三端稳定未混淆。
     * 命中隐藏开关时在该渲染入口 no-op，从源头阻断 footer 创建，
     * 不再挂 ViewTreeObserver 或递归扫描 View 树。
     */
    const val MY_NETDISK_FRAGMENT =
        "com.baidu.netdisk.ui.cloudfile.MyNetdiskFragment"

    const val INIT_SAFETY_BOTTOM_VIEW = "initSafetyBottomView"
}
