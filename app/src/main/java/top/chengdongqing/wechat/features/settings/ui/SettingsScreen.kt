package top.chengdongqing.wechat.features.settings.ui

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
import androidx.navigation.NavHostController
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.menu.WeDangerButton
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.navigation.Screen
import top.chengdongqing.wechat.core.util.appVersionName
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.features.settings.domain.model.labelRes
import top.chengdongqing.wechat.features.settings.navigation.SettingsRoute
import top.chengdongqing.wechat.features.settings.ui.connection.ConnectionSettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavHostController,
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
                        navController.navigate(SettingsRoute.NotificationSettings.route)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_display),
                    onClick = {
                        navController.navigate(SettingsRoute.DisplaySettings.route)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_privacy),
                    onClick = {
                        navController.navigate(SettingsRoute.PrivacySettings.route)
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
                        navController.navigate(SettingsRoute.MoreSettings.route)
                    }
                )
            }
            WeSettingGroup(stringResource(R.string.settings_group_features)) {
                WeSettingItem(
                    label = stringResource(R.string.settings_connection),
                    onClick = {
                        navController.navigate(SettingsRoute.ConnectionModeSettings.route)
                    }
                ) {
                    WeSettingValue(stringResource(connectionMode.labelRes))
                }
                WeSettingItem(
                    label = stringResource(R.string.settings_chat),
                    onClick = {
                        navController.navigate(SettingsRoute.ChatSettings.route)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.settings_chat_history),
                    showDivider = false,
                    onClick = {
                        navController.navigate(SettingsRoute.ChatManagement.route)
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
                        navController.navigate(SettingsRoute.About.route)
                    }
                ) {
                    WeSettingValue("${stringResource(R.string.settings_version)} $versionName")
                }
            }
            LogoutButton(navController)
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun LogoutButton(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val dialog = rememberDialogState()
    val resources = LocalResources.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.logoutResult.collect { result ->
            result.onSuccess {
                // 导航到登录页，清除回退栈
                navController.navigate(Screen.Welcome.route) {
                    popUpTo(0) { inclusive = true }
                }
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