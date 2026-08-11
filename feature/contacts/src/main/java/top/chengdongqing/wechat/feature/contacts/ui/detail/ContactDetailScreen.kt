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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.LocalCallLauncher
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun ContactDetailScreen(
    onBack: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToMoments: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSetting: () -> Unit = {},
    onNavigateToRequestAdd: () -> Unit = {},
    isAiAssistant: Boolean = false,
    viewModel: ContactDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contact by viewModel.contact.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val launchCall = LocalCallLauncher.current.rememberLauncher(contact?.id.orEmpty())
    val currentLaunchCall by rememberUpdatedState(launchCall)

    // 处理导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToChat -> onNavigateToChat()
                is NavigationEvent.LaunchCall -> currentLaunchCall(event.type)
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
            TopBar(
                moreActionVisible = !isAiAssistant && (contact?.isFriend ?: false),
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
private fun TopBar(
    moreActionVisible: Boolean,
    onBack: () -> Unit,
    onMoreClick: () -> Unit
) {
    WeTopAppBar(
        containerColor = WeTheme.colorScheme.surface,
        onBack = onBack
    ) {
        if (moreActionVisible) {
            IconButton(
                icon = DesignR.drawable.ic_more_outlined,
                description = stringResource(DesignR.string.action_more),
                onClick = onMoreClick
            )
        }
    }
}
