package top.chengdongqing.wechat.features.contacts.domain.model

import androidx.compose.runtime.Immutable
import top.chengdongqing.wechat.data.model.ContactAddSource
import top.chengdongqing.wechat.features.me.domain.model.Gender

/**
 * 联系人实体模型
 */
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
    val displayName: String
        get() = remarkName ?: nickname

    val isFriend: Boolean
        get() = relation == ContactRelation.Friend

    val isSelf: Boolean
        get() = relation == ContactRelation.Myself
}

/**
 * 联系人关系状态枚举
 */
enum class ContactRelation {
    /** 自己 */
    Myself,

    /** 已经是好友，可正常发送消息及查看朋友圈 */
    Friend,

    /** 陌生人（通过扫码、群聊等方式进入资料页，尚未添加好友） */
    NotFriend
}