package top.chengdongqing.wechat.features.contacts.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.toDomain
import top.chengdongqing.wechat.data.database.entity.toEntity
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val chatSessionDao: ChatSessionDao,
    private val messageDao: MessageDao,
    private val weDatabase: WeDatabase
) : ContactRepository {

    override fun getAllContacts(): Flow<List<Contact>> {
        return contactDao.getAll().map { it.toDomain() }
    }

    override suspend fun getContactById(userId: String): Contact? {
        return contactDao.getById(userId)?.toDomain()
    }

    override fun observeContactById(userId: String): Flow<Contact?> {
        return contactDao.getByIdFlow(userId).map { it?.toDomain() }
    }

    override suspend fun exists(userId: String): Boolean {
        return contactDao.exists(userId)
    }

    override suspend fun addContact(contact: Contact) {
        contactDao.insert(contact.toEntity())
    }

    override suspend fun updateContact(contact: Contact) {
        contactDao.update(contact.toEntity())
    }

    override suspend fun deleteContact(userId: String) {
        weDatabase.withTransaction {
            // 删除联系人
            contactDao.delete(userId)
            // 删除会话
            chatSessionDao.deleteById(userId)
            // 删除所有消息
            messageDao.deleteBySession(userId)
            // TODO 删除文件缓存
        }
    }
}