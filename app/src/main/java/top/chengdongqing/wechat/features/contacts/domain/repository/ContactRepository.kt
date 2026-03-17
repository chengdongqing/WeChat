package top.chengdongqing.wechat.features.contacts.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.ContactEntity
import top.chengdongqing.wechat.data.network.model.ChatProtocol
import top.chengdongqing.wechat.features.contacts.domain.model.Contact

interface ContactRepository {

    /**
     * 获取所有联系人
     */
    fun observeAllContacts(isBlocked: Boolean = false): Flow<List<Contact>>

    /**
     * 获取联系人详情
     */
    suspend fun getContact(userId: String): Contact?

    /**
     * 监听指定联系人的变化
     */
    fun observeContact(userId: String): Flow<Contact?>

    /**
     * 检查联系人是否存在
     */
    suspend fun exists(userId: String): Boolean

    /**
     * 添加联系人
     */
    suspend fun createContact(contact: Contact)

    /**
     * 更新联系人
     */
    suspend fun updateContact(contactId: String, updateBlock: (ContactEntity) -> ContactEntity)

    /**
     * 同步联系人资料
     */
    suspend fun syncContactProfile(protocol: ChatProtocol.ProfileResponse)

    /**
     * 删除联系人
     */
    suspend fun deleteContact(userId: String)
}