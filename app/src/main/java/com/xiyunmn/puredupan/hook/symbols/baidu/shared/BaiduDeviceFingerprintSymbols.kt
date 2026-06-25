package com.xiyunmn.puredupan.hook.symbols.baidu.shared

internal object BaiduDeviceFingerprintSymbols {
    const val APP_COMMON = "com.baidu.netdisk.kernel.architecture.AppCommon"
    const val DEVICE_ID = "com.baidu.android.common.util.DeviceId"
    const val COMMON_PARAM = "com.baidu.android.common.util.CommonParam"
    const val SOFIRE_FH = "com.baidu.sofire.ac.FH"
    const val SWAN_UUID = "com.baidu.swan.uuid.SwanUUID"

    val appCommonStringFields: List<Pair<String, String>> = listOf(
        "Host Android ID" to "sAndroidId",
        "OAID" to "OAID",
        "HONOR OAID" to "HONOR_OAID",
        "DEVUID" to "DEVUID",
        "PDEVUID" to "PDEVUID",
        "C3 AID" to "C3_AID",
        "Host Device ID" to "sDeviceId",
        "IMEI" to "sImei",
        "IMSI" to "sImsi",
        "MAC" to "sMac",
        "Channel" to "CHANNEL_NUM",
    )
}
