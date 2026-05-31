package top.chengdongqing.wechat.feature.contacts.ui.detail

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberCallLauncher
import top.chengdongqing.wechat.feature.call.ui.startCall
import top.chengdongqing.wechat.feature.contacts.ui.detail.components.ContactDetailContent

@Composable
fun ContactDetailScreen(
    onBack: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToMoments: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSetting: () -> Unit = {},
    onNavigateToRequestAdd: () -> Unit = {},
    viewModel: ContactDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contact by viewModel.contact.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val launchCall = rememberCallLauncher(contact?.id ?: "") { id, type ->
        context.startCall(id, type)
    }

    // 处理导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToChat -> onNavigateToChat()
                is NavigationEvent.LaunchCall -> launchCall(event.type)
                is NavigationEvent.NavigateToMoments -> onNavigateToMoments()
                is NavigationEvent.NavigateToProfile -> onNavigateToProfile()
                is NavigationEvent.ShowMoreOptions -> onNavigateToSetting()
                is NavigationEvent.NavigateToRequestAdd -> onNavigateToRequestAdd()
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
                showMoreAction = contact?.isFriend ?: false,
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
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            contact?.let {
                ContactDetailContent(
                    contact = it,
                    onAction = viewModel::handleAction
                )
            }
        }
    }
}

@Composable
private fun ContactDetailTopBar(
    showMoreAction: Boolean,
    onBack: () -> Unit,
    onMoreClick: () -> Unit
) {
    WeTopBar(
        containerColor = WeTheme.colorScheme.surface,
        onBack = onBack
    ) {
        if (showMoreAction) {
            ActionIcon(
                icon = R.drawable.ic_more_outlined,
                description = stringResource(R.string.action_more),
                onClick = onMoreClick
            )
        }
    }
}