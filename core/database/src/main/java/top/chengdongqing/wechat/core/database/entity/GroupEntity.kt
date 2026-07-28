package top.chengdongqing.wechat.core.database.entity

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerId: String,
    val avatarPath: String? = null,
    val announcement: String? = null,
    val remark: String? = null,
    val savedToContacts: Boolean = true,
    val showMemberNicknames: Boolean = true,
    val isFolded: Boolean = false,
    val memberVersion: Long = 1,
    val meshEnabled: Boolean = true,
    @Embedded val audit: EntityAudit = EntityAudit()
)

@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId"), Index("userId")]
)
data class GroupMemberEntity(
    val groupId: String,
    val userId: String,
    val nickname: String,
    val avatarPath: String? = null,
    val role: GroupMemberRole = GroupMemberRole.Member,
    val joinedAt: Long = System.currentTimeMillis()
)

enum class GroupMemberRole { Owner, Admin, Member }
