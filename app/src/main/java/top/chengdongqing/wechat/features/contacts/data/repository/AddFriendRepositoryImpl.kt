package top.chengdongqing.wechat.features.contacts.data.repository

import android.util.Base64
import android.util.Log
import androidx.collection.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.util.ImageExt
import top.chengdongqing.wechat.core.util.toMD5Hex
import top.chengdongqing.wechat.data.network.discovery.BLEDiscovery
import top.chengdongqing.wechat.data.network.messaging.BLEMessageSender
import top.chengdongqing.wechat.data.network.model.DiscoveryBeacon
import top.chengdongqing.wechat.data.network.model.FriendEvent
import top.chengdongqing.wechat.data.network.model.FriendProtocol
import top.chengdongqing.wechat.data.network.service.addfriend.BLEAddFriendModule
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.NfcContactEvent
import top.chengdongqing.wechat.features.contacts.domain.repository.AddFriendRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.data.model.UserProfileBeacon
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddFriendRepositoryImpl @Inject constructor(
    private val bleDiscovery: BLEDiscovery,
    private val bleAddFriendModule: BLEAddFriendModule,
    private val transmitter: BLEMessageSender,
    private val profileRepository: ProfileRepository,
    private val contactRepository: ContactRepository,
    private val imageExt: ImageExt,
    @param:IoScope private val scope: CoroutineScope
) : AddFriendRepository {

    private companion object {
        const val TAG = "AddFriendRepository"
        const val AVATAR_THUMBNAIL_SIZE = 100
        const val AVATAR_MAX_SIZE_KB = 5
    }

    private val contactCache = LruCache<String, Contact>(10)

    // ==================== NFC 事件流 ====================

    private val _nfcEvents = MutableSharedFlow<NfcContactEvent>(extraBufferCapacity = 16)
    override val nfcEvents: Flow<NfcContactEvent> = _nfcEvents.asSharedFlow()

    init {
        observeBleNfcEvents()
    }

    private fun observeBleNfcEvents() {
        scope.launch {
            bleAddFriendModule.friendEvents.collect { event ->
                when (event) {
                    is FriendEvent.NfcPeerAddRequest -> {
                        val contact =
                            buildContact(event.message.userId, event.message, event.avatarBytes)
                        _nfcEvents.emit(
                            NfcContactEvent.PeerRequest(
                                event.message.requestId,
                                contact
                            )
                        )
                    }

                    is FriendEvent.NfcPeerAddResponse -> {
                        val contact =
                            buildContact(event.message.userId, event.message, event.avatarBytes)
                        _nfcEvents.emit(
                            NfcContactEvent.PeerResponse(
                                event.message.requestId,
                                contact
                            )
                        )
                    }

                    else -> Unit
                }
            }
        }
    }

    // ==================== 二维码 ====================

    override suspend fun handleScannedQRCode(qrContent: String): Result<Contact> =
        withContext(Dispatchers.IO) {
            runCatching {
                val beacon = DiscoveryBeacon.fromByteArray(Base64.decode(qrContent, Base64.NO_WRAP))

                if (!beacon.isValid()) throw Exception("二维码已过期")

                val myUserId = requireCurrentUserId()
                if (beacon.userId == myUserId.toMD5Hex()) throw Exception("不能添加自己为好友")

                val gatt = bleDiscovery.scanAndConnect(beacon.userId)
                    ?: throw Exception("未找到对方设备")

                val (profileTransfer, avatarBytes) = bleDiscovery.readProfile(gatt)
                    ?: run { bleDiscovery.close(); throw Exception("获取资料失败") }

                bleDiscovery.close()

                parseProfile(profileTransfer, avatarBytes)
                    .also {
                        contactCache.put(it.id, it)
                    }
            }
        }

    override suspend fun generateMyQRCode(): String = withContext(Dispatchers.IO) {
        val profile = profileRepository.getProfile() ?: throw Exception("未找到个人资料")
        DiscoveryBeacon.toByteArray(DiscoveryBeacon.create(profile.id))
            .let { Base64.encodeToString(it, Base64.NO_WRAP) }
    }

    // ==================== 缓存 ====================

    override fun getContactFromCache(contactId: String): Contact? = contactCache[contactId]

    override fun setContactToCache(contactId: String, contact: Contact) {
        contactCache.put(contactId, contact)
    }

    // ==================== NFC：BLE 拉取对方资料 ====================

    override suspend fun fetchPeerContactViaBle(peerUserId: String): Contact? =
        withContext(Dispatchers.IO) {
            runCatching {
                val md5 = peerUserId.toMD5Hex()

                val gatt = bleDiscovery.scanAndConnect(md5)
                    ?: throw Exception("scanAndConnect 返回 null")

                val (transfer, avatarBytes) = bleDiscovery.readProfile(gatt)
                    ?: throw Exception("readProfile 返回 null")

                parseProfile(transfer, avatarBytes).also { contact ->
                    contactCache.put(contact.id, contact)
                }
            }.onFailure { e ->
                Log.e(TAG, "fetchPeerContactViaBle 失败: ${e.message}", e)
            }.also {
                bleDiscovery.close()
            }.getOrNull()
        }

    // ==================== NFC：发送消息 ====================

    override suspend fun sendNfcAddRequest(peerUserId: String, sessionId: String): Boolean =
        sendNfcMessage(peerUserId, logTag = "NfcAddRequest") { profile, avatarBytes ->
            FriendProtocol.ProfileResponse(
                requestId = sessionId,
                userId = profile.id,
                nickname = profile.nickname,
                signature = profile.signature,
                gender = profile.gender,
                avatarSize = avatarBytes?.size ?: 0,
                publicKey = profile.publicKey,
                timestamp = System.currentTimeMillis()
            ) to avatarBytes
        }

    override suspend fun sendNfcAddResponse(peerUserId: String, requestId: String): Boolean =
        sendNfcMessage(peerUserId, logTag = "NfcAddResponse") { profile, avatarBytes ->
            FriendProtocol.ProfileResponse(
                requestId = requestId,
                userId = profile.id,
                nickname = profile.nickname,
                signature = profile.signature,
                gender = profile.gender,
                avatarSize = avatarBytes?.size ?: 0,
                publicKey = profile.publicKey,
                timestamp = System.currentTimeMillis()
            ) to avatarBytes
        }

    /**
     * NFC 消息发送：加载我的资料 → 生成头像 → 构建消息 → 发送
     */
    private suspend fun sendNfcMessage(
        peerUserId: String,
        logTag: String,
        buildMessage: (profile: UserProfile, avatarBytes: ByteArray?) -> Pair<FriendProtocol, ByteArray?>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val profile = profileRepository.getProfile() ?: return@withContext false

            val avatarBytes = generateAvatarThumbnail(profile.avatarPath)
            val (message, binary) = buildMessage(profile, avatarBytes)

            transmitter.sendMessage(peerUserId, message, binary)
        } catch (e: Exception) {
            Log.e(TAG, "$logTag 异常: ${e.message}", e)
            false
        }
    }

    // ==================== NFC：保存联系人 ====================

    override suspend fun saveNfcContact(event: NfcContactEvent.PeerRequest): Boolean =
        doSaveContact(event.contact)

    override suspend fun saveNfcContact(event: NfcContactEvent.PeerResponse): Boolean =
        doSaveContact(event.contact)

    private suspend fun doSaveContact(contact: Contact): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                contactRepository.createContact(contact)
            }.onFailure { e ->
                Log.e(TAG, "保存联系人失败: ${e.message}", e)
            }.isSuccess
        }

    // ==================== 私有工具 ====================

    private fun buildContact(
        userId: String,
        message: FriendProtocol.ProfileResponse,
        avatarBytes: ByteArray?
    ): Contact {
        val avatarPath = avatarBytes?.let {
            imageExt.saveAvatarBytes(userId, it, isThumbnail = false)
        }
        return Contact(
            id = userId,
            nickname = message.nickname,
            avatarPath = avatarPath,
            signature = message.signature,
            gender = message.gender,
            publicKey = message.publicKey
        ).also {
            contactCache.put(it.id, it)
        }
    }

    private fun parseProfile(profile: UserProfileBeacon, avatarBytes: ByteArray?): Contact {
        val avatarPath = avatarBytes?.let {
            imageExt.saveAvatarBytes(profile.userId, it, isThumbnail = false)
        }
        return Contact(
            id = profile.userId,
            nickname = profile.nickname,
            avatarPath = avatarPath,
            signature = profile.signature,
            gender = profile.gender,
            publicKey = profile.publicKey
        )
    }

    private fun generateAvatarThumbnail(avatarPath: String?): ByteArray? =
        avatarPath?.runCatching {
            imageExt.generateThumbnailBytes(
                this,
                targetSize = AVATAR_THUMBNAIL_SIZE,
                maxSizeKB = AVATAR_MAX_SIZE_KB
            )
        }?.onFailure { Log.e(TAG, "生成头像缩略图失败", it) }?.getOrNull()

    private fun requireCurrentUserId(): String =
        profileRepository.getProfile()?.id ?: throw Exception("未找到个人资料")
}