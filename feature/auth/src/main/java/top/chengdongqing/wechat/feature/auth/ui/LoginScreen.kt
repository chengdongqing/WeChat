package top.chengdongqing.wechat.feature.auth.ui

import android.Manifest
import android.os.Build
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.informationbar.InformationBarType
import top.chengdongqing.wechat.core.designsystem.components.informationbar.WeInformationBar
import top.chengdongqing.wechat.core.designsystem.components.input.WeInput
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.LocalAppNavigator
import top.chengdongqing.wechat.core.navigation.ScreenRoute
import kotlin.time.Duration.Companion.milliseconds
import top.chengdongqing.wechat.feature.auth.R as AuthR

@Composable
fun LoginRoute(
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navigator = LocalAppNavigator.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is LoginUiEvent.NavigateToHome -> {
                        navigator.clear()
                        navigator.navigateTo(ScreenRoute.Home)
                    }
                }
            }
        }
    }

    LoginScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = navigator::back
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LoginScreen(
    state: LoginUiState = LoginUiState(),
    onIntent: (LoginUiIntent) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val localNetworkPermissionState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            rememberPermissionState(
                Manifest.permission.ACCESS_LOCAL_NETWORK
            ) {
                onIntent(LoginUiIntent.SubmitForm)
            }
        } else null

    fun checkLocalNetworkPermission() {
        if (localNetworkPermissionState != null) {
            if (!localNetworkPermissionState.status.isGranted) {
                localNetworkPermissionState.launchPermissionRequest()
            } else {
                onIntent(LoginUiIntent.SubmitForm)
            }
        } else {
            onIntent(LoginUiIntent.SubmitForm)
        }
    }

    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(AuthR.string.login_title),
                onBack = onBack,
                backIconResId = R.drawable.ic_close_outlined,
                containerColor = WeTheme.colorScheme.surface
            )
        },
        containerColor = WeTheme.colorScheme.surface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 22.dp)
        ) {
            LoginForm(
                state = state,
                onIntent = { intent ->
                    if (intent is LoginUiIntent.SubmitForm) {
                        scope.launch {
                            keyboardController?.hide()
                            delay(300.milliseconds) // 等待键盘收起动画
                            checkLocalNetworkPermission()
                        }
                    } else {
                        onIntent(intent)
                    }
                },
            )

            WeInformationBar(
                visible = state.errorMessage != null,
                message = state.errorMessage ?: "",
                type = InformationBarType.WarnStrong,
                autoClose = true,
                onDismiss = { onIntent(LoginUiIntent.ClearError) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoginForm(
    state: LoginUiState,
    onIntent: (LoginUiIntent) -> Unit,
) {
    val isKeyboardVisible = WindowInsets.isImeVisible
    val bottomPadding by animateDpAsState(
        targetValue = if (isKeyboardVisible) 0.dp else 40.dp,
        label = "ButtonMargin"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        AvatarSelector(
            avatarUri = state.avatarUri,
            onAvatarChange = { onIntent(LoginUiIntent.AvatarChanged(it)) },
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(32.dp))

        WeInput(
            value = state.userName,
            label = stringResource(AuthR.string.form_username_label),
            placeholder = stringResource(AuthR.string.form_username_placeholder),
            activeColor = WeTheme.colorScheme.divider,
            maxLength = 17,
            enabled = !state.isLoading,
            onValueChange = {
                onIntent(LoginUiIntent.UserNameChanged(it))
            }
        )

        Text(
            text = stringResource(AuthR.string.form_username_hint),
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
                isLoading = state.isLoading,
                onClick = {
                    onIntent(LoginUiIntent.SubmitForm)
                }
            )
        }
    }
}

@Preview
@Composable
private fun LoginPreview() {
    WeTheme {
        LoginScreen()
    }
}