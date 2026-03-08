package top.chengdongqing.wechat.features.chat.ui.session.input.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.popup.WePopup
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.media.MusicPlayer
import top.chengdongqing.wechat.features.chat.domain.model.InputBarActions
import top.chengdongqing.wechat.features.chat.domain.model.InputBarState
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.model.MusicTrack

@Composable
fun MusicOverlay(
    state: InputBarState,
    actions: InputBarActions
) {
    val onClose = actions.onToggleMusic

    WePopup(
        visible = state.isMusicOpen,
        padding = PaddingValues(vertical = 16.dp),
        title = "选择音乐",
        onClose = onClose
    ) {
        val context = LocalContext.current
        val player = remember { MusicPlayer(context) }
        var currentMusic by remember { mutableStateOf<MusicTrack?>(null) }

        DisposableEffect(player) {
            onDispose {
                player.release()
            }
        }

        LazyColumn {
            items(
                items = MusicTrack.entries,
                key = { it.name }
            ) { music ->
                MusicItem(
                    music = music,
                    isPlaying = currentMusic == music && player.isPlaying,
                    onTogglePlay = {
                        handleTogglePlay(player, music, currentMusic) {
                            currentMusic = it
                        }
                    },
                    onSelect = {
                        actions.onSendMessage(MessageContent.Music(music))
                        onClose()
                    }
                )
                WeDivider(modifier = Modifier.padding(start = 92.dp))
            }

            item {
                Spacer(modifier = Modifier.height(150.dp))
            }
        }
    }
}

@Composable
private fun MusicItem(
    music: MusicTrack,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier.height(90.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(onClick = onTogglePlay)
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicAlbumArt(music, isPlaying)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = music.title,
                    color = WeTheme.colorScheme.textPrimary,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = music.artist,
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 13.sp
                )
            }
        }

        WeDivider(
            orientation = Orientation.Vertical,
            modifier = Modifier.height(40.dp)
        )

        Box(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .clickable(onClick = onSelect),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "发送",
                color = WeTheme.colorScheme.link,
                fontSize = 15.sp
            )
        }
    }
}

private fun handleTogglePlay(
    player: MusicPlayer,
    clickedMusic: MusicTrack,
    currentMusic: MusicTrack?,
    onMusicChange: (MusicTrack) -> Unit
) {
    if (currentMusic == clickedMusic) {
        // 同一首歌：切换播放/暂停
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    } else {
        player.release()
        player.prepare(clickedMusic.audioRes)
        player.play()
        onMusicChange(clickedMusic)
    }
}