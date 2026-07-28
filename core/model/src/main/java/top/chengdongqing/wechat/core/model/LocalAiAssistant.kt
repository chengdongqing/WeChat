package top.chengdongqing.wechat.core.model

/** Identity for the built-in assistant. Its messages never use a network transport. */
object LocalAiAssistant {
    const val ID = "local-ai:xiaowei"
    const val NAME = "小微同学"
    const val SIGNATURE = "完全在本机运行的 AI 助手"
}
