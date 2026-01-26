package top.chengdongqing.wechat.ui.chat.session.message.types

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.data.model.MessageContent

@Composable
fun ImageContent(content: MessageContent.Image) {
    AsyncImage(
        model = content.url,
        contentDescription = null,
        modifier = Modifier.sizeIn(minWidth = 120.dp),
        contentScale = ContentScale.Crop
    )
}