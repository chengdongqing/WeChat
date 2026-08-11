package top.chengdongqing.wechat.feature.chat.ui.preview.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.slider.WeSlider
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(0.1f))
            .onTap(onClick = onClick)
            .padding(8.dp)
    ) {
        AnimatedContent(targetState = isPlaying) { isPlaying ->
            Icon(
                painter = painterResource(
                    if (isPlaying) {
                        DesignR.drawable.ic_pause_filled
                    } else {
                        DesignR.drawable.ic_play_filled
                    }
                ),
                contentDescription = stringResource(
                    if (isPlaying) {
                        DesignR.string.action_play
                    } else {
                        DesignR.string.action_pause
                    }
                ),
                modifier = Modifier.fillMaxSize(),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicControls(
    progress: Float,
    currentTimeText: String,     // 当前播放时间，mm:ss
    totalTimeText: String,       // 总时长，mm:ss
    onProgressChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        // 进度条
        WeSlider(
            value = progress,
            range = 0f..1f,
            onChange = onProgressChange
        )

        // 当前时间 / 总时长
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-8).dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(currentTimeText, color = Color.White.copy(0.5f), fontSize = 10.sp)
            Text(totalTimeText, color = Color.White.copy(0.5f), fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
