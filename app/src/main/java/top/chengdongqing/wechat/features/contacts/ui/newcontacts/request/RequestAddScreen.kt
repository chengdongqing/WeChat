package top.chengdongqing.wechat.features.contacts.ui.newcontacts.request

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
fun RequestAddScreen(
    contactId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: RequestAddViewModel = hiltViewModel { factory: RequestAddViewModel.Factory ->
        factory.create(contactId)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = rememberToastState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                SendEvent.WaitingVerify -> {
                    toast.show("已发送好友申请", icon = ToastIcon.Success)
                    delay(1000)
                    onSuccess()
                }

                SendEvent.AutoAdded -> {
                    toast.show("已添加", icon = ToastIcon.Success)
                    delay(1000)
                    onSuccess()
                }

                is SendEvent.Error -> {
                    toast.show(event.message, icon = ToastIcon.Fail)
                }
            }
        }
    }

    ContactHandleBase(
        type = FriendActionType.Apply,
        greetingText = uiState.greetingMessage,
        onGreetingChange = viewModel::updateGreeting,
        remarkText = uiState.remark,
        onRemarkChange = viewModel::updateRemark,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onComplete = viewModel::sendRequest
    )
}