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
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.util.ImageExt
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.FriendRequestDao
import top.chengdongqing.wechat.data.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.data.model.ContactAddSource
import top.chengdongqing.wechat.data.model.FriendRequestStatus
import top.chengdongqing.wechat.data.network.messaging.BLEMessageSender
import top.chengdongqing.wechat.data.network.model.FriendProtocol
import top.chengdongqing.wechat.data.network.model.FriendRequestResult
import top.chengdongqing.wechat.features.contacts.data.mapper.toDomain
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.FriendProfileResponse
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequestResponse
import top.chengdongqing.wechat.features.contacts.domain.model.IncomingFriendRequest
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

class FriendRequestRepositoryImpl @Inject constructor(
    private val database: WeDatabase,
    private val friendRequestDao: FriendRequestDao,
    private val contactRepository: ContactRepository,
    private val profileRepository: ProfileRepository,
    private val transmitter: BLEMessageSender,
    private val imageExt: ImageExt,
    @param:IoScope private val scope: CoroutineScope
) : FriendRequestRepository {

    private companion object {
        const val TAG = "FriendRequest"
        const val PROFILE_SEND_DELAY = 2000L
    }

    // ==================== 查询 ====================

    override fun observeAllRequest(): Flow<List<FriendRequest>> =
        friendRequestDao.observeAll().map { it.toDomain() }

    override fun getPendingCount(): Flow<Int> = friendRequestDao.getPendingCount()

    override fun observeUnreadCount(): Flow<Int> = friendRequestDao.observeUnreadCount()

    override suspend fun markAllIncomingAsRead() = friendRequestDao.markAllIncomingAsRead()

    override suspend fun deleteRequest(requestId: String) = friendRequestDao.deleteById(requestId)

    // ==================== 发送申请 ====================

    override suspend fun sendFriendRequest(
        targetContact: Contact,
        greetingMessage: String,
        remark: String?,
        note: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val myProfile = requireProfile()
            val avatarBytes = myProfile.avatarPath?.let { imageExt.generateThumbnailBytes(it) }
            val requestId = randomUUID()

            transmitter.sendMessage(
                targetContact.id,
                FriendProtocol.FriendRequest(
                    requestId = requestId,
                    userId = myProfile.id,
                    nickname = myProfile.nickname,
                    publicKey = myProfile.publicKey,
                    greeting = greetingMessage,
                    avatarSize = avatarBytes?.size ?: 0,
                    timestamp = System.currentTimeMillis()
                ),
                avatarBytes
            ).also { if (!it) throw Exception("无法连接到对方设备") }

            saveOutgoingRequest(requestId, targetContact, greetingMessage, remark, note)
        }
    }

    // ==================== 处理申请 ====================

    override suspend fun acceptFriendRequest(
        requestId: String,
        remark: String?,
        note: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = friendRequestDao.getById(requestId) ?: throw Exception("申请不存在")
            if (request.status != FriendRequestStatus.Pending) throw Exception("申请已处理")

            transmitter.sendMessage(
                request.userId,
                FriendProtocol.FriendResponse(
                    requestId = requestId,
                    result = FriendRequestResult.Accepted,
                    timestamp = System.currentTimeMillis()
                )
            ).also { if (!it) throw Exception("无法连接到对方设备") }

            addContactFromRequest(request, remark, note)
            friendRequestDao.update(requestId) { it.copy(status = FriendRequestStatus.Accepted) }
            scheduleProfileSend(request.userId)
        }
    }

    override suspend fun handleIncomingRequest(request: IncomingFriendRequest) =
        withContext(Dispatchers.IO) {
            runCatching {
                if (contactRepository.exists(request.peerUserId)) {
                    handleAlreadyFriend(
                        request.peerUserId, request.peerNickname,
                        request.avatarData, request.requestId
                    )
                } else {
                    saveIncomingRequest(
                        request.requestId, request.peerUserId, request.peerNickname,
                        request.peerPublicKey, request.greetingMessage, request.avatarData
                    )
                }
            }.onFailure { Log.e(TAG, "处理申请失败", it) }
        }

    override suspend fun handleRequestResponse(response: FriendRequestResponse) =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = friendRequestDao.getById(response.requestId) ?: return@runCatching
                if (response.accepted) handleAccepted(request) else handleRejected(response.requestId)
            }.onFailure { Log.e(TAG, "处理响应失败", it) }
        }

    override suspend fun handleAutoAddResponse(response: FriendProfileResponse) =
        withContext(Dispatchers.IO) {
            runCatching {
                if (contactRepository.exists(response.userId)) return@runCatching

                val originalRequest = friendRequestDao.getByPeerId(
                    peerId = response.userId,
                    isFromMe = true
                )
                val avatarPath = response.avatarData?.let {
                    imageExt.saveAvatarBytes(response.userId, it, isThumbnail = true)
                }

                contactRepository.createContact(
                    Contact(
                        id = response.userId,
                        nickname = response.nickname,
                        avatarPath = avatarPath,
                        signature = response.signature,
                        gender = response.gender,
                        remarkName = originalRequest?.remark,
                        note = originalRequest?.note,
                        publicKey = response.publicKey
                    )
                )

                originalRequest?.let { friendRequestDao.deleteById(it.id) }

                Unit
            }.onFailure { Log.e(TAG, "处理自动添加失败", it) }
        }

    override suspend fun handleFullProfileResponse(response: FriendProfileResponse) =
        withContext(Dispatchers.IO) {
            val avatarPath = response.avatarData?.let {
                imageExt.saveAvatarBytes(response.userId, it, isThumbnail = false)
            }
            contactRepository.updateContact(response.userId) { contact ->
                contact.copy(
                    avatarPath = avatarPath ?: contact.avatarPath,
                    signature = response.signature,
                    gender = response.gender
                )
            }
        }

    // ==================== 私有逻辑 ====================

    private suspend fun handleAlreadyFriend(
        peerUserId: String,
        peerNickname: String,
        avatarData: ByteArray?,
        requestId: String
    ) {
        sendAutoAddResponse(peerUserId, requestId)
        updateContactInfo(peerUserId, peerNickname, avatarData)
    }

    private suspend fun handleAccepted(request: FriendRequestEntity) {
        database.withTransaction {
            addContactFromRequest(request)
            friendRequestDao.update(request.id) { it.copy(status = FriendRequestStatus.Accepted) }
        }
        scheduleProfileSend(request.userId)
    }

    private suspend fun handleRejected(requestId: String) {
        friendRequestDao.update(requestId) { it.copy(status = FriendRequestStatus.Rejected) }
    }

    /**
     * 统一的"携带我的资料发送消息"模板，消除 sendAutoAddResponse / sendFullProfile 重复
     */
    private suspend fun sendMyProfile(
        targetUserId: String,
        logTag: String,
        isThumbnail: Boolean,
        buildMessage: (profile: UserProfile, avatarBytes: ByteArray?) -> FriendProtocol
    ) {
        runCatching {
            val myProfile = requireProfile()
            val avatarBytes = myProfile.avatarPath?.let {
                if (isThumbnail) imageExt.generateThumbnailBytes(it)
                else imageExt.generateFullAvatarBytes(it)
            }
            transmitter.sendMessage(targetUserId, buildMessage(myProfile, avatarBytes), avatarBytes)
        }.onFailure { Log.e(TAG, "$logTag 发送失败: ${it.message}", it) }
    }

    private suspend fun sendAutoAddResponse(targetUserId: String, requestId: String) =
        sendMyProfile(targetUserId, "AutoAddResponse", isThumbnail = true) { profile, avatarBytes ->
            FriendProtocol.ProfileResponse(
                requestId = requestId,
                userId = profile.id,
                nickname = profile.nickname,
                signature = profile.signature,
                gender = profile.gender,
                avatarSize = avatarBytes?.size ?: 0,
                publicKey = profile.publicKey,
                timestamp = System.currentTimeMillis()
            )
        }

    private suspend fun sendFullProfile(targetUserId: String) =
        sendMyProfile(
            targetUserId,
            "FullProfileResponse",
            isThumbnail = false
        ) { profile, avatarBytes ->
            FriendProtocol.ProfileResponse(
                requestId = randomUUID(),
                userId = profile.id,
                nickname = profile.nickname,
                signature = profile.signature,
                gender = profile.gender,
                avatarSize = avatarBytes?.size ?: 0,
                publicKey = profile.publicKey,
                timestamp = System.currentTimeMillis()
            )
        }

    private fun scheduleProfileSend(targetUserId: String) {
        scope.launch {
            delay(PROFILE_SEND_DELAY)
            sendFullProfile(targetUserId)
        }
    }

    private suspend fun updateContactInfo(
        userId: String,
        nickname: String,
        avatarBytes: ByteArray?
    ) {
        val avatarPath =
            avatarBytes?.let { imageExt.saveAvatarBytes(userId, it, isThumbnail = true) }
        contactRepository.updateContact(userId) { contact ->
            contact.copy(nickname = nickname, avatarPath = avatarPath ?: contact.avatarPath)
        }
    }

    private suspend fun saveOutgoingRequest(
        requestId: String,
        targetContact: Contact,
        greetingMessage: String,
        remark: String?,
        note: String?
    ) {
        friendRequestDao.insert(
            FriendRequestEntity(
                id = requestId,
                userId = targetContact.id,
                nickname = targetContact.nickname,
                avatarPath = targetContact.avatarPath,
                publicKey = targetContact.publicKey,
                greetingMessage = greetingMessage,
                remark = remark,
                note = note,
                status = FriendRequestStatus.Pending,
                isFromMe = true,
                isRead = true
            )
        )
    }

    private suspend fun saveIncomingRequest(
        requestId: String,
        peerUserId: String,
        peerNickname: String,
        peerPublicKey: String,
        greetingMessage: String,
        avatarData: ByteArray?
    ) {
        val avatarPath =
            avatarData?.let { imageExt.saveAvatarBytes(peerUserId, it, isThumbnail = true) }
        friendRequestDao.insert(
            FriendRequestEntity(
                id = requestId,
                userId = peerUserId,
                nickname = peerNickname,
                avatarPath = avatarPath,
                publicKey = peerPublicKey,
                greetingMessage = greetingMessage,
                status = FriendRequestStatus.Pending,
                isFromMe = false,
                isRead = false
            )
        )
    }

    private suspend fun addContactFromRequest(
        request: FriendRequestEntity,
        remark: String? = null,
        note: String? = null
    ) {
        contactRepository.createContact(
            Contact(
                id = request.userId,
                nickname = request.nickname,
                avatarPath = request.avatarPath,
                remarkName = remark ?: request.remark,
                note = note ?: request.note,
                source = ContactAddSource.QRCode,
                publicKey = request.publicKey
            )
        )
    }

    private fun requireProfile() =
        profileRepository.getProfile() ?: throw Exception("未找到个人资料")
}