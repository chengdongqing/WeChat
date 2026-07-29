package top.chengdongqing.wechat.feature.profile.ui.favorites

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
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
    var showAttachments by remember { mutableStateOf(true) }
    var showFormatting by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

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
            title = "笔记",
            containerColor = Color.White,
            onBack = { viewModel.save(onBack) },
            actions = {
                TextButton("↶")
                TextButton("↷")
                TextButton("•••") { showMore = true }
            }
        )
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
                    .padding(top = 20.dp, bottom = 10.dp),
                textStyle = TextStyle(
                    color = Color(0xFF161616),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                ),
                decorationBox = { inner ->
                    if (draft.title.isBlank()) Text(
                        "标题",
                        color = Color(0xFFBBBBBB),
                        fontSize = 21.sp
                    )
                    inner()
                }
            )
            BasicTextField(
                value = draft.content,
                onValueChange = { value -> viewModel.update { it.copy(content = value) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp),
                textStyle = TextStyle(
                    color = Color(0xFF202020),
                    fontSize = 18.sp,
                    lineHeight = 28.sp
                ),
                decorationBox = { inner ->
                    if (draft.content.isBlank()) {
                        Text(
                            "记录文字、图片或录音，内容将自动保存",
                            color = Color(0xFFBBBBBB),
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
            .height(64.dp)
            .background(Color(0xFFF8F8F8))
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            "B" to "**加粗**", "▣" to "`文本`", "1☰" to "\n1. 列表项",
            "•☰" to "\n- 列表项", "☑" to "\n- [ ] 待办事项",
            "☷" to "\n---\n", "◷" to "\n提醒："
        ).forEach { (label, syntax) ->
            Text(
                label,
                fontSize = 20.sp,
                color = Color(0xFF222222),
                modifier = Modifier
                    .clickable { onFormat(syntax) }
                    .padding(5.dp)
            )
        }
        Text(
            "⊕",
            fontSize = 29.sp,
            color = Color(0xFF222222),
            modifier = Modifier.clickable(onClick = onAdd)
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
            .background(Color(0xFFF5F5F5))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        actions.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { (label, icon, action) ->
                    Column(
                        Modifier
                            .width(72.dp)
                            .clickable(onClick = action),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .size(62.dp)
                                .background(Color.White, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(icon),
                                null,
                                tint = Color(0xFF4C4C4C),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(label, fontSize = 14.sp, color = Color(0xFF666666))
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
    if (items.isEmpty()) {
        Text("尚未添加文件", color = WeTheme.colorScheme.textSecondary)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, path ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    File(path).name,
                    modifier = Modifier.weight(1f),
                    color = WeTheme.colorScheme.textPrimary
                )
                Text(
                    "移除",
                    color = Color(0xFFFA5151),
                    modifier = Modifier
                        .clickable {
                            onChange(items.filterIndexed { itemIndex, _ -> itemIndex != index }
                                .joinToString("\n"))
                        }
                        .padding(start = 16.dp)
                )
            }
        }
    }
}
