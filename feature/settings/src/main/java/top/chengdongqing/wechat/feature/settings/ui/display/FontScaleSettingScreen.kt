package top.chengdongqing.wechat.feature.settings.ui.display

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.chat.WeMessageBubble
import top.chengdongqing.wechat.core.designsystem.components.chat.WeMessageText
import top.chengdongqing.wechat.core.designsystem.components.slider.WeSlider
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppearanceSetting
import top.chengdongqing.wechat.core.designsystem.theme.LocalFontScale
import top.chengdongqing.wechat.core.designsystem.theme.Neutral900
import top.chengdongqing.wechat.core.designsystem.theme.TextPrimaryDark
import top.chengdongqing.wechat.core.designsystem.theme.TextPrimaryLight
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.model.AppFontScale

@Composable
fun FontScaleSettingScreen(
    onBack: () -> Unit,
    viewModel: DisplaySettingsViewModel = hiltViewModel()
) {
    val initialFontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    var fontScale by remember(initialFontScale) { mutableStateOf(initialFontScale) }
    val hasChanged = fontScale != initialFontScale

    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(R.string.display_font_scale),
                onBack = onBack
            ) {
                WeButton(
                    text = stringResource(R.string.action_done),
                    size = ButtonSize.Small,
                    enabled = hasChanged
                ) {
                    viewModel.saveFontScale(fontScale)
                    onBack()
                }
            }
        },
        bottomBar = {
            FontScaleSelector(
                value = fontScale,
                onChange = { fontScale = it }
            )
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = LocalDensity.current.density
            ),
            LocalFontScale provides fontScale.value
        ) {
            ChatPreview(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun ChatPreview(
    viewModel: DisplaySettingsViewModel,
    modifier: Modifier
) {
    val resources = LocalResources.current
    val avatarPath by produceState<Any?>(R.drawable.img_logo) {
        value = viewModel.profileRepository.requireProfile().avatarPath
    }

    val messages = remember {
        val texts = listOf(
            resources.getString(R.string.display_font_scale_preview_title),
            resources.getString(R.string.display_font_scale_preview_hint),
            resources.getString(R.string.display_font_scale_preview_desc)
        )
        texts
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                state = rememberScrollState(),
                overscrollEffect = rememberBouncedOverscrollEffect()
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        messages.forEachIndexed { index, text ->
            TextMessagePreviewItem(
                text = text,
                isFromMe = index == 0,
                myAvatar = avatarPath,
                peerAvatar = R.drawable.img_logo
            )
        }
    }
}

@Composable
private fun FontScaleSelector(
    value: AppFontScale,
    onChange: (AppFontScale) -> Unit
) {
    val steps = AppFontScale.entries.lastIndex

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 32.dp)
            .padding(top = 16.dp, bottom = 28.dp)
    ) {
        FontScaleSelectorLabel(steps)

        Box(contentAlignment = Alignment.Center) {
            FontScaleScaleTicks(steps = steps)

            WeSlider(
                value = value.ordinal.toFloat(),
                range = 0f..steps.toFloat(),
                height = 32.dp,
                showTrack = false,
                handleSize = 22.dp
            ) {
                onChange(AppFontScale.entries[it.toInt()])
            }
        }
    }
}

@Composable
private fun TextMessagePreviewItem(
    text: String,
    isFromMe: Boolean,
    myAvatar: Any?,
    peerAvatar: Any?,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppearanceSetting.current.isDarkTheme
    val bubbleColor = when {
        isFromMe && isDark -> Color(0xFF3DAF72)
        isFromMe -> Color(0xFF95EC69)
        isDark -> Neutral900
        else -> White
    }
    val textColor = when {
        isFromMe -> TextPrimaryLight
        isDark -> TextPrimaryDark
        else -> TextPrimaryLight
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!isFromMe) MessagePreviewAvatar(peerAvatar)
            WeMessageBubble(isFromMe = isFromMe, color = bubbleColor) {
                WeMessageText(
                    text = AnnotatedString(text),
                    modifier = Modifier.padding(10.dp),
                    color = textColor
                )
            }
            if (isFromMe) MessagePreviewAvatar(myAvatar)
        }
    }
}

@Composable
private fun MessagePreviewAvatar(model: Any?) {
    AsyncImage(
        model = model,
        contentDescription = null,
        error = androidx.compose.ui.res.painterResource(R.drawable.img_avatar_placeholder),
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
    )
}

@Composable
private fun FontScaleSelectorLabel(steps: Int) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val stepWidth = maxWidth / steps
        val labelOffset = stepWidth * 1

        Text(
            text = stringResource(R.string.display_font_scale_normal),
            fontSize = 17.sp,
            color = WeTheme.colorScheme.textPrimary,
            modifier = Modifier.offset(x = labelOffset - 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "A",
                fontSize = 14.sp,
                color = WeTheme.colorScheme.textPrimary,
                modifier = Modifier.offset(x = (-4).dp, y = (-4).dp)
            )
            Text(
                text = "A",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = WeTheme.colorScheme.textPrimary,
                modifier = Modifier.offset(x = 8.dp)
            )
        }
    }
}

@Composable
private fun FontScaleScaleTicks(steps: Int) {
    val color = WeTheme.colorScheme.textSecondary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val width = size.width
        val centerY = size.height / 2
        val tickCount = steps + 1
        val stepWidth = width / steps

        // 主横线
        drawLine(
            color = color,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 0.5.dp.toPx()
        )

        // 刻度竖线
        repeat(tickCount) { i ->
            val x = if (i == steps) width else i * stepWidth
            drawLine(
                color = color,
                start = Offset(x, centerY - 10f),
                end = Offset(x, centerY + 10f),
                strokeWidth = 0.5.dp.toPx()
            )
        }
    }
}
