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
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.settings.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.dialog.DialogManager
import top.chengdongqing.wechat.core.designsystem.components.menu.WeDangerButton
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.SemanticError
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.NavigationKey
import top.chengdongqing.wechat.core.util.appVersionName
import top.chengdongqing.wechat.core.util.showToast
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
            WeTopAppBar(title = stringResource(R.string.settings), onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    state = rememberScrollState(),
                    overscrollEffect = rememberBouncedOverscrollEffect()
                )
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeSettingGroup(stringResource(R.string.settings_group_account)) {
                WeSettingItem(
                    label = stringResource(R.string.settings_account_security),
                    showDivider = false,
                    onClick = {
                        backStack.add(NavigationKey.AccountSecuritySettings)
                    }
                )
            }

            WeSettingGroup(stringResource(R.string.settings_group_general)) {
                WeSettingItem(
                    label = stringResource(R.string.settings_notifications),
                    onClick = {
                        backStack.add(NavigationKey.NotificationSettings)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_display),
                    onClick = {
                        backStack.add(NavigationKey.DisplaySettings)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_privacy),
                    onClick = {
                        backStack.add(NavigationKey.PrivacySettings)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_storage),
                    onClick = {
                        backStack.add(NavigationKey.StorageSettings)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_more),
                    showDivider = false,
                    onClick = {
                        backStack.add(NavigationKey.MoreSettings)
                    }
                )
            }

            WeSettingGroup(stringResource(R.string.settings_group_features)) {
                WeSettingItem(
                    label = stringResource(R.string.settings_connection),
                    onClick = {
                        backStack.add(NavigationKey.ConnectionModeSettings)
                    }
                ) {
                    WeSettingValue(stringResource(connectionMode.labelRes))
                }
                WeSettingItem(
                    label = stringResource(R.string.settings_chat),
                    onClick = {
                        backStack.add(NavigationKey.ChatSettings)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_chat_history),
                    showDivider = false,
                    onClick = {
                        backStack.add(NavigationKey.ChatManagement)
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
                        backStack.add(NavigationKey.About)
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
    val resources = LocalResources.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.logoutResult.collect { result ->
            result.onSuccess {
                // 导航到登录页，清除回退栈
                backStack.clear()
                backStack.add(NavigationKey.Guide)
            }.onFailure {
                // 提示失败
                context.showToast(resources.getString(DesignR.string.msg_process_failed))
            }
        }
    }

    val showDialog = {
        DialogManager.show(
            title = resources.getString(R.string.settings_logout_title),
            content = resources.getString(R.string.settings_logout_content),
            okColor = SemanticError,
            onOk = viewModel::exit
        )
    }

    WeDangerButton(
        label = stringResource(R.string.settings_logout),
        onClick = showDialog
    )
}
