package top.chengdongqing.wechat.features.contacts.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.model.ConnectionCapabilities
import top.chengdongqing.wechat.data.model.DiscoveryBeacon
import top.chengdongqing.wechat.data.model.Gender
import top.chengdongqing.wechat.data.model.UserProfileTransfer
import top.chengdongqing.wechat.data.network.connection.Connection
import top.chengdongqing.wechat.data.network.discovery.BLEDiscovery
import top.chengdongqing.wechat.data.network.protocol.P2PMessage
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.me.repository.ProfileRepository
import java.io.File
import javax.inject.Inject

/**
 * P2P联系人仓库
 */
class ContactP2PRepository @Inject constructor(
    private val bleDiscovery: BLEDiscovery,
    private val profileRepository: ProfileRepository,
    @param:ApplicationContext private val context: Context
) {

    companion object {
        private val contactCache = mutableMapOf<String, Contact>()

        fun getContactFromCache(contactId: String): Contact? {
            return contactCache[contactId]
        }
    }

    /**
     * 处理扫描到的二维码
     */
    suspend fun handleScannedQRCode(qrContent: String): Result<Contact> {
        return withContext(Dispatchers.IO) {
            try {
                val beacon = parseBeacon(qrContent)

                if (!beacon.isValid()) {
                    return@withContext Result.failure(Exception("二维码已过期"))
                }

                val myUserId = getCurrentUserId()
                val myUserIdHash = myUserId.toMD5Hex()

                if (beacon.userId == myUserIdHash) {
                    return@withContext Result.failure(Exception("不能添加自己为好友"))
                }

                // 使用BLE扫描并连接
                val gatt = bleDiscovery.scanAndConnect(beacon.userId)
                    ?: return@withContext Result.failure(Exception("未找到对方设备"))

                // 接收 JSON + 头像二进制
                val (profileTransfer, avatarBytes) = bleDiscovery.readProfile(gatt)
                    ?: return@withContext Result.failure(Exception("获取资料失败"))

                // 解析
                val contact = parseProfileWithAvatar(profileTransfer, avatarBytes)

                // 保存到本地缓存
                saveContactToCache(contact)

                bleDiscovery.close()

                Result.success(contact)
            } catch (_: Exception) {
                Result.failure(Exception("不支持该二维码"))
            }
        }
    }

    /**
     * 保存到本地缓存
     */
    private fun saveContactToCache(contact: Contact) {
        contactCache[contact.id] = contact
    }

    /**
     * 解析资料（包含头像二进制）
     */
    private fun parseProfileWithAvatar(
        profile: UserProfileTransfer,
        avatarBytes: ByteArray?
    ): Contact {
        try {
            // 保存头像二进制到本地
            val avatarPath = if (avatarBytes != null) {
                saveAvatarToLocal(profile.userId, avatarBytes)
            } else {
                null
            }

            return Contact(
                id = profile.userId,
                name = profile.nickname,
                avatarPath = avatarPath,
                signature = profile.signature,
                gender = Gender.fromIndex(profile.gender)
            )
        } catch (e: Exception) {
            throw Exception("解析用户资料失败: ${e.message}")
        }
    }

    /**
     * 保存头像二进制到本地
     */
    private fun saveAvatarToLocal(userId: String, bytes: ByteArray): String? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            val file = File(context.cacheDir, "avatar_$userId.jpg")
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }

            bitmap.recycle()

            Log.d("ContactP2P", "头像已保存: ${file.absolutePath}")

            file.absolutePath
        } catch (e: Exception) {
            Log.e("ContactP2P", "保存头像失败", e)
            null
        }
    }

    /**
     * 发送好友申请
     */
    suspend fun sendFriendRequest(
        connection: Connection,
        targetUserId: String,
        verificationMessage: String
    ): Result<Unit> {
        return try {
            val myProfile = profileRepository.getCurrentProfileOnce()
                ?: return Result.failure(Exception("未找到个人资料"))

            val request = P2PMessage.FriendRequest(
                requestId = randomUUID(),
                fromUserId = myProfile.id,
                fromNickname = myProfile.nickname,
                toUserId = targetUserId,
                verificationMessage = verificationMessage,
                timestamp = System.currentTimeMillis()
            )

            connection.send(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取用户资料
     */
    private suspend fun fetchUserProfile(
        connection: Connection,
        userIdHash: String  // 这是哈希值
    ): Contact {
        val myProfile = profileRepository.getCurrentProfileOnce()
            ?: throw Exception("未找到个人资料")

        // 1. 发送请求（不需要传真实userId，对方会返回）
        val request = P2PMessage.GetProfileRequest(
            userId = "",  // 留空，表示获取连接对方的资料
            requesterId = myProfile.id,
            includeAvatar = true
        )
        connection.send(request)

        // 2. 等待响应（3秒超时）
        val response = withTimeout(3000) {
            // 监听消息流，等待GetProfileResponse
            connection.messageFlow()
                .firstOrNull { it is P2PMessage.GetProfileResponse }
                    as? P2PMessage.GetProfileResponse
                ?: throw Exception("获取用户资料超时")
        }

        // 3. 转换为Contact
        return Contact(
            id = response.userId,
            name = response.nickname
        )
    }

    /**
     * 解析二维码
     */
    private fun parseBeacon(qrContent: String): DiscoveryBeacon {
        val bytes = Base64.decode(qrContent, Base64.NO_WRAP)
        return DiscoveryBeacon.fromByteArray(bytes)
    }

    /**
     * 生成我的二维码
     */
    suspend fun generateMyQRCode(): String {
        val profile = profileRepository.getCurrentProfileOnce()
            ?: throw Exception("未找到个人资料")

        val beacon = DiscoveryBeacon.create(
            userId = profile.id,
            capabilities = getMyCapabilities()
        )

        val bytes = DiscoveryBeacon.toByteArray(beacon)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * 获取当前用户ID
     */
    private suspend fun getCurrentUserId(): String {
        return profileRepository.getCurrentProfileOnce()?.id
            ?: throw Exception("未找到个人资料")
    }

    /**
     * 获取当前设备支持的连接能力
     */
    private fun getMyCapabilities(): Int {
        return ConnectionCapabilities.getDeviceCapabilities(context)
    }
}

/**
 * 字符串转MD5十六进制
 */
private fun String.toMD5Hex(): String {
    val md = java.security.MessageDigest.getInstance("MD5")
    val digest = md.digest(this.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}