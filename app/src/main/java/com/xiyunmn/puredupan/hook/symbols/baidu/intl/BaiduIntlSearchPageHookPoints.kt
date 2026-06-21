package com.xiyunmn.puredupan.hook.symbols.baidu.intl

internal object BaiduIntlSearchPageHookPoints {
    const val MAIN_SEARCH_SCREEN_KT = "com.mars.feature.search.main.MainSearchScreenKt"
    const val SEARCH_HINT_VM = "com.mars.feature.search.hint.SearchHintVM"
    const val SEARCH_AI_RECOMMEND_KT = "com.mars.feature.search.hint.SearchAIRecommendKt"
    const val SEARCH_HISTORY_SCREEN_KT =
        "com.mars.feature.search.hint.searchhistory.view.SearchHistoryScreenKt"
    const val COMPOSE_SERVICE_PLATFORM_IMPL =
        "com.mars.data.base.commonintf.dependant.AndroidComposeServicePlatformImpl"
    const val COMPOSER = "androidx.compose.runtime.Composer"
    const val NAV_CONTROLLER = "androidx.navigation.NavController"
    const val TEXT_FIELD_VALUE = "androidx.compose.ui.text.input.TextFieldValue"
    const val FUNCTION0 = "kotlin.jvm.functions.Function0"
    const val FUNCTION1 = "kotlin.jvm.functions.Function1"
    const val SEARCH_DEFAULT_CONTENT_LISTENER_SUFFIX = "\$SearchDefaultContentListener"
    const val SEARCH_DEFAULT_CONTENT_LISTENER_TOKEN = "SearchDefaultContentListener"

    const val QUERY_AI_RECOMMEND_CACHE_ID = "intl_search_page_ai_recommend_query"
    const val SEARCH_AI_RECOMMEND_CARD_CACHE_ID = "intl_search_page_ai_recommend_card"
    const val SEARCH_HINT_CONTAINER_CACHE_ID = "intl_search_page_hint_container"
    const val MAIN_SEARCH_BAR_CACHE_ID = "intl_search_page_main_search_bar"

    const val LEGACY_SEARCH_DEFAULT_CONTENT_HELPER =
        "com.baidu.netdisk.util.SearchDefaultContentHelper"
    const val NEW_FEED_SEARCH_DEFAULT_CONTENT_HELPER =
        "com.baidu.netdisk.newfeedhome.feedhome.logic.util.SearchDefaultContentHelper"

    val searchHintVmMetadataTokens = listOf(
        "queryAIRecommend",
        "queryTextAndHotRecommend",
        "queryImageRecommend",
        "queryFileRecommend",
        "AIRecommendResult",
    )

    val searchAiRecommendCardMetadataTokens = listOf(
        "SearchAIRecommendCard",
        "TextRecommends",
        "ImageRecommends",
        "FileRecommends",
        "HotRecommends",
        "AIRecommendResult",
    )

    val mainSearchScreenMetadataTokens = listOf(
        "MainSearchScreen",
        "SearchBar",
        "defaultSearchText",
        "fromSeachHistory",
    )

    val searchHintMetadataTokens = listOf(
        "SearchHint",
        "NavController",
        "search_release",
    )

    val searchHistoryMetadataTokens = listOf(
        "SearchHistory",
        "SearchHistoryCard",
        "SearchHistoryItem",
    )

    val searchDefaultContentHelperCacheIds = listOf(
        LEGACY_SEARCH_DEFAULT_CONTENT_HELPER to "intl_search_page_placeholder_display_legacy",
        NEW_FEED_SEARCH_DEFAULT_CONTENT_HELPER to "intl_search_page_placeholder_display_new_feed",
    )
}
