package top.chengdongqing.wechat.feature.contacts.ui.friendrequest.request

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
fun RequestAddFriendScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: RequestAddFriendViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                SendEvent.WaitingVerify -> {
                    ToastManager.success(resources.getString(R.string.contact_msg_request_sent))
                    delay(1.seconds)
                    onSuccess()
                }

                is SendEvent.Error -> {
                    ToastManager.fail(event.message)
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
