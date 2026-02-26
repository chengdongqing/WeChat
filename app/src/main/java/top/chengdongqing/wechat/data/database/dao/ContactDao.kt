package top.chengdongqing.wechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.ContactEntity

@Dao
interface ContactDao : BaseDao<ContactEntity> {

    @Query("SELECT * FROM contacts")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :userId")
    fun observeById(userId: String): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE id = :userId")
    suspend fun getById(userId: String): ContactEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM contacts WHERE id = :userId)")
    suspend fun exists(userId: String): Boolean

    @Transaction
    suspend fun update(contactId: String, updateBlock: (ContactEntity) -> ContactEntity) {
        val old = getById(contactId) ?: return
        val new = updateBlock(old).copy(
            audit = old.audit.copy(updatedAt = System.currentTimeMillis())
        )
        update(new)
    }

    @Query("DELETE FROM contacts WHERE id = :userId")
    suspend fun deleteById(userId: String)
}