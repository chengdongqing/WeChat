package top.chengdongqing.wechat.feature.chat.ui.session.input.panel

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R

/**
 * 更多操作枚举
 */
enum class MoreAction(
    @get:StringRes val labelRes: Int,
    @get:DrawableRes val icon: Int
) {
    Album(R.string.chat_action_album, R.drawable.ic_album_filled),
    Camera(R.string.chat_action_camera, R.drawable.ic_camera_filled),
    VideoCall(R.string.chat_action_video_call, R.drawable.ic_video_filled),
    Location(R.string.chat_action_location, R.drawable.ic_location_filled),
    Transfer(R.string.chat_action_transfer, R.drawable.ic_transfer_filled),
    Favorite(R.string.chat_action_favorite, R.drawable.ic_favorites_filled),
    Voice(R.string.chat_action_voice, R.drawable.ic_mic2_filled),
    ContactCard(R.string.chat_action_card, R.drawable.ic_person_filled),
    File(R.string.chat_action_file, R.drawable.ic_folder_filled),
    App(R.string.chat_action_app, R.drawable.ic_apk_filled),
    Music(R.string.chat_action_music, R.drawable.ic_music_filled),
    Live(R.string.chat_action_live, R.drawable.ic_video_filled);
}
