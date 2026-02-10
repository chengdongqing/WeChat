package top.chengdongqing.wechat.features.contacts.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.contacts.ui.detail.components.ContactDetailContent

@Composable
fun ContactDetailScreen(
    contactId: String,
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToCall: (String) -> Unit = {},
    onNavigateToMoments: (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToSetting: (String) -> Unit = {},
    onNavigateToRequestAdd: (String) -> Unit = {},
    viewModel: ContactDetailViewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
        factory.create(contactId)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val contact = uiState.contact ?: return

    // 处理导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToChat -> onNavigateToChat(event.contactId)
                is NavigationEvent.NavigateToCall -> onNavigateToCall(event.contactId)
                is NavigationEvent.NavigateToMoments -> onNavigateToMoments(event.contactId)
                is NavigationEvent.NavigateToProfile -> onNavigateToProfile(event.contactId)
                is NavigationEvent.ShowMoreOptions -> onNavigateToSetting(event.contactId)
                is NavigationEvent.NavigateToRequestAdd -> onNavigateToRequestAdd(event.contactId)
                else -> {}
            }
        }
    }

    // 显示错误
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            ContactDetailTopBar(
                showMoreAction = !contact.isMyself,
                onBack = onBack
            ) {
                viewModel.handleAction(ContactAction.ShowMore)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            ContactDetailContent(
                contact = contact,
                onAction = viewModel::handleAction
            )
        }
    }
}

/**
 * 联系人详情顶部导航栏
 */
@Composable
private fun ContactDetailTopBar(
    showMoreAction: Boolean,
    onBack: () -> Unit,
    onMoreClick: () -> Unit
) {
    WeTopBar(
        title = "",
        containerColor = Color.White,
        onBack = onBack
    ) {
        if (showMoreAction) {
            ActionIcon(
                iconResId = R.drawable.ic_more_outlined,
                description = "更多",
                onClick = onMoreClick
            )
        }
    }
}