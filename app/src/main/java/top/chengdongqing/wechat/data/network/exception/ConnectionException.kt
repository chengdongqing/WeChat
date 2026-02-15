package top.chengdongqing.wechat.data.network.exception

class ConnectionException(
    message: String, cause: Throwable? = null
) : Exception(message, cause)