package top.chengdongqing.wechat.core.model

object LocalAiAssistant {
    const val ID = "wxid_xiaowei"
    const val NAME = "小微同学"
    const val SIGNATURE = "本地 AI 助手"
}

fun LocalAiAssistant.toContact() = Contact(
    id = ID,
    nickname = NAME,
    note = SIGNATURE,
    relation = ContactRelation.AIAssistant
)
