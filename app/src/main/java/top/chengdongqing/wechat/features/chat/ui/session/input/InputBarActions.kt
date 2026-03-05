package top.chengdongqing.wechat.features.chat.ui.session.input

import top.chengdongqing.wechat.features.call.model.CallType
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.input.panel.MoreAction

/**
 * 输入栏所有操作契约
 */
data class InputBarActions(
    // -------- 文本 --------
    /** 输入内容变更 */
    val onTextChange: (String) -> Unit = {},
    /** 输入框行数变更 */
    val onLineCountChange: (Int) -> Unit = {},
    /** 发送文本消息 */
    val onSendText: () -> Unit = {},
    /** 插入表情文字 */
    val onInsertEmoji: (description: String) -> Unit = {},
    /** 表情退格 */
    val onEmojiBackspace: () -> Unit = {},
    /** 切换全屏输入 */
    val onToggleExpand: () -> Unit = {},

    // -------- 模式切换 --------
    /** 切换到指定 mode */
    val onSwitchMode: (InputMode) -> Unit = {},
    /** 切换到语音模式 */
    val onSwitchToVoice: () -> Unit = {},
    /** 切换回文字模式（含自动弹出键盘） */
    val onSwitchToText: () -> Unit = {},

    // -------- 媒体 / 更多 --------
    /** 更多面板操作分发 */
    val onMoreAction: (action: MoreAction, isLongClick: Boolean) -> Unit = { _, _ -> },
    /** 语音消息发送 */
    val onVoiceSend: (path: String, duration: Long) -> Unit = { _, _ -> },
    /** 语音转文字结果回填 */
    val onSpeechResult: (text: String) -> Unit = {},
    /** 表情消息发送 */
    val onSendSticker: (MessageContent.Sticker) -> Unit = {},

    // -------- 透传 --------
    /** 发起通话 */
    val onLaunchCall: (CallType) -> Unit = {}
)