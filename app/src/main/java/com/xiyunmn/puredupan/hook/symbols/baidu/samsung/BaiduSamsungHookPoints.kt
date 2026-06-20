package com.xiyunmn.puredupan.hook.symbols.baidu.samsung

/**
 * Stable hook points verified against Baidu Netdisk Samsung host.
 */
internal object BaiduSamsungHookPoints {
    const val NEW_FEED_HOME_TITLE_BAR_FRAGMENT =
        "com.baidu.netdisk.newfeedhome.feedhome.ui.view.fragment.FHTitleBarFragment"

    val FEED_FRAGMENT_CLASSES = listOf(
        "com.baidu.netdisk.newfeedhome.feedhome.ui.view.fragment.FHFeedFragment",
        "com.baidu.netdisk.feedhome.ui.view.fragment.FHFeedFragment",
    )
    const val HOME_STORY_CARD_VIEW =
        "com.baidu.netdisk.newstory.ui.view.home.HomeStoryCardView"
    val HOME_SAVE_CARD_VIEWS = listOf(
        "com.baidu.netdisk.newfeedhome.feedhome.ui.view.fragment.NewHomeSaveCardView",
    )
    val HOME_RECENT_CARD_VIEWS = listOf(
        "com.baidu.netdisk.newfeedhome.feedhome.ui.view.fragment.NewHomeRecentCardView",
    )

    const val ABOUT_ME_TOP_FRAGMENT =
        "com.baidu.netdisk.ui.aboutme.view.AboutMeTopFragment"
    const val ABOUT_ME_TOP_FRAGMENT_ON_VIEW_CREATED_METHOD = "onViewCreated"

    const val SPLASH_MANAGER =
        "com.baidu.netdisk.advertise.splash.SplashManager"
    const val ADVERTISE_HOT_START_MANAGER =
        "com.baidu.netdisk.advertise.AdvertiseHotStartManager"

    const val BUSINESS_OP_DIALOG =
        "com.baidu.netdisk.ui.operation.BusinessOPDialog"
    const val BUSINESS_OP_DIALOG_SHOW_DIALOG_METHOD = "showDialog"
    const val BUSINESS_OP_DIALOG_ON_CREATE_VIEW_METHOD = "onCreateView"

    const val ABOUT_ME_GAME_CENTER_FRAGMENT =
        "com.baidu.netdisk.operation.ui.fragment.game.AboutMeGameCenterFragment"
    const val GAME_CENTER_VIEW_MODEL =
        "com.baidu.netdisk.ui.aboutme.viewmodel.GameCenterViewModel"
    const val GAME_CENTER_AMIS_OPEN_METHOD = "isAmisOpen"
    const val GAME_CENTER_FETCH_CONFIG_METHOD = "fetchGameCenterConfig"
    const val ANDROIDX_FRAGMENT_MANAGER = "androidx.fragment.app.FragmentManager"
    const val ANDROIDX_LIFECYCLE_OWNER = "androidx.lifecycle.LifecycleOwner"

    const val OEM_PUSH_ON_CREATE_METHOD = "onCreate"
    const val OEM_PUSH_ON_START_COMMAND_METHOD = "onStartCommand"
    const val OEM_PUSH_ON_START_METHOD = "onStart"
    const val OEM_PUSH_ON_BIND_METHOD = "onBind"
    const val OEM_PUSH_ON_HANDLE_INTENT_METHOD = "onHandleIntent"
    const val OEM_PUSH_ON_RECEIVE_METHOD = "onReceive"
    const val OEM_PUSH_ON_MESSAGE_RECEIVED_METHOD = "onMessageReceived"
    const val OEM_PUSH_ON_NEW_TOKEN_METHOD = "onNewToken"
    const val OEM_PUSH_ON_TOKEN_ERROR_METHOD = "onTokenError"
    const val OEM_PUSH_HMS_REMOTE_MESSAGE = "com.huawei.hms.push.RemoteMessage"
    const val OEM_PUSH_HONOR_MESSAGE = "com.hihonor.push.sdk.HonorPushDataMsg"

    val OEM_PUSH_ON_START_COMMAND_SERVICE_CLASSES = listOf(
        "com.huawei.hms.support.api.push.service.HmsMsgService",
        "com.baidu.techain.push.HWPushMsgService",
        "com.heytap.msp.push.service.DataMessageCallbackService",
        "com.heytap.msp.push.service.CompatibleDataMessageCallbackService",
        "com.xiaomi.push.service.XMPushService",
        "com.vivo.push.sdk.service.CommandService",
    )
    val OEM_PUSH_ON_CREATE_SERVICE_CLASSES = listOf(
        "com.xiaomi.push.service.XMPushService",
        "com.xiaomi.push.service.XMJobService",
        "com.vivo.push.sdk.service.CommandService",
    )
    val OEM_PUSH_ON_START_SERVICE_CLASSES = listOf(
        "com.xiaomi.push.service.XMPushService",
        "com.xiaomi.mipush.sdk.PushMessageHandler",
        "com.xiaomi.mipush.sdk.MessageHandleService",
    )
    val OEM_PUSH_ON_BIND_SERVICE_CLASSES = listOf(
        "com.huawei.hms.support.api.push.service.HmsMsgService",
        "com.baidu.techain.push.HWPushMsgService",
        "com.heytap.msp.push.service.DataMessageCallbackService",
        "com.heytap.msp.push.service.CompatibleDataMessageCallbackService",
        "com.xiaomi.push.service.XMPushService",
        "com.xiaomi.push.service.XMJobService",
        "com.xiaomi.mipush.sdk.PushMessageHandler",
        "com.xiaomi.mipush.sdk.MessageHandleService",
        "com.vivo.push.sdk.service.CommandService",
    )
    val OEM_PUSH_ON_HANDLE_INTENT_SERVICE_CLASSES = listOf(
        "com.meizu.cloud.pushsdk.NotificationService",
    )
    val OEM_PUSH_RECEIVER_CLASSES = listOf(
        "com.huawei.hms.support.api.push.PushMsgReceiver",
        "com.huawei.hms.support.api.push.PushReceiver",
        "com.baidu.techain.push.MIUIPushReceiver",
        "com.baidu.techain.push.MZPushReceiver",
        "com.baidu.techain.push.VIVOPushReceiver",
        "com.xiaomi.push.service.receivers.PingReceiver",
        "com.xiaomi.mipush.sdk.PushMessageReceiver",
        "com.vivo.push.sdk.BasePushMessageReceiver",
        "com.vivo.push.sdk.PushServiceReceiver",
        "com.meizu.cloud.pushsdk.MzPushMessageReceiver",
        "com.meizu.cloud.pushsdk.MzPushSystemReceiver",
    )
    val OEM_PUSH_HUAWEI_MESSAGE_SERVICE_CLASSES = listOf(
        "com.baidu.techain.push.HWPushMsgService",
    )
    val OEM_PUSH_HONOR_MESSAGE_SERVICE_CLASSES = listOf(
        "com.baidu.techain.push.HonorPushMsgService",
    )
}
