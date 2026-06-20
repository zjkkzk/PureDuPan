package com.xiyunmn.puredupan.hook.dexkit.baidu.intl

import com.xiyunmn.puredupan.hook.config.model.FeatureKeys
import com.xiyunmn.puredupan.hook.dexkit.DexKitHostContext
import com.xiyunmn.puredupan.hook.dexkit.DexKitTargetDescriptor
import com.xiyunmn.puredupan.hook.dexkit.DexKitTargetRegistry
import com.xiyunmn.puredupan.hook.dexkit.DexKitWarmUpTask
import com.xiyunmn.puredupan.hook.feature.baidu.intl.performance.IntlAlbumAiInitBlockHook
import com.xiyunmn.puredupan.hook.feature.baidu.intl.performance.IntlNonCoreDiffSocketDelayHook
import com.xiyunmn.puredupan.hook.feature.baidu.intl.performance.IntlStoryDouyinInitBlockHook
import com.xiyunmn.puredupan.hook.feature.baidu.intl.startup.hotstart.IntlHotStartSplashDexKitResolver

internal object BaiduIntlDexKitTargetRegistry : DexKitTargetRegistry {
    override val descriptors = listOf(
        DexKitTargetDescriptor(
            id = IntlHotStartSplashDexKitResolver.CACHE_ID,
            target = "intl hot-start splash resolver",
            feature = "block hot-start splash ad",
        ),
        DexKitTargetDescriptor(
            id = IntlStoryDouyinInitBlockHook.STORY_INIT_CACHE_ID,
            target = "intl story init method",
            feature = "delay Story/Douyin startup init",
        ),
        DexKitTargetDescriptor(
            id = IntlNonCoreDiffSocketDelayHook.SOCKET_REGISTER_CACHE_ID,
            target = "intl diff socket register method",
            feature = "delay non-core diff socket registration",
        ),
        DexKitTargetDescriptor(
            id = IntlAlbumAiInitBlockHook.DIRECT_ALBUM_AI_INIT_CACHE_ID,
            target = "intl album init method",
            feature = "delay album AI startup init",
        ),
    )

    override fun buildTasks(host: DexKitHostContext, classLoader: ClassLoader): List<DexKitWarmUpTask> {
        val tasks = mutableListOf<DexKitWarmUpTask>()

        if (host.isFeatureAvailable(FeatureKeys.KEY_REMOVE_HOT_START_SPLASH)) {
            tasks += DexKitWarmUpTask(IntlHotStartSplashDexKitResolver.CACHE_ID) {
                IntlHotStartSplashDexKitResolver.resolve(classLoader) != null
            }
        }
        if (host.isFeatureAvailable(FeatureKeys.KEY_BLOCK_INTL_STORY_DOUYIN_INIT)) {
            tasks += DexKitWarmUpTask(IntlStoryDouyinInitBlockHook.STORY_INIT_CACHE_ID) {
                IntlStoryDouyinInitBlockHook.warmUpDexKitCache(classLoader)
            }
        }
        if (host.isFeatureAvailable(FeatureKeys.KEY_DELAY_INTL_NON_CORE_DIFF_SOCKET)) {
            tasks += DexKitWarmUpTask(IntlNonCoreDiffSocketDelayHook.SOCKET_REGISTER_CACHE_ID) {
                IntlNonCoreDiffSocketDelayHook.warmUpDexKitCache(classLoader)
            }
        }
        if (host.isFeatureAvailable(FeatureKeys.KEY_BLOCK_INTL_ALBUM_AI_INIT)) {
            tasks += DexKitWarmUpTask(IntlAlbumAiInitBlockHook.DIRECT_ALBUM_AI_INIT_CACHE_ID) {
                IntlAlbumAiInitBlockHook.warmUpDexKitCache(classLoader)
            }
        }

        return tasks
    }
}
