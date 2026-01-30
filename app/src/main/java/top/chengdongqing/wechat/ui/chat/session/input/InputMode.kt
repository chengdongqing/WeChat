package top.chengdongqing.wechat.ui.chat.session.input

/**
 * 输入模式
 */
enum class InputMode {
    /** 文本输入模式 */
    TEXT,

    /** 语音输入模式 */
    VOICE,

    /** 表情面板模式 */
    EMOJI,

    /** 更多功能面板模式 */
    MORE;

    val isVoice: Boolean get() = this == VOICE
    val isEmoji: Boolean get() = this == EMOJI
    val isMore: Boolean get() = this == MORE
    val isText: Boolean get() = this == TEXT
    val isPanelMode: Boolean get() = isEmoji || isMore
}