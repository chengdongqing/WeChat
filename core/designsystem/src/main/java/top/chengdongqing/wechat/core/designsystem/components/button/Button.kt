package top.chengdongqing.wechat.core.designsystem.components.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppearanceSetting
import top.chengdongqing.wechat.core.designsystem.theme.WeColorScheme
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

/**
 * 按钮
 *
 * @param text 按钮文字
 * @param type 类型
 * @param size 大小
 * @param prefix 前缀
 * @param enabled 是否启用
 * @param isLoading 是否加载中
 * @param colors 颜色配置
 * @param onClick 点击事件
 */
@Composable
fun WeButton(
    text: String,
    modifier: Modifier = Modifier,
    type: ButtonType = ButtonType.Primary,
    size: ButtonSize = ButtonSize.Large,
    prefix: (@Composable (contentColor: Color) -> Unit)? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors(type),
    onClick: (() -> Unit)? = null
) {
    val isClickable = enabled && !isLoading
    val sizeConfig = ButtonDefaults.sizeConfig(size)
    val contentColor = colors.currentContentColor(enabled)
    val containerColor = colors.currentContainerColor(enabled)

    Box(
        modifier = modifier
            .height(sizeConfig.height)
            .defaultMinSize(minWidth = sizeConfig.minWidth)
            .clip(RoundedCornerShape(sizeConfig.roundedSize))
            .background(containerColor)
            .clickable(
                enabled = isClickable,
                onClickLabel = text,
                role = Role.Button,
                onClick = { onClick?.invoke() }
            )
            .padding(horizontal = sizeConfig.horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(sizeConfig.iconSpacing)
        ) {
            if (isLoading) {
                WeLoading(color = contentColor)
            } else {
                prefix?.invoke(contentColor)
            }

            Text(
                text = text,
                color = contentColor,
                style = sizeConfig.textStyle,
                maxLines = 1
            )
        }
    }
}

enum class ButtonType {
    Primary,
    Plain,
    Danger
}

enum class ButtonSize {
    Large,
    Small
}

@Immutable
data class ButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color = containerColor,
    val disabledContentColor: Color = contentColor
) {
    @Composable
    fun currentContainerColor(enabled: Boolean): Color {
        return if (enabled) containerColor else disabledContainerColor
    }

    @Composable
    fun currentContentColor(enabled: Boolean): Color {
        return if (enabled) contentColor else disabledContentColor
    }
}

@Immutable
data class ButtonSizeConfig(
    val height: Dp,
    val minWidth: Dp,
    val horizontalPadding: Dp,
    val textStyle: TextStyle,
    val iconSpacing: Dp,
    val loadingSize: Dp,
    val roundedSize: Dp
)

object ButtonDefaults {

    /**
     * 根据 Type 获取 ButtonColors
     */
    @Composable
    fun buttonColors(
        type: ButtonType,
        colorScheme: WeColorScheme = WeTheme.colorScheme
    ): ButtonColors {
        val isDark = LocalAppearanceSetting.current.isDarkTheme

        return when (type) {
            ButtonType.Primary -> ButtonColors(
                containerColor = colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = if (isDark) Color(0xFF373737) else Color(0xFFDEDEDE),
                disabledContentColor = if (isDark) Color(0xFF6B6B6B) else Color(0xFFBBBBBB)
            )

            ButtonType.Plain -> ButtonColors(
                containerColor = if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f),
                contentColor = colorScheme.textPrimary,
                disabledContainerColor = if (isDark) Color(0xFF242424) else Color.Black.copy(0.05f),
                disabledContentColor = if (isDark) Color(0xFF555555) else Color(0xFFC8C8C8)
            )

            ButtonType.Danger -> ButtonColors(
                containerColor = colorScheme.surface,
                contentColor = colorScheme.danger,
                disabledContainerColor = if (isDark) Color(0xFF242424) else Color(0xFFFAFAFA),
                disabledContentColor = if (isDark) Color(0xFF5A2A2A) else Color(0xFFFAC8C8)
            )
        }
    }

    /**
     * 根据 Size 获取尺寸规范配置
     */
    @Composable
    fun sizeConfig(size: ButtonSize): ButtonSizeConfig {
        return when (size) {
            ButtonSize.Large -> ButtonSizeConfig(
                height = 48.dp,
                minWidth = 184.dp,
                horizontalPadding = 24.dp,
                textStyle = WeTheme.typography.bodyLarge,
                iconSpacing = 8.dp,
                loadingSize = 20.dp,
                roundedSize = 8.dp
            )

            ButtonSize.Small -> ButtonSizeConfig(
                height = 32.dp,
                minWidth = 0.dp,
                horizontalPadding = 12.dp,
                textStyle = WeTheme.typography.bodyMedium,
                iconSpacing = 4.dp,
                loadingSize = 14.dp,
                roundedSize = 6.dp
            )
        }
    }
}

@Preview
@Composable
private fun ButtonPreview() {
    WeTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WeTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(40.dp, 40.dp, 40.dp, 0.dp)
                .verticalScroll(
                    state = rememberScrollState(),
                    overscrollEffect = rememberBouncedOverscrollEffect()
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WeButton(
                text = "主要操作"
            )
            WeButton(
                text = "主要操作",
                isLoading = true
            )
            WeButton(
                text = "按钮禁用",
                enabled = false
            )
            WeButton(
                text = "次要操作",
                type = ButtonType.Plain
            )
            WeButton(
                text = "次要操作",
                type = ButtonType.Plain,
                isLoading = true
            )
            WeButton(
                text = "按钮禁用",
                type = ButtonType.Plain,
                enabled = false
            )
            WeButton(
                text = "警示操作",
                type = ButtonType.Danger
            )
            WeButton(
                text = "警示操作",
                type = ButtonType.Danger,
                isLoading = true
            )
            WeButton(
                text = "按钮禁用",
                type = ButtonType.Danger,
                enabled = false
            )
            Row(
                modifier = Modifier.width(184.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeButton(
                    text = "按钮",
                    size = ButtonSize.Small
                )
                WeButton(
                    text = "按钮",
                    type = ButtonType.Plain,
                    size = ButtonSize.Small
                )
                WeButton(
                    text = "按钮",
                    type = ButtonType.Danger,
                    size = ButtonSize.Small
                )
            }
            WeButton(
                text = "拍照",
                prefix = { color ->
                    Icon(
                        painter = painterResource(R.drawable.ic_camera_filled),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = color
                    )
                }
            )
            WeButton(
                text = "宽度拉满",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}