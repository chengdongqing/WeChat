package top.chengdongqing.wechat.features.profile.data.mapper

import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.ContactRelation
import top.chengdongqing.wechat.features.profile.domain.model.UserProfile

fun UserProfile.toContact() = Contact(
    id = id,
    nickname = nickname,
    avatarPath = avatarPath,
    signature = signature,
    gender = gender,
    relation = ContactRelation.Myself
)