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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberCallLauncher
import top.chengdongqing.wechat.features.call.ui.startCall
import top.chengdongqing.wechat.features.contacts.ui.detail.components.ContactDetailContent

@Composable
fun ContactDetailScreen(
    contactId: String,
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToMoments: (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToSetting: (String) -> Unit = {},
    onNavigateToRequestAdd: (String) -> Unit = {},
    viewModel: ContactDetailViewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
        factory.create(contactId)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contact = viewModel.contact.collectAsStateWithLifecycle().value ?: return
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val launchCall = rememberCallLauncher(contact.id) { id, type ->
        context.startCall(id, type)
    }

    // 处理导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToChat -> onNavigateToChat(contactId)
                is NavigationEvent.LaunchCall -> launchCall(event.type)
                is NavigationEvent.NavigateToMoments -> onNavigateToMoments(contactId)
                is NavigationEvent.NavigateToProfile -> onNavigateToProfile(contactId)
                is NavigationEvent.ShowMoreOptions -> onNavigateToSetting(contactId)
                is NavigationEvent.NavigateToRequestAdd -> onNavigateToRequestAdd(contactId)
                else -> {}
            }
        }
    }

    // 显示错误
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
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