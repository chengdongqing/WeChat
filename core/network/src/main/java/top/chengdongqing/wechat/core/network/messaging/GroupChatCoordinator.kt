package top.chengdongqing.wechat.core.network.messaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.room3.withWriteTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.model.GroupMemberSnapshot
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.database.WeDatabase
import top.chengdongqing.wechat.core.database.dao.ChatSessionDao
import top.chengdongqing.wechat.core.database.dao.GroupDao
import top.chengdongqing.wechat.core.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.core.database.entity.GroupEntity
import top.chengdongqing.wechat.core.database.entity.GroupMemberEntity
import top.chengdongqing.wechat.core.database.entity.GroupMemberRole
import top.chengdongqing.wechat.core.model.ContactResult
import top.chengdongqing.wechat.core.network.connection.ChatTransportManager
import top.chengdongqing.wechat.core.network.crypto.PacketSigner
import top.chengdongqing.wechat.core.network.model.Packet
import top.chengdongqing.wechat.core.network.model.PacketType
import top.chengdongqing.wechat.core.network.security.KeyStoreManager
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Singleton
class GroupChatCoordinator @Inject constructor(
    private val database: WeDatabase,
    private val groupDao: GroupDao,
    private val chatSessionDao: ChatSessionDao,
    private val profileRepository: ProfileRepository,
    private val transport: ChatTransportManager,
    private val packetSigner: PacketSigner,
    private val keyStoreManager: KeyStoreManager,
    private val json: Json,
    @param:ApplicationContext private val context: Context
) {
    suspend fun createGroup(selected: List<ContactResult>, customName: String? = null): Result<String> =
        runCatching {
            val me = profileRepository.requireProfile()
            val selectedMembers = selected.distinctBy { it.id }.filterNot { it.id == me.id }
            require(selectedMembers.isNotEmpty()) { "请至少选择一位联系人" }
            val groupId = "group_${UUID.randomUUID().toString().replace("-", "")}"
            val members = buildList {
                add(GroupMemberEntity(groupId, me.id, me.nickname, me.avatarPath, GroupMemberRole.Owner))
                selectedMembers.forEach {
                    add(GroupMemberEntity(groupId, it.id, it.nickname, it.avatarPath))
                }
            }
            val name = customName?.trim()?.takeIf(String::isNotEmpty)
                ?: members.take(4).joinToString("、") { it.nickname }.take(32)
            val avatarPath = generateGroupAvatar(groupId, members)
            val group = GroupEntity(groupId, name, me.id, avatarPath = avatarPath)
            database.withWriteTransaction {
                groupDao.create(group, members)
                chatSessionDao.insert(
                    ChatSessionEntity(
                        id = groupId,
                        contactId = groupId,
                        contactName = name,
                        contactAvatar = avatarPath,
                        lastMessageId = null,
                        lastMessage = null,
                        lastMessageType = null,
                        lastMessageTime = null
                    )
                )
            }

            sendSnapshot(group, members)
            groupId
        }

    suspend fun syncGroup(groupId: String): Result<Unit> = runCatching {
        val me = profileRepository.requireProfile()
        val currentMembers = groupDao.getMembers(groupId)
        val myRole = currentMembers.firstOrNull { it.userId == me.id }?.role
        require(myRole == GroupMemberRole.Owner || myRole == GroupMemberRole.Admin) {
            "无权修改群资料"
        }
        groupDao.incrementVersion(groupId)
        val group = groupDao.getById(groupId) ?: error("群聊不存在")
        sendSnapshot(group, groupDao.getMembers(groupId))
    }

    private suspend fun sendSnapshot(
        group: GroupEntity,
        members: List<GroupMemberEntity>
    ) {
        val me = profileRepository.requireProfile()
        val unsigned = ChatProtocol.GroupSnapshot(
            messageId = UUID.randomUUID().toString(),
            senderId = me.id,
            signature = "",
            groupId = group.id,
            name = group.name,
            announcement = group.announcement,
            ownerId = group.ownerId,
            memberVersion = group.memberVersion,
            members = members.map {
                GroupMemberSnapshot(it.userId, it.nickname, it.avatarPath, it.role.name)
            }
        )
        val signed = unsigned.copy(
            signature = packetSigner.sign(unsigned, keyStoreManager.getPrivateKey())
        )
        val packet = Packet(
            PacketType.TEXT,
            json.encodeToString<ChatProtocol>(signed).toByteArray(Charsets.UTF_8)
        )
        members.asSequence()
            .map { it.userId }
            .filter { it != me.id }
            .distinct()
            .forEach { transport.send(it, packet) }
    }

    internal fun generateGroupAvatar(
        groupId: String,
        members: List<GroupMemberEntity>
    ): String {
        val size = 240
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(230, 230, 230))
        val shown = members.take(9)
        val columns = if (shown.size <= 4) 2 else 3
        val rows = (shown.size + columns - 1) / columns
        val gap = 6
        val cell = (size - gap * (columns + 1)) / columns
        val contentHeight = rows * cell + (rows + 1) * gap
        val top = (size - contentHeight) / 2
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = cell * .42f
        }
        shown.forEachIndexed { index, member ->
            val left = gap + (index % columns) * (cell + gap)
            val y = top + gap + (index / columns) * (cell + gap)
            val avatar = member.avatarPath
                ?.takeIf { File(it).isFile }
                ?.let(BitmapFactory::decodeFile)
            if (avatar != null) {
                canvas.drawBitmap(
                    avatar,
                    null,
                    android.graphics.Rect(left, y, left + cell, y + cell),
                    paint
                )
            } else {
                paint.color = listOf(
                    Color.rgb(87, 160, 219),
                    Color.rgb(91, 184, 132),
                    Color.rgb(228, 156, 79),
                    Color.rgb(157, 124, 196)
                )[index % 4]
                canvas.drawRect(left.toFloat(), y.toFloat(), (left + cell).toFloat(), (y + cell).toFloat(), paint)
                val label = member.nickname.trim().take(1).ifEmpty { "群" }
                val baseline = y + cell / 2f - (textPaint.ascent() + textPaint.descent()) / 2
                canvas.drawText(label, left + cell / 2f, baseline, textPaint)
            }
        }
        val directory = File(context.filesDir, "group_avatars").apply { mkdirs() }
        val file = File(directory, "$groupId.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
        bitmap.recycle()
        return file.absolutePath
    }
}
