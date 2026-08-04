package top.chengdongqing.wechat.feature.settings.ui.notification

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.radio.WeRadioGroup
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.playback.RingtoneSound
import top.chengdongqing.wechat.core.playback.toUri

@Composable
fun RingtoneSettingScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val initialRingtone by viewModel.ringtone.collectAsStateWithLifecycle()
    var ringtone by remember(initialRingtone) { mutableStateOf(initialRingtone) }
    val ringtoneOptions = remember {
        RingtoneSound.entries.map {
            resources.getString(it.labelRes) to it
        }
    }
    val hasChanged = ringtone != initialRingtone

    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(R.string.notification_ringtone),
                onBack = onBack
            ) {
                WeButton(
                    text = stringResource(R.string.action_done),
                    size = ButtonSize.Small,
                    enabled = hasChanged
                ) {
                    viewModel.setRingtone(ringtone)
                    onBack()
                }
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
                options = ringtoneOptions,
                value = ringtone
            ) {
                ringtone = it
                viewModel.previewSound(it.toUri(context))
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
