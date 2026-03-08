package top.chengdongqing.wechat.features.chat.domain.model

/**
 * 输入模式
 */
enum class InputMode {
    /** 文本输入模式 */
    Text,

    /** 语音输入模式 */
    Voice,

    /** 表情面板模式 */
    Emoji,

    /** 更多功能面板模式 */
    More;

    val isVoice: Boolean get() = this == Voice
    val isEmoji: Boolean get() = this == Emoji
    val isMore: Boolean get() = this == More
    val isText: Boolean get() = this == Text
    val isPanelMode: Boolean get() = isEmoji || isMore
}