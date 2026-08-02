package top.chengdongqing.wechat.feature.contacts.ui.friendrequest.verify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastIcon
import top.chengdongqing.wechat.core.designsystem.components.toast.rememberToastState
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.ContactHandleBase
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.FriendActionType

@Composable
fun AcceptFriendRequestScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AcceptFriendRequestViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = rememberToastState()
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AcceptFriendRequestEvent.AcceptSuccess -> {
                    toast.show(
                        title = resources.getString(R.string.contact_msg_friend_added),
                        icon = ToastIcon.Success
                    )
                    delay(1000)
                    onSuccess()
                }

                is AcceptFriendRequestEvent.ShowError -> {
                    toast.show(event.message, icon = ToastIcon.Fail)
                }
            }
        }
    }

    ContactHandleBase(
        type = FriendActionType.Verify,
        remark = uiState.remark,
        onRemarkChange = viewModel::updateRemark,
        note = uiState.note,
        onNoteChange = viewModel::updateNote,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onComplete = viewModel::accept
    )
}