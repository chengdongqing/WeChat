package top.chengdongqing.wechat.features.settings.domain.model

/**
 * 字体大小
 */
enum class AppFontSize(
    val label: String,
    val scale: Float
) {
    Small("极小", 0.85f),
    Normal("标准", 1.0f),
    Medium("中等", 1.15f),
    Large("大", 1.3f),
    XLarge("超大", 1.45f),
    XXLarge("特大", 1.6f),
    Huge("巨大", 1.75f),
    Max("极大", 1.9f)
}