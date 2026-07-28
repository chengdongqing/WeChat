package top.chengdongqing.wechat.feature.chat.ui.session.input.music

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.data.model.MusicTrack
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.Black
import top.chengdongqing.wechat.core.designsystem.theme.White

@Composable
fun MusicAlbumArt(music: MusicTrack, isPlaying: Boolean) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = remember(music.id, music.coverPath, music.coverData) {
                music.coverModel()
            },
            contentDescription = "专辑封面",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )

        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(0.5.dp, White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_filled),
                    contentDescription = "播放",
                    tint = White,
                    modifier = Modifier
                        .size(16.dp)
                )
            }
        } else {
            val transition = rememberInfiniteTransition(label = "MusicIcon")
            val scale by transition.animateFloat(
                initialValue = 1f,
                targetValue = 1.375f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ScaleAnimation"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_filled),
                    contentDescription = "播放",
                    modifier = Modifier
                        .size(16.dp)
                        .scale(scale),
                    tint = White
                )
            }
        }
    }
}
