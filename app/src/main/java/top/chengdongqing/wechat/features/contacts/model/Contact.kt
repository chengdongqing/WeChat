package top.chengdongqing.wechat.features.contacts.model

import top.chengdongqing.wechat.data.model.Gender

/**
 * 联系人实体模型
 * * @property id 唯一标识符（通常为用户微信号或系统生成 UUID）
 * @property avatarUrl 头像链接地址
 * @property remarkName 备注名称（用户主动设置的昵称，优先级最高）
 * @property gender 性别：[Gender.Male], [Gender.Female] 或 null (未设置)
 * @property name 原始昵称（对方自己设置的名称）
 * @property tags 联系人标签列表，用于分类管理
 * @property note 详细备注信息（如来源、描述等）
 * @property momentPhotos 朋友圈缩略图资源 ID 列表（用于在详细资料页展示最近动态）
 * @property relation 与当前用户的关系状态，参见 [ContactRelation]
 */
data class Contact(
    val id: String,
    val avatarUrl: String,
    val remarkName: String,
    val gender: Gender? = null,
    val name: String = "",
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