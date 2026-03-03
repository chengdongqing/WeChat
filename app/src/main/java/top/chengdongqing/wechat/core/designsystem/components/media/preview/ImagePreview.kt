package top.chengdongqing.wechat.core.designsystem.components.media.preview

import android.content.Context
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.videoplayer.VideoPlayerDefaults
import top.chengdongqing.wechat.core.designsystem.components.videoplayer.WeVideoPlayer
import top.chengdongqing.wechat.core.designsystem.components.videoplayer.rememberVideoPlayerState
import java.io.File

@Composable
fun ImagePreview(
    uri: Uri,
    zoomableState: ZoomableState = rememberZoomableState(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val motionVideoUri by produceState<Uri?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            extractMotionPhotoVideo(context, uri)
        }
    }

    val isMotionPhoto = motionVideoUri != null
    var motionEnabled by remember { mutableStateOf(false) }

    // 退出时清理缓存
    DisposableEffect(Unit) {
        onDispose {
            motionVideoUri?.toFile()?.delete()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (motionEnabled && motionVideoUri != null) {
            val state = rememberVideoPlayerState(videoSource = motionVideoUri!!)
            WeVideoPlayer(state) {
                VideoPlayerDefaults.ControlBar(
                    state,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-60).dp)
                )
            }
        } else {
            val state = rememberZoomableImageState(zoomableState)
            ZoomableAsyncImage(
                model = uri,
                state = state,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                onClick = { onDismiss() },
            )
        }

        if (isMotionPhoto) {
            MotionPhotoToggle(
                enabled = motionEnabled,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(26.dp, (-18).dp),
                onClick = { motionEnabled = !motionEnabled },
            )
        }
    }
}

@Composable
private fun MotionPhotoToggle(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        if (enabled) 0.95f else 1f,
        spring(stiffness = Spring.StiffnessMediumLow)
    )
    val containerColor by animateColorAsState(
        if (enabled) Color.White else Color.White.copy(alpha = 0.25f)
    )
    val contentColor by animateColorAsState(
        if (enabled) Color.Black else Color.White
    )

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.4f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(
                    if (enabled) R.drawable.ic_motion_outlined
                    else R.drawable.ic_motion_off_outlined
                ),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "实况",
                fontSize = 13.sp,
                color = contentColor,
                fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

/** 从 JPEG 文件尾部提取内嵌 MP4，写到 cache 后返回其 Uri；非动态图片返回 null */
private fun extractMotionPhotoVideo(context: Context, uri: Uri): Uri? {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

    // 内嵌 MP4 的起始标记
    val marker = byteArrayOf(0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70) // ftyp box

    val index = bytes.indexOfSequence(marker).takeIf { it > 0 } ?: return null

    val videoBytes = bytes.copyOfRange(index, bytes.size)
    val cacheFile = File(context.cacheDir, "motion_${uri.lastPathSegment}.mp4")
    cacheFile.writeBytes(videoBytes)

    return cacheFile.toUri()
}

private fun ByteArray.indexOfSequence(target: ByteArray): Int {
    outer@ for (i in 0..size - target.size) {
        for (j in target.indices) {
            if (this[i + j] != target[j]) continue@outer
        }
        return i
    }
    return -1
}