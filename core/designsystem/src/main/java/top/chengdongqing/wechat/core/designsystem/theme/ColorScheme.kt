package top.chengdongqing.wechat.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class WeColorScheme(
    val primary: Color = Green100,
    val primarySecondary: Color = Green150,
    val danger: Color = Red100,
    val link: Color = Blue60,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val elevated: Color,
    val elevatedGrey: Color = Grey68,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
)

internal val LightColorScheme = WeColorScheme(
    background = Neutral100,
    surface = White,
    surfaceVariant = Neutral50,
    elevated = White,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textTertiary = TextTertiaryLight,
    divider = DividerLight,
)

internal val DarkColorScheme = WeColorScheme(
    background = Neutral1000,
    surface = Neutral950,
    surfaceVariant = Neutral900,
    elevated = DarkElevated,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textTertiary = TextTertiaryDark,
    divider = DividerDark,
)