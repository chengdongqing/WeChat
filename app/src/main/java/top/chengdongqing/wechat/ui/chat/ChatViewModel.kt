package top.chengdongqing.wechat.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.util.DeviceUtils
import top.chengdongqing.wechat.core.util.prepareMediaResource
import top.chengdongqing.wechat.data.model.P2PPeer
import top.chengdongqing.wechat.data.network.P2pConnectionManager
import top.chengdongqing.wechat.data.repository.ChatRepository

class ChatViewModel(
    private val repository: ChatRepository,
    private val connectionManager: P2pConnectionManager,
    application: Application
) : AndroidViewModel(application) {

    private val myDeviceName = DeviceUtils.getDeviceName(application)

    // 1. 【大幅简化】邻居列表不再由 ViewModel 手动维护逻辑
    // 直接从 connectionManager 观察 StateFlow，UI 会自动刷新
    val nearbyPeers: StateFlow<List<P2PPeer>> = connectionManager.peers

    // 2. 观察来自 Repository 的消息流
    val messages = repository.messages

    init {
        // ViewModel 只负责发令：开始发现！
        // 至于怎么扫描、怎么清理离线(CleanupTimer)，那是 Manager 内部实现的细节
        // connectionManager.startDiscovery(myDeviceName)

        // 绑定消息接收：让网络层收到的消息直接进仓库入库
        // (注：这部分逻辑也可以放在 Repository 内部 init，看你喜好)
        observeIncomingMessages()
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            connectionManager.messageFlow.collect { envelope ->
                repository.onMessageReceived(envelope)
            }
        }
    }

    fun toggleDiscovery(enable: Boolean) {
        if (enable) {
            connectionManager.startDiscovery(myDeviceName)
        } else {
            connectionManager.stopDiscovery()
        }
    }

    fun sendText(peer: P2PPeer, text: String) {
        viewModelScope.launch {
            // 调用 Repository 的发送方法（它内部会处理网络发送和数据库存储）
            val result = repository.sendText(peer, text)
            result.onFailure {
                // 处理 UI 反馈，比如弹一个 Toast
            }
        }
    }

    fun sendImage(peer: P2PPeer, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val mediaResource = prepareMediaResource(context, uri) ?: return@launch
            val result = repository.sendImage(peer, mediaResource)
            result.onFailure {
                // 处理 UI 反馈，比如弹一个 Toast
            }
        }
    }

    /**
     * 4. 【连接逻辑】
     * 现在的 connect 是接口的一部分，不再在这里手写 Ktor 逻辑
     */
    fun connectToPeer(peer: P2PPeer) {
        viewModelScope.launch {
            val success = connectionManager.connect(peer)
            if (success) {
                // 连接成功后的逻辑，比如跳转页面或显示连接状态
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 停止发现，释放资源
        connectionManager.stopDiscovery()
    }
}