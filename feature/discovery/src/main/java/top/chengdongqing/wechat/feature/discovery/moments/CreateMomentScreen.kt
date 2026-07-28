package top.chengdongqing.wechat.feature.discovery.moments

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.common.camera.rememberCameraLauncher
import top.chengdongqing.wechat.core.common.media.model.VisualMediaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMomentScreen(
    onBack: () -> Unit,
    viewModel: MomentsViewModel = hiltViewModel()
) {
    var content by remember { mutableStateOf("") }
    val images = remember { mutableStateListOf<android.net.Uri>() }
    var video by remember { mutableStateOf<android.net.Uri?>(null) }
    var editingVideo by remember { mutableStateOf<android.net.Uri?>(null) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { selected ->
        video = null
        images.clear()
        images.addAll(selected.take(9))
    }
    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { selected ->
        selected?.let {
            images.clear()
            editingVideo = it
        }
    }
    val camera = rememberCameraLauncher { uri, type ->
        images.clear()
        if (type == VisualMediaType.Video) editingVideo = uri
    }
    editingVideo?.let { source ->
        MomentVideoEditor(
            source = source,
            onCancel = { editingVideo = null },
            onComplete = {
                video = it
                editingVideo = null
            }
        )
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发表文字") },
                navigationIcon = { TextButton(onClick = onBack) { Text("取消") } },
                actions = {
                    Button(
                        enabled = content.isNotBlank() || images.isNotEmpty() || video != null,
                        onClick = {
                            video?.let { viewModel.publishVideo(content, it) }
                                ?: viewModel.publish(content, images)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF07C160))
                    ) { Text("发表") }
                }
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it.take(2000) },
                placeholder = { Text("这一刻的想法…") },
                modifier = Modifier.fillMaxWidth().height(180.dp)
            )
            Spacer(Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(images) {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        modifier = Modifier.size(92.dp).clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            video?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "待发布视频",
                    modifier = Modifier.size(width = 160.dp, height = 220.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("照片") }
                Button(onClick = {
                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                }) { Text("视频") }
                Button(onClick = { camera(VisualMediaType.Video) }) { Text("拍摄") }
            }
        }
    }
}
