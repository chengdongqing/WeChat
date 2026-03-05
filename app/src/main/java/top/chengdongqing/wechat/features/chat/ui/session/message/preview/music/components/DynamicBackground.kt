package top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.MusicPreviewScreen

@Preview
@Composable
private fun Preview() {
    WeTheme {
        MusicPreviewScreen { }
    }
}

@Composable
fun DynamicBackground(albumArt: Int) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AsyncImage(
            model = albumArt,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(100.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.45f
        )
    }
}