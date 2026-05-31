package top.chengdongqing.wechat.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.common.navigation.CommonKey
import top.chengdongqing.wechat.core.common.navigation.SettingsKey
import top.chengdongqing.wechat.core.common.util.appVersionName
import top.chengdongqing.wechat.core.common.util.showToast
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.menu.WeDangerButton
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.feature.settings.domain.model.labelRes
import top.chengdongqing.wechat.feature.settings.ui.connection.ConnectionSettingsViewModel

@Composable
fun SettingsScreen(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    viewModel: ConnectionSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val versionName = remember { context.appVersionName }

    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WeTopBar(title = stringResource(R.string.settings), onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    state = rememberScrollState(),
                    overscrollEffect = rememberBounceOverscrollEffect()
                )
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeSettingGroup(stringResource(R.string.settings_group_general)) {
                WeSettingItem(
                    label = stringResource(R.string.settings_notifications),
                    onClick = {
                        backStack.add(SettingsKey.Notification)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_display),
                    onClick = {
                        backStack.add(SettingsKey.Display)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_privacy),
                    onClick = {
                        backStack.add(SettingsKey.Privacy)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_storage),
                    onClick = {}
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_more),
                    showDivider = false,
                    onClick = {
                        backStack.add(SettingsKey.More)
                    }
                )
            }

            WeSettingGroup(stringResource(R.string.settings_group_features)) {
                WeSettingItem(
                    label = stringResource(R.string.settings_connection),
                    onClick = {
                        backStack.add(SettingsKey.ConnectionMode)
                    }
                ) {
                    WeSettingValue(stringResource(connectionMode.labelRes))
                }
                WeSettingItem(
                    label = stringResource(R.string.settings_chat),
                    onClick = {
                        backStack.add(SettingsKey.ChatSettings)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_chat_history),
                    showDivider = false,
                    onClick = {
                        backStack.add(SettingsKey.ChatManagement)
                    }
                )
            }

            WeSettingGroup(stringResource(R.string.settings_group_help)) {
                WeSettingItem(
                    label = stringResource(R.string.settings_help),
                    onClick = {}
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_about),
                    showDivider = false,
                    onClick = {
                        backStack.add(SettingsKey.About)
                    }
                ) {
                    WeSettingValue("${stringResource(R.string.settings_version)} $versionName")
                }
            }

            LogoutButton(backStack)
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun LogoutButton(
    backStack: NavBackStack<NavKey>,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val dialog = rememberDialogState()
    val resources = LocalResources.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.logoutResult.collect { result ->
            result.onSuccess {
                // 导航到登录页，清除回退栈
                backStack.clear()
                backStack.add(CommonKey.Welcome)
            }.onFailure {
                // 提示失败
                context.showToast(resources.getString(R.string.msg_process_failed))
            }
        }
    }

    val showDialog = {
        dialog.show(
            title = resources.getString(R.string.settings_logout_title),
            content = resources.getString(R.string.settings_logout_content),
            okColor = Danger,
            onOk = viewModel::exit
        )
    }

    WeDangerButton(
        label = stringResource(R.string.settings_logout),
        onClick = showDialog
    )
}