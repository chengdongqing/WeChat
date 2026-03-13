package top.chengdongqing.wechat.features.chat.ui.preview.music

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MusicInfo(title: String, artist: String, modifier: Modifier) {
    Column(modifier = modifier) {
        MarqueeText(
            text = title,
            style = TextStyle(
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = artist,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MarqueeText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = style,
        maxLines = 1,
        modifier = modifier.basicMarquee(
            iterations = Int.MAX_VALUE,
            initialDelayMillis = 2000
        )
    )
}