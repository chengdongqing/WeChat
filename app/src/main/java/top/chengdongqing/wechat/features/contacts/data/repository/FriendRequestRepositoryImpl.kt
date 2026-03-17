package top.chengdongqing.wechat.features.contacts.data.repository

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.core.util.toBytes
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.FriendRequestDao
import top.chengdongqing.wechat.data.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.data.model.ContactAddSource
import top.chengdongqing.wechat.data.model.FriendRequestStatus
import top.chengdongqing.wechat.data.network.ble.BLEConnectionManager
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
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import java.io.File
import javax.inject.Inject

class FriendRequestRepositoryImpl @Inject constructor(
    private val database: WeDatabase,
    private val friendRequestDao: FriendRequestDao,
    private val contactRepository: ContactRepository,
    private val profileRepository: ProfileRepository,
    private val privateFileManager: PrivateFileManager,
    private val bleConnectionManager: BLEConnectionManager
) : FriendRequestRepository {

    private companion object {
        const val TAG = "FriendRequestRepository"
    }

    override fun observeAllRequests(): Flow<List<FriendRequest>> =
        friendRequestDao.observeAll().map { it.toDomain() }

    override fun getPendingCount(): Flow<Int> = friendRequestDao.getPendingCount()

    override fun observeUnreadCount(): Flow<Int> = friendRequestDao.observeUnreadCount()

    override suspend fun markAllIncomingAsRead() = friendRequestDao.markAllIncomingAsRead()

    override suspend fun deleteRequest(requestId: String) = friendRequestDao.deleteById(requestId)

    override suspend fun sendFriendRequest(
        targetContact: Contact,
        greetingMessage: String,
        remark: String?,
        note: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val myProfile = profileRepository.requireProfile()
            val avatarBytes = myProfile.avatarPath?.let {
                File(it).toBytes()
            }
            val requestId = randomUUID()

            bleConnectionManager.sendMessage(
                targetUserId = targetContact.id,
                message = FriendProtocol.FriendRequest(
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

    override suspend fun acceptFriendRequest(
        requestId: String,
        remark: String?,
        note: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = friendRequestDao.getById(requestId) ?: throw Exception("申请不存在")
            if (request.status != FriendRequestStatus.Pending) throw Exception("申请已处理")

            bleConnectionManager.sendMessage(
                targetUserId = request.userId,
                message = FriendProtocol.FriendResponse(
                    requestId = requestId,
                    result = FriendRequestResult.Accepted,
                    timestamp = System.currentTimeMillis()
                )
            ).also { if (!it) throw Exception("无法连接到对方设备") }

            addContactFromRequest(request, remark, note)
            friendRequestDao.update(requestId) { it.copy(status = FriendRequestStatus.Accepted) }
        }
    }

    override suspend fun handleIncomingRequest(request: IncomingFriendRequest) =
        withContext(Dispatchers.IO) {
            runCatching {
                if (contactRepository.exists(request.peerUserId)) {
                    handleAlreadyFriend(
                        request.peerUserId, request.peerNickname,
                        request.avatarData
                    )
                } else {
                    saveIncomingRequest(
                        request.requestId, request.peerUserId, request.peerNickname,
                        request.peerPublicKey, request.greetingMessage, request.avatarData
                    )
                }
            }.onFailure {
                Log.e(TAG, "处理申请失败", it)
            }
        }

    override suspend fun handleRequestResponse(response: FriendRequestResponse) =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = friendRequestDao.getById(response.requestId)
                    ?: throw Exception("未查询到对应的好友请求，requestId=${response.requestId}")

                if (response.accepted) {
                    handleAccepted(request)
                } else {
                    handleRejected(response.requestId)
                }
            }.onFailure {
                Log.e(TAG, "处理响应失败", it)
            }
        }

    override suspend fun handleAutoAdd(response: FriendProfileResponse) =
        withContext(Dispatchers.IO) {
            runCatching {
                if (contactRepository.exists(response.userId)) {
                    throw Exception("联系人已存在, userId=${response.userId}")
                }

                val originalRequest = friendRequestDao.getByPeerId(
                    peerId = response.userId,
                    isFromMe = true
                )
                val avatarPath = response.avatarData?.let {
                    privateFileManager.saveAvatar(
                        userId = response.userId,
                        sourceBytes = it
                    ).getOrNull()
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

                originalRequest?.let {
                    friendRequestDao.deleteById(it.id)
                }

                Unit
            }.onFailure {
                Log.e(TAG, "处理自动添加失败", it)
            }
        }

    private suspend fun handleAlreadyFriend(
        peerUserId: String,
        peerNickname: String,
        avatarData: ByteArray?
    ) {
        updateContactInfo(peerUserId, peerNickname, avatarData)
    }

    private suspend fun handleAccepted(request: FriendRequestEntity) {
        database.withTransaction {
            addContactFromRequest(request)
            friendRequestDao.update(request.id) { it.copy(status = FriendRequestStatus.Accepted) }
        }
    }

    private suspend fun handleRejected(requestId: String) {
        friendRequestDao.update(requestId) { it.copy(status = FriendRequestStatus.Rejected) }
    }

    private suspend fun updateContactInfo(
        userId: String,
        nickname: String,
        avatarBytes: ByteArray?
    ) {
        val avatarPath = avatarBytes?.let {
            privateFileManager.saveAvatar(
                userId = userId,
                sourceBytes = it
            ).getOrNull()
        }

        contactRepository.updateContact(userId) { contact ->
            contact.copy(
                nickname = nickname,
                avatarPath = avatarPath ?: contact.avatarPath
            )
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
        val avatarPath = avatarData?.let {
            privateFileManager.saveAvatar(
                userId = peerUserId,
                sourceBytes = it
            ).getOrNull()
        }

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
}