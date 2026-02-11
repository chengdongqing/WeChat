package top.chengdongqing.wechat.features.me.ui.profile.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.input.WeInput
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.me.ui.profile.ProfileEventEffect
import top.chengdongqing.wechat.features.me.ui.profile.ProfileField
import top.chengdongqing.wechat.features.me.ui.profile.ProfileViewModel

@Composable
fun EditSignatureScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var signature by remember { mutableStateOf("") }
    LaunchedEffect(uiState.profile) {
        uiState.profile?.signature?.let { signature = it }
    }

    ProfileEventEffect(viewModel, onBack)

    Scaffold(
        topBar = {
            WeTopBar(title = "个性签名", onBack = onBack) {
                WeButton(
                    "保存",
                    size = ButtonSize.Small,
                    enabled = signature != uiState.profile?.signature
                ) {
                    viewModel.updateField(ProfileField.Signature(signature))
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