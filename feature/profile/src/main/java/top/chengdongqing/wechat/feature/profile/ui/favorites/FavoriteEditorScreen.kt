package top.chengdongqing.wechat.feature.profile.ui.favorites

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import java.io.File

@Composable
fun FavoriteEditorScreen(
    favoriteId: String?,
    onBack: () -> Unit,
    viewModel: FavoriteEditorViewModel = hiltViewModel()
) {
    LaunchedEffect(favoriteId) { viewModel.load(favoriteId) }
    val draft by viewModel.draft.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAttachments by remember { mutableStateOf(false) }
    var showFormatting by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showAutoSaveHint by remember { mutableStateOf(false) }

    LaunchedEffect(draft) {
        if (draft.title.isNotBlank() || draft.content.isNotBlank() || draft.mediaPaths.isNotBlank()) {
            showAutoSaveHint = true
            delay(1500)
            showAutoSaveHint = false
        }
    }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val paths = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    runCatching {
                        val directory = File(context.filesDir, "favorites").apply { mkdirs() }
                        val extension = context.contentResolver.getType(uri)
                            ?.substringAfterLast('/')?.take(8)?.ifBlank { "bin" } ?: "bin"
                        val target = File(
                            directory,
                            "${System.currentTimeMillis()}_${uri.hashCode()}.$extension"
                        )
                        context.contentResolver.openInputStream(uri).use { input ->
                            requireNotNull(input)
                            target.outputStream().use(input::copyTo)
                        }
                        target.absolutePath
                    }.getOrNull()
                }
            }
            viewModel.update { value ->
                val existing = value.mediaPaths.lineSequence().filter(String::isNotBlank).toList()
                value.copy(mediaPaths = (existing + paths).distinct().joinToString("\n"))
            }
        }
    }

    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { uri -> importUris(listOf(uri)) }
    }
    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            importUris(it)
        }
    val recordAudio =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.data?.let { uri -> importUris(listOf(uri)) }
        }

    Column(Modifier
        .fillMaxSize()
        .background(Color.White)) {
        WeTopAppBar(
            title = "",
            containerColor = Color.White,
            onBack = { viewModel.save(onBack) },
            actions = {
                val isEnabled = draft.title.isNotBlank() || draft.content.isNotBlank()
                TextButton(
                    onClick = { viewModel.save(onBack) },
                    enabled = isEnabled
                ) {
                    Text(
                        "完成",
                        color = if (isEnabled) Color(0xFF07C160) else Color(0xFFCCCCCC),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(R.drawable.ic_more_outlined) { showMore = true }
            }
        )
        AnimatedVisibility(
            visible = showAutoSaveHint,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F7F7))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "已自动保存",
                    color = Color(0xFF999999),
                    fontSize = 12.sp
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            BasicTextField(
                value = draft.title,
                onValueChange = { value -> viewModel.update { it.copy(title = value) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 16.dp),
                textStyle = TextStyle(
                    color = Color(0xFF191919),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                ),
                decorationBox = { inner ->
                    if (draft.title.isBlank()) Text(
                        "标题",
                        color = Color(0xFFC1C1C1),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    inner()
                }
            )
            BasicTextField(
                value = draft.content,
                onValueChange = { value -> viewModel.update { it.copy(content = value) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                textStyle = TextStyle(
                    color = Color(0xFF191919),
                    fontSize = 17.sp,
                    lineHeight = 28.sp
                ),
                decorationBox = { inner ->
                    if (draft.content.isBlank()) {
                        Text(
                            "记录文字、图片或录音",
                            color = Color(0xFFC1C1C1),
                            fontSize = 17.sp
                        )
                    }
                    inner()
                }
            )
            if (draft.mediaPaths.isNotBlank()) {
                MediaPathList(draft.mediaPaths) { updated ->
                    viewModel.update { it.copy(mediaPaths = updated) }
                }
            }
        }
        NoteFormatBar(
            onFormat = { syntax ->
                viewModel.update { it.copy(content = it.content + syntax) }
                showFormatting = true
            },
            onAdd = { showAttachments = !showAttachments }
        )
        if (showAttachments) {
            AttachmentPanel(
                onPhoto = {
                    viewModel.update { it.copy(type = "MEDIA") }
                    pickMedia.launch(arrayOf("image/*", "video/*"))
                },
                onCamera = {
                    viewModel.update { it.copy(type = "MEDIA") }
                    pickMedia.launch(arrayOf("image/*"))
                },
                onLocation = { viewModel.update { it.copy(type = "LOCATION") } },
                onVoiceInput = { pickAudio.launch(arrayOf("audio/*")) },
                onFile = {
                    viewModel.update { it.copy(type = "MEDIA") }
                    pickMedia.launch(arrayOf("*/*"))
                },
                onRecord = {
                    viewModel.update { it.copy(type = "VOICE") }
                    recordAudio.launch(Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION))
                }
            )
        }
    }
    if (showMore) {
        AlertDialog(
            onDismissRequest = { showMore = false },
            title = { Text("笔记操作") },
            text = { Text("内容会自动保存。") },
            confirmButton = {
                TextButton(onClick = {
                    showMore = false
                    viewModel.save(onBack)
                }) { Text("完成") }
            },
            dismissButton = {
                Row {
                    if (favoriteId != null) {
                        TextButton(onClick = {
                            showMore = false
                            viewModel.delete(onBack)
                        }) { Text("删除", color = Color(0xFFFA5151)) }
                    }
                    TextButton(onClick = { showMore = false }) { Text("取消") }
                }
            }
        )
    }
}

@Composable
private fun NoteFormatBar(onFormat: (String) -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(Color(0xFFF7F7F7))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "B" to "**加粗**",
                "☑" to "\n- [ ] ",
                "•☰" to "\n- ",
                "1☰" to "\n1. ",
                "☷" to "\n---\n"
            ).forEach { (label, syntax) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onFormat(syntax) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 16.sp,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_plus_circle_outlined),
            contentDescription = "更多",
            tint = Color(0xFF333333),
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onAdd)
                .padding(6.dp)
        )
    }
}

@Composable
private fun AttachmentPanel(
    onPhoto: () -> Unit,
    onCamera: () -> Unit,
    onLocation: () -> Unit,
    onVoiceInput: () -> Unit,
    onFile: () -> Unit,
    onRecord: () -> Unit
) {
    val actions = listOf(
        Triple("照片", R.drawable.ic_album_filled, onPhoto),
        Triple("拍摄", R.drawable.ic_camera_filled, onCamera),
        Triple("位置", R.drawable.ic_location_filled, onLocation),
        Triple("语音输入", R.drawable.ic_mic_filled, onVoiceInput),
        Triple("文件", R.drawable.ic_folder_filled, onFile),
        Triple("录音", R.drawable.ic_voice_outlined, onRecord)
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F7F7))
            .padding(top = 10.dp, bottom = 30.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        actions.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { (label, icon, action) ->
                    Column(
                        Modifier
                            .width(64.dp)
                            .clickable(onClick = action),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .background(Color.White, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(icon),
                                null,
                                tint = Color(0xFF333333),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(label, fontSize = 12.sp, color = Color(0xFF888888))
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeSelector(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "RICH_TEXT" to "富文本", "VOICE" to "语音",
            "LOCATION" to "位置", "MEDIA" to "富媒体"
        ).forEach { (type, label) ->
            Text(
                label,
                color = if (selected == type) Color.White else WeTheme.colorScheme.textSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        if (selected == type) Color(0xFF07C160) else WeTheme.colorScheme.background,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelect(type) }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun RichTextEditor(content: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "加粗" to "**加粗文字**",
                "标题" to "\n## 标题",
                "项目" to "\n- 列表项",
                "编号" to "\n1. 列表项",
                "待办" to "\n- [ ] 待办事项",
                "引用" to "\n> 引用",
                "分隔线" to "\n---\n"
            ).forEach { (label, syntax) ->
                OutlinedButton(onClick = { onChange(content + syntax) }) { Text(label) }
            }
        }
        OutlinedTextField(
            value = content,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("记录文字、清单或说明") },
            minLines = 14
        )
        Text("支持 Markdown 格式，发送时会保留正文结构。", color = WeTheme.colorScheme.textSecondary)
    }
}

@Composable
private fun LocationEditor(content: String, onChange: (String) -> Unit) {
    val parts = content.split('|', limit = 3)
    val latitude = parts.getOrNull(0).orEmpty()
    val longitude = parts.getOrNull(1).orEmpty()
    val address = parts.getOrNull(2).orEmpty()
    fun update(lat: String = latitude, lng: String = longitude, addr: String = address) {
        onChange("$lat|$lng|$addr")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            latitude, { update(lat = it) }, Modifier.weight(1f),
            label = { Text("纬度") }, singleLine = true
        )
        OutlinedTextField(
            longitude, { update(lng = it) }, Modifier.weight(1f),
            label = { Text("经度") }, singleLine = true
        )
    }
    OutlinedTextField(
        address, { update(addr = it) }, Modifier.fillMaxWidth(),
        label = { Text("详细地址") }, minLines = 3
    )
}

@Composable
private fun MediaPathList(paths: String, onChange: (String) -> Unit) {
    val items = paths.lineSequence().filter(String::isNotBlank).toList()
    if (items.isEmpty()) return

    Column(
        modifier = Modifier.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEachIndexed { index, path ->
            val file = File(path)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                if (path.endsWith(".mp4", ignoreCase = true) ||
                    path.endsWith(".jpg", ignoreCase = true) ||
                    path.endsWith(".jpeg", ignoreCase = true) ||
                    path.endsWith(".png", ignoreCase = true)
                ) {
                    AsyncImage(
                        model = file,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_file_filled),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF888888)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            file.name,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF333333),
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            onChange(items.filterIndexed { i, _ -> i != index }.joinToString("\n"))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
