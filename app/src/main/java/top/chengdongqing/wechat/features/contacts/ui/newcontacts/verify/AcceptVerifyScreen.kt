package top.chengdongqing.wechat.features.contacts.ui.newcontacts.verify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastIcon
import top.chengdongqing.wechat.core.designsystem.components.toast.rememberToastState
import top.chengdongqing.wechat.features.contacts.ui.newcontacts.components.ContactHandleBase
import top.chengdongqing.wechat.features.contacts.ui.newcontacts.components.FriendActionType

@Composable
fun AcceptVerifyScreen(
    requestId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AcceptVerifyViewModel = hiltViewModel { factory: AcceptVerifyViewModel.Factory ->
        factory.create(requestId)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = rememberToastState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AcceptVerifyEvent.AcceptSuccess -> {
                    toast.show("已添加好友", icon = ToastIcon.Success)
                    delay(1000)
                    onSuccess()
                }

                is AcceptVerifyEvent.ShowError -> {
                    toast.show(event.message, icon = ToastIcon.Fail)
                }
            }
        }
    }

    ContactHandleBase(
        type = FriendActionType.Verify,
        remarkText = uiState.remark,
        onRemarkChange = viewModel::updateRemark,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onComplete = viewModel::accept
    )
}