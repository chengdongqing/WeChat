package top.chengdongqing.wechat.features.contacts.data.repository

import android.util.LruCache
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.database.entity.ContactEntity
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.contacts.data.mapper.toDomain
import top.chengdongqing.wechat.features.contacts.data.mapper.toEntity
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val database: WeDatabase,
    private val contactDao: ContactDao,
    private val chatSessionDao: ChatSessionDao,
    private val chatSessionRepository: ChatSessionRepository,
    private val connectionInfoDao: ConnectionInfoDao
) : ContactRepository {

    // 联系人缓存
    private val contactCache = LruCache<String, Contact>(100)

    override fun observeAllContacts(isBlocked: Boolean): Flow<List<Contact>> {
        return contactDao.observeAll(isBlocked).map { it.toDomain() }
    }

    override suspend fun getContact(userId: String): Contact? {
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

    override fun observeContact(userId: String): Flow<Contact?> {
        return contactDao.observeById(userId).map { it?.toDomain() }
    }

    override suspend fun exists(userId: String): Boolean {
        return getContact(userId) != null
    }

    override suspend fun createContact(contact: Contact) {
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
        database.withTransaction {
            // 删除会话的所有信息、所有消息记录、所有会话文件
            chatSessionRepository.deleteSession(userId)
            chatSessionDao.deleteById(userId)
            // 删除联系人
            contactDao.deleteById(userId)
            // 删除连接信息
            connectionInfoDao.deleteById(userId)
        }

        contactCache.remove(userId)
    }
}