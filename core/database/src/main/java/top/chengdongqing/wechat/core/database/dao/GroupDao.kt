package top.chengdongqing.wechat.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.database.entity.GroupEntity
import top.chengdongqing.wechat.core.database.entity.GroupMemberEntity

@Dao
interface GroupDao : BaseDao<GroupEntity> {
    @Query("SELECT * FROM groups WHERE savedToContacts = 1 ORDER BY updatedAt DESC")
    fun observeSavedGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getById(groupId: String): GroupEntity?

    @Query("SELECT * FROM groups WHERE id = :groupId")
    fun observeById(groupId: String): Flow<GroupEntity?>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY joinedAt")
    suspend fun getMembers(groupId: String): List<GroupMemberEntity>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY joinedAt")
    fun observeMembers(groupId: String): Flow<List<GroupMemberEntity>>

    @androidx.room3.Insert(onConflict = androidx.room3.OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(members: List<GroupMemberEntity>)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun removeMember(groupId: String, userId: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteMembers(groupId: String)

    @Query("UPDATE groups SET memberVersion = memberVersion + 1, updatedAt = :updatedAt WHERE id = :groupId")
    suspend fun incrementVersion(groupId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE groups SET name = :name, updatedAt = :updatedAt WHERE id = :groupId")
    suspend fun updateName(groupId: String, name: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE groups SET announcement = :announcement, updatedAt = :updatedAt WHERE id = :groupId")
    suspend fun updateAnnouncement(groupId: String, announcement: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE groups SET remark = :remark, updatedAt = :updatedAt WHERE id = :groupId")
    suspend fun updateRemark(groupId: String, remark: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE groups SET savedToContacts = :saved, updatedAt = :updatedAt WHERE id = :groupId")
    suspend fun updateSavedToContacts(groupId: String, saved: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE groups SET showMemberNicknames = :show, updatedAt = :updatedAt WHERE id = :groupId")
    suspend fun updateShowMemberNicknames(groupId: String, show: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE groups SET isFolded = :folded, updatedAt = :updatedAt WHERE id = :groupId")
    suspend fun updateFolded(groupId: String, folded: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE group_members SET nickname = :nickname WHERE groupId = :groupId AND userId = :userId")
    suspend fun updateMemberNickname(groupId: String, userId: String, nickname: String)

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Transaction
    suspend fun create(group: GroupEntity, members: List<GroupMemberEntity>) {
        insert(group)
        upsertMembers(members)
    }

    @Transaction
    suspend fun replace(group: GroupEntity, members: List<GroupMemberEntity>) {
        insert(group)
        deleteMembers(group.id)
        upsertMembers(members)
    }
}
