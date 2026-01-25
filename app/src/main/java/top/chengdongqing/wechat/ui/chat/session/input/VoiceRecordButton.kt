package top.chengdongqing.wechat.ui.chat.session.input

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun VoiceRecordButton() {
    Text(
        "按住 说话",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}