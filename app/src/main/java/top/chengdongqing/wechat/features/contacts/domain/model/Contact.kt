package top.chengdongqing.wechat.features.contacts.domain.model

import top.chengdongqing.wechat.data.model.Gender

/**
 * 联系人实体模型
 */
data class Contact(
    val id: String,
    val nickname: String,
    val avatarPath: String? = null,
    val remarkName: String? = null,
    val gender: Gender? = null,
    val signature: String? = null,
    val tags: List<String>? = null,
    val note: String? = null,
    val relation: ContactRelation = ContactRelation.NotFriend
) {
    /**
     * UI 显示名称：优先显示备注名 [remarkName]，若无备注则显示原始昵称 [nickname]
     */
    val displayName: String
        get() = remarkName ?: nickname

    val isFriend: Boolean
        get() = relation == ContactRelation.Friend

    val isMyself: Boolean
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