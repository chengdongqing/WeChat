package top.chengdongqing.wechat.feature.chat.ui.preview.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.core.common.media.MusicPlayer
import top.chengdongqing.wechat.core.data.model.MusicTrack
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.designsystem.window.StatusBarAppearanceEffect
import top.chengdongqing.wechat.core.util.format
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MusicPreviewScreen(music: MusicTrack, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember { MusicPlayer(context) }
    val cover = remember(music.id, music.coverPath, music.coverData) { music.coverModel() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 准备音频
    LaunchedEffect(music) {
        delay(300)
        player.setMetadata(music.title, music.artist, cover)
        music.audioPath?.let(player::prepare) ?: player.prepare(music.audioRes)
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
        MusicBackground(cover)

        Scaffold(
            topBar = {
                WeTopAppBar(
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
                    albumArt = cover,
                    isPlaying = player.isPlaying
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：歌曲信息
                    MusicInfo(
                        title = music.title,
                        artist = music.artist,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    )

                    // 右侧：播放/暂停按钮
                    PlayButton(isPlaying = player.isPlaying) {
                        player.togglePlay()
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 播放控制区
                MusicControls(
                    progress = player.progress,
                    currentTimeText = (player.duration * player.progress).toInt().milliseconds.format(),
                    totalTimeText = player.duration.milliseconds.format(),
                    onProgressChange = { player.seekTo(it) }
                )
            }
        }
    }
}
