package top.chengdongqing.wechat.feature.profile.ui.login

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.informationbar.InformationBarType
import top.chengdongqing.wechat.core.designsystem.components.informationbar.WeInformationBar
import top.chengdongqing.wechat.core.designsystem.components.input.WeInput
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.profile.ui.login.components.AvatarSelector

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSetupComplete: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.setup_title),
                onBack = onBack,
                backIconResId = R.drawable.ic_close_outlined,
                containerColor = WeTheme.colorScheme.surface
            )
        },
        containerColor = WeTheme.colorScheme.surface
    ) { paddingValues ->
        LoginContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onNicknameChange = viewModel::updateNickname,
            onAvatarChange = viewModel::updateAvatar,
            onComplete = {
                scope.launch {
                    keyboardController?.hide()
                    delay(300) // 等待键盘收起动画
                    viewModel.completeSetup(onSetupComplete)
                }
            },
            onErrorDismiss = viewModel::clearError
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoginContent(
    modifier: Modifier,
    uiState: LoginUiState,
    onNicknameChange: (String) -> Unit,
    onAvatarChange: (Uri?) -> Unit,
    onComplete: () -> Unit,
    onErrorDismiss: () -> Unit
) {
    val isKeyboardVisible = WindowInsets.isImeVisible
    val bottomPadding by animateDpAsState(
        targetValue = if (isKeyboardVisible) 0.dp else 40.dp,
        label = "ButtonMargin"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            AvatarSelector(
                avatarUri = uiState.avatarUri,
                onAvatarChange = onAvatarChange,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(32.dp))

            WeInput(
                value = uiState.nickname,
                label = stringResource(R.string.setup_nickname_label),
                placeholder = stringResource(R.string.setup_nickname_placeholder),
                activeColor = WeTheme.colorScheme.divider,
                maxLength = 17,
                enabled = !uiState.isLoading,
                onValueChange = onNicknameChange
            )

            Text(
                text = stringResource(R.string.setup_nickname_hint),
                fontSize = 13.sp,
                color = WeTheme.colorScheme.textSecondary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 4.dp, top = 12.dp)
            )

            // 弹性空白区域，将按钮推到底部
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 24.dp)
            )

            Column(
                modifier = Modifier
                    .imePadding()
                    .padding(bottom = bottomPadding)
            ) {
                WeButton(
                    text = stringResource(R.string.action_ok),
                    enabled = !uiState.isLoading,
                    onClick = onComplete
                )
            }
        }

        WeInformationBar(
            visible = uiState.errorMessage != null,
            message = uiState.errorMessage ?: "",
            type = InformationBarType.WarnStrong,
            autoClose = true,
            onDismiss = onErrorDismiss
        )
    }
}