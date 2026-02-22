package top.chengdongqing.wechat.features.contacts.domain.model

sealed class NfcConnectionState {
    object Waiting : NfcConnectionState()
    object Connecting : NfcConnectionState()
    object Connected : NfcConnectionState()
    data class Failed(val reason: String) : NfcConnectionState()
}

sealed class NfcAddState {
    object Idle : NfcAddState()
    object WaitingForPeer : NfcAddState()
    object PeerReady : NfcAddState()
    object Exchanging : NfcAddState()
    object Success : NfcAddState()
    object Timeout : NfcAddState()
    data class Error(val message: String) : NfcAddState()
}

sealed class NfcContactEvent {
    /**
     * 对方发来的添加申请（含对方资料）
     */
    data class PeerRequest(
        val requestId: String,
        val contact: Contact
    ) : NfcContactEvent()

    /**
     * 对方确认了我的申请（含对方资料）
     */
    data class PeerResponse(
        val requestId: String,
        val contact: Contact
    ) : NfcContactEvent()
}