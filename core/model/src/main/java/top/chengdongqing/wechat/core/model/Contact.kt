package top.chengdongqing.wechat.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Contact(
    val id: String,
    val nickname: String,
    val avatarPath: String? = null,
    val remarkName: String? = null,
    val gender: Gender? = null,
    val signature: String? = null,
    val note: String? = null,
    val relation: ContactRelation = ContactRelation.NotFriend,
    val source: ContactAddSource? = ContactAddSource.QRCode,
    val isFromMe: Boolean = true,
    val isBlocked: Boolean = false,
    val addedAt: Long? = null,
    val publicKey: String? = null,
    val version: Long = 0
) {
    val displayName: String get() = remarkName ?: nickname
    val isFriend: Boolean get() = relation == ContactRelation.Friend
    val isStranger: Boolean get() = relation == ContactRelation.NotFriend
    val isSelf: Boolean get() = relation == ContactRelation.Myself
    val isAI: Boolean get() = relation == ContactRelation.AIAssistant
}
