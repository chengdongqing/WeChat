package top.chengdongqing.wechat.feature.chat.ui.session.input.panel

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import top.chengdongqing.wechat.feature.chat.R
import top.chengdongqing.wechat.core.designsystem.R as DesignR

/**
 * 更多操作枚举
 */
enum class MoreAction(
    @get:StringRes val labelRes: Int,
    @get:DrawableRes val icon: Int
) {
    Album(R.string.chat_action_album, DesignR.drawable.ic_album_filled),
    Camera(R.string.chat_action_camera, DesignR.drawable.ic_camera_filled),
    VideoCall(R.string.chat_action_video_call, DesignR.drawable.ic_video_filled),
    Location(R.string.chat_action_location, DesignR.drawable.ic_location_filled),
    Transfer(R.string.chat_action_transfer, DesignR.drawable.ic_transfer_filled),
    Favorite(R.string.chat_action_favorite, DesignR.drawable.ic_favorites_filled),
    Voice(R.string.chat_action_voice, DesignR.drawable.ic_mic2_filled),
    ContactCard(R.string.chat_action_card, DesignR.drawable.ic_person_filled),
    File(R.string.chat_action_file, DesignR.drawable.ic_folder_filled),
    App(R.string.chat_action_app, DesignR.drawable.ic_apk_filled),
    Music(R.string.chat_action_music, DesignR.drawable.ic_music_filled),
    Live(R.string.chat_action_live, DesignR.drawable.ic_video_filled);
}
