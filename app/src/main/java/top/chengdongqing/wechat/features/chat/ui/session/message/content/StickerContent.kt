package top.chengdongqing.wechat.features.chat.ui.session.message.content

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.util.asAssetPath
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent

@Composable
fun StickerContent(content: MessageContent.Sticker) {
    AsyncImage(
        model = content.localPath.asAssetPath,
        contentDescription = null,
        modifier = Modifier.sizeIn(minWidth = 120.dp),
        contentScale = ContentScale.Crop
    )
}