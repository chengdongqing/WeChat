package top.chengdongqing.wechat.feature.contacts.data.repository

import android.util.Log
import androidx.room3.withWriteTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.common.file.PrivateFileManager
import top.chengdongqing.wechat.core.common.file.toBytes
import top.chengdongqing.wechat.core.data.model.FriendEvent
import top.chengdongqing.wechat.core.data.model.FriendProtocol
import top.chengdongqing.wechat.core.data.model.FriendRequestResult
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.data.repository.FriendRequestRepository
import top.chengdongqing.wechat.core.data.repository.PrivacySettingsRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.data.storage.AssetOwner
import top.chengdongqing.wechat.core.data.storage.AssetOwnerType
import top.chengdongqing.wechat.core.data.storage.AssetReferenceManager
import top.chengdongqing.wechat.core.database.WeDatabase
import top.chengdongqing.wechat.core.database.dao.FriendRequestDao
import top.chengdongqing.wechat.core.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.model.ContactAddSource
import top.chengdongqing.wechat.core.model.FriendRequest
import top.chengdongqing.wechat.core.model.FriendRequestStatus
import top.chengdongqing.wechat.core.network.ble.BLEConnectionManager
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.core.util.toSHA256Hex
import top.chengdongqing.wechat.feature.contacts.data.mapper.toDomain
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class FriendRequestRepositoryImpl @Inject constructor(
    private val database: WeDatabase,
    private val friendRequestDao: FriendRequestDao,
    private val contactRepository: ContactRepository,
    private val profileRepository: ProfileRepository,
    private val privateFileManager: PrivateFileManager,
    private val bleConnectionManager: BLEConnectionManager,
    private val assetReferenceManager: AssetReferenceManager,
    private val privacySettingsRepository: PrivacySettingsRepository
) : FriendRequestRepository {

    private companion object {
        const val TAG = "FriendRequestRepository"
    }

    private val _friendEvents = MutableSharedFlow<FriendEvent>()
    override val friendEvents: Flow<FriendEvent> = _friendEvents.asSharedFlow()

    override fun observeAllRequests(): Flow<List<FriendRequest>> =
        friendRequestDao.observeAll().map { it.toDomain() }

    override fun getPendingCount(): Flow<Int> = friendRequestDao.getPendingCount()

    override fun observeUnreadCount(): Flow<Int> = friendRequestDao.observeUnreadCount()

    override suspend fun markAllIncomingAsRead() = friendRequestDao.markAllIncomingAsRead()

    override suspend fun checkAndMarkExpired(expireDays: Int) {
        val expireThreshold = System.currentTimeMillis() - expireDays.days.inWholeMilliseconds

        friendRequestDao.markExpired(
            beforeTime = expireThreshold,
            expiredStatus = FriendRequestStatus.Expired,
            pendingStatus = FriendRequestStatus.Pending
        )
    }

    override suspend fun deleteRequest(requestId: String) {
        // 查询请求详情
        val request = friendRequestDao.getById(requestId) ?: return

        // 删除请求
        friendRequestDao.deleteById(requestId)

        // 删除头像文件
        assetReferenceManager.detach(AssetOwner(AssetOwnerType.FriendRequest, request.id))
    }

    override suspend fun sendFriendRequest(
        targetContact: Contact,
        greeting: String,
        remark: String?,
        note: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 获取我的个人资料
            val myProfile = profileRepository.requireProfile()
            val avatarBytes = myProfile.avatarPath?.let {
                File(it).toBytes()
            }

            // 生成请求ID
            val requestId = randomUUID()

            // 保存申请记录
            saveOutgoingRequest(
                requestId = requestId,
                targetContact = targetContact,
                greeting = greeting,
                remark = remark,
                note = note,
                source = targetContact.source
            )

            // 发送请求
            bleConnectionManager.sendMessage(
                targetUserId = targetContact.id,
                message = FriendProtocol.FriendRequest(
                    requestId = requestId,
                    userId = myProfile.id,
                    nickname = myProfile.nickname,
                    publicKey = myProfile.publicKey,
                    greeting = greeting,
                    source = targetContact.source,
                    timestamp = System.currentTimeMillis()
                ),
                avatarBytes
            ).onFailure {
                // 失败后删除申请记录
                friendRequestDao.deleteById(requestId)

                throw Exception("无法连接到对方设备")
            }

            Unit
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

            // 发送接受申请的响应
            sendAcceptedResponse(request.userId, requestId)

            // 添加对方到通讯录
            addContactFromRequest(request, remark, note)

            // 更新请求状态
            friendRequestDao.update(requestId) {
                it.copy(status = FriendRequestStatus.Accepted)
            }

            // 推送加好友成功通知
            _friendEvents.emit(FriendEvent.Added(request.nickname, request.userId))
        }
    }

    override suspend fun handleIncomingRequest(
        request: FriendProtocol.FriendRequest,
        avatarData: ByteArray?
    ) = withContext(Dispatchers.IO) {
        runCatching {
            // 如果在通讯录存在，则直接同意（对方删了我，但我没有删除对方，对方再重新加我的情况）
            if (contactRepository.exists(request.userId)) {
                sendAcceptedResponse(request.userId, request.requestId)
            } else {
                // 保存申请记录
                saveIncomingRequest(
                    requestId = request.requestId,
                    userId = request.userId,
                    nickname = request.nickname,
                    publicKey = request.publicKey,
                    greeting = request.greeting,
                    avatarData = avatarData,
                    source = request.source
                )

                // 不需要验证时直接通过
                if (!friendVerifyEnabled()) {
                    delay(1000)
                    acceptFriendRequest(request.requestId)
                }
            }
        }.onFailure {
            Log.e(TAG, "处理申请失败", it)
        }
    }

    /**
     * 发送接受申请响应
     */
    private suspend fun sendAcceptedResponse(userId: String, requestId: String) {
        bleConnectionManager.sendMessage(
            targetUserId = userId,
            message = FriendProtocol.FriendResponse(
                requestId = requestId,
                result = FriendRequestResult.Accepted,
                timestamp = System.currentTimeMillis()
            )
        ).onFailure {
            throw Exception("无法连接到对方设备")
        }
    }

    override suspend fun handleRequestResponse(response: FriendProtocol.FriendResponse) =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = friendRequestDao.getById(response.requestId)
                    ?: throw Exception("未查询到对应的好友请求，requestId=${response.requestId}")

                if (response.result == FriendRequestResult.Accepted) {
                    handleAccepted(request)
                } else {
                    handleRejected(response.requestId)
                }
            }.onFailure {
                Log.e(TAG, "处理响应失败", it)
            }
        }

    private suspend fun handleAccepted(request: FriendRequestEntity) {
        database.withWriteTransaction {
            addContactFromRequest(request)

            friendRequestDao.update(request.id) {
                it.copy(status = FriendRequestStatus.Accepted)
            }

            // 推送加好友成功通知
            _friendEvents.emit(FriendEvent.Added(request.nickname, request.userId))
        }
    }

    private suspend fun handleRejected(requestId: String) {
        friendRequestDao.update(requestId) {
            it.copy(status = FriendRequestStatus.Rejected)
        }
    }

    private suspend fun saveOutgoingRequest(
        requestId: String,
        targetContact: Contact,
        greeting: String,
        remark: String?,
        note: String?,
        source: ContactAddSource?
    ) {
        // 注册文件引用
        targetContact.avatarPath?.let {
            assetReferenceManager.attach(
                localPath = it,
                checksum = File(it).toSHA256Hex(),
                owner = AssetOwner(AssetOwnerType.FriendRequest, requestId)
            )
        }
        friendRequestDao.insert(
            FriendRequestEntity(
                id = requestId,
                userId = targetContact.id,
                nickname = targetContact.nickname,
                avatarPath = targetContact.avatarPath,
                publicKey = targetContact.publicKey,
                greeting = greeting,
                remark = remark,
                note = note,
                status = FriendRequestStatus.Pending,
                isFromMe = true,
                isRead = true,
                source = source
            )
        )
    }

    private suspend fun saveIncomingRequest(
        requestId: String,
        userId: String,
        nickname: String,
        publicKey: String,
        greeting: String,
        avatarData: ByteArray?,
        source: ContactAddSource?
    ) {
        // 保存头像
        val avatarPath = avatarData?.let {
            privateFileManager.saveAvatar(
                userId = userId,
                sourceBytes = it
            ).getOrNull()
        }
        // 注册文件引用
        avatarPath?.let {
            assetReferenceManager.attach(
                localPath = it,
                checksum = File(it).toSHA256Hex(),
                owner = AssetOwner(AssetOwnerType.FriendRequest, requestId)
            )
        }

        // 保存请求记录
        friendRequestDao.insert(
            FriendRequestEntity(
                id = requestId,
                userId = userId,
                nickname = nickname,
                avatarPath = avatarPath,
                publicKey = publicKey,
                greeting = greeting,
                status = FriendRequestStatus.Pending,
                isFromMe = false,
                isRead = false,
                source = source
            )
        )
    }

    /**
     * 根据请求记录创建联系人
     */
    private suspend fun addContactFromRequest(
        request: FriendRequestEntity,
        remark: String? = null,
        note: String? = null
    ) {
        // 注册文件引用
        request.avatarPath?.let {
            assetReferenceManager.attach(
                localPath = it,
                checksum = File(it).toSHA256Hex(),
                owner = AssetOwner(AssetOwnerType.Contact, request.userId)
            )
        }

        contactRepository.createContact(
            Contact(
                id = request.userId,
                nickname = request.nickname,
                avatarPath = request.avatarPath,
                remarkName = remark ?: request.remark,
                note = note ?: request.note,
                source = request.source,
                isFromMe = request.isFromMe,
                publicKey = request.publicKey
            )
        )
    }

    /**
     * 是否开启好友验证
     */
    private suspend fun friendVerifyEnabled(): Boolean =
        privacySettingsRepository.friendVerifyEnabled.first()
}
