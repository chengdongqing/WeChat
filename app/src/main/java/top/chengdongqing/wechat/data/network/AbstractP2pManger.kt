package top.chengdongqing.wechat.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import top.chengdongqing.wechat.data.model.P2PPeer

abstract class AbstractP2pManger {
    // 协议前缀
    protected val protocolPrefix: String = "WeChat"

    // 协议分隔符
    protected val protocolSeparator: String = "|"

    /**
     * 编码身份信息
     */
    protected fun encodeIdentity(id: String, name: String): String =
        arrayOf(protocolPrefix, id, name).joinToString(protocolSeparator)

    /**
     * 解码身份信息
     */
    protected fun decodeIdentity(text: String): P2PPeer? {
        if (text.startsWith("$protocolPrefix$protocolSeparator")) {
            val parts = text.split(protocolSeparator)
            if (parts.size >= 3) {
                val (_, id, name) = parts
                return object : P2PPeer {
                    override val id: String = id
                    override val name: String = name
                }
            }
        }
        return null
    }

    /**
     * 更新列表中的元素，如果 ID 匹配则替换，否则添加
     */
    protected fun <T> MutableStateFlow<List<T>>.upsert(
        item: T,
        idSelector: (T) -> Any
    ) {
        this.update { current ->
            val index = current.indexOfFirst { idSelector(it) == idSelector(item) }
            if (index != -1) {
                current.toMutableList().apply { set(index, item) }
            } else {
                current + item
            }
        }
    }
}