package top.chengdongqing.wechat.features.contacts.ui.add.nfc

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.nfc.HcePreferredService
import top.chengdongqing.wechat.core.nfc.NfcAvailability
import top.chengdongqing.wechat.core.nfc.NfcReaderDispatch
import top.chengdongqing.wechat.core.nfc.rememberNfcAvailability
import top.chengdongqing.wechat.features.contacts.domain.model.NfcConnectionState
import top.chengdongqing.wechat.features.contacts.ui.add.nfc.components.NfcConnected
import top.chengdongqing.wechat.features.contacts.ui.add.nfc.components.NfcConnecting
import top.chengdongqing.wechat.features.contacts.ui.add.nfc.components.NfcFailed
import top.chengdongqing.wechat.features.contacts.ui.add.nfc.components.NfcModeSwitch
import top.chengdongqing.wechat.features.contacts.ui.add.nfc.components.NfcUnavailable
import top.chengdongqing.wechat.features.contacts.ui.add.nfc.components.NfcWaiting

@Composable
fun NfcAddContactScreen(
    viewModel: NfcAddContactViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val nfcAvailability = rememberNfcAvailability()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isReaderMode by remember { mutableStateOf(true) }

    // 启用读卡器和前台优先注册
    if (nfcAvailability == NfcAvailability.Enabled) {
        HcePreferredService()
        NfcReaderDispatch(isReaderMode) { userId ->
            viewModel.onNfcDetected(userId)
        }
    }

    Scaffold(
        topBar = { NfcTopBar(onBack) },
        containerColor = WeTheme.colorScheme.surfaceVariant
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (nfcAvailability) {
                NfcAvailability.NotSupported ->
                    NfcUnavailable(
                        title = "设备不支持 NFC",
                        description = "你的手机没有 NFC 芯片，无法使用碰一碰功能。\n可以使用扫一扫来添加好友。",
                        actionLabel = "返回",
                        onAction = onBack
                    )

                NfcAvailability.Disabled ->
                    NfcUnavailable(
                        title = "NFC 未开启",
                        description = "请前往系统设置开启 NFC 功能后，再回来使用碰一碰。",
                        actionLabel = "前往开启 NFC",
                        onAction = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                    )

                NfcAvailability.Enabled ->
                    NfcMainContent(
                        uiState = uiState,
                        isReaderMode = isReaderMode,
                        onModeChange = { isReaderMode = it },
                        onAddFriend = viewModel::onAddFriend,
                        onRetry = viewModel::onRetry
                    )
            }
        }
    }
}

@Composable
private fun NfcMainContent(
    uiState: NfcAddContactUiState,
    isReaderMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    onAddFriend: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NfcModeSwitch(
            isReaderMode = isReaderMode,
            onModeChange = onModeChange,
            modifier = Modifier.padding(top = 20.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.94f))
                        .togetherWith(fadeOut(tween(200)))
                },
                label = "nfc_state"
            ) { state ->
                when (state.connectionState) {
                    is NfcConnectionState.Waiting ->
                        NfcWaiting(isReaderMode)

                    is NfcConnectionState.Connecting ->
                        NfcConnecting()

                    is NfcConnectionState.Connected ->
                        NfcConnected(
                            contact = state.profile!!,
                            addState = state.addState,
                            onAddFriend = onAddFriend
                        )

                    is NfcConnectionState.Failed ->
                        NfcFailed(
                            reason = state.connectionState.reason,
                            onRetry = onRetry
                        )
                }
            }
        }
    }
}

@Composable
private fun NfcTopBar(onBack: () -> Unit) {
    WeTopBar(
        title = "碰一碰",
        onBack = onBack,
        containerColor = WeTheme.colorScheme.surfaceVariant
    )
}