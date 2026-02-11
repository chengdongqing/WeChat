package top.chengdongqing.wechat.features.contacts.domain.repository

import top.chengdongqing.wechat.features.contacts.domain.model.Contact

interface ContactP2PRepository {

    /**
     * 处理扫描到的二维码
     *
     * @param qrContent 二维码内容
     * @return 联系人信息
     */
    suspend fun handleScannedQRCode(qrContent: String): Result<Contact>

    /**
     * 生成我的二维码内容
     *
     * @return Base64 编码的二维码数据
     */
    suspend fun generateMyQRCode(): String

    /**
     * 从缓存获取联系人
     *
     * @param contactId 联系人ID
     * @return 联系人信息（如果存在）
     */
    fun getContactFromCache(contactId: String): Contact?
}