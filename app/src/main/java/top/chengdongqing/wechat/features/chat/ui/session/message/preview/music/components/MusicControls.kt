package top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicControls(
    progress: Float,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onProgressChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.1f))
            ) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.ic_pause_filled else R.drawable.ic_play_filled
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

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
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(White.copy(alpha = 0.1f))
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(sliderState.value)
                            .height(2.dp)
                            .background(White.copy(alpha = 0.4f))
                    )
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-8).dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("01:24", color = Color.White.copy(0.5f), fontSize = 10.sp)
            Text("04:05", color = Color.White.copy(0.5f), fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}