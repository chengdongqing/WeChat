package top.chengdongqing.wechat.core.data.repository

interface TemporaryChatRepository {
    suspend fun start(
        peerId: String,
        peerName: String,
        peerAvatar: String?,
        peerPublicKey: String
    ): Result<Long>
}
