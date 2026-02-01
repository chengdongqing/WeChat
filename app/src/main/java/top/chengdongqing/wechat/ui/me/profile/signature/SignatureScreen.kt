package top.chengdongqing.wechat.ui.me.profile.signature

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.ui.components.button.ButtonSize
import top.chengdongqing.wechat.ui.components.button.WeButton
import top.chengdongqing.wechat.ui.components.input.WeInput
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar

@Composable
fun SignatureScreen(onBack: () -> Unit) {
    var signature by remember { mutableStateOf("这么近 那么美") }

    Scaffold(
        topBar = {
            WeTopBar("个性签名", onBack = onBack) {
                WeButton("保存", size = ButtonSize.SMALL, disabled = false)
            }
        },
        containerColor = Color(0xFFEDEDED)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            WeInput(
                value = signature,
                maxLength = 30,
                singleLine = false
            ) {
                signature = it
            }
        }
    }
}