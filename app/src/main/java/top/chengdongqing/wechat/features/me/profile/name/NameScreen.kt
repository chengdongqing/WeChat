package top.chengdongqing.wechat.features.me.profile.name

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.input.WeInput
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun NameScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("海盐芝士不加糖") }

    Scaffold(
        topBar = {
            WeTopBar("更改名字", onBack = onBack) {
                WeButton("保存", size = ButtonSize.Small, disabled = false)
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            WeInput(name, maxLength = 17) {
                name = it
            }

            Text(
                text = "好名字可以让你的朋友更容易记住你。",
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp)
            )
        }
    }
}