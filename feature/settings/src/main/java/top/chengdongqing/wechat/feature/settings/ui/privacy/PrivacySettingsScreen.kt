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
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.settings.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.NavigationKey

@Composable
fun PrivacySettingsScreen(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    val friendVerifyEnabled by viewModel.friendVerifyEnabled.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WeTopAppBar(
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
                    overscrollEffect = rememberBouncedOverscrollEffect()
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
                    backStack.add(NavigationKey.AddMeMethodSettings)
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
                    backStack.add(NavigationKey.ContactBlacklist)
                }
            )
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
