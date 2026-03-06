package top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.White

@Composable
fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(0.1f))
            .padding(4.dp)
    ) {
        Icon(
            painter = painterResource(
                if (isPlaying) R.drawable.ic_pause_filled else R.drawable.ic_play_filled
            ),
            contentDescription = if (isPlaying) "暂停" else "播放",
            modifier = Modifier.fillMaxSize(),
            tint = Color.White.copy(alpha = 0.8f)
        )
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
        Slider(
            value = progress,
            onValueChange = onProgressChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.5f),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            thumb = {
                Spacer(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(y = 4.dp)
                        .clip(CircleShape)
                        .background(White)
                )
            },
            track = { sliderState ->
                Box {
                    // 轨道底色
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(White.copy(alpha = 0.1f))
                    )
                    // 已播放部分
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(sliderState.value)
                            .height(2.dp)
                            .background(White.copy(alpha = 0.4f))
                    )
                }
            }
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