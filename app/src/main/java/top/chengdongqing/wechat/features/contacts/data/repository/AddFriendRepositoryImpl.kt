package top.chengdongqing.wechat.features.contacts.data.repository

import android.util.Base64
import android.util.Log
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.util.toMD5Hex
import top.chengdongqing.wechat.data.network.discovery.BLEDiscovery
import top.chengdongqing.wechat.data.network.model.DiscoveryBeacon
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.repository.AddFriendRepository
import top.chengdongqing.wechat.features.me.data.model.ProfileBeacon
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddFriendRepositoryImpl @Inject constructor(
    private val bleDiscovery: BLEDiscovery,
    private val profileRepository: ProfileRepository,
    private val privateFileManager: PrivateFileManager
) : AddFriendRepository {

    private companion object {
        const val TAG = "AddFriendRepository"
    }

    private val contactCache = LruCache<String, Contact>(10)

    override suspend fun handleScannedQRCode(qrContent: String): Result<Contact> =
        withContext(Dispatchers.IO) {
            runCatching {
                val beacon = DiscoveryBeacon.fromByteArray(Base64.decode(qrContent, Base64.NO_WRAP))
                if (!beacon.isValid()) {
                    throw Exception("二维码已过期")
                }

                val myUserId = profileRepository.requireProfile().id
                if (beacon.userId == myUserId.toMD5Hex()) {
                    throw Exception("不能添加自己为好友")
                }

                val gatt = bleDiscovery.scanAndConnect(beacon.userId)
                    ?: throw Exception("未找到对方设备")
                val (profileTransfer, avatarBytes) = bleDiscovery.readProfile(gatt) ?: let {
                    bleDiscovery.close()
                    throw Exception("获取资料失败")
                }
                bleDiscovery.close()

                parseProfile(profileTransfer, avatarBytes).also {
                    contactCache.put(it.id, it)
                }
            }
        }

    override suspend fun generateMyQRCode(): String = withContext(Dispatchers.IO) {
        val userId = profileRepository.requireProfile().id
        val beaconBytes = DiscoveryBeacon.toByteArray(DiscoveryBeacon.create(userId))
        Base64.encodeToString(beaconBytes, Base64.NO_WRAP)
    }

    override fun getContactFromCache(contactId: String): Contact? = contactCache[contactId]

    override fun setContactToCache(contactId: String, contact: Contact) {
        contactCache.put(contactId, contact)
    }

    private suspend fun parseProfile(profile: ProfileBeacon, avatarBytes: ByteArray?): Contact {
        val avatarPath = avatarBytes?.let {
            privateFileManager.saveAvatar(
                userId = profile.userId,
                sourceBytes = it
            ).getOrNull()
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

    override suspend fun fetchProfile(userId: String): Contact? =
        withContext(Dispatchers.IO) {
            runCatching {
                val md5 = userId.toMD5Hex()

                val gatt = bleDiscovery.scanAndConnect(md5)
                    ?: throw Exception("scanAndConnect 返回 null")

                val (transfer, avatarBytes) = bleDiscovery.readProfile(gatt)
                    ?: throw Exception("readProfile 返回 null")

                parseProfile(transfer, avatarBytes).also { contact ->
                    contactCache.put(contact.id, contact)
                }
            }.onFailure { e ->
                Log.w(TAG, "获取对方的个人资料失败：userId=$userId, message=${e.message}")
            }.also {
                bleDiscovery.close()
            }.getOrNull()
        }
}