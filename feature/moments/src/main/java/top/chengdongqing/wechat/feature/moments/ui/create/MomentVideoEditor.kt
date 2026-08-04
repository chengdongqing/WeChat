package top.chengdongqing.wechat.feature.moments.ui.create

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.playback.video.WeVideoPlayer
import top.chengdongqing.wechat.core.playback.video.rememberVideoPlayerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentVideoEditor(
    source: Uri,
    onCancel: () -> Unit,
    onComplete: (Uri) -> Unit
) {
    val context = LocalContext.current
    val duration = remember(source) {
        MediaMetadataRetriever().runCatching {
            setDataSource(context, source)
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 1L
        }.getOrDefault(1L).coerceAtLeast(1L)
    }
    var range by remember(source) { mutableStateOf(0f..duration.toFloat()) }
    var filter by remember { mutableStateOf(MomentVideoFilter.Original) }
    var exporting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var recording by remember { mutableStateOf(false) }
    var voiceOver by remember { mutableStateOf<Uri?>(null) }
    val exporter = remember { VideoExportManager(context) }
    val voiceRecorder = remember { VoiceOverRecorder(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceRecorder.start()
            recording = true
        }
    }
    val player = rememberVideoPlayerState(source)
    DisposableEffect(Unit) {
        onDispose {
            exporter.cancel()
            if (recording) voiceRecorder.stop(discard = true)
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        WeTopAppBar(
            title = "编辑视频",
            onBack = onCancel,
            containerColor = Color.Black,
            contentColor = Color.White
        )
        WeVideoPlayer(player, Modifier.fillMaxWidth().weight(1f))
        Text(
            "裁剪  ${formatMs(range.start.toLong())} - ${formatMs(range.endInclusive.toLong())}",
            color = Color.White,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
        )
        RangeSlider(
            value = range,
            onValueChange = {
                if (it.endInclusive - it.start >= 1_000f) range = it
            },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(vertical = 14.dp)
        ) {
            items(MomentVideoFilter.entries) { item ->
                Text(
                    item.title,
                    color = Color.White,
                    modifier = Modifier
                        .background(if (item == filter) Color(0xFF07C160) else Color(0xFF343434))
                        .clickable { filter = item }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = {
                if (recording) {
                    voiceOver = voiceRecorder.stop()
                    recording = false
                } else if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    voiceRecorder.start()
                    voiceOver = null
                    recording = true
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }) {
                Text(if (recording) "停止配音" else if (voiceOver != null) "重录配音" else "开始配音")
            }
            if (voiceOver != null) {
                Button(onClick = {
                    voiceOver = null
                    voiceRecorder.stop(discard = true)
                }) { Text("删除配音") }
            }
        }
        error?.let { Text(it, color = Color.Red, modifier = Modifier.padding(horizontal = 18.dp)) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (exporting) CircularProgressIndicator()
            Button(enabled = !exporting, onClick = {
                exporting = true
                exporter.export(
                    source,
                    MomentVideoEdit(
                        range.start.toLong(),
                        range.endInclusive.toLong(),
                        filter,
                        voiceOver
                    ),
                    onProgress = {}
                ) { result ->
                    exporting = false
                    result.onSuccess(onComplete).onFailure {
                        error = it.message ?: "视频导出失败"
                    }
                }
            }) { Text("完成") }
        }
    }
}

private fun formatMs(value: Long) = "%d:%02d".format(value / 60_000, value / 1_000 % 60)
