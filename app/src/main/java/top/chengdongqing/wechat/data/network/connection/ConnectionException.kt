package top.chengdongqing.wechat.data.network.connection

import top.chengdongqing.wechat.data.database.entity.SendError

/**
 * 连接异常类
 */
class ConnectionException(
    message: String,
    val failReason: SendError
) : Exception(message)