package top.chengdongqing.wechat.features.contacts.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.NfcContactEvent

interface AddFriendRepository {

    /**
     * 处理扫描到的二维码
     */
    suspend fun handleScannedQRCode(qrContent: String): Result<Contact>

    /**
     * 生成我的二维码内容
     */
    suspend fun generateMyQRCode(): String

    /**
     * 从缓存获取联系人
     */
    fun getContactFromCache(contactId: String): Contact?

    /**
     * 保存联系人信息到缓存备用
     */
    fun setContactToCache(contactId: String, contact: Contact)

    /**
     * 通过 BLE 拉取对方资料
     */
    suspend fun fetchPeerContactViaBle(peerUserId: String): Contact?

    /**
     * NFC 事件流（由底层 BLE 服务推送）
     */
    val nfcEvents: Flow<NfcContactEvent>

    /**
     * 发送 NfcAddRequest（我点击了添加，通知对方）
     */
    suspend fun sendNfcAddRequest(peerUserId: String, sessionId: String): Boolean

    /**
     * 发送 NfcAddResponse（我确认对方申请，携带我的完整资料）
     */
    suspend fun sendNfcAddResponse(peerUserId: String, requestId: String): Boolean

    /**
     * 将对方保存到通讯录
     */
    suspend fun saveNfcContact(event: NfcContactEvent.PeerRequest): Boolean
    suspend fun saveNfcContact(event: NfcContactEvent.PeerResponse): Boolean
}