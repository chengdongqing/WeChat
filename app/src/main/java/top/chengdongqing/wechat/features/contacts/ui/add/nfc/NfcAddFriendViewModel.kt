package top.chengdongqing.wechat.features.contacts.ui.add.nfc

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.util.toMD5Hex
import top.chengdongqing.wechat.data.network.discovery.BLEDiscovery
import top.chengdongqing.wechat.data.network.protocol.P2PMessage
import top.chengdongqing.wechat.data.network.protocol.P2PMessageTransmitter
import top.chengdongqing.wechat.data.network.service.modules.BLEModule
import top.chengdongqing.wechat.data.network.service.modules.FriendRequestEvent
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import java.io.File
import javax.inject.Inject

private const val TAG = "NfcAddFriendVM"

/**
 * 从 BLE 拉取到的对方资料（展示用）
 */
data class NfcPeerProfile(
    val id: String,
    val nickname: String,
    val avatarUrl: String,
    val avatarBytes: ByteArray? = null,
    val region: String = "",
    val signature: String = ""
)

// ==================== UI 状态 ====================

data class NfcAddFriendUiState(
    /** NFC / BLE 连接状态 */
    val connectionState: NfcConnectionState = NfcConnectionState.Waiting,
    /** 从 BLE 拉取到的对方资料 */
    val peerProfile: NfcPeerProfile? = null,
    /** 添加流程状态 */
    val addState: NfcAddState = NfcAddState.Idle
)

sealed class NfcConnectionState {
    /** 等待碰一碰 */
    object Waiting : NfcConnectionState()

    /** 正在通过 BLE 拉取对方信息 */
    object Connecting : NfcConnectionState()

    /** 拉取成功，展示对方信息 */
    object Connected : NfcConnectionState()

    /** 连接失败 */
    data class Failed(val reason: String) : NfcConnectionState()
}

sealed class NfcAddState {
    /** 初始状态：还未点击添加 */
    object Idle : NfcAddState()

    /** 我已点击，等待对方点击 */
    object WaitingForPeer : NfcAddState()

    /** 对方已点击，等待我点击（且我还未点击） */
    object PeerReady : NfcAddState()

    /** 双方都已点击，正在通过 BLE 交换资料 */
    object Exchanging : NfcAddState()

    /** 添加成功 */
    object Success : NfcAddState()

    /** 添加失败 */
    data class Error(val message: String) : NfcAddState()

    /** 等待超时 */
    object Timeout : NfcAddState()
}

// ==================== ViewModel ====================

@HiltViewModel
class NfcAddFriendViewModel @Inject constructor(
    private val bleDiscovery: BLEDiscovery,
    private val bleModule: BLEModule,
    private val p2pTransmitter: P2PMessageTransmitter,
    private val profileRepository: ProfileRepository,
    private val contactRepository: ContactRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NfcAddFriendUiState())
    val uiState: StateFlow<NfcAddFriendUiState> = _uiState.asStateFlow()

    /** 我方当前登录资料（懒加载，首次使用时获取） */
    private var myProfile: UserProfile? = null

    /**
     * 本次会话唯一ID。
     * 规则：将双方 userId 排序后拼接再 MD5，保证双方生成的 ID 完全一致，实现幂等。
     */
    private var sessionRequestId: String = ""

    /** 对方 userId（从 NFC Tag 读取） */
    private var peerUserId: String = ""

    /**
     * 对方已发来的 NfcAddRequest 暂存。
     * 当对方先点击、我后点击时，我点击后直接使用此数据存库并回复。
     */
    private var pendingPeerRequest: Pair<P2PMessage.NfcAddRequest, ByteArray?>? = null

    /** WaitingForPeer 超时 Job */
    private var timeoutJob: Job? = null

    companion object {
        /** 等待对方点击的最长时间（毫秒） */
        private const val WAITING_TIMEOUT_MS = 60_000L
    }

    init {
        loadMyProfile()
        observeBleEvents()
    }

    private fun loadMyProfile() {
        viewModelScope.launch {
            myProfile = profileRepository.getCurrentProfile().firstOrNull()
            Log.d("NfcAddFriendVM", "✅ myProfile 加载完成: ${myProfile?.id}")
        }
    }

    // ==================== 公共接口 ====================

    /**
     * NFC 碰触触发
     *
     * 由 NFC Intent 解析后调用，传入从 NFC Tag 读取的对方 userId。
     */
    fun onNfcDetected(peerUserIdFromNfc: String) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📲 onNfcDetected 触发，peerUserId=$peerUserIdFromNfc")
        Log.d(TAG, "   当前 nfcState=${_uiState.value.connectionState}")

        val currentState = _uiState.value.connectionState
        if (currentState is NfcConnectionState.Connecting ||
            currentState is NfcConnectionState.Connected
        ) {
            Log.w(TAG, "⚠️ 已在连接/已连接状态，忽略重复触发")
            return
        }

        val myId = myProfile?.id
        if (myId == null) {
            Log.e(
                TAG,
                "❌ myProfile 为 null，无法生成 sessionRequestId（是否 loadMyProfile 还未完成？）"
            )
            return
        }

        peerUserId = peerUserIdFromNfc
        sessionRequestId = buildSessionId(myId, peerUserIdFromNfc)
        Log.d(TAG, "   sessionRequestId=$sessionRequestId")

        viewModelScope.launch {
            Log.d(TAG, "🔄 切换状态: Waiting → Connecting")
            _uiState.update { it.copy(connectionState = NfcConnectionState.Connecting) }

            Log.d(TAG, "📡 开始通过 BLE 拉取对方信息...")
            val peerProfile = fetchPeerProfileViaBle(peerUserIdFromNfc)

            if (peerProfile != null) {
                Log.d(TAG, "✅ BLE 拉取成功: nickname=${peerProfile.nickname}")
                Log.d(TAG, "🔄 切换状态: Connecting → Connected")
                _uiState.update {
                    it.copy(
                        connectionState = NfcConnectionState.Connected,
                        peerProfile = peerProfile
                    )
                }
            } else {
                Log.e(TAG, "❌ BLE 拉取失败，切换到 Failed 状态")
                _uiState.update {
                    it.copy(connectionState = NfcConnectionState.Failed("连接失败，请重新碰一碰"))
                }
            }
        }
    }

    /**
     * 用户点击"添加到通讯录"
     */
    fun onAddFriend() {
        val currentAddState = _uiState.value.addState

        // 防止重复点击
        if (currentAddState !is NfcAddState.Idle && currentAddState !is NfcAddState.PeerReady) return

        viewModelScope.launch {
            val my = ensureMyProfile() ?: run {
                _uiState.update { it.copy(addState = NfcAddState.Error("获取个人资料失败")) }
                return@launch
            }

            if (currentAddState is NfcAddState.PeerReady) {
                // 对方已先点击，我现在点击 → 双方都已就绪，立即完成交换
                handleIAmReady(my)
            } else {
                // 我先点击，等待对方
                handleISentFirst(my)
            }
        }
    }

    /**
     * 重试（连接失败或超时后）
     */
    fun onRetry() {
        timeoutJob?.cancel()
        _uiState.value = NfcAddFriendUiState()
        pendingPeerRequest = null
        sessionRequestId = ""
        peerUserId = ""
    }

    // ==================== 私有：BLE 事件监听 ====================

    private fun observeBleEvents() {
        viewModelScope.launch {
            Log.d(TAG, "👂 开始监听 BLE 事件流")
            bleModule.friendRequestEvents.collect { event ->
                Log.d(TAG, "📨 收到 BLE 事件: ${event::class.simpleName}")
                when (event) {
                    is FriendRequestEvent.NfcPeerAddRequest -> {
                        val incomingRequestId = event.message.requestId

                        // 对方是被动方，没走NFC流程，sessionRequestId为空，直接接受
                        if (sessionRequestId.isEmpty()) {
                            Log.d(TAG, "📨 被动方收到请求，直接接受 requestId=$incomingRequestId")
                            sessionRequestId = incomingRequestId
                            onPeerSentAddRequest(event.message, event.avatarBytes)
                            return@collect
                        }

                        // 主动方正常匹配
                        if (incomingRequestId == sessionRequestId) {
                            Log.d(TAG, "✅ requestId 匹配，处理 NfcAddRequest")
                            onPeerSentAddRequest(event.message, event.avatarBytes)
                        } else {
                            Log.w(TAG, "⚠️ requestId 不匹配，忽略")
                        }
                    }

                    is FriendRequestEvent.NfcPeerAddResponse -> {
                        Log.d(
                            TAG,
                            "   requestId=${event.message.requestId}，sessionRequestId=$sessionRequestId"
                        )
                        if (event.message.requestId == sessionRequestId) {
                            Log.d(TAG, "✅ requestId 匹配，处理 NfcAddResponse")
                            onPeerConfirmedAdd(event.message, event.avatarBytes)
                        } else {
                            Log.w(TAG, "⚠️ requestId 不匹配，忽略")
                        }
                    }

                    else -> Log.d(TAG, "   非 NFC 事件，忽略")
                }
            }
        }
    }


    // ==================== 私有：碰一碰业务逻辑 ====================

    /**
     * 我先点击的分支：发送 NfcAddRequest，进入等待状态
     */
    private suspend fun handleISentFirst(my: UserProfile) {
        _uiState.update { it.copy(addState = NfcAddState.WaitingForPeer) }

        val sent = sendMyAddRequest(my)
        if (!sent) {
            _uiState.update { it.copy(addState = NfcAddState.Error("发送请求失败，请重试")) }
            return
        }

        // 启动超时
        startWaitingTimeout()
    }

    /**
     * 对方已先点击、我后点击的分支：
     * 直接存库、发送 NfcAddResponse，完成交换
     */
    private suspend fun handleIAmReady(my: UserProfile) {
        val (peerRequest, peerAvatarBytes) = pendingPeerRequest ?: run {
            // 理论上不应走到这里，保险处理
            _uiState.update { it.copy(addState = NfcAddState.Error("数据异常，请重试")) }
            return
        }

        _uiState.update { it.copy(addState = NfcAddState.Exchanging) }
        timeoutJob?.cancel()

        // 1. 保存对方到通讯录
        val saved = savePeerContact(peerRequest, peerAvatarBytes)
        if (!saved) {
            _uiState.update { it.copy(addState = NfcAddState.Error("保存联系人失败")) }
            return
        }

        // 2. 发送 NfcAddResponse（携带我的完整资料）
        sendMyAddResponse(my, peerRequest.requestId)

        _uiState.update { it.copy(addState = NfcAddState.Success) }
    }

    /**
     * 收到对方发来的 NfcAddRequest（对方点击了添加）
     */
    private fun onPeerSentAddRequest(
        message: P2PMessage.NfcAddRequest,
        avatarBytes: ByteArray?
    ) {
        Log.d(
            TAG,
            "⭐ onPeerSentAddRequest 开始，requestId=${message.requestId}，sessionRequestId=$sessionRequestId"
        )

        if (sessionRequestId.isNotEmpty() && message.requestId != sessionRequestId) {
            Log.w(TAG, "收到非本次会话的 NfcAddRequest，忽略")
            return
        }

        val currentAddState = _uiState.value.addState
        Log.d(TAG, "⭐ 当前 addState=$currentAddState")

        when (currentAddState) {
            is NfcAddState.WaitingForPeer -> {
                Log.d(TAG, "⭐ 分支：WaitingForPeer，双方都就绪")
                viewModelScope.launch {
                    _uiState.update { it.copy(addState = NfcAddState.Exchanging) }
                    timeoutJob?.cancel()

                    val my = ensureMyProfile() ?: run {
                        Log.e(TAG, "❌ ensureMyProfile 返回 null")
                        _uiState.update { it.copy(addState = NfcAddState.Error("获取个人资料失败")) }
                        return@launch
                    }
                    Log.d(TAG, "⭐ myProfile 获取成功: ${my.id}")

                    val saved = savePeerContact(message, avatarBytes)
                    Log.d(TAG, "⭐ savePeerContact 结果: $saved")
                    if (!saved) {
                        _uiState.update { it.copy(addState = NfcAddState.Error("保存联系人失败")) }
                        return@launch
                    }

                    sendMyAddResponse(my, message.requestId)
                    Log.d(TAG, "⭐ sendMyAddResponse 已调用")

                    _uiState.update { it.copy(addState = NfcAddState.Success) }
                    Log.d(TAG, "⭐ 状态已更新为 Success")
                }
            }

            is NfcAddState.Idle -> {
                Log.d(TAG, "⭐ 分支：Idle，对方先点，切换到 PeerReady")
                pendingPeerRequest = Pair(message, avatarBytes)

                // 直接用消息里的字段构建 peerProfile，不需要再走 BLE
                val peerProfile = NfcPeerProfile(
                    id = message.userId,
                    nickname = message.nickname,
                    avatarUrl = "", // 实际使用时从本地缓存或内存传递 avatarBytes
                    avatarBytes = avatarBytes,
                    signature = message.signature ?: ""
                )

                _uiState.update {
                    it.copy(
                        connectionState = NfcConnectionState.Connected,
                        peerProfile = peerProfile,
                        addState = NfcAddState.PeerReady
                    )
                }
                Log.d(TAG, "⭐ 状态已更新为 PeerReady")
            }

            is NfcAddState.Success -> {
                // 我已成功，对方补点，直接回复
                viewModelScope.launch {
                    val my = ensureMyProfile() ?: return@launch
                    sendMyAddResponse(my, message.requestId)
                }
            }

            else -> {
                Log.w(TAG, "⚠️ 收到 NfcAddRequest 但当前状态为 $currentAddState，忽略")
            }
        }
    }

    /**
     * 收到对方发来的 NfcAddResponse（对方确认了我的申请，交换完成）
     */
    private fun onPeerConfirmedAdd(
        message: P2PMessage.NfcAddResponse,
        avatarBytes: ByteArray?
    ) {
        if (message.userId != peerUserId) {
            Log.w(TAG, "收到非本次会话的 NfcAddResponse，忽略")
            return
        }

        if (_uiState.value.addState !is NfcAddState.WaitingForPeer) {
            Log.w(TAG, "收到 NfcAddResponse 但当前状态不是 WaitingForPeer，忽略")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(addState = NfcAddState.Exchanging) }
            timeoutJob?.cancel()

            val saved = savePeerContact(message, avatarBytes)
            _uiState.update {
                it.copy(
                    addState = if (saved) NfcAddState.Success
                    else NfcAddState.Error("保存联系人失败")
                )
            }
        }
    }

    // ==================== 私有：BLE 数据拉取 ====================

    /**
     * 通过 BLE 拉取对方资料
     */
    private suspend fun fetchPeerProfileViaBle(peerUserId: String): NfcPeerProfile? {
        return try {
            val md5 = peerUserId.toMD5Hex()
            Log.d(TAG, "🔵 BLE 扫描开始，peerUserId=$peerUserId，MD5前缀=${md5.take(8)}")

            val gatt = bleDiscovery.scanAndConnect(md5)
            if (gatt == null) {
                Log.e(TAG, "❌ BLE scanAndConnect 返回 null，扫描超时或对方未开启 BLE")
                return null
            }
            Log.d(TAG, "✅ BLE 连接成功，开始读取 profile")

            val result = bleDiscovery.readProfile(gatt)
            bleDiscovery.close()

            if (result == null) {
                Log.e(TAG, "❌ readProfile 返回 null，读取超时或协议异常")
                return null
            }

            val (transfer, avatarBytes) = result
            Log.d(
                TAG,
                "✅ readProfile 成功: userId=${transfer.userId}，nickname=${transfer.nickname}，avatarSize=${avatarBytes?.size ?: 0}"
            )

            NfcPeerProfile(
                id = transfer.userId,
                nickname = transfer.nickname,
                avatarUrl = "", // 实际使用时从本地缓存或内存传递 avatarBytes
                avatarBytes = avatarBytes,
                signature = transfer.signature ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchPeerProfileViaBle 异常: ${e.message}", e)
            null
        }
    }

    // ==================== 私有：发送消息 ====================

    /**
     * 发送 NfcAddRequest（我点击了添加，通知对方）
     */
    private suspend fun sendMyAddRequest(my: UserProfile): Boolean {
        val avatarBytes = loadMyAvatarBytes(my)
        val request = P2PMessage.NfcAddRequest(
            requestId = sessionRequestId,
            userId = my.id,
            nickname = my.nickname,
            signature = my.signature,
            gender = my.gender,
            avatarSize = avatarBytes?.size ?: 0,
            timestamp = System.currentTimeMillis()
        )
        return p2pTransmitter.sendMessage(
            targetUserId = peerUserId,
            message = request,
            binaryData = avatarBytes
        )
    }

    /**
     * 发送 NfcAddResponse（我确认对方的申请，同时携带我的完整资料）
     */
    private suspend fun sendMyAddResponse(my: UserProfile, requestId: String) {
        val avatarBytes = loadMyAvatarBytes(my)
        val response = P2PMessage.NfcAddResponse(
            requestId = requestId,
            userId = my.id,
            nickname = my.nickname,
            signature = my.signature,
            gender = my.gender,
            avatarSize = avatarBytes?.size ?: 0,
            timestamp = System.currentTimeMillis()
        )
        p2pTransmitter.sendMessage(
            targetUserId = peerUserId,
            message = response,
            binaryData = avatarBytes
        )
    }

    // ==================== 私有：数据存取 ====================

    /**
     * 将对方（NfcAddRequest）保存到通讯录
     */
    private suspend fun savePeerContact(
        message: P2PMessage.NfcAddRequest,
        avatarBytes: ByteArray?
    ): Boolean {
        return try {
            val contact = Contact(
                id = message.userId,
                nickname = message.nickname,
                signature = message.signature,
                gender = message.gender,
                avatarPath = saveAvatarToLocal(message.userId, avatarBytes)
            )
            contactRepository.addContact(contact)
            Log.d(TAG, "已保存联系人（来自 NfcAddRequest）: ${message.nickname}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "保存联系人失败（NfcAddRequest）", e)
            false
        }
    }

    /**
     * 将对方（NfcAddResponse）保存到通讯录
     */
    private suspend fun savePeerContact(
        message: P2PMessage.NfcAddResponse,
        avatarBytes: ByteArray?
    ): Boolean {
        return try {
            val contact = Contact(
                id = message.userId,
                nickname = message.nickname,
                signature = message.signature,
                gender = message.gender,
                avatarPath = saveAvatarToLocal(message.userId, avatarBytes)
            )
            contactRepository.addContact(contact)
            Log.d(TAG, "已保存联系人（来自 NfcAddResponse）: ${message.nickname}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "保存联系人失败（NfcAddResponse）", e)
            false
        }
    }

    private suspend fun ensureMyProfile(): UserProfile? {
        if (myProfile == null) {
            myProfile = profileRepository.getCurrentProfile().first()
        }
        return myProfile
    }

    private fun loadMyAvatarBytes(my: UserProfile): ByteArray? {
        return try {
            null //my.avatarPath?.let { File(it).readBytes() }
        } catch (e: Exception) {
            Log.w(TAG, "读取本地头像失败", e)
            null
        }
    }

    private fun saveAvatarToLocal(userId: String, avatarBytes: ByteArray?): String? {
        if (avatarBytes == null) return null
        return try {
            // 实际路径需替换为你项目中的本地存储目录
            val file = File("/data/user/0/top.chengdongqing.wechat/files/avatars/${userId}.jpg")
            file.parentFile?.mkdirs()
            file.writeBytes(avatarBytes)
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "保存头像失败", e)
            null
        }
    }

    // ==================== 私有：工具 ====================

    /**
     * 双方用同一套规则生成 sessionId：
     * 将双方 userId 排序拼接后 MD5，保证两边生成结果完全一致
     */
    private fun buildSessionId(myId: String, peerId: String): String {
        return listOf(myId, peerId).sorted().joinToString("_").toMD5Hex()
    }

    private fun startWaitingTimeout() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(WAITING_TIMEOUT_MS)
            if (_uiState.value.addState is NfcAddState.WaitingForPeer) {
                Log.d(TAG, "等待对方超时")
                _uiState.update { it.copy(addState = NfcAddState.Timeout) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timeoutJob?.cancel()
        bleDiscovery.close()
    }
}