package top.chengdongqing.wechat.features.contacts.data.repository

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.database.dao.FriendRequestDao
import top.chengdongqing.wechat.data.database.entity.ContactEntity
import top.chengdongqing.wechat.data.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.database.entity.RequestStatus
import top.chengdongqing.wechat.data.network.discovery.BLEDiscovery
import top.chengdongqing.wechat.data.network.protocol.P2PMessage
import top.chengdongqing.wechat.data.network.protocol.RequestAction
import top.chengdongqing.wechat.data.network.service.P2PService
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
    private val json: Json
) {

    /**
     * 获取所有收到的好友申请
     */
    fun getIncomingRequests(): Flow<List<FriendRequest>> {
        return friendRequestDao.getAllByDirection(RequestDirection.INCOMING)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * 获取待处理数量
     */
    fun getPendingCount(): Flow<Int> {
        return friendRequestDao.getPendingCount()
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

                // 2. 构造P2P消息
                val message = P2PMessage.FriendRequest(
                    requestId = requestId,
                    fromUserId = myProfile.id,
                    fromNickname = myProfile.nickname,
                    fromAvatarPath = myProfile.avatarPath,
                    toUserId = targetContact.id,
                    greetingMessage = greetingMessage,
                    remark = remark,
                    tags = tags,
                    note = note,
                    timestamp = System.currentTimeMillis()
                )

                // 3. 通过BLE发送
                val success = sendP2PMessage(targetContact.id, message)
                if (!success) {
                    return@withContext Result.failure(Exception("无法连接到对方设备"))
                }

                // 4. 保存到本地数据库（发出的申请）
                val entity = FriendRequestEntity(
                    requestId = requestId,
                    fromUserId = myProfile.id,
                    fromNickname = myProfile.nickname,
                    fromAvatarPath = myProfile.avatarPath,
                    toUserId = targetContact.id,
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
                // 1. 获取申请详情
                val request = friendRequestDao.getById(requestId)
                    ?: return@withContext Result.failure(Exception("申请不存在"))

                if (request.status != RequestStatus.PENDING) {
                    return@withContext Result.failure(Exception("申请已处理"))
                }

                // 2. 发送接受响应
                val response = P2PMessage.FriendRequestResponse(
                    requestId = requestId,
                    action = RequestAction.ACCEPT,
                    remark = remark,
                    tags = tags,
                    note = note,
                    timestamp = System.currentTimeMillis()
                )

                sendP2PMessage(request.fromUserId, response)

                // 3. 添加到通讯录
                val contactEntity = ContactEntity(
                    userId = request.fromUserId,
                    nickname = request.fromNickname,
                    avatarPath = request.fromAvatarPath,
                    signature = null,
                    gender = 0,
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

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 拒绝好友申请
     */
//    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
//        return withContext(Dispatchers.IO) {
//            try {
//                val request = friendRequestDao.getById(requestId)
//                    ?: return@withContext Result.failure(Exception("申请不存在"))
//
//                // 发送拒绝响应（可选）
//                val connection = bleDiscovery.scanAndConnect(request.fromUserId.toMD5Hex())
//
//                connection?.send(
//                    P2PMessage.FriendRequestResponse(
//                        requestId = requestId,
//                        action = RequestAction.REJECT,
//                        remark = null,
//                        tags = null,
//                        note = null,
//                        timestamp = System.currentTimeMillis()
//                    )
//                )
//
//                // 更新状态
//                friendRequestDao.updateStatus(
//                    requestId,
//                    RequestStatus.REJECTED,
//                    System.currentTimeMillis()
//                )
//
//                Result.success(Unit)
//            } catch (e: Exception) {
//                Result.failure(e)
//            }
//        }
//    }

    /**
     * 发送 P2P 消息
     */
    @SuppressLint("MissingPermission")
    private suspend fun sendP2PMessage(targetUserId: String, message: P2PMessage): Boolean {
        return try {
            // 1. 连接到对方设备
            val gatt = bleDiscovery.scanAndConnect(targetUserId.toMD5Hex())
                ?: return false

            // 2. 序列化消息
            val messageJson = json.encodeToString(message)
            val messageBytes = messageJson.toByteArray(Charsets.UTF_8)

            Log.d(
                "FriendRequest",
                "发送消息: ${message::class.simpleName}, 大小: ${messageBytes.size}"
            )

            // 3. 获取写入特征
            val service = gatt.getService(P2PService.SERVICE_UUID)
            val characteristic = service?.getCharacteristic(P2PService.CHARACTERISTIC_UUID)

            if (characteristic == null) {
                Log.e("FriendRequest", "未找到特征")
                gatt.close()
                return false
            }

            // 4. 写入数据
            val success = bleDiscovery.writeCharacteristic(gatt, characteristic, messageBytes)

            // 5. 关闭连接
            delay(1000)  // 等待写入完成
            gatt.close()

            Log.d("FriendRequest", "消息发送${if (success) "成功" else "失败"}")

            success
        } catch (e: Exception) {
            Log.e("FriendRequest", "发送消息失败", e)
            false
        }
    }

    /**
     * 处理收到的好友申请
     */
    suspend fun handleIncomingRequest(message: P2PMessage.FriendRequest) {
        withContext(Dispatchers.IO) {
            try {
                // 检查是否已经是好友
                if (contactDao.exists(message.fromUserId)) {
                    return@withContext
                }

                // 保存申请
                val entity = FriendRequestEntity(
                    requestId = message.requestId,
                    fromUserId = message.fromUserId,
                    fromNickname = message.fromNickname,
                    fromAvatarPath = message.fromAvatarPath,
                    toUserId = message.toUserId,
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

                // TODO: 发送系统通知
            } catch (e: Exception) {
                e.printStackTrace()
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
                        // 对方接受了，添加到通讯录
                        val contactEntity = ContactEntity(
                            userId = request.toUserId,
                            nickname = request.toUserId,  // 需要获取对方完整资料
                            avatarPath = null,
                            signature = null,
                            gender = 0,
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
                e.printStackTrace()
            }
        }
    }
}

// 扩展函数
private fun FriendRequestEntity.toDomain(): FriendRequest {
    return FriendRequest(
        requestId = requestId,
        fromUserId = fromUserId,
        fromNickname = fromNickname,
        fromAvatarPath = fromAvatarPath,
        greetingMessage = greetingMessage,
        remark = remark,
        status = status,
        timestamp = createAt
    )
}

private fun String.toMD5Hex(): String {
    val md = java.security.MessageDigest.getInstance("MD5")
    val digest = md.digest(this.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}