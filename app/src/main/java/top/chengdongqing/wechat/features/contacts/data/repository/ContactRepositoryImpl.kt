package top.chengdongqing.wechat.features.contacts.data.repository

import android.util.LruCache
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.ContactEntity
import top.chengdongqing.wechat.features.contacts.data.mapper.toDomain
import top.chengdongqing.wechat.features.contacts.data.mapper.toEntity
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val weDatabase: WeDatabase,
    private val contactDao: ContactDao,
    private val chatSessionDao: ChatSessionDao,
    private val messageDao: MessageDao,
    private val connectionInfoDao: ConnectionInfoDao
) : ContactRepository {

    // 联系人缓存
    private val contactCache = LruCache<String, Contact>(100)

    override fun observeAllContacts(): Flow<List<Contact>> {
        return contactDao.observeAll().map { it.toDomain() }
    }

    override suspend fun getContactById(userId: String): Contact? {
        // 先从缓存拿
        synchronized(contactCache) {
            contactCache.get(userId)?.let { return it }
        }
        // 缓存没有，查库
        val contact = contactDao.getById(userId)?.toDomain()
        // 查到后回填缓存
        if (contact != null) {
            synchronized(contactCache) {
                contactCache.put(userId, contact)
            }
        }
        return contact
    }

    override fun observeContactById(userId: String): Flow<Contact?> {
        return contactDao.observeById(userId).map { it?.toDomain() }
    }

    override suspend fun exists(userId: String): Boolean {
        return contactDao.exists(userId)
    }

    override suspend fun addContact(contact: Contact) {
        contactDao.insert(contact.toEntity())
    }

    override suspend fun updateContact(
        contactId: String,
        updateBlock: (ContactEntity) -> ContactEntity
    ) {
        contactDao.update(contactId, updateBlock)
        contactCache.remove(contactId)
    }

    override suspend fun deleteContact(userId: String) {
        weDatabase.withTransaction {
            // 删除联系人
            contactDao.deleteById(userId)
            // 删除会话
            chatSessionDao.deleteById(userId)
            // 删除所有消息
            messageDao.deleteBySessionId(userId)
            // 删除连接信息
            connectionInfoDao.deleteById(userId)
            // TODO 删除文件缓存
        }

        contactCache.remove(userId)
    }
}