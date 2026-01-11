package top.chengdongqing.wechat.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.util.IdManager
import top.chengdongqing.wechat.core.util.getDeviceName
import top.chengdongqing.wechat.core.util.prepareMediaResource
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer
import top.chengdongqing.wechat.data.network.P2pConnectionManager
import top.chengdongqing.wechat.data.repository.ChatRepository

class ChatViewModel(
    private val repository: ChatRepository,
    private val connectionManager: P2pConnectionManager,
    application: Application
) : AndroidViewModel(application) {
    // 设备列表流
    val nearbyPeers = connectionManager.peers

    // 消息列表流
    val messages = repository.messages

    // 是否扫描中
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering = _isDiscovering.asStateFlow()

    // 扫描自动结束定时器
    private var discoveryTimeoutJob: Job? = null

    // 设备id和name
    private val deviceId by lazy { IdManager(application).getMyId() }
    private val deviceName by lazy { application.getDeviceName() }

    init {
        // 启动消息接收程序
        connectionManager.startMessageServer()
    }

    /**
     * 切换扫描状态
     */
    fun toggleDiscovery() {
        if (!isDiscovering.value) {
            startDiscovery()
        } else {
            stopDiscovery()
        }
    }

    /**
     * 开始扫描设备
     */
    fun startDiscovery() {
        _isDiscovering.value = true
        connectionManager.startDiscovery(deviceName)

        // 开启自动停止计时
        discoveryTimeoutJob?.cancel()
        discoveryTimeoutJob = viewModelScope.launch {
            delay(30000) // 30秒倒计时
            stopDiscovery()
        }
    }

    /**
     * 停止扫描设备
     */
    fun stopDiscovery() {
        discoveryTimeoutJob?.cancel()
        connectionManager.stopDiscovery()
        _isDiscovering.value = false
    }

    fun sendText(peer: P2PPeer, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val payload = ChatPayload.Text(content = text)
            val envelope = MessageEnvelope(
                senderId = deviceId,
                senderName = deviceName,
                payload = payload
            )
            val result = repository.sendText(peer, envelope)
            result.onFailure {
                // 处理 UI 反馈，比如弹一个 Toast
            }
        }
    }

    fun sendMedia(peer: P2PPeer, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            // 获取媒体文件信息
            val mediaResource = prepareMediaResource(application, uri) ?: return@launch
            val payload = ChatPayload.Media(
                fileId = randomUUID(),
                fileName = mediaResource.fileName,
                mimeType = mediaResource.mimeType,
                size = mediaResource.size,
                localPath = mediaResource.file.absolutePath,
                thumbBase64 = mediaResource.thumbBase64
            )
            val envelope = MessageEnvelope(
                senderId = deviceId,
                senderName = deviceName,
                payload = payload
            )
            val result = repository.sendMedia(peer, envelope, mediaResource.file)
            result.onFailure {
                // 处理 UI 反馈，比如弹一个 Toast
            }
        }
    }

    fun connectToPeer(peer: P2PPeer) {
        viewModelScope.launch {
            val success = connectionManager.connect(peer)
            if (success) {
                // TODO 连接成功后的逻辑，比如跳转页面或显示连接状态
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        // 停止扫描设备
        stopDiscovery()
        // 停止接收消息
        connectionManager.stopMessageServer()
    }
}