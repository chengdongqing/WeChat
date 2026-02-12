package top.chengdongqing.wechat.features.me.ui.profile.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.input.WeInput
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import top.chengdongqing.wechat.features.me.ui.profile.ProfileEventEffect
import top.chengdongqing.wechat.features.me.ui.profile.ProfileField
import top.chengdongqing.wechat.features.me.ui.profile.ProfileViewModel

@Composable
fun EditNameScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var nickname by remember { mutableStateOf("") }
    LaunchedEffect(uiState.profile) {
        uiState.profile?.nickname?.let { nickname = it }
    }

    ProfileEventEffect(viewModel, onBack)

    Scaffold(
        topBar = {
            WeTopBar(title = "更改名字", onBack = onBack) {
                WeButton(
                    "保存",
                    size = ButtonSize.Small,
                    enabled = nickname != uiState.profile?.nickname
                            && UserProfile.isValidName(nickname)
                ) {
                    viewModel.updateField(ProfileField.Nickname(nickname))
                }
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            WeInput(nickname, maxLength = 17) {
                nickname = it
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