package top.chengdongqing.wechat.data.network.service

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