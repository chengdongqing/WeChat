package top.chengdongqing.wechat.core.model

/**
 * 字体缩放
 */
enum class AppFontScale(val value: Float) {
    Small(0.85f),
    Normal(1.0f),
    Medium(1.15f),
    Large(1.3f),
    XLarge(1.45f),
    XXLarge(1.6f),
    Huge(1.75f),
    Max(1.9f);

    companion object {
        fun fromName(name: String?): AppFontScale =
            entries.find { it.name == name } ?: Normal
    }
}
