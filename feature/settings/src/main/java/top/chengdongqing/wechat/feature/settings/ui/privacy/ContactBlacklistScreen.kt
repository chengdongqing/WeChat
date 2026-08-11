package top.chengdongqing.wechat.feature.settings.ui.privacy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.settings.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.contact.ContactListItem
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun ContactBlacklistScreen(
    onBack: () -> Unit,
    onNavigateToContactDetail: (contactId: String) -> Unit,
    viewModel: ContactBlacklistViewModel = hiltViewModel()
) {
    val contacts by viewModel.blockedContacts.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(R.string.privacy_blacklist_title),
                onBack = onBack
            )
        },
        containerColor = WeTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            overscrollEffect = rememberBouncedOverscrollEffect()
        ) {
            items(
                items = contacts,
                key = { it.id }
            ) { contact ->
                ContactListItem(
                    displayName = contact.displayName,
                    avatarModel = contact.avatarPath,
                    note = contact.note,
                    modifier = Modifier.clickable {
                        onNavigateToContactDetail(contact.id)
                    }
                )
                WeDivider(modifier = Modifier.padding(start = 68.dp))
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
