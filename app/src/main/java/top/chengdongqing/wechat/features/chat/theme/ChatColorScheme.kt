package top.chengdongqing.wechat.features.chat.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.Dark_Surface
import top.chengdongqing.wechat.core.designsystem.theme.Dark_Surface2
import top.chengdongqing.wechat.core.designsystem.theme.Grey_F7
import top.chengdongqing.wechat.core.designsystem.theme.LocalIsDarkTheme
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
    val recordBackground: Color,     // 底部弧形背景
    val recordActionDefault: Color,  // 操作按钮默认背景
    val recordActionCancel: Color,   // 取消按钮激活色
    val recordActionConvert: Color,  // 转文字按钮激活色
    val recordWaveBar: Color,        // 声纹条颜色
)

private val ChatLightColors = ChatColorScheme(
    bubbleOutgoing = Color(0xFF95EC69),
    bubbleIncoming = White,
    bubbleTextOutgoing = White,
    bubbleTextIncoming = TextPrimaryLight,
    timestamp = TextSecondaryLight,
    bottomBarBackground = Grey_F7,
    textField = White,

    recordBackground = Color(0xFFF7F7F7),
    recordActionDefault = Color(0xFFE9E9E9),
    recordActionCancel = Danger,
    recordActionConvert = Color(0xFFD8D8D8),
    recordWaveBar = Color(0xFF191919)
)

private val ChatDarkColors = ChatColorScheme(
    bubbleOutgoing = Color(0xFF3DAF72),
    bubbleIncoming = Dark_Surface2,
    bubbleTextOutgoing = TextPrimaryDark,
    bubbleTextIncoming = TextPrimaryDark,
    timestamp = TextSecondaryDark,
    bottomBarBackground = Dark_Surface,
    textField = Color(0xFF282828),

    recordBackground = Color(0xFF1C1C1C),
    recordActionDefault = Color(0xFF3A3A3A),
    recordActionCancel = Danger,
    recordActionConvert = Color(0xFF4A4A4A),
    recordWaveBar = Color(0xFFE5E5E5)
)

val LocalChatColorScheme = staticCompositionLocalOf { ChatLightColors }

@Composable
fun ChatTheme(
    isDark: Boolean = LocalIsDarkTheme.current,
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