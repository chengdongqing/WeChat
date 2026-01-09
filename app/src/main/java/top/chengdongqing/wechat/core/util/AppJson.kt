package top.chengdongqing.wechat.core.util

import kotlinx.serialization.json.Json

object AppJson {
    val instance = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        // 如果你想在调试时肉眼看 JSON 更舒服，可以加上：
//        prettyPrint = true
        // 允许非严格模式（比如忽略分号等，增强兼容性）
        isLenient = true
    }
}