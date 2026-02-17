package top.chengdongqing.wechat.data.network.messaging

import top.chengdongqing.wechat.features.call.domain.model.CallType

sealed class SignalingMessage {
    abstract val fromUserId: String

    data class Offer(
        override val fromUserId: String,
        val sdp: String
    ) : SignalingMessage()

    data class Answer(
        override val fromUserId: String,
        val sdp: String
    ) : SignalingMessage()

    data class IceCandidate(
        override val fromUserId: String,
        val candidate: String,
        val sdpMid: String?,
        val sdpMLineIndex: Int
    ) : SignalingMessage()

    data class Hangup(
        override val fromUserId: String
    ) : SignalingMessage()

    data class CallRequest(
        override val fromUserId: String,
        val callType: CallType
    ) : SignalingMessage()
}