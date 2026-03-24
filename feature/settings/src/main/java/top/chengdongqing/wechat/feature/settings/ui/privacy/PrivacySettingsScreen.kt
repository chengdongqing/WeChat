package top.chengdongqing.wechat.feature.settings.ui.privacy

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import top.chengdongqing.wechat.core.common.navigation.SettingsRoute
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect

@Composable
fun PrivacySettingsScreen(
    navController: NavHostController,
    onBack: () -> Unit,
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    val friendVerifyEnabled by viewModel.friendVerifyEnabled.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.settings_privacy),
                onBack = onBack
            )
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
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeSettingItem(
                label = stringResource(R.string.privacy_add_verify),
                showArrow = false,
                showDivider = false
            ) {
                WeSwitch(
                    checked = friendVerifyEnabled,
                    onChange = viewModel::toggleFriendVerify
                )
            }
            WeSettingItem(
                label = stringResource(R.string.privacy_add_method),
                showDivider = false,
                onClick = {
                    navController.navigate(SettingsRoute.AddMeMethodSetting.route)
                }
            )
            WeSettingGroup(stringResource(R.string.settings_privacy)) {
                WeSettingItem(
                    label = stringResource(R.string.privacy_chat_only),
                    onClick = {}
                )
                WeSettingItem(
                    label = stringResource(R.string.privacy_moments),
                    onClick = {}
                )
                WeSettingItem(
                    label = stringResource(R.string.privacy_channels),
                    onClick = {}
                )
                WeSettingItem(
                    label = stringResource(R.string.privacy_top_stories),
                    onClick = {}
                )
                WeSettingItem(
                    label = stringResource(R.string.privacy_sports),
                    showDivider = false,
                    onClick = {}
                )
            }
            WeSettingItem(
                label = stringResource(R.string.privacy_blacklist),
                showDivider = false,
                onClick = {
                    navController.navigate(SettingsRoute.ContactBlacklist.route)
                }
            )
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}