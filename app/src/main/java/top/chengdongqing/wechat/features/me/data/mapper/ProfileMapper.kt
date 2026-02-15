package top.chengdongqing.wechat.features.me.data.mapper

import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.ContactRelation
import top.chengdongqing.wechat.features.me.domain.model.UserProfile

fun UserProfile.toContact() = Contact(
    id = id,
    nickname = nickname,
    avatarPath = avatarPath,
    signature = signature,
    gender = gender,
    relation = ContactRelation.Myself
)