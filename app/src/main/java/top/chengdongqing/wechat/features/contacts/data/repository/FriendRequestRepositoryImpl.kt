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
import top.chengdongqing.wechat.data.database.entity.AddSource
import top.chengdongqing.wechat.data.database.entity.ContactEntity
import top.chengdongqing.wechat.data.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.database.entity.RequestStatus
import top.chengdongqing.wechat.data.database.entity.toDomain
import top.chengdongqing.wechat.data.network.protocol.P2PMessage
import top.chengdongqing.wechat.data.network.protocol.P2PMessageTransmitter
import top.chengdongqing.wechat.data.network.protocol.RequestAction
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.FriendProfileResponse
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequestResponse
import top.chengdongqing.wechat.features.contacts.domain.model.IncomingFriendRequest
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * 好友申请业务仓库
 *
 * 职责：
 * - 管理好友申请的发送、接收、处理
 * - 处理自动添加回复（对方保留着我）
 * - 同步好友完整资料
 */
class FriendRequestRepositoryImpl @Inject constructor(
    private val friendRequestDao: FriendRequestDao,
    private val contactDao: ContactDao,
    private val profileRepository: ProfileRepository,
    private val transmitter: P2PMessageTransmitter,
    private val imageExt: ImageExt,
    private val json: Json
) : FriendRequestRepository {

    private companion object {
        const val TAG = "FriendRequest"
        const val PROFILE_SEND_DELAY = 2000L  // 延迟发送完整资料
    }

    // ==================== 查询 ====================

    /**
     * 获取所有好友申请（发出和收到）
     */
    override fun getRequests(): Flow<List<FriendRequest>> {
        return friendRequestDao.getAll().map { it.toDomain() }
    }

    /**
     * 获取待处理数量
     */
    override fun getPendingCount(): Flow<Int> = friendRequestDao.getPendingCount()

    /**
     * 获取未读数量
     */
    override fun observeUnreadCount(): Flow<Int> = friendRequestDao.observeUnreadCount()

    // ==================== 标记已读 ====================

    /**
     * 标记所有收到的申请为已读
     */
    override suspend fun markAllIncomingAsRead() {
        withContext(Dispatchers.IO) {
            friendRequestDao.markAllIncomingAsRead(currentTimeMillis())
        }
    }

    // ==================== 删除 ====================

    /**
     * 删除申请记录
     */
    override suspend fun delete(requestId: String) {
        friendRequestDao.delete(requestId)
    }

    // ==================== 发送申请 ====================

    /**
     * 发送好友申请
     *
     * @param targetContact 目标联系人
     * @param greetingMessage 打招呼消息
     * @param remark 备注名
     * @param tags 标签列表
     * @param note 备忘
     */
    override suspend fun sendFriendRequest(
        targetContact: Contact,
        greetingMessage: String,
        remark: String?,
        tags: List<String>?,
        note: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. 获取个人资料
            val myProfile = profileRepository.getCurrentProfileSnapshot()
                ?: throw Exception("未找到个人资料")

            // 2. 准备头像数据
            val avatarBytes = myProfile.avatarPath?.let { imageExt.generateThumbnailBytes(it) }

            // 3. 构造并发送消息
            val requestId = randomUUID()
            val message = P2PMessage.FriendRequest(
                requestId = requestId,
                peerUserId = myProfile.id,
                peerNickname = myProfile.nickname,
                greetingMessage = greetingMessage,
                avatarSize = avatarBytes?.size ?: 0,
                timestamp = currentTimeMillis()
            )

            transmitter.sendMessage(targetContact.id, message, avatarBytes)
                .also { if (!it) throw Exception("无法连接到对方设备") }

            // 4. 保存申请记录
            saveOutgoingRequest(requestId, targetContact, greetingMessage, remark, tags, note)

            Log.d(TAG, "已发送好友申请: ${targetContact.nickname}")
            Unit
        }
    }

    // ==================== 接受申请 ====================

    /**
     * 接受好友申请
     */
    override suspend fun acceptFriendRequest(
        requestId: String,
        remark: String?,
        tags: List<String>?,
        note: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. 验证申请
            val request = friendRequestDao.getById(requestId)
                ?: throw Exception("申请不存在")

            if (request.status != RequestStatus.Pending) {
                throw Exception("申请已处理")
            }

            // 2. 发送接受响应
            val response = P2PMessage.FriendRequestResponse(
                requestId = requestId,
                action = RequestAction.ACCEPT,
                timestamp = currentTimeMillis()
            )

            transmitter.sendMessage(request.peerUserId, response)
                .also { if (!it) throw Exception("无法连接到对方设备") }

            // 3. 添加到通讯录
            addContactFromRequest(request, remark, tags, note)

            // 4. 更新申请状态
            friendRequestDao.updateStatus(requestId, RequestStatus.Accepted, currentTimeMillis())

            // 5. 延迟发送完整资料
            scheduleProfileSend(request.peerUserId)

            Log.d(TAG, "已接受申请: ${request.peerNickname}")
            Unit
        }
    }

    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        TODO("Provide the return value")
    }

    // ==================== 处理收到的消息 ====================

    /**
     * 处理收到的好友申请
     */
    override suspend fun handleIncomingRequest(request: IncomingFriendRequest) =
        withContext(Dispatchers.IO) {
            try {
                // 1. 检查是否已是好友
                if (contactDao.exists(request.peerUserId)) {
                    handleAlreadyFriend(
                        request.peerUserId,
                        request.peerNickname,
                        request.avatarData,
                        request.requestId
                    )
                    return@withContext
                }

                // 2. 保存新申请
                saveIncomingRequest(
                    request.requestId,
                    request.peerUserId,
                    request.peerNickname,
                    request.greetingMessage,
                    request.avatarData,
                    request.timestamp
                )
                Log.d(TAG, "已保存申请: ${request.peerNickname}")

            } catch (e: Exception) {
                Log.e(TAG, "处理申请失败", e)
            }
        }

    /**
     * 处理申请响应（接受/拒绝）
     */
    override suspend fun handleRequestResponse(response: FriendRequestResponse) =
        withContext(Dispatchers.IO) {
            try {
                val request = friendRequestDao.getById(response.requestId) ?: return@withContext

                when {
                    response.accepted -> handleAccepted(request)
                    else -> handleRejected(response.requestId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理响应失败", e)
            }
        }

    /**
     * 处理自动添加回复（对方还保留着我）
     */
    override suspend fun handleAutoAddResponse(response: FriendProfileResponse) =
        withContext(Dispatchers.IO) {
            try {
                // 1. 检查是否已是好友
                if (contactDao.exists(response.userId)) {
                    Log.d(TAG, "已是好友，忽略自动添加")
                    return@withContext
                }

                // 2. 查询原申请记录（获取备注等信息）
                val originalRequest = friendRequestDao.getByPeerUserId(
                    response.userId,
                    RequestDirection.Outgoing
                )

                // 3. 保存头像
                val avatarPath = response.avatarData?.let {
                    imageExt.saveAvatarBytes(response.userId, it, isThumbnail = true)
                }

                // 4. 添加到通讯录
                val contact = ContactEntity(
                    userId = response.userId,
                    nickname = response.nickname,
                    avatarPath = avatarPath,
                    signature = response.signature,
                    gender = response.gender,
                    remarkName = originalRequest?.remark,  // 使用原备注
                    tags = originalRequest?.tags,
                    note = originalRequest?.note,
                    addedAt = currentTimeMillis(),
                    updatedAt = currentTimeMillis()
                )

                contactDao.insert(contact)

                // 5. 清理申请记录
                originalRequest?.let { friendRequestDao.delete(it.id) }

                Log.d(TAG, "✅ 自动添加成功: ${response.nickname}")

            } catch (e: Exception) {
                Log.e(TAG, "处理自动添加失败", e)
            }
        }

    /**
     * 处理完整资料响应
     */
    override suspend fun handleFullProfileResponse(response: FriendProfileResponse) =
        withContext(Dispatchers.IO) {
            try {
                val contact = contactDao.getById(response.userId) ?: return@withContext

                // 保存完整头像
                val avatarPath = response.avatarData?.let {
                    imageExt.saveAvatarBytes(response.userId, it, isThumbnail = false)
                }

                // 更新联系人信息
                val updated = contact.copy(
                    avatarPath = avatarPath ?: contact.avatarPath,
                    signature = response.signature,
                    gender = response.gender,
                    updatedAt = currentTimeMillis()
                )

                contactDao.update(updated)
                Log.d(TAG, "完整资料已更新: ${response.nickname}")

            } catch (e: Exception) {
                Log.e(TAG, "处理完整资料失败", e)
            }
        }

    // ==================== 私有辅助方法 ====================

    /**
     * 处理已是好友的情况（自动回复）
     */
    private suspend fun handleAlreadyFriend(
        peerUserId: String,
        peerNickname: String,
        avatarData: ByteArray?,
        requestId: String,
    ) {
        Log.d(TAG, "$peerNickname 已在通讯录，自动回复")

        // 1. 发送自动添加回复
        sendAutoAddResponse(peerUserId, requestId)

        // 2. 更新好友信息（昵称、头像可能变了）
        updateContactInfo(peerUserId, peerNickname, avatarData)
    }

    /**
     * 发送自动添加回复
     */
    private suspend fun sendAutoAddResponse(targetUserId: String, requestId: String) {
        try {
            val myProfile = profileRepository.getCurrentProfileSnapshot() ?: return

            val avatarBytes = myProfile.avatarPath?.let {
                imageExt.generateThumbnailBytes(it)
            }

            val message = P2PMessage.AutoAddResponse(
                requestId = requestId,
                userId = myProfile.id,
                nickname = myProfile.nickname,
                signature = myProfile.signature,
                gender = myProfile.gender,
                avatarSize = avatarBytes?.size ?: 0,
                timestamp = currentTimeMillis()
            )

            transmitter.sendMessage(targetUserId, message, avatarBytes)

            Log.d(TAG, "✅ 已发送自动添加回复")
        } catch (e: Exception) {
            Log.e(TAG, "发送自动添加回复失败", e)
        }
    }

    /**
     * 更新联系人信息
     */
    private suspend fun updateContactInfo(
        userId: String,
        nickname: String,
        avatarBytes: ByteArray?
    ) {
        val avatarPath = avatarBytes?.let {
            imageExt.saveAvatarBytes(userId, it, isThumbnail = true)
        }

        val contact = contactDao.getById(userId) ?: return

        contactDao.update(
            contact.copy(
                nickname = nickname,
                avatarPath = avatarPath ?: contact.avatarPath,
                updatedAt = currentTimeMillis()
            )
        )
    }

    /**
     * 处理对方接受了申请
     */
    private suspend fun handleAccepted(request: FriendRequestEntity) {
        Log.d(TAG, "对方接受了申请: ${request.peerNickname}")

        // 1. 添加到通讯录
        addContactFromRequest(request)

        // 2. 更新状态
        friendRequestDao.updateStatus(
            request.id,
            RequestStatus.Accepted,
            currentTimeMillis()
        )

        // 3. 发送完整资料
        scheduleProfileSend(request.peerUserId)
    }

    /**
     * 处理对方拒绝了申请
     */
    private suspend fun handleRejected(requestId: String) {
        friendRequestDao.updateStatus(requestId, RequestStatus.Rejected, currentTimeMillis())
        Log.d(TAG, "申请被拒绝")
    }

    /**
     * 延迟发送完整资料
     */
    private fun scheduleProfileSend(targetUserId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(PROFILE_SEND_DELAY)
            sendFullProfile(targetUserId)
        }
    }

    /**
     * 发送完整资料
     */
    private suspend fun sendFullProfile(targetUserId: String) {
        try {
            val myProfile = profileRepository.getCurrentProfileSnapshot() ?: return

            val avatarBytes = myProfile.avatarPath?.let {
                imageExt.generateFullAvatarBytes(it)
            }

            val message = P2PMessage.FullProfileResponse(
                requestId = randomUUID(),
                userId = myProfile.id,
                nickname = myProfile.nickname,
                signature = myProfile.signature,
                gender = myProfile.gender,
                avatarSize = avatarBytes?.size ?: 0,
                timestamp = currentTimeMillis()
            )

            val success = transmitter.sendMessage(targetUserId, message, avatarBytes)

            Log.d(TAG, if (success) "✅ 完整资料已发送" else "完整资料发送失败")
        } catch (e: Exception) {
            Log.e(TAG, "发送完整资料失败", e)
        }
    }

    /**
     * 保存发出的申请
     */
    private suspend fun saveOutgoingRequest(
        requestId: String,
        targetContact: Contact,
        greetingMessage: String,
        remark: String?,
        tags: List<String>?,
        note: String?
    ) {
        val entity = FriendRequestEntity(
            id = requestId,
            peerUserId = targetContact.id,
            peerNickname = targetContact.nickname,
            peerAvatarPath = targetContact.avatarPath,
            greetingMessage = greetingMessage,
            remark = remark,
            tags = tags?.let { json.encodeToString(it) },
            note = note,
            status = RequestStatus.Pending,
            direction = RequestDirection.Outgoing,
            isRead = true,  // 发出的申请标记为已读
            createAt = currentTimeMillis(),
            updatedAt = currentTimeMillis()
        )

        friendRequestDao.insert(entity)
    }

    /**
     * 保存收到的申请
     */
    private suspend fun saveIncomingRequest(
        requestId: String,
        peerUserId: String,
        peerNickname: String,
        greetingMessage: String,
        avatarData: ByteArray?,
        timestamp: Long
    ) {
        val avatarPath = avatarData?.let {
            imageExt.saveAvatarBytes(peerUserId, it, isThumbnail = true)
        }

        val entity = FriendRequestEntity(
            id = requestId,
            peerUserId = peerUserId,
            peerNickname = peerNickname,
            peerAvatarPath = avatarPath,
            greetingMessage = greetingMessage,
            status = RequestStatus.Pending,
            direction = RequestDirection.Incoming,
            isRead = false,  // 收到的申请标记为未读
            createAt = timestamp,
            updatedAt = currentTimeMillis()
        )

        friendRequestDao.insert(entity)
    }

    /**
     * 从申请记录添加联系人
     */
    private suspend fun addContactFromRequest(
        request: FriendRequestEntity,
        remark: String? = null,
        tags: List<String>? = null,
        note: String? = null
    ) {
        val contact = ContactEntity(
            userId = request.peerUserId,
            nickname = request.peerNickname,
            avatarPath = request.peerAvatarPath,
            remarkName = remark ?: request.remark,
            tags = tags?.let { json.encodeToString(it) } ?: request.tags,
            note = note ?: request.note,
            source = AddSource.QRCode,
            addedAt = currentTimeMillis(),
            updatedAt = currentTimeMillis()
        )

        contactDao.insert(contact)
    }

    /**
     * 获取当前时间戳
     */
    private fun currentTimeMillis() = System.currentTimeMillis()
}