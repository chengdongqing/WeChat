package top.chengdongqing.wechat.core.network.service

/**
 * 子服务模块共有方法定义
 */
interface ServiceModule {

    /**
     * 启动服务
     */
    fun start()

    /**
     * 停止服务
     */
    fun stop()
}