package top.chengdongqing.wechat.features.settings.ui.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.radio.WeRadioGroup
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.settings.domain.model.AppLanguage

@Composable
fun LanguageSettingScreen(onBack: () -> Unit) {
    val languageOptions = remember { AppLanguage.entries.map { it.label to it } }
    var language by remember { mutableStateOf(AppLanguage.FollowSystem) }

    Scaffold(
        topBar = {
            WeTopBar(title = "多语言", onBack = onBack) {
                WeButton(text = "完成", size = ButtonSize.Small, enabled = false)
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            WeRadioGroup(
                options = languageOptions,
                value = language
            ) {
                language = it
            }
        }
    }
}