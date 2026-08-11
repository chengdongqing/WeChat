package top.chengdongqing.wechat.feature.chat.ui.session.message

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.chat.R

/**
 * 消息操作类型（单条消息长按菜单）
 */
enum class MessageAction(
    @get:DrawableRes val icon: Int,
    @get:StringRes val labelRes: Int
) {
    Copy(DesignR.drawable.ic_copy_filled, R.string.message_action_copy),
    Delete(DesignR.drawable.ic_delete_filled, R.string.message_action_delete),
    Cancel(DesignR.drawable.ic_recall_outlined, R.string.message_action_cancel),
    Recall(DesignR.drawable.ic_recall_outlined, R.string.message_action_recall),
    AddSticker(DesignR.drawable.ic_plus_circle_outlined, R.string.message_action_add_sticker),
    Forward(DesignR.drawable.ic_forward_filled, R.string.message_action_forward),
    Favorite(DesignR.drawable.ic_favorites_filled, R.string.message_action_favorite),
    Edit(DesignR.drawable.ic_edit_filled, R.string.message_action_edit),
    Remind(DesignR.drawable.ic_bell_filled, R.string.message_action_remind),
    MultiSelect(DesignR.drawable.ic_multi_select_outlined, R.string.message_action_multi_select),
    SpeakerMode(DesignR.drawable.ic_speaker_filled, R.string.message_action_speaker),
    EarpieceMode(DesignR.drawable.ic_ear_filled, R.string.message_action_earpiece),
    Quote(DesignR.drawable.ic_quote_filled, R.string.message_action_quote),
    Download(DesignR.drawable.ic_download_filled, R.string.message_action_download)
}

/**
 * 消息操作类型（多选模式底栏）
 */
enum class MultiMessageAction(
    @get:DrawableRes val icon: Int,
    @get:StringRes val labelRes: Int
) {
    Forward(DesignR.drawable.ic_forward_outlined, R.string.message_action_forward),
    Favorite(DesignR.drawable.ic_favorites_outlined, R.string.message_action_favorite),
    Delete(DesignR.drawable.ic_delete_outlined, R.string.message_action_delete),
    Download(DesignR.drawable.ic_download_outlined, R.string.message_action_download)
}
