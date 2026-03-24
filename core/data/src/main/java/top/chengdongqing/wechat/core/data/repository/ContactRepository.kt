package top.chengdongqing.wechat.core.data.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.database.entity.ContactEntity
import top.chengdongqing.wechat.core.model.Contact

interface ContactRepository {
    fun observeAllContacts(isBlocked: Boolean = false): Flow<List<Contact>>
    suspend fun getContact(userId: String): Contact?
    fun observeContact(userId: String): Flow<Contact?>
    suspend fun exists(userId: String): Boolean
    suspend fun createContact(contact: Contact)
    suspend fun updateContact(contactId: String, updateBlock: (ContactEntity) -> ContactEntity)
    suspend fun syncContactProfile(protocol: ChatProtocol.ProfileResponse)
    suspend fun deleteContact(userId: String)
}
