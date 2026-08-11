package top.chengdongqing.wechat.core.model

object LocalAiAssistant {
    const val ID = "wxid_xiaowei"
}

fun LocalAiAssistant.toContact(name: String, signature: String) = Contact(
    id = ID,
    nickname = name,
    note = signature,
    signature = signature,
    relation = ContactRelation.AIAssistant
)
