package top.chengdongqing.wechat.feature.contacts.ui.add.nfc

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.contacts.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.contacts.ui.add.nfc.util.HcePreferredService
import top.chengdongqing.wechat.feature.contacts.ui.add.nfc.util.NfcAvailability
import top.chengdongqing.wechat.feature.contacts.ui.add.nfc.util.NfcReaderDispatch
import top.chengdongqing.wechat.feature.contacts.ui.add.nfc.util.rememberNfcAvailability

@Composable
fun NFCAddFriendScreen(
    onBack: () -> Unit,
    onNavigateToContact: (id: String) -> Unit,
    viewModel: NFCAddFriendViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val nfcAvailability = rememberNfcAvailability()
    var isReaderMode by remember { mutableStateOf(true) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 启用读卡器和前台优先注册
    if (nfcAvailability == NfcAvailability.Enabled) {
        HcePreferredService()
        NfcReaderDispatch(isReaderMode) { userId ->
            viewModel.handleNfcDetected(userId) {
                onNavigateToContact(userId)
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(R.string.add_contact_option_nfc_desc),
                onBack = onBack,
                containerColor = Color.Unspecified
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (nfcAvailability) {
                NfcAvailability.NotSupported ->
                    NfcUnavailable(
                        title = stringResource(R.string.nfc_not_supported_title),
                        description = stringResource(R.string.nfc_not_supported_description),
                        actionLabel = stringResource(DesignR.string.action_back),
                        onAction = onBack
                    )

                NfcAvailability.Disabled ->
                    NfcUnavailable(
                        title = stringResource(R.string.nfc_disabled_title),
                        description = stringResource(R.string.nfc_disabled_description),
                        actionLabel = stringResource(R.string.nfc_action_go_to_settings),
                        onAction = {
                            context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                        }
                    )

                NfcAvailability.Enabled ->
                    NfcMainContent(
                        isReaderMode = isReaderMode,
                        onModeChange = { isReaderMode = it }
                    )
            }
        }
    }

    LoadingDialog(uiState.isLoading)
}

@Composable
private fun NfcMainContent(
    isReaderMode: Boolean,
    onModeChange: (Boolean) -> Unit,
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
            NfcWaiting(isReaderMode)
        }
    }
}
