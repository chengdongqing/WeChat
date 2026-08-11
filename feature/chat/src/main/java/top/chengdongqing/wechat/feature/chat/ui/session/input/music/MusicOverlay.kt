package top.chengdongqing.wechat.feature.chat.ui.session.input.music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.edit
import androidx.core.graphics.scale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.data.model.MusicTrack
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.popup.WePopup
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.playback.MusicPlayer
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.feature.chat.R
import top.chengdongqing.wechat.feature.chat.ui.session.input.InputBarActions
import top.chengdongqing.wechat.feature.chat.ui.session.input.InputBarState
import java.io.ByteArrayOutputStream
import java.io.File
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun MusicOverlay(
    state: InputBarState,
    actions: InputBarActions,
    libraryViewModel: MusicLibraryViewModel = hiltViewModel()
) {
    val onDismiss = actions.onToggleMusic
    val context = LocalContext.current

    WePopup(
        visible = state.isMusicOpen,
        padding = PaddingValues(vertical = 16.dp),
        title = stringResource(R.string.music_select_title),
        onDismiss = onDismiss
    ) {
        val player = remember { MusicPlayer(context) }
        var currentMusic by remember { mutableStateOf<MusicTrack?>(null) }
        var localMusic by remember {
            mutableStateOf(loadLocalMusic(context))
        }
        var hiddenMusicIds by remember {
            mutableStateOf(loadHiddenMusicIds(context))
        }
        var musicOrder by remember {
            mutableStateOf(loadMusicOrder(context))
        }
        val visibleMusic = (MusicTrack.entries + localMusic)
            .filterNot { it.id in hiddenMusicIds }
            .sortedWith(
                compareBy<MusicTrack> {
                    musicOrder.indexOf(it.id).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
                }.thenBy { (MusicTrack.entries + localMusic).indexOf(it) }
            )
        var pendingAudio by remember { mutableStateOf<Uri?>(null) }
        val coverPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { coverUri ->
            val audioUri = pendingAudio
            pendingAudio = null
            if (audioUri != null && coverUri != null) {
                runCatching { importLocalMusic(context, audioUri, coverUri) }
                    .onSuccess {
                        libraryViewModel.addFiles(it)
                        localMusic = localMusic + it
                        saveLocalMusic(context, localMusic)
                        musicOrder = musicOrder + it.id
                        saveMusicOrder(context, musicOrder)
                    }
            }
        }
        val audioPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                pendingAudio = uri
                coverPicker.launch("image/*")
            }
        }

        DisposableEffect(player) {
            onDispose {
                player.release()
            }
        }

        LazyColumn {
            item {
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .fillParentMaxWidth()
                        .clickable { audioPicker.launch("audio/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "＋ 添加本地音乐",
                        color = WeTheme.colorScheme.link,
                        fontSize = 15.sp
                    )
                }
                WeDivider()
            }
            items(
                items = visibleMusic,
                key = { it.id }
            ) { music ->
                MusicItem(
                    music = music,
                    isPlaying = currentMusic == music && player.isPlaying,
                    onTogglePlay = {
                        handleTogglePlay(player, music, currentMusic) {
                            currentMusic = it
                        }
                    },
                    onSelect = {
                        actions.onSendMessage(
                            MessageContent.Music(prepareForSend(music))
                        )
                        onDismiss()
                    },
                    onMoveToFront = {
                        musicOrder = listOf(music.id) +
                            visibleMusic.map { it.id }.filterNot { it == music.id }
                        saveMusicOrder(context, musicOrder)
                    },
                    onDelete = {
                        if (currentMusic?.id == music.id) {
                            player.release()
                            currentMusic = null
                        }
                        if (music.isLocal) {
                            libraryViewModel.deleteFiles(music)
                            localMusic = localMusic.filterNot { it.id == music.id }
                            saveLocalMusic(context, localMusic)
                        } else {
                            hiddenMusicIds = hiddenMusicIds + music.id
                            saveHiddenMusicIds(context, hiddenMusicIds)
                        }
                        musicOrder = musicOrder.filterNot { it == music.id }
                        saveMusicOrder(context, musicOrder)
                    }
                )
                WeDivider(modifier = Modifier.padding(start = 92.dp))
            }

            item {
                Spacer(modifier = Modifier.height(150.dp))
            }
        }
    }
}

@Composable
private fun MusicItem(
    music: MusicTrack,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSelect: () -> Unit,
    onMoveToFront: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember(music.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.height(90.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .combinedClickable(
                    onClick = onTogglePlay,
                    onLongClick = { showMenu = true }
                )
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicAlbumArt(music, isPlaying)
            if (showMenu) {
                MusicManagePopover(
                    music = music,
                    onDismiss = { showMenu = false },
                    onMoveToFront = {
                        onMoveToFront()
                        showMenu = false
                    },
                    onDelete = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = music.title,
                    color = WeTheme.colorScheme.textPrimary,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = music.artist,
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 13.sp
                )
            }
        }

        WeDivider(
            orientation = Orientation.Vertical,
            modifier = Modifier.height(40.dp)
        )

        Box(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .clickable(onClick = onSelect),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(DesignR.string.action_send),
                color = WeTheme.colorScheme.link,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun MusicManagePopover(
    music: MusicTrack,
    onDismiss: () -> Unit,
    onMoveToFront: () -> Unit,
    onDelete: () -> Unit
) {
    Popup(
        popupPositionProvider = MusicPopoverPositionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = WeTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.width(220.dp)) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = remember(music.id, music.coverPath, music.coverData) {
                            music.coverModel()
                        },
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(Modifier
                        .padding(start = 10.dp)
                        .weight(1f)) {
                        Text(music.title, color = WeTheme.colorScheme.textPrimary, fontSize = 14.sp)
                        Text(music.artist, color = WeTheme.colorScheme.textSecondary, fontSize = 12.sp)
                    }
                }
                WeDivider()
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = onMoveToFront, modifier = Modifier.weight(1f)) {
                        Text("移到最前", color = WeTheme.colorScheme.textPrimary)
                    }
                    TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                        Text("删除", color = Color(0xFFFA5151))
                    }
                }
            }
        }
    }
}

private object MusicPopoverPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = (anchorBounds.left + 16)
            .coerceIn(8, (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8))
        val above = anchorBounds.top - popupContentSize.height - 8
        val y = if (above >= 8) above else anchorBounds.bottom + 8
        return IntOffset(x, y)
    }
}

private fun handleTogglePlay(
    player: MusicPlayer,
    clickedMusic: MusicTrack,
    currentMusic: MusicTrack?,
    onMusicChange: (MusicTrack) -> Unit
) {
    if (currentMusic == clickedMusic) {
        // 同一首歌：切换播放/暂停
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    } else {
        player.release()
        clickedMusic.audioPath?.let(player::prepare)
            ?: player.prepare(clickedMusic.audioRes)
        player.play()
        onMusicChange(clickedMusic)
    }
}

private const val MUSIC_PREFS = "local_music_library"
private const val MUSIC_ITEMS = "items"
private const val MUSIC_ORDER = "order"
private const val HIDDEN_MUSIC_IDS = "hidden_ids"

private fun loadLocalMusic(context: Context): List<MusicTrack> {
    val value = context.getSharedPreferences(MUSIC_PREFS, Context.MODE_PRIVATE)
        .getString(MUSIC_ITEMS, null) ?: return emptyList()
    return runCatching { Json.decodeFromString<List<MusicTrack>>(value) }.getOrDefault(emptyList())
}

private fun saveLocalMusic(context: Context, tracks: List<MusicTrack>) {
    context.getSharedPreferences(MUSIC_PREFS, Context.MODE_PRIVATE)
        .edit {
            putString(MUSIC_ITEMS, Json.encodeToString(tracks.map { it.copy(coverData = null) }))
        }
}

private fun loadMusicOrder(context: Context): List<String> =
    context.getSharedPreferences(MUSIC_PREFS, Context.MODE_PRIVATE)
        .getString(MUSIC_ORDER, null)
        ?.let { runCatching { Json.decodeFromString<List<String>>(it) }.getOrNull() }
        ?: emptyList()

private fun saveMusicOrder(context: Context, order: List<String>) {
    context.getSharedPreferences(MUSIC_PREFS, Context.MODE_PRIVATE).edit {
        putString(MUSIC_ORDER, Json.encodeToString(order))
    }
}

private fun loadHiddenMusicIds(context: Context): Set<String> =
    context.getSharedPreferences(MUSIC_PREFS, Context.MODE_PRIVATE)
        .getStringSet(HIDDEN_MUSIC_IDS, emptySet())
        ?.toSet()
        ?: emptySet()

private fun saveHiddenMusicIds(context: Context, ids: Set<String>) {
    context.getSharedPreferences(MUSIC_PREFS, Context.MODE_PRIVATE).edit {
        putStringSet(HIDDEN_MUSIC_IDS, ids)
    }
}

private fun prepareForSend(track: MusicTrack): MusicTrack {
    if (!track.isLocal || track.coverData != null) return track
    val path = track.coverPath ?: return track
    return track.copy(coverData = encodeCover(File(path)))
}

private fun importLocalMusic(context: Context, audioUri: Uri, coverUri: Uri): MusicTrack {
    val id = randomUUID()
    val directory = File(context.filesDir, "music").apply { mkdirs() }
    val mimeType = context.contentResolver.getType(audioUri) ?: "audio/*"
    val audioExtension = android.webkit.MimeTypeMap.getSingleton()
        .getExtensionFromMimeType(mimeType) ?: "audio"
    val audioFile = File(directory, "$id.$audioExtension")
    val coverFile = File(directory, "$id.cover")
    context.contentResolver.openInputStream(audioUri)!!.use { input ->
        audioFile.outputStream().use(input::copyTo)
    }
    context.contentResolver.openInputStream(coverUri)!!.use { input ->
        coverFile.outputStream().use(input::copyTo)
    }
    val coverData = encodeCover(coverFile)

    val retriever = MediaMetadataRetriever()
    val metadata = runCatching {
        context.contentResolver.openFileDescriptor(audioUri, "r")!!.use {
            retriever.setDataSource(it.fileDescriptor)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: audioFile.nameWithoutExtension
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "未知歌手"
            title to artist
        }
    }.getOrDefault(audioFile.nameWithoutExtension to "未知歌手")
    retriever.release()
    return MusicTrack(
        id = id,
        title = metadata.first,
        artist = metadata.second,
        audioPath = audioFile.absolutePath,
        coverPath = coverFile.absolutePath,
        coverData = coverData,
        mimeType = mimeType,
        size = audioFile.length()
    )
}

private fun encodeCover(file: File): String? =
    BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
        val scale = minOf(1f, 512f / maxOf(bitmap.width, bitmap.height))
        val thumbnail = if (scale < 1f) bitmap.scale(
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt()
        ) else bitmap
        ByteArrayOutputStream().use { output ->
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 82, output)
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }.also {
            if (thumbnail !== bitmap) thumbnail.recycle()
            bitmap.recycle()
        }
    }
