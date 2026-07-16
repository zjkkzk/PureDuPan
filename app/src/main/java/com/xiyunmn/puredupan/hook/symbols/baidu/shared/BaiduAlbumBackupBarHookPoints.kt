package com.xiyunmn.puredupan.hook.symbols.baidu.shared

internal object BaiduAlbumBackupBarHookPoints {
    /**
     * 相册备份栏数据/逻辑层落点。
     *
     * [ALBUM_BACKUP_BAR_ADD_USE_CASE] 的 realExecute 把 AlbumBackupBar 放进
     * viewModel.bottomBars。命中隐藏开关时在该入口短路返回 true 且不 put，
     * 备份栏根本不进入 bottomBars，无 View 生成。
     *
     * 国内版 13.28.9 类名、realExecute 方法名（bridge 为 realExecute2）稳定未混淆，
     * 走稳定直连。三星版（kotlin.jy2）/国际版（b8.__）类名与方法名均混淆，
     * 走 DexKit：Kotlin @Metadata d2 数组三端都保留明文
     * [ALBUM_BACKUP_BAR_ADD_USE_CASE] + [REAL_EXECUTE_METADATA_TOKEN]，作为强锚点。
     */
    const val ALBUM_BACKUP_BAR_ADD_USE_CASE =
        "com.baidu.netdisk.allfiles.listfragment.extraview.floatingbar.AlbumBackupBarAddUseCase"

    /** UseCase realExecute 方法名（国内明文；三星/国际混淆为 ____，桥接自 ______）。 */
    const val REAL_EXECUTE_METHOD = "realExecute"

    /** 视图模型 bottomBars 持有者接口，参数形状校验用。 */
    const val FILE_LIST_VIEW_MODEL =
        "com.baidu.netdisk.filelist.IFileListViewModel"

    /** DexKit Metadata d2 锚点 token（与 [ALBUM_BACKUP_BAR_ADD_USE_CASE] 组合唯一定位）。 */
    const val REAL_EXECUTE_METADATA_TOKEN = "realExecute"
    const val ADD_USE_CASE_METADATA_TOKEN =
        "Lcom/baidu/netdisk/allfiles/listfragment/extraview/floatingbar/AlbumBackupBarAddUseCase;"
}
