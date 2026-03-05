package top.chengdongqing.wechat.features.settings.ui.privacy

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.components.contact.ContactListItem
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.features.contacts.domain.model.ContactItem

@Composable
fun ContactBlacklistScreen(
    onBack: () -> Unit,
    onNavigateToContactDetail: (contactId: String) -> Unit
) {
    val contacts by remember { mutableStateOf(emptyList<ContactItem>()) }

    Scaffold(
        topBar = {
            WeTopBar(title = "通讯录黑名单", onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            overscrollEffect = rememberBounceOverscrollEffect()
        ) {
            items(
                items = contacts,
                key = { it.id }
            ) { contact ->
                ContactListItem(
                    contact = contact,
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