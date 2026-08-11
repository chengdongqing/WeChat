package top.chengdongqing.wechat.feature.contacts.data.repository

import android.util.LruCache
import androidx.core.net.toUri
import androidx.room3.withWriteTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.data.storage.AssetOwner
import top.chengdongqing.wechat.core.data.storage.AssetOwnerType
import top.chengdongqing.wechat.core.data.storage.AssetReferenceManager
import top.chengdongqing.wechat.core.database.WeDatabase
import top.chengdongqing.wechat.core.database.dao.ChatSessionDao
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.database.dao.ContactDao
import top.chengdongqing.wechat.core.database.entity.ContactEntity
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.network.crypto.PacketSigner
import top.chengdongqing.wechat.core.util.downloadAvatar
import top.chengdongqing.wechat.core.util.getOrPutAsync
import top.chengdongqing.wechat.core.util.toSHA256Hex
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
    private val assetReferenceManager: AssetReferenceManager
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
        contact.avatarPath?.let { path ->
            assetReferenceManager.attach(
                path,
                File(path).toSHA256Hex(),
                AssetOwner(AssetOwnerType.Contact, contact.id)
            )
        }
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

        if (newAvatarPath != null && newAvatarPath != oldAvatarPath) {
            assetReferenceManager.attach(
                newAvatarPath,
                File(newAvatarPath).toSHA256Hex(),
                AssetOwner(AssetOwnerType.Contact, userId)
            )
        }
    }

    override suspend fun deleteContact(userId: String) {
        val contact = contactDao.getById(userId) ?: return

        // 会话删除包含独立的资源清理，不应嵌套在联系人数据库事务中。
        chatSessionRepository.deleteSession(userId)
        database.withWriteTransaction {
            chatSessionDao.deleteById(userId)
            // 删除联系人
            contactDao.deleteById(userId)
            // 删除连接信息
            connectionInfoDao.deleteById(userId)
            // 使公钥缓存失效
            packetSigner.invalidateCache(userId)
        }

        assetReferenceManager.detach(AssetOwner(AssetOwnerType.Contact, contact.id))

        contactCache.remove(userId)
    }
}
