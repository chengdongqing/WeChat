package top.chengdongqing.wechat.data.network.service

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