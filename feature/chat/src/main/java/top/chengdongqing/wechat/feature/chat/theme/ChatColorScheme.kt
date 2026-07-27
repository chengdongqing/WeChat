package top.chengdongqing.wechat.feature.chat.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppearanceSetting
import top.chengdongqing.wechat.core.designsystem.theme.Neutral50
import top.chengdongqing.wechat.core.designsystem.theme.Neutral900
import top.chengdongqing.wechat.core.designsystem.theme.Neutral950
import top.chengdongqing.wechat.core.designsystem.theme.SemanticError
import top.chengdongqing.wechat.core.designsystem.theme.TextPrimaryDark
import top.chengdongqing.wechat.core.designsystem.theme.TextPrimaryLight
import top.chengdongqing.wechat.core.designsystem.theme.TextSecondaryDark
import top.chengdongqing.wechat.core.designsystem.theme.TextSecondaryLight
import top.chengdongqing.wechat.core.designsystem.theme.White

@Immutable
data class ChatColorScheme(
    // 消息气泡
    val bubbleOutgoing: Color,
    val bubbleIncoming: Color,
    val bubbleTextOutgoing: Color,
    val bubbleTextIncoming: Color,

    // 时间戳
    val timestamp: Color,

    // 底栏
    val bottomBarBackground: Color,
    // 输入框
    val textField: Color,

    // 录音相关
    val recordBackground: Color,
    val recordActionDefault: Color,
    val recordActionCancel: Color,
    val recordActionConvert: Color,
    val recordActionLabel: Color,
    val recordWaveBar: Color,
)

private val ChatLightColors = ChatColorScheme(
    bubbleOutgoing = Color(0xFF95EC69),
    bubbleIncoming = White,
    bubbleTextOutgoing = TextPrimaryLight,
    bubbleTextIncoming = TextPrimaryLight,
    timestamp = TextSecondaryLight,
    bottomBarBackground = Neutral50,
    textField = White,

    recordBackground = Color(0xFFF7F7F7),
    recordActionDefault = Color(0xFFE9E9E9),
    recordActionCancel = SemanticError,
    recordActionConvert = Color(0xFFD8D8D8),
    recordActionLabel = TextPrimaryDark,
    recordWaveBar = Color(0xFF191919)
)

private val ChatDarkColors = ChatColorScheme(
    bubbleOutgoing = Color(0xFF3DAF72),
    bubbleIncoming = Neutral900,
    bubbleTextOutgoing = TextPrimaryLight,
    bubbleTextIncoming = TextPrimaryDark,
    timestamp = TextSecondaryDark,
    bottomBarBackground = Neutral950,
    textField = Color(0xFF282828),

    recordBackground = Color(0xFF1C1C1C),
    recordActionDefault = Color(0xFF3A3A3A),
    recordActionCancel = SemanticError,
    recordActionConvert = Color(0xFF4A4A4A),
    recordActionLabel = TextPrimaryDark,
    recordWaveBar = Color(0xFFE5E5E5)
)

val LocalChatColorScheme = staticCompositionLocalOf { ChatLightColors }

@Composable
fun ChatTheme(
    isDark: Boolean = LocalAppearanceSetting.current.isDarkTheme,
    content: @Composable () -> Unit
) {
    val colors = if (isDark) ChatDarkColors else ChatLightColors
    CompositionLocalProvider(LocalChatColorScheme provides colors) {
        content()
    }
}

object ChatTheme {
    val colorScheme: ChatColorScheme
        @Composable get() = LocalChatColorScheme.current
}