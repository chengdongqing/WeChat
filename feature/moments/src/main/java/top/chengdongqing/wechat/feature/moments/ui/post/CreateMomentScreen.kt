package top.chengdongqing.wechat.feature.moments.ui.post

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.camera.rememberCameraLauncher
import top.chengdongqing.wechat.core.media.model.VisualMediaType
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.feature.moments.ui.list.MomentsViewModel

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
        if (selected.isNotEmpty()) {
            video = null
            images.addAll(selected.take(9 - images.size))
        }
    }
    val camera = rememberCameraLauncher { uri, type ->
        if (type == VisualMediaType.Image) {
            video = null
            images.add(uri)
        } else {
            images.clear()
            editingVideo = uri
        }
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
                title = { },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("取消", color = Color.Black, fontSize = 16.sp)
                    }
                },
                actions = {
                    Button(
                        enabled = content.isNotBlank() || images.isNotEmpty() || video != null,
                        onClick = {
                            video?.let { viewModel.publishVideo(content, it) }
                                ?: viewModel.publish(content, images)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF07C160),
                            disabledContainerColor = Color(0xFF07C160).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("发表", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TextField(
                value = content,
                onValueChange = { content = it.take(2000) },
                placeholder = { Text("这一刻的想法…", color = Color.Gray, fontSize = 16.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(fontSize = 16.sp)
            )

            // Image Grid
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (video != null) {
                    Box(modifier = Modifier.size(100.dp)) {
                        AsyncImage(
                            model = video,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    val allItems =
                        images.toList() + if (images.size < 9) listOf(null) else emptyList()
                    allItems.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { uri ->
                                if (uri != null) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .background(Color(0xFFF7F7F7))
                                            .clickable {
                                                picker.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_plus_outlined),
                                            contentDescription = "添加",
                                            modifier = Modifier.size(32.dp),
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            CreateMomentOptionRow("所在位置", R.drawable.ic_location_marker)
            CreateMomentOptionRow("谁可以看", R.drawable.ic_group_chat_outlined, "公开")
            CreateMomentOptionRow("提醒谁看", R.drawable.ic_person_filled)
        }
    }
}

@Composable
private fun CreateMomentOptionRow(title: String, icon: Int, value: String? = null) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(title, modifier = Modifier.weight(1f), fontSize = 16.sp)
            if (value != null) {
                Text(
                    value,
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_right_outlined),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 52.dp),
            thickness = 0.5.dp,
            color = Color.LightGray.copy(alpha = 0.5f)
        )
    }
}
