package top.chengdongqing.wechat.core.utils

import kotlinx.serialization.json.Json

object AppJson {
    val instance = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        // 允许非严格模式（比如忽略分号等，增强兼容性）
        isLenient = true
    }
}