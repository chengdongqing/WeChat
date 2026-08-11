package top.chengdongqing.wechat.feature.settings.ui.security

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.settings.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.menu.WeDangerButton
import top.chengdongqing.wechat.core.designsystem.components.pin.PinEntry
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

private enum class PinStep { VerifyOld, Create, Confirm }

@Composable
fun AppLockSettingsScreen(
    onBack: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    var step by remember(viewModel.isEnabled) {
        mutableStateOf(if (viewModel.isEnabled) PinStep.VerifyOld else PinStep.Create)
    }
    var firstPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var disableAfterVerification by remember { mutableStateOf(false) }

    val title = when (step) {
        PinStep.VerifyOld -> "输入当前密码"
        PinStep.Create -> "设置密码"
        PinStep.Confirm -> "确认密码"
    }

    Scaffold(
        topBar = {
            WeTopAppBar(title = stringResource(R.string.settings_app_lock), onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                PinEntry(
                    title = title,
                    error = error,
                    onPinComplete = { pin ->
                        when (step) {
                            PinStep.VerifyOld -> {
                                if (!viewModel.verify(pin)) {
                                    error = if (viewModel.isTemporarilyLocked) {
                                        "尝试次数过多，请稍后重试"
                                    } else {
                                        "密码错误"
                                    }
                                } else if (disableAfterVerification) {
                                    viewModel.disable()
                                    onBack()
                                } else {
                                    error = null
                                    step = PinStep.Create
                                }
                            }

                            PinStep.Create -> {
                                firstPin = pin
                                error = null
                                step = PinStep.Confirm
                            }

                            PinStep.Confirm -> {
                                if (pin == firstPin) {
                                    viewModel.save(pin)
                                    onBack()
                                } else {
                                    error = "两次输入不一致，请重新设置"
                                    firstPin = ""
                                    step = PinStep.Create
                                }
                            }
                        }
                    }
                )
            }

            if (viewModel.isEnabled && step == PinStep.VerifyOld) {
                WeDangerButton("取消应用锁") {
                    disableAfterVerification = true
                    error = "请输入当前密码以取消应用锁"
                }
            }
        }
    }
}
