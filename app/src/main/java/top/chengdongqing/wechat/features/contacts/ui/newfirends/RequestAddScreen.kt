package top.chengdongqing.wechat.features.contacts.ui.newfirends

import androidx.compose.runtime.Composable
import top.chengdongqing.wechat.features.contacts.ui.newfirends.components.FriendActionType
import top.chengdongqing.wechat.features.contacts.ui.newfirends.components.FriendHandleBase

@Composable
fun RequestAddScreen(
    contactId: String,
    onBack: () -> Unit
) {
    FriendHandleBase(
        type = FriendActionType.Apply,
        contactId = contactId,
        onBack = onBack,
        onComplete = {}
    )
}