package top.chengdongqing.wechat.feature.favorites.ui

import android.Manifest
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.camera.rememberCameraLauncher
import top.chengdongqing.wechat.core.media.model.VisualMediaType
import top.chengdongqing.wechat.core.media.picker.rememberMediaPickerLauncher
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.slider.WeSlider
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.model.LocationInfo
import top.chengdongqing.wechat.core.location.model.LocationPreviewInfo
import top.chengdongqing.wechat.core.location.picker.rememberPickLocationLauncher
import top.chengdongqing.wechat.core.location.preview.previewLocation
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.feature.favorites.model.FavoriteAttachment
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private val WeGreen = Color(0xFF07C160)
private val CardBackground = Color(0xFFF7F7F7)

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
    var body by remember { mutableStateOf(TextFieldValue("")) }
    var attachmentsOpen by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(draft.content) {
        if (draft.content != body.text) body = TextFieldValue(
            draft.content,
            TextRange(draft.content.length)
        )
    }

    fun addAttachment(item: FavoriteAttachment) {
        viewModel.update { it.copy(attachments = it.attachments + item) }
        attachmentsOpen = false
    }

    fun importUris(uris: List<Uri>) {
        scope.launch {
            val imported = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri -> context.importFavoriteUri(uri) }
            }
            if (imported.isNotEmpty()) {
                viewModel.update { it.copy(attachments = it.attachments + imported) }
                attachmentsOpen = false
            }
        }
    }

    val pickMedia = rememberMediaPickerLauncher { medias, _, _ ->
        importUris(medias.map { it.uri })
    }
    val launchCamera = rememberCameraLauncher { uri, _ -> importUris(listOf(uri)) }
    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { importUris(it) }

    fun stopRecording(keep: Boolean) {
        val active = recorder ?: return
        val file = recordingFile
        val duration = System.currentTimeMillis() - recordingStartedAt
        runCatching { active.stop() }
        active.release()
        recorder = null
        recordingFile = null
        if (keep && file != null && file.length() > 0) {
            addAttachment(
                FavoriteAttachment(
                    id = randomUUID(),
                    kind = FavoriteAttachment.Kind.AUDIO,
                    path = file.absolutePath,
                    mimeType = "audio/mp4",
                    displayName = file.name,
                    durationMs = duration.coerceAtLeast(1)
                )
            )
        } else file?.delete()
    }

    fun startRecording() {
        val file = context.newFavoriteFile("AUD_", ".m4a")
        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        val started = runCatching {
            created.setAudioSource(MediaRecorder.AudioSource.MIC)
            created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            created.setAudioEncodingBitRate(96_000)
            created.setAudioSamplingRate(44_100)
            created.setOutputFile(file.absolutePath)
            created.prepare()
            created.start()
        }.isSuccess
        if (started) {
            recorder = created
            recordingFile = file
            recordingStartedAt = System.currentTimeMillis()
            attachmentsOpen = false
        } else {
            created.release()
            file.delete()
        }
    }

    val audioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { if (it) startRecording() }
    val pickLocation = rememberPickLocationLauncher { location: LocationInfo ->
        addAttachment(
            FavoriteAttachment(
                id = randomUUID(),
                kind = FavoriteAttachment.Kind.LOCATION,
                latitude = location.coordinate.latitude,
                longitude = location.coordinate.longitude,
                locationName = location.name,
                address = location.address.orEmpty(),
                mapUri = location.staticMapUri?.toString().orEmpty()
            )
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.runCatching { stop() }
            recorder?.release()
        }
    }

    Column(Modifier
        .fillMaxSize()
        .background(Color.White)) {
        WeTopAppBar(
            title = "笔记",
            containerColor = Color.White,
            onBack = { viewModel.save(onBack) },
            actions = {
                TextButton(
                    onClick = { viewModel.save(onBack) },
                    enabled = draft.title.isNotBlank() || body.text.isNotBlank() ||
                            draft.attachments.isNotEmpty()
                ) { Text("完成", color = WeGreen, fontWeight = FontWeight.Bold) }
                EditorIcon(R.drawable.ic_more_outlined, "更多") { showMore = true }
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
                    .padding(top = 14.dp, bottom = 14.dp),
                textStyle = TextStyle(
                    Color(0xFF191919),
                    24.sp,
                    FontWeight.Bold,
                    lineHeight = 32.sp
                ),
                decorationBox = { inner ->
                    if (draft.title.isBlank()) Text(
                        "标题",
                        color = Color(0xFFC1C1C1),
                        fontSize = 24.sp
                    )
                    inner()
                }
            )
            BasicTextField(
                value = body,
                onValueChange = { changed ->
                    val edited = continueListOnNewLine(body, changed)
                    body = edited
                    viewModel.update { draftValue -> draftValue.copy(content = edited.text) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                textStyle = TextStyle(Color(0xFF191919), 17.sp, lineHeight = 28.sp),
                decorationBox = { inner ->
                    if (body.text.isBlank()) Text(
                        "记录文字、图片、位置或录音",
                        color = Color(0xFFC1C1C1),
                        fontSize = 17.sp
                    )
                    inner()
                }
            )
            draft.attachments.forEach { attachment ->
                AttachmentCard(
                    attachment = attachment,
                    onDelete = {
                        viewModel.update { value ->
                            value.copy(attachments = value.attachments.filterNot {
                                it.id == attachment.id
                            })
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
            if (recorder != null) {
                RecordingCard(recordingStartedAt) { stopRecording(true) }
                Spacer(Modifier.height(12.dp))
            }
        }
        Column(Modifier.imePadding()) {
            FormatBar(
                onFormat = {
                    body = applyFormat(body, it).also { formatted ->
                        viewModel.update { value -> value.copy(content = formatted.text) }
                    }
                },
                onAdd = { attachmentsOpen = !attachmentsOpen }
            )
            if (attachmentsOpen) {
                AttachmentPanel(
                    onPhoto = { pickMedia(VisualMediaType.ImageAndVideo, 99) },
                    onCamera = { launchCamera(VisualMediaType.ImageAndVideo) },
                    onLocation = pickLocation,
                    onFile = { pickFile.launch(arrayOf("*/*")) },
                    onRecord = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) startRecording()
                        else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }

    if (showMore) {
        AlertDialog(
            onDismissRequest = { showMore = false },
            title = { Text("笔记操作") },
            text = { Text("文字和附件会自动保存。") },
            confirmButton = {
                TextButton(onClick = { showMore = false; viewModel.save(onBack) }) { Text("完成") }
            },
            dismissButton = {
                Row {
                    if (favoriteId != null) TextButton(onClick = {
                        showMore = false
                        viewModel.delete(onBack)
                    }) { Text("删除", color = Color(0xFFFA5151)) }
                    TextButton(onClick = { showMore = false }) { Text("取消") }
                }
            }
        )
    }
}

@Composable
private fun FormatBar(onFormat: (FormatAction) -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(CardBackground)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf(
            "B" to FormatAction.BOLD,
            "T" to FormatAction.HEADING,
            "1☰" to FormatAction.NUMBERED,
            "•☰" to FormatAction.BULLET,
            "☑" to FormatAction.CHECKBOX,
            "—" to FormatAction.DIVIDER
        ).forEach { (label, action) ->
            Text(
                label,
                color = Color(0xFF222222),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onFormat(action) }
                    .padding(8.dp)
            )
        }
        Icon(
            painterResource(R.drawable.ic_plus_circle_outlined),
            "添加内容",
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onAdd)
                .padding(5.dp)
        )
    }
}

private enum class FormatAction { BOLD, HEADING, NUMBERED, BULLET, CHECKBOX, DIVIDER }

/**
 * Handles the editor behaviour users expect from a note app:
 * 1. "1. item" continues as "2. "
 * 2. bullets and checkboxes retain their marker
 * 3. pressing enter on an empty list item exits the list
 */
private fun continueListOnNewLine(
    previous: TextFieldValue,
    changed: TextFieldValue
): TextFieldValue {
    if (!previous.selection.collapsed || !changed.selection.collapsed) return changed
    if (changed.text.length != previous.text.length + 1) return changed

    val previousCursor = previous.selection.start
    val changedCursor = changed.selection.start
    if (changedCursor != previousCursor + 1 ||
        changed.text.getOrNull(previousCursor) != '\n'
    ) return changed

    val lineStart = previous.text.lastIndexOf('\n', previousCursor - 1).let { it + 1 }
    val line = previous.text.substring(lineStart, previousCursor)
    val continuation = listContinuation(line) ?: return changed

    if (continuation.isEmptyItem) {
        val text = changed.text.removeRange(lineStart, changedCursor)
        return TextFieldValue(text, TextRange(lineStart))
    }

    val text = changed.text.substring(0, changedCursor) +
            continuation.nextPrefix +
            changed.text.substring(changedCursor)
    return TextFieldValue(text, TextRange(changedCursor + continuation.nextPrefix.length))
}

private data class ListContinuation(val nextPrefix: String, val isEmptyItem: Boolean)

private fun listContinuation(line: String): ListContinuation? {
    val checkbox = CHECKBOX_LINE.matchEntire(line)
    if (checkbox != null) {
        val indent = checkbox.groupValues[1]
        return ListContinuation(
            nextPrefix = "$indent- [ ] ",
            isEmptyItem = checkbox.groupValues[3].isBlank()
        )
    }

    val numbered = NUMBERED_LINE.matchEntire(line)
    if (numbered != null) {
        val indent = numbered.groupValues[1]
        val number = numbered.groupValues[2].toLongOrNull() ?: return null
        val separator = numbered.groupValues[3]
        return ListContinuation(
            nextPrefix = "$indent${number + 1}$separator ",
            isEmptyItem = numbered.groupValues[4].isBlank()
        )
    }

    val bullet = BULLET_LINE.matchEntire(line)
    if (bullet != null) {
        val indent = bullet.groupValues[1]
        val marker = bullet.groupValues[2]
        return ListContinuation(
            nextPrefix = "$indent$marker ",
            isEmptyItem = bullet.groupValues[3].isBlank()
        )
    }
    return null
}

private val CHECKBOX_LINE = Regex("""^(\s*)-\s+\[([ xX])]\s*(.*)$""")
private val NUMBERED_LINE = Regex("""^(\s*)(\d+)([.)、])\s*(.*)$""")
private val BULLET_LINE = Regex("""^(\s*)([-*•])\s+(.*)$""")

private fun applyFormat(value: TextFieldValue, action: FormatAction): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(0, value.text.length)
    val selected = value.text.substring(start, end)
    val insertion = when (action) {
        FormatAction.BOLD -> if (selected.isEmpty()) "**加粗文字**" else "**$selected**"
        FormatAction.HEADING -> if (selected.isEmpty()) "## 标题" else "## $selected"
        FormatAction.NUMBERED -> if (selected.isEmpty()) "1. " else selected.lineSequence()
            .mapIndexed { i, line -> "${i + 1}. $line" }.joinToString("\n")

        FormatAction.BULLET -> if (selected.isEmpty()) "- " else selected.lineSequence()
            .joinToString("\n") { "- $it" }

        FormatAction.CHECKBOX -> if (selected.isEmpty()) "- [ ] " else selected.lineSequence()
            .joinToString("\n") { "- [ ] $it" }

        FormatAction.DIVIDER -> "\n---\n"
    }
    val prefix = if (start > 0 && value.text[start - 1] != '\n' &&
        action !in setOf(FormatAction.BOLD)
    ) "\n" else ""
    val replacement = prefix + insertion
    val text = value.text.replaceRange(start, end, replacement)
    return TextFieldValue(text, TextRange(start + replacement.length))
}

@Composable
private fun AttachmentPanel(
    onPhoto: () -> Unit,
    onCamera: () -> Unit,
    onLocation: () -> Unit,
    onFile: () -> Unit,
    onRecord: () -> Unit
) {
    val actions = listOf(
        Triple("照片/视频", R.drawable.ic_album_filled, onPhoto),
        Triple("拍照", R.drawable.ic_camera_filled, onCamera),
        Triple("位置", R.drawable.ic_location_filled, onLocation),
        Triple("文件", R.drawable.ic_folder_filled, onFile),
        Triple("录音", R.drawable.ic_voice_outlined, onRecord)
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        actions.forEach { (label, icon, action) ->
            Column(
                Modifier
                    .width(66.dp)
                    .clickable(onClick = action),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(58.dp)
                        .background(Color.White, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(icon), label, modifier = Modifier.size(27.dp)) }
                Spacer(Modifier.height(7.dp))
                Text(label, fontSize = 11.sp, color = Color(0xFF777777), maxLines = 1)
            }
        }
    }
}

@Composable
private fun AttachmentCard(attachment: FavoriteAttachment, onDelete: () -> Unit) {
    val context = LocalContext.current
    Box(Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(4.dp))
        .background(CardBackground)) {
        when (attachment.kind) {
            FavoriteAttachment.Kind.IMAGE, FavoriteAttachment.Kind.VIDEO -> Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(attachment.path),
                    contentDescription = attachment.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                )
                if (attachment.kind == FavoriteAttachment.Kind.VIDEO) {
                    Icon(
                        painterResource(R.drawable.ic_play_filled),
                        "播放视频",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            FavoriteAttachment.Kind.LOCATION -> Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.previewLocation(
                            LocationPreviewInfo(
                                coordinate = GeoPoint(
                                    attachment.latitude,
                                    attachment.longitude
                                ),
                                name = attachment.locationName.ifBlank { "位置" },
                                address = attachment.address.takeIf(String::isNotBlank)
                            )
                        )
                    }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_location_filled),
                    null,
                    tint = Color(0xFF777777),
                    modifier = Modifier.size(34.dp)
                )
                Spacer(Modifier.width(18.dp))
                Column {
                    Text(attachment.locationName.ifBlank { "已选位置" }, fontSize = 18.sp)
                    if (attachment.address.isNotBlank()) Text(
                        attachment.address,
                        color = Color(0xFF888888),
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FavoriteAttachment.Kind.AUDIO -> AudioCard(attachment)
            FavoriteAttachment.Kind.FILE -> Row(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_file_filled),
                    null,
                    tint = Color(0xFF777777),
                    modifier = Modifier.size(38.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    attachment.displayName.ifBlank { File(attachment.path).name },
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(7.dp)
                .size(25.dp)
                .background(Color.Black.copy(alpha = .55f), CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                "删除附件",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AudioCard(attachment: FavoriteAttachment) {
    val context = LocalContext.current
    var player by remember(attachment.path) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    val duration = attachment.durationMs.coerceAtLeast(1L)
    DisposableEffect(attachment.path) {
        onDispose { player?.release() }
    }
    LaunchedEffect(playing) {
        while (playing) {
            position = player?.currentPosition?.toLong() ?: 0
            delay(100.milliseconds)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(if (playing) R.drawable.ic_pause_filled else R.drawable.ic_play_filled),
            if (playing) "暂停" else "播放",
            tint = WeGreen,
            modifier = Modifier
                .size(38.dp)
                .clickable {
                    if (playing) {
                        player?.pause()
                        playing = false
                    } else {
                        if (player == null) {
                            player = MediaPlayer().apply {
                                setDataSource(context, Uri.fromFile(File(attachment.path)))
                                prepare()
                                seekTo(position.toInt())
                                setOnCompletionListener {
                                    playing = false
                                    position = 0
                                    seekTo(0)
                                }
                            }
                        }
                        player?.start()
                        playing = true
                    }
                }
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            WeSlider(
                value = position.toFloat(),
                range = 0f..duration.toFloat(),
                step = 1,
                height = 30.dp,
                handleSize = 12.dp,
                modifier = Modifier.fillMaxWidth(),
                onChange = { target ->
                    position = target.toLong().coerceIn(0L, duration)
                    player?.seekTo(position.toInt())
                }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${formatDuration(position)} / ${formatDuration(duration)}",
                color = Color(0xFF666666),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun RecordingCard(startedAt: Long, onFinish: () -> Unit) {
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(startedAt) {
        while (true) {
            elapsed = System.currentTimeMillis() - startedAt
            delay(200.milliseconds)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(Color.White, RoundedCornerShape(4.dp))
            .clickable(onClick = onFinish)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier
            .size(9.dp)
            .background(Color(0xFFFA5151), CircleShape))
        Spacer(Modifier.width(22.dp))
        Text(
            "正在录音 ${formatDuration(elapsed)}",
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )
        Text("完成", color = WeGreen, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditorIcon(icon: Int, description: String, onClick: () -> Unit) {
    Icon(
        painterResource(icon),
        description,
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
            .padding(8.dp)
    )
}

private fun Context.newFavoriteFile(prefix: String, suffix: String): File {
    val directory = File(filesDir, "favorites").apply { mkdirs() }
    return File.createTempFile(prefix, suffix, directory)
}

private fun Context.importFavoriteUri(uri: Uri): FavoriteAttachment? = runCatching {
    val mime = contentResolver.getType(uri).orEmpty()
    var displayName = ""
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) displayName = it.getString(0).orEmpty()
    }
    val suffix = displayName.substringAfterLast('.', "").takeIf(String::isNotBlank)
        ?.let { ".$it" } ?: when {
        mime.startsWith("image/") -> ".jpg"
        mime.startsWith("video/") -> ".mp4"
        mime.startsWith("audio/") -> ".m4a"
        else -> ".bin"
    }
    val file = newFavoriteFile("ATT_", suffix)
    contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input)
        file.outputStream().use(input::copyTo)
    }
    val kind = when {
        mime.startsWith("image/") -> FavoriteAttachment.Kind.IMAGE
        mime.startsWith("video/") -> FavoriteAttachment.Kind.VIDEO
        mime.startsWith("audio/") -> FavoriteAttachment.Kind.AUDIO
        else -> FavoriteAttachment.Kind.FILE
    }
    FavoriteAttachment(
        id = randomUUID(),
        kind = kind,
        path = file.absolutePath,
        mimeType = mime,
        displayName = displayName.ifBlank { file.name },
        durationMs = if (kind == FavoriteAttachment.Kind.AUDIO) mediaDuration(file) else 0
    )
}.getOrNull()

private fun mediaDuration(file: File): Long = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(file.absolutePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
    } finally {
        retriever.release()
    }
}.getOrDefault(0L)

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
