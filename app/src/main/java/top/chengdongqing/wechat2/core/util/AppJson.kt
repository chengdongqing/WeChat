package top.chengdongqing.wechat2.core.util

import kotlinx.serialization.json.Json

/**
 * 全局 JSON 序列化配置
 *
 * 使用 Kotlinx Serialization 提供编译时类型安全的序列化
 */
object AppJson {
    val instance = Json {
        // 忽略 JSON 中未知的字段（向后兼容）
        ignoreUnknownKeys = true

        // 序列化时包含默认值
        encodeDefaults = true

        // 允许非严格 JSON 格式
        isLenient = true

        // 美化输出（调试用，生产环境可关闭）
        prettyPrint = false

        // 允许结构化类名称（用于多态）
        classDiscriminator = "type"

        // 强制要求所有属性都有值（严格模式）
        coerceInputValues = true  // 将无效值转为默认值
    }
}