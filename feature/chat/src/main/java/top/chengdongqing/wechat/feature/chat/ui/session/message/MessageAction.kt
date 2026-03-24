package top.chengdongqing.wechat.feature.chat.ui.session.message

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R

/**
 * 消息操作类型（单条消息长按菜单）
 */
enum class MessageAction(
    @get:DrawableRes val icon: Int,
    @get:StringRes val labelRes: Int
) {
    Copy(R.drawable.ic_copy_filled, R.string.message_action_copy),
    Delete(R.drawable.ic_delete_filled, R.string.message_action_delete),
    Cancel(R.drawable.ic_recall_outlined, R.string.message_action_cancel),
    Recall(R.drawable.ic_recall_outlined, R.string.message_action_recall),
    Forward(R.drawable.ic_forward_filled, R.string.message_action_forward),
    Favorite(R.drawable.ic_favorites_filled, R.string.message_action_favorite),
    Remind(R.drawable.ic_bell_filled, R.string.message_action_remind),
    MultiSelect(R.drawable.ic_multi_select_outlined, R.string.message_action_multi_select),
    SpeakerMode(R.drawable.ic_speaker_filled, R.string.message_action_speaker),
    EarpieceMode(R.drawable.ic_ear_filled, R.string.message_action_earpiece),
    Quote(R.drawable.ic_quote_filled, R.string.message_action_quote),
    Download(R.drawable.ic_download_filled, R.string.message_action_download)
}

/**
 * 消息操作类型（多选模式底栏）
 */
enum class MultiMessageAction(
    @get:DrawableRes val icon: Int,
    @get:StringRes val labelRes: Int
) {
    Forward(R.drawable.ic_forward_outlined, R.string.message_action_forward),
    Favorite(R.drawable.ic_favorites_outlined, R.string.message_action_favorite),
    Delete(R.drawable.ic_delete_outlined, R.string.message_action_delete),
    Download(R.drawable.ic_download_outlined, R.string.message_action_download)
}
