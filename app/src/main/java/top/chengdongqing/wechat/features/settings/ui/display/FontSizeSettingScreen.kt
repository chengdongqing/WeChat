package top.chengdongqing.wechat.features.settings.ui.display

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.slider.WeSlider
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageItem
import top.chengdongqing.wechat.features.settings.domain.model.AppFontSize

@Composable
fun FontSizeSettingScreen(onBack: () -> Unit) {
    val initialFontSize = remember { AppFontSize.Normal }
    var fontSize by remember { mutableStateOf(initialFontSize) }
    val hasChanged = fontSize != initialFontSize

    Scaffold(
        topBar = {
            WeTopBar(title = "字体大小", onBack = onBack) {
                WeButton(
                    text = "完成", size = ButtonSize.Small, enabled = hasChanged
                ) {}
            }
        }, bottomBar = {
            FontSizeSelector(
                value = fontSize, onChange = { fontSize = it })
        }, containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState(), rememberBounceOverscrollEffect())
                .padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatPreview(scale = fontSize.scale)
        }
    }
}

@Composable
private fun ChatPreview(scale: Float) {
    val messages = remember {
        val texts = listOf(
            "预览字体大小",
            "拖动下面的滑块，可设置字体大小",
            "设置后，会改变聊天和朋友圈的字体大小。如果在使用过程中存在问题或意见，可反馈给微信团队"
        )
        val id = randomUUID()
        texts.mapIndexed { index, text ->
            ChatMessage(
                id = id,
                sessionId = id,
                senderId = id,
                content = MessageContent.Text(text),
                isFromMe = index == 0
            )
        }
    }

    for (message in messages) {
        MessageItem(
            message = message, myAvatar = R.drawable.img_avatar, peerAvatar = R.drawable.img_logo
        )
    }
}

@Composable
private fun FontSizeSelector(
    value: AppFontSize, onChange: (AppFontSize) -> Unit
) {
    val steps = AppFontSize.entries.lastIndex

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
            .padding(horizontal = 32.dp)
            .padding(top = 16.dp, bottom = 28.dp)
    ) {
        FontSizeSelectorLabel(steps)

        Box(contentAlignment = Alignment.Center) {
            FontSizeScaleTicks(steps = steps)

            WeSlider(
                value = value.ordinal.toFloat(),
                range = 0f..steps.toFloat(),
                height = 32.dp,
                showTrack = false,
                handleSize = 22.dp
            ) {
                onChange(AppFontSize.entries[it.toInt()])
            }
        }
    }
}

@Composable
private fun FontSizeSelectorLabel(steps: Int) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val stepWidth = maxWidth / steps
        val labelOffset = stepWidth * 1 // index = 1
        Text(
            text = "标准",
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
                modifier = Modifier.offset(x = (-4).dp, y = (-4).dp)
            )
            Text(
                text = "A",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(x = 8.dp)
            )
        }
    }
}

@Composable
private fun FontSizeScaleTicks(steps: Int) {
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
            color = Color.Black.copy(alpha = 0.6f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 0.5.dp.toPx()
        )

        // 刻度竖线
        repeat(tickCount) { i ->
            val x = if (i == steps) width else i * stepWidth
            drawLine(
                color = Color.Black.copy(alpha = 0.6f),
                start = Offset(x, centerY - 10f),
                end = Offset(x, centerY + 10f),
                strokeWidth = 0.5.dp.toPx()
            )
        }
    }
}