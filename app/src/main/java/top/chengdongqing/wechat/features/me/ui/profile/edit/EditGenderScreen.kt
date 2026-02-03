package top.chengdongqing.wechat.features.me.ui.profile.edit

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
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.radio.WeRadioGroup
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.data.model.Gender

@Composable
fun EditGenderScreen(onBack: () -> Unit) {
    val genderOptions = remember {
        Gender.entries.filter { it != Gender.Unknown }.map { it.label to it }
    }
    var selectedGender by remember { mutableStateOf(Gender.Unknown) }

    Scaffold(
        topBar = {
            WeTopBar("设置性别", onBack = onBack) {
                WeButton("完成", size = ButtonSize.Small, disabled = false)
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(top = 12.dp)
        ) {
            WeRadioGroup(
                options = genderOptions,
                value = selectedGender
            ) {
                selectedGender = it
            }
        }
    }
}