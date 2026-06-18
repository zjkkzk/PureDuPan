package com.xiyunmn.puredupan.hook.core

object Constants {
    const val TAG = "WangPanHook"
    const val BAIDU_NETDISK_PACKAGE = "com.baidu.netdisk"
    const val BAIDU_DRIVE_INTL_PACKAGE = "com.baidu.drive.app"

    @Deprecated(
        message = "Use HostRegistry/HostProfile instead of the single-host constant.",
        replaceWith = ReplaceWith("BAIDU_NETDISK_PACKAGE"),
    )
    const val TARGET_PACKAGE = BAIDU_NETDISK_PACKAGE
}
