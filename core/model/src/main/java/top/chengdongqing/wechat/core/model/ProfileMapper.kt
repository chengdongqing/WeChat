package top.chengdongqing.wechat.core.model

fun UserProfile.toContact() = Contact(
    id = id,
    nickname = nickname,
    avatarPath = avatarPath,
    signature = signature,
    gender = gender,
    relation = ContactRelation.Myself
)
