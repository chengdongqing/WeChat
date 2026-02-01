package top.chengdongqing.wechat.ui.contacts.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.Contact
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar
import top.chengdongqing.wechat.ui.contacts.detail.components.ContactDetailContent

/**
 * 联系人详情页面
 *
 * @param contactId 联系人ID
 * @param onBack 返回回调
 * @param onNavigateToChat 导航到聊天页面
 * @param onNavigateToCall 导航到通话页面
 * @param onNavigateToMoments 导航到朋友圈
 * @param onNavigateToProfile 导航到资料编辑
 */
@Composable
fun ContactDetailScreen(
    contactId: String,
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToCall: (String) -> Unit = {},
    onNavigateToMoments: (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToSetting: (String) -> Unit = {},
    viewModel: ContactDetailViewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
        factory.create(contactId)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 处理导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToChat -> onNavigateToChat(event.contactId)
                is NavigationEvent.NavigateToCall -> onNavigateToCall(event.contactId)
                is NavigationEvent.NavigateToMoments -> onNavigateToMoments(event.contactId)
                is NavigationEvent.NavigateToProfile -> onNavigateToProfile(event.contactId)
                is NavigationEvent.ShowMoreOptions -> onNavigateToSetting(event.contactId)
            }
        }
    }

    Scaffold(
        topBar = {
            ContactDetailTopBar(
                onBack = onBack,
                onMoreClick = { viewModel.handleAction(ContactAction.ShowMore) }
            )
        },
        containerColor = Color(0xFFEDEDED)
    ) { innerPadding ->
        ContactDetailScrollableContent(
            contact = uiState.contact,
            onAction = viewModel::handleAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * 联系人详情顶部导航栏
 */
@Composable
private fun ContactDetailTopBar(
    onBack: () -> Unit,
    onMoreClick: () -> Unit
) {
    WeTopBar(
        title = "",
        containerColor = Color.White,
        onBack = onBack
    ) {
        ActionIcon(
            iconResId = R.drawable.ic_more_outlined,
            description = "更多",
            onClick = onMoreClick
        )
    }
}

/**
 * 联系人详情可滚动内容区域
 */
@Composable
private fun ContactDetailScrollableContent(
    contact: Contact,
    onAction: (ContactAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
    ) {
        ContactDetailContent(
            contact = contact,
            onAction = onAction
        )
    }
}

/**
 * 操作类型
 */
sealed class ContactAction {
    data object SendMessage : ContactAction()
    data object VoiceVideoCall : ContactAction()
    data object ViewMoments : ContactAction()
    data object ViewProfile : ContactAction()
    data object ShowMore : ContactAction()
}