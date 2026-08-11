package top.chengdongqing.wechat.feature.contacts.ui.friendrequest.verify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.contacts.R
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastManager
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.ContactHandleBase
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.FriendActionType
import kotlin.time.Duration.Companion.seconds

@Composable
fun AcceptFriendRequestScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AcceptFriendRequestViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AcceptFriendRequestEvent.AcceptSuccess -> {
                    ToastManager.show(resources.getString(R.string.contact_msg_friend_added))
                    delay(1.seconds)
                    onSuccess()
                }

                is AcceptFriendRequestEvent.ShowError -> {
                    ToastManager.fail(event.message)
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
