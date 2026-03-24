package top.chengdongqing.wechat.feature.contacts.data.repository

import android.util.LruCache
import androidx.core.net.toUri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.common.file.PrivateFileManager
import top.chengdongqing.wechat.core.common.util.downloadAvatar
import top.chengdongqing.wechat.core.common.util.getOrPutAsync
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.database.WeDatabase
import top.chengdongqing.wechat.core.database.dao.ChatSessionDao
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.database.dao.ContactDao
import top.chengdongqing.wechat.core.database.entity.ContactEntity
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.network.crypto.PacketSigner
import top.chengdongqing.wechat.core.network.session.FileReferenceManager
import top.chengdongqing.wechat.feature.contacts.data.mapper.toDomain
import top.chengdongqing.wechat.feature.contacts.data.mapper.toEntity
import java.io.File
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val database: WeDatabase,
    private val contactDao: ContactDao,
    private val chatSessionDao: ChatSessionDao,
    private val chatSessionRepository: ChatSessionRepository,
    private val connectionInfoDao: ConnectionInfoDao,
    private val packetSigner: PacketSigner,
    private val privateFileManager: PrivateFileManager,
    private val fileReferenceManager: FileReferenceManager
) : ContactRepository {

    // 联系人缓存
    private val contactCache = LruCache<String, Contact>(100)

    override fun observeAllContacts(isBlocked: Boolean): Flow<List<Contact>> {
        return contactDao.observeAll(isBlocked).map { it.toDomain() }
    }

    override suspend fun getContact(userId: String): Contact? {
        return contactCache.getOrPutAsync(userId) {
            contactDao.getById(userId)?.toDomain()
        }
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

        // 更新会话
        getContact(contactId)?.let { contact ->
            chatSessionDao.update(contactId) { session ->
                session.copy(
                    contactName = contact.displayName,
                    contactAvatar = contact.avatarPath
                )
            }
        }
    }

    override suspend fun syncContactProfile(protocol: ChatProtocol.ProfileResponse) {
        val newProfile = protocol.profile
        val userId = newProfile.userId

        // 查询旧头像
        val oldAvatarPath = getContact(userId)?.avatarPath

        // 下载新头像
        val newAvatarPath = newProfile.avatarUrl?.let { url ->
            val file = File.createTempFile("IMG_", ".jpg")
            downloadAvatar(url, file).getOrNull()?.let {
                privateFileManager.saveAvatar(userId, file.toUri()).getOrNull()
            }
        }

        // 更新联系人资料
        updateContact(newProfile.userId) { contact ->
            contact.copy(
                avatarPath = newAvatarPath ?: contact.avatarPath,
                nickname = newProfile.nickname,
                signature = newProfile.signature,
                gender = newProfile.gender,
                version = System.currentTimeMillis() // 更新版本号
            )
        }

        // 删除旧文件
        if (newAvatarPath != null && oldAvatarPath != null) {
            val toDelete = fileReferenceManager.release(oldAvatarPath)
            toDelete?.let { privateFileManager.deleteFile(it) }
        }
    }

    override suspend fun deleteContact(userId: String) {
        val contact = contactDao.getById(userId) ?: return

        database.withTransaction {
            // 删除会话的所有信息、所有消息记录、所有会话文件
            chatSessionRepository.deleteSession(userId)
            chatSessionDao.deleteById(userId)
            // 删除联系人
            contactDao.deleteById(userId)
            // 删除连接信息
            connectionInfoDao.deleteById(userId)
            // 使公钥缓存失效
            packetSigner.invalidateCache(userId)
        }

        val toDelete = fileReferenceManager.release(contact.avatarPath)
        toDelete?.let { privateFileManager.deleteFile(it) }

        contactCache.remove(userId)
    }
}