package top.chengdongqing.wechat.features.contacts.data.repository

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.util.ImageExt
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.database.dao.FriendRequestDao
import top.chengdongqing.wechat.data.database.entity.ContactEntity
import top.chengdongqing.wechat.data.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.database.entity.RequestStatus
import top.chengdongqing.wechat.data.network.discovery.BLEDiscovery
import top.chengdongqing.wechat.data.network.protocol.P2PMessage
import top.chengdongqing.wechat.data.network.protocol.P2PMessageTransmitter
import top.chengdongqing.wechat.data.network.protocol.RequestAction
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest
import top.chengdongqing.wechat.features.me.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRequestRepository @Inject constructor(
    private val friendRequestDao: FriendRequestDao,
    private val contactDao: ContactDao,
    private val bleDiscovery: BLEDiscovery,
    private val profileRepository: ProfileRepository,
    private val imageExt: ImageExt,
    private val json: Json
) {

    private val transmitter by lazy {
        P2PMessageTransmitter(bleDiscovery, json)
    }

    /**
     * 获取所有发出和收到的好友申请
     */
    fun getRequests(): Flow<List<FriendRequest>> {
        return friendRequestDao.getAll()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * 获取待处理数量
     */
    fun getPendingCount(): Flow<Int> {
        return friendRequestDao.getPendingCount()
    }

    /**
     * 删除请求
     */
    suspend fun delete(requestId: String) {
        return friendRequestDao.delete(requestId)
    }

    /**
     * 发送好友申请
     */
    suspend fun sendFriendRequest(
        targetContact: Contact,
        greetingMessage: String,
        remark: String?,
        tags: List<String>?,
        note: String?
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val myProfile = profileRepository.getCurrentProfileOnce()
                    ?: return@withContext Result.failure(Exception("未找到个人资料"))

                // 1. 生成请求ID
                val requestId = randomUUID()

                // 生成缩略图数据
                val avatarBytes = myProfile.avatarPath?.let { path ->
                    imageExt.generateThumbnailBytes(path)
                }

                // 2. 构造P2P消息
                val message = P2PMessage.FriendRequest(
                    requestId = requestId,
                    peerUserId = myProfile.id,
                    peerNickname = myProfile.nickname,
                    greetingMessage = greetingMessage,
                    remark = remark,
                    tags = tags,
                    note = note,
                    avatarSize = avatarBytes?.size ?: 0,
                    timestamp = System.currentTimeMillis()
                )

                // 发送消息 + 头像二进制
                val success = transmitter.sendMessage(
                    targetUserId = targetContact.id,
                    message = message,
                    binaryData = avatarBytes
                )

                if (!success) {
                    return@withContext Result.failure(Exception("无法连接到对方设备"))
                }

                // 4. 保存到本地数据库（发出的申请）
                val entity = FriendRequestEntity(
                    id = requestId,
                    peerUserId = targetContact.id,
                    peerNickname = targetContact.nickname,
                    peerAvatarPath = targetContact.avatarPath,
                    greetingMessage = greetingMessage,
                    remark = remark,
                    tags = tags?.let { json.encodeToString(it) },
                    note = note,
                    status = RequestStatus.PENDING,
                    direction = RequestDirection.OUTGOING,
                    createAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                friendRequestDao.insert(entity)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 接受好友申请
     */
    suspend fun acceptFriendRequest(
        requestId: String,
        remark: String?,
        tags: List<String>?,
        note: String?
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 获取申请详情
                val request = friendRequestDao.getById(requestId)
                    ?: return@withContext Result.failure(Exception("申请不存在"))
                // 状态是否待处理
                if (request.status != RequestStatus.PENDING) {
                    return@withContext Result.failure(Exception("申请已处理"))
                }

                // 发送接受的响应
                val response = P2PMessage.FriendRequestResponse(
                    requestId = requestId,
                    action = RequestAction.ACCEPT,
                    remark = remark,
                    tags = tags,
                    note = note,
                    timestamp = System.currentTimeMillis()
                )
                val sendSuccess = transmitter.sendMessage(
                    targetUserId = request.peerUserId,
                    message = response
                )
                if (!sendSuccess) {
                    return@withContext Result.failure(Exception("无法连接到对方设备，请稍后重试"))
                }

                // 添加到通讯录
                val contactEntity = ContactEntity(
                    userId = request.peerUserId,
                    nickname = request.peerNickname,
                    avatarPath = request.peerAvatarPath,
                    remarkName = remark ?: request.remark,  // 优先使用新设置的备注
                    tags = tags?.let { json.encodeToString(it) } ?: request.tags,
                    note = note ?: request.note,
                    addedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                contactDao.insert(contactEntity)

                // 4. 更新申请状态
                friendRequestDao.updateStatus(
                    requestId,
                    RequestStatus.ACCEPTED,
                    System.currentTimeMillis()
                )

                // 主动发送我的资料
                CoroutineScope(Dispatchers.IO).launch {
                    delay(2000)
                    sendMyFullProfile(request.peerUserId)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 发送我的完整资料
     */
    private suspend fun sendMyFullProfile(targetUserId: String) {
        try {
            val myProfile = profileRepository.getCurrentProfileOnce() ?: return

            val avatarBytes = myProfile.avatarPath?.let { path ->
                imageExt.generateFullAvatarBytes(path)
            }

            val message = P2PMessage.FullProfileResponse(
                requestId = randomUUID(),
                userId = myProfile.id,
                nickname = myProfile.nickname,
                signature = myProfile.signature,
                gender = myProfile.gender,
                avatarSize = avatarBytes?.size ?: 0,
                timestamp = System.currentTimeMillis()
            )

            val success = transmitter.sendMessage(targetUserId, message, avatarBytes)

            if (success) {
                Log.d("FriendRequest", "✅ 完整资料已发送")
            } else {
                Log.w("FriendRequest", "完整资料发送失败，将在后台重试")
            }
        } catch (e: Exception) {
            Log.e("FriendRequest", "发送完整资料失败", e)
        }
    }

    /**
     * 处理收到的好友申请
     */
    suspend fun handleIncomingRequest(message: P2PMessage.FriendRequest, avatarBytes: ByteArray?) {
        withContext(Dispatchers.IO) {
            try {
                // 检查是否已经是好友
                if (contactDao.exists(message.peerUserId)) {
                    Log.d("FriendRequest", "已经是好友")
//                    return@withContext
                }

                // 保存头像二进制
                val avatarPath = avatarBytes?.let { bytes ->
                    imageExt.saveAvatarBytes(message.peerUserId, bytes, isThumbnail = true)
                }

                // 保存申请
                val entity = FriendRequestEntity(
                    id = message.requestId,
                    peerUserId = message.peerUserId,
                    peerNickname = message.peerNickname,
                    peerAvatarPath = avatarPath,
                    greetingMessage = message.greetingMessage,
                    remark = message.remark,
                    tags = message.tags?.let { json.encodeToString(it) },
                    note = message.note,
                    status = RequestStatus.PENDING,
                    direction = RequestDirection.INCOMING,
                    createAt = message.timestamp,
                    updatedAt = System.currentTimeMillis()
                )

                friendRequestDao.insert(entity)
                Log.d("FriendRequest", "好友申请已保存")
            } catch (e: Exception) {
                Log.e("FriendRequest", "处理申请失败", e)
            }
        }
    }

    /**
     * 处理好友申请响应
     */
    suspend fun handleRequestResponse(message: P2PMessage.FriendRequestResponse) {
        withContext(Dispatchers.IO) {
            try {
                val request = friendRequestDao.getById(message.requestId) ?: return@withContext

                when (message.action) {
                    RequestAction.ACCEPT -> {
                        Log.d("FriendRequest", "对方接受了好友申请")

                        // 对方接受了，添加到通讯录
                        val contactEntity = ContactEntity(
                            userId = request.peerUserId,
                            nickname = request.peerNickname,
                            avatarPath = request.peerAvatarPath,
                            remarkName = message.remark,
                            tags = message.tags?.let { json.encodeToString(it) },
                            note = message.note,
                            addedAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )

                        contactDao.insert(contactEntity)

                        friendRequestDao.updateStatus(
                            message.requestId,
                            RequestStatus.ACCEPTED,
                            System.currentTimeMillis()
                        )

                        // 主动发送我的资料
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(2000)
                            sendMyFullProfile(request.peerUserId)
                        }
                    }

                    RequestAction.REJECT -> {
                        friendRequestDao.updateStatus(
                            message.requestId,
                            RequestStatus.REJECTED,
                            System.currentTimeMillis()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("FriendRequest", "处理响应失败", e)
            }
        }
    }

    /**
     * 处理完整资料响应
     */
    suspend fun handleFullProfileResponse(
        message: P2PMessage.FullProfileResponse,
        avatarBytes: ByteArray?
    ) {
        withContext(Dispatchers.IO) {
            try {
                // 保存完整头像
                val avatarPath = avatarBytes?.let { bytes ->
                    imageExt.saveAvatarBytes(message.userId, bytes, isThumbnail = false)
                }

                val contact = contactDao.getById(message.userId)

                if (contact != null) {
                    val updated = contact.copy(
                        avatarPath = avatarPath ?: contact.avatarPath,
                        signature = message.signature,
                        gender = message.gender,
                        updatedAt = System.currentTimeMillis()
                    )

                    contactDao.update(updated)

                    Log.d("FriendRequest", "完整资料已更新")
                }
            } catch (e: Exception) {
                Log.e("FriendRequest", "处理完整资料失败", e)
            }
        }
    }
}