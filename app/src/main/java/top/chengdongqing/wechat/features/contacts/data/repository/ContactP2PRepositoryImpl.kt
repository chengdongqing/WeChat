package top.chengdongqing.wechat.features.contacts.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.di.DefaultScope
import top.chengdongqing.wechat.core.util.ImageExt
import top.chengdongqing.wechat.core.util.toMD5Hex
import top.chengdongqing.wechat.data.network.discovery.BLEDiscovery
import top.chengdongqing.wechat.data.network.model.ConnectionCapabilities
import top.chengdongqing.wechat.data.network.model.DiscoveryBeacon
import top.chengdongqing.wechat.data.network.model.P2PMessage
import top.chengdongqing.wechat.data.network.model.P2PMessageTransmitter
import top.chengdongqing.wechat.data.network.service.modules.BLEModule
import top.chengdongqing.wechat.data.network.service.modules.FriendRequestEvent
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.NfcContactEvent
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.data.model.UserProfileTransfer
import top.chengdongqing.wechat.features.me.domain.model.Gender
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactP2PRepositoryImpl @Inject constructor(
    private val bleDiscovery: BLEDiscovery,
    private val bleModule: BLEModule,
    private val transmitter: P2PMessageTransmitter,
    private val profileRepository: ProfileRepository,
    private val contactRepository: ContactRepository,
    private val imageExt: ImageExt,
    @param:DefaultScope private val scope: CoroutineScope,
    @param:ApplicationContext private val context: Context
) : ContactP2PRepository {

    private companion object {
        const val TAG = "ContactP2P"
        const val AVATAR_THUMBNAIL_SIZE = 100
        const val AVATAR_MAX_SIZE_KB = 5

        val contactCache = mutableMapOf<String, Contact>()
    }

    // ==================== NFC 事件流 ====================

    private val _nfcEvents = MutableSharedFlow<NfcContactEvent>(extraBufferCapacity = 16)

    /**
     * NFC 事件流：将底层 BLEModule 事件转换为上层 NfcContactEvent 向外暴露
     */
    override val nfcEvents: Flow<NfcContactEvent> = _nfcEvents.asSharedFlow()

    init {
        observeBleNfcEvents()
    }

    /**
     * 订阅底层 BLE NFC 事件，转换后推入 nfcEvents
     */
    private fun observeBleNfcEvents() {
        scope.launch {
            bleModule.friendRequestEvents.collect { event ->
                when (event) {
                    is FriendRequestEvent.NfcPeerAddRequest -> {
                        val contact = buildContact(event.message, event.avatarBytes)
                        _nfcEvents.emit(
                            NfcContactEvent.PeerRequest(
                                requestId = event.message.requestId,
                                contact = contact
                            )
                        )
                    }

                    is FriendRequestEvent.NfcPeerAddResponse -> {
                        val contact = buildContact(event.message, event.avatarBytes)
                        _nfcEvents.emit(
                            NfcContactEvent.PeerResponse(
                                requestId = event.message.requestId,
                                contact = contact
                            )
                        )
                    }

                    else -> Unit
                }
            }
        }
    }

    override suspend fun handleScannedQRCode(qrContent: String): Result<Contact> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val beacon = parseBeacon(qrContent)

                if (!beacon.isValid()) {
                    throw Exception("二维码已过期")
                }

                val myUserId = getCurrentUserId()
                if (beacon.userId == myUserId.toMD5Hex()) {
                    throw Exception("不能添加自己为好友")
                }

                val gatt = bleDiscovery.scanAndConnect(beacon.userId)
                    ?: throw Exception("未找到对方设备")

                val (profileTransfer, avatarBytes) = bleDiscovery.readProfile(gatt)
                    ?: throw Exception("获取资料失败")

                val contact = parseProfile(profileTransfer, avatarBytes)
                contactCache[contact.id] = contact
                bleDiscovery.close()

                Log.d(TAG, "✅ 扫码成功: ${contact.nickname}")
                contact
            }
        }
    }

    override suspend fun generateMyQRCode(): String {
        return withContext(Dispatchers.IO) {
            val profile = profileRepository.getProfile()
                ?: throw Exception("未找到个人资料")

            val beacon = DiscoveryBeacon.create(
                userId = profile.id,
                capabilities = getMyCapabilities()
            )

            val bytes = DiscoveryBeacon.toByteArray(beacon)
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    override fun getContactFromCache(contactId: String): Contact? {
        return contactCache[contactId]
    }

    override fun setContactToCache(contactId: String, contact: Contact) {
        contactCache[contactId] = contact
    }

    // ==================== NFC：BLE 拉取对方资料 ====================

    /**
     * NFC 碰触后，通过 BLE GATT 读取对方完整资料
     */
    override suspend fun fetchPeerContactViaBle(peerUserId: String): Contact? {
        return withContext(Dispatchers.IO) {
            try {
                val md5 = peerUserId.toMD5Hex()
                Log.d(TAG, "🔵 BLE 扫描开始，peerUserId=$peerUserId，MD5前缀=${md5.take(8)}")

                val gatt = bleDiscovery.scanAndConnect(md5)
                if (gatt == null) {
                    Log.e(TAG, "❌ scanAndConnect 返回 null，对方未开启 BLE 或扫描超时")
                    return@withContext null
                }

                val result = bleDiscovery.readProfile(gatt)
                bleDiscovery.close()

                if (result == null) {
                    Log.e(TAG, "❌ readProfile 返回 null，读取超时或协议异常")
                    return@withContext null
                }

                val (transfer, avatarBytes) = result
                Log.d(
                    TAG,
                    "✅ readProfile 成功: userId=${transfer.userId}，nickname=${transfer.nickname}"
                )

                val contact = parseProfile(transfer, avatarBytes)
                contactCache[contact.id] = contact
                contact
            } catch (e: Exception) {
                Log.e(TAG, "❌ fetchPeerContactViaBle 异常: ${e.message}", e)
                bleDiscovery.close()
                null
            }
        }
    }

    // ==================== NFC：发送消息 ====================

    /**
     * 发送 NfcAddRequest（我点击添加，通知对方）
     */
    override suspend fun sendNfcAddRequest(peerUserId: String, sessionId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val my = profileRepository.getProfile()
                    ?: return@withContext false.also {
                        Log.e(
                            TAG,
                            "❌ sendNfcAddRequest: 获取我的资料失败"
                        )
                    }

                val avatarBytes = generateAvatarThumbnail(my.avatarPath)
                val request = P2PMessage.NfcAddRequest(
                    requestId = sessionId,
                    userId = my.id,
                    nickname = my.nickname,
                    signature = my.signature,
                    gender = my.gender,
                    avatarSize = avatarBytes?.size ?: 0,
                    timestamp = System.currentTimeMillis()
                )
                transmitter.sendMessage(
                    targetUserId = peerUserId,
                    message = request,
                    binaryData = avatarBytes
                ).also {
                    Log.d(
                        TAG,
                        if (it) "✅ NfcAddRequest 发送成功" else "❌ NfcAddRequest 发送失败"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ sendNfcAddRequest 异常: ${e.message}", e)
                false
            }
        }
    }

    /**
     * 发送 NfcAddResponse（确认对方申请，携带我的完整资料）
     */
    override suspend fun sendNfcAddResponse(peerUserId: String, requestId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val my = profileRepository.getProfile()
                    ?: return@withContext false.also {
                        Log.e(
                            TAG,
                            "❌ sendNfcAddResponse: 获取我的资料失败"
                        )
                    }

                val avatarBytes = generateAvatarThumbnail(my.avatarPath)
                val response = P2PMessage.NfcAddResponse(
                    requestId = requestId,
                    userId = my.id,
                    nickname = my.nickname,
                    signature = my.signature,
                    gender = my.gender,
                    avatarSize = avatarBytes?.size ?: 0,
                    timestamp = System.currentTimeMillis()
                )
                transmitter.sendMessage(
                    targetUserId = peerUserId,
                    message = response,
                    binaryData = avatarBytes
                ).also {
                    Log.d(
                        TAG,
                        if (it) "✅ NfcAddResponse 发送成功" else "❌ NfcAddResponse 发送失败"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ sendNfcAddResponse 异常: ${e.message}", e)
                false
            }
        }
    }

    // ==================== NFC：保存联系人 ====================

    override suspend fun saveNfcContact(event: NfcContactEvent.PeerRequest): Boolean {
        return doSaveContact(event.contact)
    }

    override suspend fun saveNfcContact(event: NfcContactEvent.PeerResponse): Boolean {
        return doSaveContact(event.contact)
    }

    // ==================== 私有工具 ====================

    private suspend fun doSaveContact(contact: Contact): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                contactRepository.createContact(contact)
                Log.d(TAG, "✅ 联系人已保存: ${contact.nickname}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ 保存联系人失败: ${e.message}", e)
                false
            }
        }
    }

    /**
     * 从 NfcAddRequest 构建 Contact
     */
    private fun buildContact(message: P2PMessage.NfcAddRequest, avatarBytes: ByteArray?): Contact {
        val avatarPath = avatarBytes?.let {
            imageExt.saveAvatarBytes(message.userId, it, isThumbnail = false)
        }
        return Contact(
            id = message.userId,
            nickname = message.nickname,
            avatarPath = avatarPath,
            signature = message.signature,
            gender = message.gender
        ).also { contactCache[it.id] = it }
    }

    /**
     * 从 NfcAddResponse 构建 Contact
     */
    private fun buildContact(message: P2PMessage.NfcAddResponse, avatarBytes: ByteArray?): Contact {
        val avatarPath = avatarBytes?.let {
            imageExt.saveAvatarBytes(message.userId, it, isThumbnail = false)
        }
        return Contact(
            id = message.userId,
            nickname = message.nickname,
            avatarPath = avatarPath,
            signature = message.signature,
            gender = message.gender
        ).also { contactCache[it.id] = it }
    }

    /**
     * 解析二维码
     */
    private fun parseBeacon(qrContent: String): DiscoveryBeacon {
        val bytes = Base64.decode(qrContent, Base64.NO_WRAP)
        return DiscoveryBeacon.fromByteArray(bytes)
    }

    /**
     * 解析 BLE 传输资料并保存头像
     */
    private fun parseProfile(profile: UserProfileTransfer, avatarBytes: ByteArray?): Contact {
        val avatarPath = avatarBytes?.let {
            imageExt.saveAvatarBytes(profile.userId, it, isThumbnail = false)
        }
        return Contact(
            id = profile.userId,
            nickname = profile.nickname,
            avatarPath = avatarPath,
            signature = profile.signature,
            gender = Gender.fromIndex(profile.gender)
        )
    }

    /**
     * 生成头像缩略图
     */
    private fun generateAvatarThumbnail(avatarPath: String?): ByteArray? {
        return avatarPath?.let { path ->
            try {
                imageExt.generateThumbnailBytes(
                    path,
                    targetSize = AVATAR_THUMBNAIL_SIZE,
                    maxSizeKB = AVATAR_MAX_SIZE_KB
                )
            } catch (e: Exception) {
                Log.e(TAG, "生成头像缩略图失败", e)
                null
            }
        }
    }

    private fun getCurrentUserId(): String {
        return profileRepository.getProfile()?.id
            ?: throw Exception("未找到个人资料")
    }

    private fun getMyCapabilities(): Int {
        return ConnectionCapabilities.getDeviceCapabilities(context)
    }
}