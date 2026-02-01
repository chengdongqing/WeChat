package top.chengdongqing.wechat.ui.me.profile.gender

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.data.model.Gender
import top.chengdongqing.wechat.ui.components.button.ButtonSize
import top.chengdongqing.wechat.ui.components.button.WeButton
import top.chengdongqing.wechat.ui.components.radio.WeRadioGroup
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar

@Composable
fun GenderScreen(onBack: () -> Unit) {
    val genderOptions = remember {
        Gender.entries.filter { it != Gender.Unknow }.map { it.label to it }
    }
    var selectedGender by remember { mutableStateOf(Gender.Unknow) }

    Scaffold(
        topBar = {
            WeTopBar("设置性别", onBack = onBack) {
                WeButton("完成", size = ButtonSize.Small, disabled = false)
            }
        },
        containerColor = Color(0xFFEDEDED)
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