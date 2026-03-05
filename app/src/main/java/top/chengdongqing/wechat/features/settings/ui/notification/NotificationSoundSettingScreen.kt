package top.chengdongqing.wechat.features.settings.ui.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.radio.WeRadioGroup
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.features.settings.domain.model.NotificationSound

@Composable
fun NotificationSoundSettingScreen(onBack: () -> Unit) {
    val soundOptions = remember { NotificationSound.entries.map { it.label to it } }
    var sound by remember { mutableStateOf(NotificationSound.FollowSystem) }

    Scaffold(
        topBar = {
            WeTopBar(title = "消息提示音", onBack = onBack) {
                WeButton(text = "完成", size = ButtonSize.Small, enabled = false)
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    state = rememberScrollState(),
                    overscrollEffect = rememberBounceOverscrollEffect()
                )
                .padding(innerPadding)
        ) {
            WeRadioGroup(
                options = soundOptions,
                value = sound
            ) {
                sound = it
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}