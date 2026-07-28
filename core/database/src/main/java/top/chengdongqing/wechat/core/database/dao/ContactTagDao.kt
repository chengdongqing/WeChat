package top.chengdongqing.wechat.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.database.entity.ContactEntity
import top.chengdongqing.wechat.core.database.entity.ContactTagEntity
import top.chengdongqing.wechat.core.database.entity.ContactTagMemberEntity

data class ContactTagSummary(
    val id: String,
    val name: String,
    val memberCount: Int
)

@Dao
interface ContactTagDao {
    @Query(
        """SELECT t.id, t.name, COUNT(m.contactId) AS memberCount
           FROM contact_tags t LEFT JOIN contact_tag_members m ON t.id = m.tagId
           GROUP BY t.id ORDER BY t.updatedAt DESC, t.name"""
    )
    fun observeTags(): Flow<List<ContactTagSummary>>

    @Query("SELECT * FROM contact_tags WHERE id = :tagId")
    suspend fun getTag(tagId: String): ContactTagEntity?

    @Query(
        """SELECT c.* FROM contacts c INNER JOIN contact_tag_members m ON c.id = m.contactId
           WHERE m.tagId = :tagId ORDER BY COALESCE(c.remarkName, c.nickname)"""
    )
    fun observeContacts(tagId: String): Flow<List<ContactEntity>>

    @Query("SELECT tagId FROM contact_tag_members WHERE contactId = :contactId")
    fun observeTagIds(contactId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: ContactTagEntity)

    @Query("UPDATE contact_tags SET name = :name, updatedAt = :updatedAt WHERE id = :tagId")
    suspend fun rename(tagId: String, name: String, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMembers(members: List<ContactTagMemberEntity>)

    @Query("DELETE FROM contact_tag_members WHERE tagId = :tagId")
    suspend fun clearMembers(tagId: String)

    @Query("DELETE FROM contact_tag_members WHERE contactId = :contactId")
    suspend fun clearContactTags(contactId: String)

    @Query("DELETE FROM contact_tags WHERE id = :tagId")
    suspend fun delete(tagId: String)

    @Transaction
    suspend fun replaceMembers(tagId: String, contactIds: Set<String>) {
        clearMembers(tagId)
        addMembers(contactIds.map { ContactTagMemberEntity(tagId, it) })
    }

    @Transaction
    suspend fun replaceContactTags(contactId: String, tagIds: Set<String>) {
        clearContactTags(contactId)
        addMembers(tagIds.map { ContactTagMemberEntity(it, contactId) })
    }
}
