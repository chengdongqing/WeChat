package top.chengdongqing.wechat.features.contacts.data.model

import top.chengdongqing.wechat.data.model.Gender

/**
 * 联系人实体模型
 */
data class Contact(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val remarkName: String = "",
    val gender: Gender? = null,
    val signature: String? = null,
    val tags: List<String> = emptyList(),
    val note: String = "",
    val momentPhotos: List<Int> = emptyList(),
    val relation: ContactRelation = ContactRelation.NotFriend
) {
    /**
     * UI 显示名称：优先显示备注名 [remarkName]，若无备注则显示原始昵称 [name]
     */
    val displayName: String
        get() = remarkName.ifBlank { name }

    /**
     * 是否是好友
     */
    val isFriend: Boolean
        get() = relation == ContactRelation.Friend
}

/**
 * 联系人关系状态枚举
 */
enum class ContactRelation {
    /** 已经是好友，可正常发送消息及查看朋友圈 */
    Friend,

    /** 陌生人（通过扫码、群聊等方式进入资料页，尚未添加好友） */
    NotFriend,

    /** 黑名单：已屏蔽该用户，阻断所有消息往来 */
    BlackList
}