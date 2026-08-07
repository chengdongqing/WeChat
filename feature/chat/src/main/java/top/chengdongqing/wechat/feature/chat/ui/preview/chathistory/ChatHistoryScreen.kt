package top.chengdongqing.wechat.feature.chat.ui.preview.chathistory

import android.net.Uri
import android.text.format.Formatter.formatFileSize
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.common.media.model.MediaItem
import top.chengdongqing.wechat.core.common.media.model.MediaType
import top.chengdongqing.wechat.core.common.media.preview.previewMedias
import top.chengdongqing.wechat.core.common.time.toFullDateTime
import top.chengdongqing.wechat.core.data.model.ChatHistoryItem
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.text.RichTextMode
import top.chengdongqing.wechat.core.designsystem.text.parseRichText
import top.chengdongqing.wechat.core.designsystem.text.rememberEmojiInlineContent
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppearanceSetting
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.model.LocationPreviewInfo
import top.chengdongqing.wechat.core.location.preview.previewLocation
import top.chengdongqing.wechat.core.playback.VoicePlayer
import top.chengdongqing.wechat.core.util.format
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.ChatHistoryContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.MusicContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.VoiceIcon
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ChatHistoryScreen(
    content: MessageContent.ChatHistory,
    onBack: () -> Unit,
    onOpenHistory: (MessageContent.ChatHistory) -> Unit,
    onOpenFile: (ChatHistoryItem) -> Unit,
    onOpenMusic: (top.chengdongqing.wechat.core.data.model.MusicTrack) -> Unit
) {
    Scaffold(
        topBar = { WeTopAppBar(title = content.title, onBack = onBack) },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(WeTheme.colorScheme.background),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
        ) {
            items(content.items) { item ->
                ChatHistoryRow(item, onOpenHistory, onOpenFile, onOpenMusic)
            }
        }
    }
}

@Composable
private fun ChatHistoryRow(
    item: ChatHistoryItem,
    onOpenHistory: (MessageContent.ChatHistory) -> Unit,
    onOpenFile: (ChatHistoryItem) -> Unit,
    onOpenMusic: (top.chengdongqing.wechat.core.data.model.MusicTrack) -> Unit
) {
    val context = LocalContext.current
    val language = LocalAppearanceSetting.current.appLanguage

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(item.senderName, color = WeTheme.colorScheme.textSecondary, fontSize = 13.sp)
            Text(
                text = item.timestamp.toFullDateTime(language),
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 13.sp
            )
        }
        when (item.kind) {
            "image", "video" -> if (!item.localPath.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .padding(start = 20.dp, bottom = 16.dp)
                        .size(132.dp)
                        .clickable { previewHistoryMedia(context, item) }
                ) {
                    AsyncImage(
                        model = item.localPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                    )
                    if (item.kind == "video") {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_filled),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(42.dp)
                        )
                        Text(
                            text = (item.duration ?: 0).milliseconds.format(),
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        )
                    }
                }
            } else HistoryText(item.text)

            "sticker" -> if (!item.localPath.isNullOrBlank()) {
                AsyncImage(
                    model = item.localPath,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(start = 20.dp, bottom = 16.dp)
                        .size(132.dp)
                )
            } else HistoryText(item.text)

            "file" -> Row(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    .fillMaxWidth()
                    .clickable { onOpenFile(item) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.text, fontSize = 17.sp, color = WeTheme.colorScheme.textPrimary)
                    item.fileSize?.let {
                        Text(
                            formatFileSize(context, it),
                            fontSize = 13.sp,
                            color = WeTheme.colorScheme.textSecondary
                        )
                    }
                }
                Image(painterResource(R.drawable.ic_file_filled), null, Modifier.size(48.dp))
            }

            "location" -> HistoryLocation(item)
            "voice" -> HistoryVoice(item)
            "text" -> HistorySelectableText(item.text)
            "history" -> item.nestedHistory?.let { nested ->
                Box(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(WeTheme.colorScheme.background)
                        .clickable {
                            onOpenHistory(MessageContent.ChatHistory(nested.title, nested.items))
                        }
                ) {
                    ChatHistoryContent(MessageContent.ChatHistory(nested.title, nested.items))
                }
            } ?: HistoryText(item.text)

            "music" -> item.music?.let { music ->
                Box(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                        .width(270.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenMusic(music) }
                ) {
                    MusicContent(MessageContent.Music(music))
                }
            } ?: HistoryText(item.text)

            else -> HistoryText(item.text)
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 20.dp),
            color = WeTheme.colorScheme.divider
        )
    }
}

@Composable
private fun HistoryLocation(item: ChatHistoryItem) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(WeTheme.colorScheme.background)
            .clickable {
                val lat = item.latitude
                val lng = item.longitude
                if (lat == null || lng == null) {
                    context.showToast("位置信息已失效")
                    return@clickable
                }
                context.previewLocation(
                    LocationPreviewInfo(
                        coordinate = GeoPoint(lat, lng),
                        address = item.address.orEmpty(),
                        name = item.poiName.orEmpty()
                    )
                )
            }
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(
                item.poiName ?: item.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = WeTheme.colorScheme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.address?.let {
                Text(
                    it,
                    fontSize = 13.sp,
                    color = WeTheme.colorScheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(color = WeTheme.colorScheme.divider)
        AsyncImage(
            model = item.localPath,
            contentDescription = item.poiName ?: item.text,
            error = painterResource(R.drawable.img_location_placeholder),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
        )
    }
}

@Composable
private fun HistoryVoice(item: ChatHistoryItem) {
    val context = LocalContext.current
    val player = remember { VoicePlayer(context) }
    var playing by remember { mutableStateOf(false) }
    DisposableEffect(player) { onDispose { player.stop() } }
    Row(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
            .width((105L + ((item.duration ?: 0L) / 1000L).coerceAtMost(50L) * 2L).toInt().dp)
            .clip(RoundedCornerShape(6.dp))
            .background(WeTheme.colorScheme.background)
            .clickable {
                val path = item.localPath ?: return@clickable
                if (playing) {
                    player.stop()
                    playing = false
                } else {
                    playing = true
                    player.play(path, true) { playing = false }
                }
            }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VoiceIcon(isFromMe = false, isPlaying = playing, color = WeTheme.colorScheme.textPrimary)
        Spacer(Modifier.weight(1f))
        Text(
            "${maxOf(1L, (item.duration ?: 0L) / 1000L)}″",
            fontSize = 15.sp,
            color = WeTheme.colorScheme.textPrimary
        )
    }
}

private fun previewHistoryMedia(context: android.content.Context, item: ChatHistoryItem) {
    val path = item.localPath ?: return
    val isVideo = item.kind == "video"
    context.previewMedias(
        listOf(
            MediaItem(
                uri = Uri.fromFile(File(path)),
                filename = File(path).name,
                mediaType = if (isVideo) MediaType.Video else MediaType.Image,
                mimeType = item.mimeType ?: if (isVideo) "video/*" else "image/*",
                width = item.width ?: 0,
                height = item.height ?: 0,
                size = item.fileSize ?: 0,
                duration = item.duration ?: 0
            )
        )
    )
}

@Composable
private fun HistoryText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        color = WeTheme.colorScheme.textPrimary
    )
}

@Composable
private fun HistorySelectableText(text: String) {
    val annotatedText = remember(text) {
        text.parseRichText(mode = RichTextMode.EmojiOnly)
    }
    val inlineContent = rememberEmojiInlineContent(annotatedText, emojiSize = 22.sp)

    SelectionContainer {
        Text(
            text = annotatedText,
            inlineContent = inlineContent,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            fontSize = 17.sp,
            lineHeight = 23.sp,
            color = WeTheme.colorScheme.textPrimary
        )
    }
}
