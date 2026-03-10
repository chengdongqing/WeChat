package top.chengdongqing.wechat.features.chat.ui.session.message.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.MusicBackground

@Composable
fun MusicContent(content: MessageContent.Music) {
    val music = content.music

    Box(modifier = Modifier.height(80.dp)) {
        MusicBackground(music.albumArtRes)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(music.albumArtRes),
                contentDescription = stringResource(R.string.message_music_album_cover),
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = music.title,
                    color = White,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = music.artist,
                    color = White.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            }
            Box(modifier = Modifier.padding(end = 8.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_filled),
                    contentDescription = stringResource(R.string.action_play),
                    tint = White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}