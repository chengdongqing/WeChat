package top.chengdongqing.wechat.core.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "contact_tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class ContactTagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contact_tag_members",
    primaryKeys = ["tagId", "contactId"],
    foreignKeys = [
        ForeignKey(
            entity = ContactTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId"), Index("contactId")]
)
data class ContactTagMemberEntity(
    val tagId: String,
    val contactId: String,
    val addedAt: Long = System.currentTimeMillis()
)
