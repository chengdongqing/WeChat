package top.chengdongqing.wechat.feature.chat.ui.preview.music

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.designsystem.theme.Black

@Composable
fun MusicBackground(@DrawableRes albumArtRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.8f))
    ) {
        AsyncImage(
            model = albumArtRes,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(90.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )
    }
}