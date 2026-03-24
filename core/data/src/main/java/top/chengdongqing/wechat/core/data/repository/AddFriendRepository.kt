package top.chengdongqing.wechat.core.data.repository

import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.model.ContactAddSource

interface AddFriendRepository {
    suspend fun handleScannedQRCode(qrContent: String): Result<Contact>
    suspend fun generateMyQRCode(): String
    fun getContactFromCache(contactId: String): Contact?
    suspend fun fetchProfile(userId: String, source: ContactAddSource? = null): Contact?
}
