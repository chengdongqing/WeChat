package top.chengdongqing.wechat.features.chat.ui.session.input.panel

import androidx.annotation.DrawableRes
import top.chengdongqing.wechat.R

/**
 * 更多操作枚举
 */
enum class MoreAction(
    val label: String,
    @get:DrawableRes val icon: Int
) {
    Album("照片", R.drawable.ic_album_filled),
    Camera("拍摄", R.drawable.ic_camera_filled),
    VideoCall("视频通话", R.drawable.ic_video_filled),
    Location("位置", R.drawable.ic_location_filled),
    Transfer("转账", R.drawable.ic_transfer_filled),
    Favorite("收藏", R.drawable.ic_favorites_filled),
    Voice("语音输入", R.drawable.ic_mic2_filled),
    Card("个人名片", R.drawable.ic_person_filled),
    File("文件", R.drawable.ic_folder_filled),
    App("应用程序", R.drawable.ic_apk_filled),
    Music("音乐", R.drawable.ic_music_filled);
}