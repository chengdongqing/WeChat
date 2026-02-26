package top.chengdongqing.wechat.features.contacts.data.repository

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.util.ImageExt
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.FriendRequestDao
import top.chengdongqing.wechat.data.database.entity.AddSource
import top.chengdongqing.wechat.data.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.database.entity.RequestStatus
import top.chengdongqing.wechat.data.network.protocol.P2PMessage
import top.chengdongqing.wechat.data.network.protocol.P2PMessageTransmitter
import top.chengdongqing.wechat.data.network.protocol.RequestAction
import top.chengdongqing.wechat.features.contacts.data.mapper.toDomain
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.FriendProfileResponse
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequestResponse
import top.chengdongqing.wechat.features.contacts.domain.model.IncomingFriendRequest
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

class FriendRequestRepositoryImpl @Inject constructor(
    private val weDatabase: WeDatabase,
    private val friendRequestDao: FriendRequestDao,
    private val contactRepository: ContactRepository,
    private val profileRepository: ProfileRepository,
    private val transmitter: P2PMessageTransmitter,
    private val imageExt: ImageExt
) : FriendRequestRepository {

    private companion object {
        const val TAG = "FriendRequest"
        const val PROFILE_SEND_DELAY = 2000L  // 延迟发送完整资料
    }

    override fun observeAllRequest(): Flow<List<FriendRequest>> {
        return friendRequestDao.observeAll().map { it.toDomain() }
    }

    override fun getPendingCount(): Flow<Int> = friendRequestDao.getPendingCount()

    override fun observeUnreadCount(): Flow<Int> = friendRequestDao.observeUnreadCount()

    override suspend fun markAllIncomingAsRead() {
        friendRequestDao.markAllIncomingAsRead()
    }

    override suspend fun deleteRequest(requestId: String) {
        friendRequestDao.deleteById(requestId)
    }

    override suspend fun sendFriendRequest(
        targetContact: Contact,
        greetingMessage: String,
        remark: String?,
        note: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val myProfile = profileRepository.getCurrentProfileSnapshot()
                ?: throw Exception("未找到个人资料")

            val avatarBytes = myProfile.avatarPath?.let {
                imageExt.generateThumbnailBytes(it)
            }

            // 构造并发送消息
            val requestId = randomUUID()
            val message = P2PMessage.FriendRequest(
                requestId = requestId,
                peerUserId = myProfile.id,
                peerNickname = myProfile.nickname,
                greetingMessage = greetingMessage,
                avatarSize = avatarBytes?.size ?: 0,
                timestamp = currentTimestamp()
            )
            transmitter.sendMessage(targetContact.id, message, avatarBytes)
                .also { if (!it) throw Exception("无法连接到对方设备") }

            // 保存申请记录
            saveOutgoingRequest(requestId, targetContact, greetingMessage, remark, note)
        }
    }

    override suspend fun acceptFriendRequest(
        requestId: String,
        remark: String?,
        note: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 判断申请记录的状态是否正常
            val request = friendRequestDao.getById(requestId)
                ?: throw Exception("申请不存在")
            if (request.status != RequestStatus.Pending) {
                throw Exception("申请已处理")
            }

            // 发送接受响应
            val response = P2PMessage.FriendRequestResponse(
                requestId = requestId,
                action = RequestAction.ACCEPT,
                timestamp = currentTimestamp()
            )
            transmitter.sendMessage(request.peerId, response)
                .also { if (!it) throw Exception("无法连接到对方设备") }

            // 添加到通讯录
            addContactFromRequest(request, remark, note)

            // 更新申请状态
            friendRequestDao.update(requestId) { request ->
                request.copy(
                    status = RequestStatus.Accepted
                )
            }

            // 延迟发送完整资料
            scheduleProfileSend(request.peerId)
        }
    }

    override suspend fun handleIncomingRequest(request: IncomingFriendRequest) =
        withContext(Dispatchers.IO) {
            try {
                // 检查是否已是好友
                if (contactRepository.exists(request.peerUserId)) {
                    // 已经是好友走静默通过
                    handleAlreadyFriend(
                        request.peerUserId,
                        request.peerNickname,
                        request.avatarData,
                        request.requestId
                    )
                    return@withContext
                }

                // 保存新申请
                saveIncomingRequest(
                    request.requestId,
                    request.peerUserId,
                    request.peerNickname,
                    request.greetingMessage,
                    request.avatarData
                )
            } catch (e: Exception) {
                Log.e(TAG, "处理申请失败", e)
            }
        }

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

    override suspend fun handleAutoAddResponse(response: FriendProfileResponse) =
        withContext(Dispatchers.IO) {
            try {
                // 检查是否已是好友
                if (contactRepository.exists(response.userId)) {
                    return@withContext
                }

                // 查询原申请记录（获取备注等信息）
                val originalRequest = friendRequestDao.getByPeerId(
                    response.userId,
                    RequestDirection.Outgoing
                )

                // 保存头像
                val avatarPath = response.avatarData?.let {
                    imageExt.saveAvatarBytes(response.userId, it, isThumbnail = true)
                }

                // 添加到通讯录
                val contact = Contact(
                    id = response.userId,
                    nickname = response.nickname,
                    avatarPath = avatarPath,
                    signature = response.signature,
                    gender = response.gender,
                    remarkName = originalRequest?.remark,
                    note = originalRequest?.note
                )
                contactRepository.addContact(contact)

                // 清理申请记录
                originalRequest?.let { friendRequestDao.deleteById(it.id) }
            } catch (e: Exception) {
                Log.e(TAG, "处理自动添加失败", e)
            }
        }

    override suspend fun handleFullProfileResponse(response: FriendProfileResponse) =
        withContext(Dispatchers.IO) {
            // 保存完整头像
            val avatarPath = response.avatarData?.let {
                imageExt.saveAvatarBytes(response.userId, it, isThumbnail = false)
            }

            // 更新联系人信息
            contactRepository.updateContact(response.userId) { contact ->
                contact.copy(
                    avatarPath = avatarPath ?: contact.avatarPath,
                    signature = response.signature,
                    gender = response.gender
                )
            }
        }

    /**
     * 处理已是好友的情况（自动回复）
     */
    private suspend fun handleAlreadyFriend(
        peerUserId: String,
        peerNickname: String,
        avatarData: ByteArray?,
        requestId: String,
    ) {
        // 发送自动添加回复
        sendAutoAddResponse(peerUserId, requestId)

        // 更新好友信息（昵称、头像可能变了）
        updateContactInfo(peerUserId, peerNickname, avatarData)
    }

    /**
     * 发送自动添加回复
     */
    private suspend fun sendAutoAddResponse(targetUserId: String, requestId: String) {
        try {
            // 获取我的信息
            val myProfile = profileRepository.getCurrentProfileSnapshot() ?: return

            // 生成头像数据
            val avatarBytes = myProfile.avatarPath?.let {
                imageExt.generateThumbnailBytes(it)
            }

            // 发送自动同意添加的响应，并携带我的个人信息
            val message = P2PMessage.AutoAddResponse(
                requestId = requestId,
                userId = myProfile.id,
                nickname = myProfile.nickname,
                signature = myProfile.signature,
                gender = myProfile.gender,
                avatarSize = avatarBytes?.size ?: 0,
                timestamp = currentTimestamp()
            )
            transmitter.sendMessage(targetUserId, message, avatarBytes)
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
        // 保存头像到本地
        val avatarPath = avatarBytes?.let {
            imageExt.saveAvatarBytes(userId, it, isThumbnail = true)
        }

        // 更新数据库
        contactRepository.updateContact(userId) { contact ->
            contact.copy(
                nickname = nickname,
                avatarPath = avatarPath ?: contact.avatarPath
            )
        }
    }

    /**
     * 处理对方接受了申请
     */
    private suspend fun handleAccepted(request: FriendRequestEntity) {
        weDatabase.withTransaction {
            // 添加到通讯录
            addContactFromRequest(request)
            // 更新请求状态
            friendRequestDao.update(request.id) { request ->
                request.copy(
                    status = RequestStatus.Accepted
                )
            }
        }

        // 发送完整资料
        scheduleProfileSend(request.peerId)
    }

    /**
     * 处理对方拒绝了申请
     */
    private suspend fun handleRejected(requestId: String) {
        friendRequestDao.update(requestId) { request ->
            request.copy(
                status = RequestStatus.Rejected
            )
        }
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
            // 获取我的个人资料
            val myProfile = profileRepository.getCurrentProfileSnapshot() ?: return

            // 获取头像数据
            val avatarBytes = myProfile.avatarPath?.let {
                imageExt.generateFullAvatarBytes(it)
            }

            // 构建并发送完整资料
            val message = P2PMessage.FullProfileResponse(
                requestId = randomUUID(),
                userId = myProfile.id,
                nickname = myProfile.nickname,
                signature = myProfile.signature,
                gender = myProfile.gender,
                avatarSize = avatarBytes?.size ?: 0,
                timestamp = currentTimestamp()
            )
            transmitter.sendMessage(targetUserId, message, avatarBytes)
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
        note: String?
    ) {
        val entity = FriendRequestEntity(
            id = requestId,
            peerId = targetContact.id,
            peerName = targetContact.nickname,
            peerAvatarPath = targetContact.avatarPath,
            greetingMessage = greetingMessage,
            remark = remark,
            note = note,
            status = RequestStatus.Pending,
            direction = RequestDirection.Outgoing,
            isRead = true
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
        avatarData: ByteArray?
    ) {
        // 保存头像到本地
        val avatarPath = avatarData?.let {
            imageExt.saveAvatarBytes(peerUserId, it, isThumbnail = true)
        }

        // 构建并保存请求记录
        val entity = FriendRequestEntity(
            id = requestId,
            peerId = peerUserId,
            peerName = peerNickname,
            peerAvatarPath = avatarPath,
            greetingMessage = greetingMessage,
            status = RequestStatus.Pending,
            direction = RequestDirection.Incoming,
            isRead = false
        )
        friendRequestDao.insert(entity)
    }

    /**
     * 从申请记录添加联系人
     */
    private suspend fun addContactFromRequest(
        request: FriendRequestEntity,
        remark: String? = null,
        note: String? = null
    ) {
        val contact = Contact(
            id = request.peerId,
            nickname = request.peerName,
            avatarPath = request.peerAvatarPath,
            remarkName = remark ?: request.remark,
            note = note ?: request.note,
            source = AddSource.QRCode
        )
        contactRepository.addContact(contact)
    }

    /**
     * 获取当前时间戳
     */
    private fun currentTimestamp() = System.currentTimeMillis()
}