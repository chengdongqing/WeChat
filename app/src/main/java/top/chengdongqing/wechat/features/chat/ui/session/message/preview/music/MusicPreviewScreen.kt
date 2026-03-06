package top.chengdongqing.wechat.features.chat.ui.session.message.preview.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.designsystem.util.StatusBarAppearanceEffect
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components.MusicBackground
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components.MusicControls
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components.MusicInfo
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components.PlayButton
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components.VinylRecord

@Composable
fun MusicPreviewScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val albumArt = R.drawable.img_album_art_2

    val player = remember { MusicPlayer(context) }

    // 准备音频
    LaunchedEffect(Unit) {
        player.prepare(R.raw.music_2)
        player.play()
    }

    // 播放中每 200ms 拉一次进度
    LaunchedEffect(player.isPlaying) {
        while (player.isPlaying) {
            player.updateProgress()
            delay(200)
        }
    }

    // 离开页面时释放 MediaPlayer
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    StatusBarAppearanceEffect(isDark = false)

    Box {
        MusicBackground(albumArt)

        Scaffold(
            topBar = {
                WeTopBar(
                    onBack = onBack,
                    contentColor = White,
                    containerColor = Color.Unspecified
                )
            },
            containerColor = Color.Unspecified
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1.5f))

                // 唱片
                VinylRecord(
                    isPlaying = player.isPlaying,
                    albumArt = albumArt
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 左侧：歌曲信息
                    MusicInfo(
                        title = "这是我一生中最勇敢的瞬间",
                        artist = "棱镜乐队",
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    )

                    // 右侧：播放/暂停按钮
                    PlayButton(isPlaying = player.isPlaying) {
                        player.togglePlay()
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 播放控制区
                MusicControls(
                    progress = player.progress,
                    currentTimeText = (player.duration * player.progress).toInt().toTimeString(),
                    totalTimeText = player.duration.toTimeString(),
                    onProgressChange = { player.seekTo(it) }
                )
            }
        }
    }
}

/** 将毫秒格式化为 mm:ss */
private fun Int.toTimeString(): String {
    val totalSeconds = this / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}