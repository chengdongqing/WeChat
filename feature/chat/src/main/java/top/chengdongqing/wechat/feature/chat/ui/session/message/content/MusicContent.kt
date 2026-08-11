package top.chengdongqing.wechat.feature.chat.ui.session.message.content

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.feature.chat.R
import top.chengdongqing.wechat.feature.chat.ui.preview.music.MusicBackground
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun MusicContent(content: MessageContent.Music) {
    val music = content.music
    val cover = remember(music.id, music.coverPath, music.coverData) {
        music.coverModel()
    }

    Box(modifier = Modifier.height(80.dp)) {
        MusicBackground(cover)

        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = cover,
                contentDescription = stringResource(R.string.message_music_album_cover),
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
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
                    painter = painterResource(DesignR.drawable.ic_play_filled),
                    contentDescription = stringResource(DesignR.string.action_play),
                    tint = White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
