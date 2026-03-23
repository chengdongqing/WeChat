package top.chengdongqing.wechat.core.media.preview

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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

@Composable
fun ImagePreview(
    uri: Uri,
    zoomableState: ZoomableState = rememberZoomableState(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val motionVideoUri by produceState<Uri?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            context.extractMotionPhotoVideo(uri)
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

/**
 * 从指定的图片 Uri 中提取动态照片（Motion Photo）内嵌的视频文件。
 * * 动态照片本质上是一个 JPEG 文件，其末尾追加了一个完整的 MP4 视频数据。
 * 本方法通过扫描 MP4 的特征头（ftyp）定位视频起始点并将其导出到缓存目录。
 *
 * @param uri 原始图片的 Uri
 * @return 提取出的 MP4 文件的 Uri，若非动态照片或提取失败则返回 null
 */
private fun Context.extractMotionPhotoVideo(uri: Uri): Uri? {
    // 搜索 MP4 通用的 ftyp box 标识
    val marker = "ftyp".toByteArray()
    // 在缓存目录创建目标文件
    val cacheFile = File(cacheDir, "motion_${System.currentTimeMillis()}.mp4")

    return runCatching {
        // 使用 ParcelFileDescriptor 以只读模式打开原始文件
        contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            FileInputStream(pfd.fileDescriptor).use { input ->
                val channel = input.channel
                val size = channel.size()

                /*
                 * 使用内存映射文件（Mmap）。
                 * 优点：不需要将整个大文件加载到 JVM 堆内存中，避免 OOM（内存溢出）。
                 * 系统会根据需要将文件页加载到物理内存，搜索效率极高。
                 */
                val mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size)

                var foundIndex = -1

                /*
                 * 算法逻辑：从文件末尾向前搜索。
                 * 动态照片的视频数据通常位于文件尾部，倒序搜索能显著减少扫描字节数。
                 */
                for (i in (size - marker.size).toInt() downTo 4) {
                    var match = true
                    for (j in marker.indices) {
                        if (mappedBuffer.get(i + j) != marker[j]) {
                            match = false
                            break
                        }
                    }
                    if (match) {
                        /*
                         * 定位成功。
                         * 减去 4 是为了包含 ftyp box 前面的 4 字节长度字段（size field），
                         * 确保导出的 MP4 文件结构完整。
                         *
                         * 同时校验 box size 合法性（16~1024 字节），防止 JPEG 数据中
                         * 碰巧出现 "ftyp" 字节序列导致误识别。
                         */
                        val boxSize = ((mappedBuffer.get(i - 4).toInt() and 0xFF) shl 24) or
                                ((mappedBuffer.get(i - 3).toInt() and 0xFF) shl 16) or
                                ((mappedBuffer.get(i - 2).toInt() and 0xFF) shl 8) or
                                (mappedBuffer.get(i - 1).toInt() and 0xFF)
                        if (boxSize in 16..1024) {
                            foundIndex = i - 4
                            break
                        }
                    }
                }

                if (foundIndex >= 0) {
                    val videoLength = size - foundIndex

                    // 执行提取逻辑
                    FileOutputStream(cacheFile).use { output ->
                        /*
                         * 使用 transferTo（零拷贝技术）。
                         * 数据直接在内核缓冲区之间传输，不经过用户态内存，
                         * 这是 Android 中处理文件切分最快的方式。
                         */
                        channel.transferTo(foundIndex.toLong(), videoLength, output.channel)
                    }
                    cacheFile.toUri()
                } else {
                    null // 未找到 MP4 特征头
                }
            }
        }
    }.getOrNull()
}