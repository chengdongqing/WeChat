package top.chengdongqing.wechat.features.contacts.domain.repository

import top.chengdongqing.wechat.features.contacts.domain.model.Contact

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
    suspend fun fetchProfile(userId: String): Contact?

}