package top.chengdongqing.wechat.feature.contacts.ui.friendrequest.request

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastIcon
import top.chengdongqing.wechat.core.designsystem.components.toast.rememberToastState
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.components.ContactHandleBase
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.components.FriendActionType

@Composable
fun RequestAddScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: RequestAddViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = rememberToastState()
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                SendEvent.WaitingVerify -> {
                    toast.show(
                        title = resources.getString(R.string.contact_msg_request_sent),
                        icon = ToastIcon.Success
                    )
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
        greeting = uiState.greeting,
        onGreetingChange = viewModel::updateGreeting,
        remark = uiState.remark,
        onRemarkChange = viewModel::updateRemark,
        note = uiState.note,
        onNoteChange = viewModel::updateNote,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onComplete = viewModel::sendRequest
    )
}