package top.chengdongqing.wechat.features.contacts.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.util.ImageExt
import top.chengdongqing.wechat.core.util.toMD5Hex
import top.chengdongqing.wechat.data.model.ConnectionCapabilities
import top.chengdongqing.wechat.data.model.DiscoveryBeacon
import top.chengdongqing.wechat.data.model.Gender
import top.chengdongqing.wechat.data.model.UserProfileTransfer
import top.chengdongqing.wechat.data.network.discovery.BLEDiscovery
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

class ContactP2PRepositoryImpl @Inject constructor(
    private val bleDiscovery: BLEDiscovery,
    private val profileRepository: ProfileRepository,
    private val imageExt: ImageExt,
    @param:ApplicationContext private val context: Context
) : ContactP2PRepository {

    private companion object {
        const val TAG = "ContactP2P"

        private val contactCache = mutableMapOf<String, Contact>()
    }

    override suspend fun handleScannedQRCode(qrContent: String): Result<Contact> {
        return withContext(Dispatchers.IO) {
            runCatching {
                // 1. 解析二维码
                val beacon = parseBeacon(qrContent)

                if (!beacon.isValid()) {
                    throw Exception("二维码已过期")
                }

                // 2. 验证不是自己
                val myUserId = getCurrentUserId()
                if (beacon.userId == myUserId.toMD5Hex()) {
                    throw Exception("不能添加自己为好友")
                }

                // 3. 扫描并连接设备
                val gatt = bleDiscovery.scanAndConnect(beacon.userId)
                    ?: throw Exception("未找到对方设备")

                // 4. 读取资料
                val (profileTransfer, avatarBytes) = bleDiscovery.readProfile(gatt)
                    ?: throw Exception("获取资料失败")

                // 5. 解析并保存头像
                val contact = parseProfile(profileTransfer, avatarBytes)

                // 6. 缓存联系人
                contactCache[contact.id] = contact

                // 7. 关闭连接
                bleDiscovery.close()

                Log.d(TAG, "✅ 扫码成功: ${contact.nickname}")
                contact
            }
        }
    }

    override suspend fun generateMyQRCode(): String {
        return withContext(Dispatchers.IO) {
            val profile = profileRepository.getCurrentProfileOnce()
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

    /**
     * 解析二维码
     */
    private fun parseBeacon(qrContent: String): DiscoveryBeacon {
        val bytes = Base64.decode(qrContent, Base64.NO_WRAP)
        return DiscoveryBeacon.fromByteArray(bytes)
    }

    /**
     * 解析资料并保存头像
     */
    private suspend fun parseProfile(
        profile: UserProfileTransfer,
        avatarBytes: ByteArray?
    ): Contact {
        // 保存头像
        val avatarPath = avatarBytes?.let { bytes ->
            imageExt.saveAvatarBytes(profile.userId, bytes, isThumbnail = false)
        }

        return Contact(
            id = profile.userId,
            nickname = profile.nickname,
            avatarPath = avatarPath,
            signature = profile.signature,
            gender = Gender.fromIndex(profile.gender),
        )
    }

    /**
     * 获取当前用户ID
     */
    private suspend fun getCurrentUserId(): String {
        return profileRepository.getCurrentProfileOnce()?.id
            ?: throw Exception("未找到个人资料")
    }

    /**
     * 获取设备能力
     */
    private fun getMyCapabilities(): Int {
        return ConnectionCapabilities.getDeviceCapabilities(context)
    }
}