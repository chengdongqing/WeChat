package top.chengdongqing.wechat.features.contacts.ui.add.nfc

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.util.toMD5Hex
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.NfcAddState
import top.chengdongqing.wechat.features.contacts.domain.model.NfcConnectionState
import top.chengdongqing.wechat.features.contacts.domain.model.NfcContactEvent
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

private const val TAG = "NfcAddFriendVM"

data class NfcAddFriendUiState(
    val connectionState: NfcConnectionState = NfcConnectionState.Waiting,
    val profile: Contact? = null,
    val addState: NfcAddState = NfcAddState.Idle
)

@HiltViewModel
class NfcAddFriendViewModel @Inject constructor(
    private val contactP2PRepository: ContactP2PRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NfcAddFriendUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * 本次会话唯一 ID，由双方 userId 排序拼接后 MD5 生成，保证双端结果一致。
     * 被动方（未碰触 NFC）在收到 PeerRequest 后由事件中的 requestId 赋值。
     */
    private var sessionRequestId = ""

    /** 对方 userId，主动方从 NFC Tag 读取，被动方从 PeerRequest 事件获取 */
    private var peerUserId = ""

    /**
     * 暂存对方先发来的 PeerRequest。
     * 对方先点击、我后点击时，直接用此数据完成存库和回复，无需再等 BLE 事件。
     */
    private var pendingPeerRequest: NfcContactEvent.PeerRequest? = null

    private var timeoutJob: Job? = null

    companion object {
        private const val WAITING_TIMEOUT_MS = 60_000L
    }

    init {
        observeNfcEvents()
    }

    // ==================== 公共接口 ====================

    /**
     * NFC 碰触触发，传入从 Tag 读取的对方 userId。
     * 防重：已在 Connecting / Connected 状态时忽略。
     */
    fun onNfcDetected(peerUserIdFromNfc: String) {
        val currentState = _uiState.value.connectionState
        if (currentState is NfcConnectionState.Connecting ||
            currentState is NfcConnectionState.Connected
        ) return

        viewModelScope.launch {
            val myId = getMyUserId() ?: return@launch

            peerUserId = peerUserIdFromNfc
            sessionRequestId = buildSessionId(myId, peerUserIdFromNfc)
            Log.d(TAG, "📲 NFC 碰触，peer=$peerUserIdFromNfc，session=$sessionRequestId")

            _uiState.update { it.copy(connectionState = NfcConnectionState.Connecting) }

            val peerContact = contactP2PRepository.fetchPeerContactViaBle(peerUserIdFromNfc)
            if (peerContact != null) {
                _uiState.update {
                    it.copy(
                        connectionState = NfcConnectionState.Connected,
                        profile = peerContact
                    )
                }
            } else {
                Log.e(TAG, "❌ BLE 拉取失败")
                _uiState.update {
                    it.copy(connectionState = NfcConnectionState.Failed("连接失败，请重新碰一碰"))
                }
            }
        }
    }

    /**
     * 用户点击"添加到通讯录"。
     * Idle 状态：我先点击，发送 Request 等待对方。
     * PeerReady 状态：对方已先点击，直接完成交换。
     */
    fun onAddFriend() {
        val currentAddState = _uiState.value.addState
        if (currentAddState !is NfcAddState.Idle && currentAddState !is NfcAddState.PeerReady) return

        viewModelScope.launch {
            if (currentAddState is NfcAddState.PeerReady) {
                handleIAmReady()
            } else {
                handleISentFirst()
            }
        }
    }

    /** 连接失败或超时后重置所有状态，回到初始界面 */
    fun onRetry() {
        timeoutJob?.cancel()
        _uiState.value = NfcAddFriendUiState()
        pendingPeerRequest = null
        sessionRequestId = ""
        peerUserId = ""
    }

    // ==================== NFC 事件监听 ====================

    private fun observeNfcEvents() {
        viewModelScope.launch {
            contactP2PRepository.nfcEvents.collect { event ->
                when (event) {
                    is NfcContactEvent.PeerRequest -> handleIncomingRequest(event)
                    is NfcContactEvent.PeerResponse -> handleIncomingResponse(event)
                }
            }
        }
    }

    private fun handleIncomingRequest(event: NfcContactEvent.PeerRequest) {
        // 被动方：未经 NFC 碰触，sessionRequestId 为空，直接用事件中的 requestId 建立会话
        if (sessionRequestId.isEmpty()) {
            sessionRequestId = event.requestId
            peerUserId = event.contact.id
            Log.d(TAG, "📨 被动方收到 Request，session=${event.requestId}")
            onPeerSentAddRequest(event)
            return
        }

        if (event.requestId == sessionRequestId) {
            onPeerSentAddRequest(event)
        } else {
            Log.w(TAG, "⚠️ PeerRequest requestId 不匹配，忽略")
        }
    }

    private fun handleIncomingResponse(event: NfcContactEvent.PeerResponse) {
        if (event.requestId == sessionRequestId) {
            onPeerConfirmedAdd(event)
        } else {
            Log.w(TAG, "⚠️ PeerResponse requestId 不匹配，忽略")
        }
    }

    // ==================== 碰一碰业务逻辑 ====================

    /** 我先点击：发送 NfcAddRequest 并启动等待超时 */
    private suspend fun handleISentFirst() {
        _uiState.update { it.copy(addState = NfcAddState.WaitingForPeer) }

        val sent = contactP2PRepository.sendNfcAddRequest(
            peerUserId = peerUserId,
            sessionId = sessionRequestId
        )
        if (!sent) {
            _uiState.update { it.copy(addState = NfcAddState.Error("发送请求失败，请重试")) }
            return
        }

        startWaitingTimeout()
    }

    /** 对方先点击、我后点击：保存联系人并发送 NfcAddResponse 完成交换 */
    private suspend fun handleIAmReady() {
        val peerRequest = pendingPeerRequest ?: run {
            _uiState.update { it.copy(addState = NfcAddState.Error("数据异常，请重试")) }
            return
        }

        _uiState.update { it.copy(addState = NfcAddState.Exchanging) }
        timeoutJob?.cancel()

        if (!contactP2PRepository.saveNfcContact(peerRequest)) {
            _uiState.update { it.copy(addState = NfcAddState.Error("保存联系人失败")) }
            return
        }

        contactP2PRepository.sendNfcAddResponse(peerUserId, peerRequest.requestId)
        _uiState.update { it.copy(addState = NfcAddState.Success) }
    }

    /**
     * 收到对方的 NfcAddRequest：
     * - WaitingForPeer：双方都已就绪，保存联系人并回复 Response
     * - Idle：对方先点击，暂存 Request，展示对方资料，进入 PeerReady 等待我点击
     * - Success：我已完成，对方补点，直接回复 Response
     */
    private fun onPeerSentAddRequest(event: NfcContactEvent.PeerRequest) {
        when (val state = _uiState.value.addState) {
            is NfcAddState.WaitingForPeer -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(addState = NfcAddState.Exchanging) }
                    timeoutJob?.cancel()

                    if (!contactP2PRepository.saveNfcContact(event)) {
                        _uiState.update { it.copy(addState = NfcAddState.Error("保存联系人失败")) }
                        return@launch
                    }

                    contactP2PRepository.sendNfcAddResponse(peerUserId, event.requestId)
                    _uiState.update { it.copy(addState = NfcAddState.Success) }
                }
            }

            is NfcAddState.Idle -> {
                pendingPeerRequest = event
                _uiState.update {
                    it.copy(
                        connectionState = NfcConnectionState.Connected,
                        profile = event.contact,
                        addState = NfcAddState.PeerReady
                    )
                }
            }

            is NfcAddState.Success -> {
                viewModelScope.launch {
                    contactP2PRepository.sendNfcAddResponse(peerUserId, event.requestId)
                }
            }

            else -> Log.w(TAG, "⚠️ 收到 PeerRequest，当前状态 $state 无需处理")
        }
    }

    /**
     * 收到对方的 NfcAddResponse：保存联系人，完成交换。
     * WaitingForPeer / Exchanging 均接受，防止状态竞争。
     */
    private fun onPeerConfirmedAdd(event: NfcContactEvent.PeerResponse) {
        val state = _uiState.value.addState
        if (state !is NfcAddState.WaitingForPeer && state !is NfcAddState.Exchanging) {
            Log.w(TAG, "⚠️ 收到 PeerResponse，当前状态 $state 无需处理")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(addState = NfcAddState.Exchanging) }
            timeoutJob?.cancel()

            val saved = contactP2PRepository.saveNfcContact(event)
            _uiState.update {
                it.copy(
                    addState = if (saved) NfcAddState.Success
                    else NfcAddState.Error("保存联系人失败")
                )
            }
        }
    }

    // ==================== 工具 ====================

    private suspend fun getMyUserId(): String? {
        return profileRepository.getCurrentProfile().firstOrNull()?.id
            .also { if (it == null) Log.e(TAG, "❌ 获取 myProfile 失败") }
    }

    /**
     * 双方 userId 排序拼接后 MD5，保证两端生成结果完全一致
     */
    private fun buildSessionId(myId: String, peerId: String): String {
        return listOf(myId, peerId).sorted().joinToString("_").toMD5Hex()
    }

    private fun startWaitingTimeout() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(WAITING_TIMEOUT_MS)
            if (_uiState.value.addState is NfcAddState.WaitingForPeer) {
                Log.d(TAG, "⏰ 等待对方超时")
                _uiState.update { it.copy(addState = NfcAddState.Timeout) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timeoutJob?.cancel()
    }
}