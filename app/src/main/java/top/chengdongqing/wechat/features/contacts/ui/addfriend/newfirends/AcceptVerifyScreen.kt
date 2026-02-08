package top.chengdongqing.wechat.features.contacts.ui.addfriend.newfirends

import androidx.compose.runtime.Composable
import top.chengdongqing.wechat.features.contacts.ui.addfriend.newfirends.components.FriendActionType
import top.chengdongqing.wechat.features.contacts.ui.addfriend.newfirends.components.FriendHandleBase

@Composable
fun AcceptVerifyScreen(
    contactId: String,
    onBack: () -> Unit
) {
    FriendHandleBase(
        type = FriendActionType.Verify,
        contactId = contactId,
        onBack = onBack,
        onComplete = {}
    )
}