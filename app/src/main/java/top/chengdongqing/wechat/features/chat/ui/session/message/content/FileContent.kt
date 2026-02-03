package top.chengdongqing.wechat.features.chat.ui.session.message.content

import android.text.format.Formatter.formatFileSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.data.model.MessageContent

@Composable
fun FileContent(content: MessageContent.File) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .clickable {}
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = content.fileName,
                fontSize = 16.sp,
                color = WeTheme.colorScheme.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatFileSize(context, content.fileSize),
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_file_filled),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
    }
}