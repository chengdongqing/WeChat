package top.chengdongqing.wechat.features.contacts.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.database.entity.ContactEntity
import top.chengdongqing.wechat.data.database.entity.toDomain
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val contactDao: ContactDao
) {

    /**
     * 获取所有联系人
     */
    fun getAllContacts(): Flow<List<Contact>> {
        return contactDao.getAll()
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    /**
     * 根据ID获取联系人
     */
    suspend fun getContactById(userId: String): Contact? {
        return contactDao.getById(userId)?.toDomain()
    }

    /**
     * 是否存在联系人
     */
    suspend fun existsContact(userId: String): Boolean {
        return contactDao.exists(userId)
    }

    /**
     * 更新联系人
     */
    suspend fun updateContact(contact: ContactEntity) {
        contactDao.update(contact)
    }

    /**
     * 删除联系人
     */
    suspend fun deleteContact(userId: String) {
        contactDao.delete(userId)
    }
}