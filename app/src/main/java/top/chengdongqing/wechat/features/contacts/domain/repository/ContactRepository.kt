package top.chengdongqing.wechat.features.contacts.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.features.contacts.domain.model.Contact

interface ContactRepository {

    /**
     * 获取所有联系人
     */
    fun getAllContacts(): Flow<List<Contact>>

    /**
     * 根据ID获取联系人
     */
    suspend fun getContactById(userId: String): Contact?

    /**
     * 监听指定联系人的变化
     */
    fun observeContactById(userId: String): Flow<Contact?>

    /**
     * 检查联系人是否存在
     */
    suspend fun exists(userId: String): Boolean

    /**
     * 添加联系人
     */
    suspend fun addContact(contact: Contact)

    /**
     * 更新联系人
     */
    suspend fun updateContact(contact: Contact)

    /**
     * 删除联系人
     */
    suspend fun deleteContact(userId: String)
}