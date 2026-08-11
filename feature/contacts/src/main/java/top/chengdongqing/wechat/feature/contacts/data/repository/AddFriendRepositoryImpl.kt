package top.chengdongqing.wechat.feature.contacts.data.repository

import android.util.Base64
import android.util.Log
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.data.repository.AddFriendRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.model.ContactAddSource
import top.chengdongqing.wechat.core.model.ProfileBeacon
import top.chengdongqing.wechat.core.network.ble.BLEConnectionManager
import top.chengdongqing.wechat.core.util.toMD5Hex
import top.chengdongqing.wechat.feature.contacts.domain.model.DiscoveryBeacon
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddFriendRepositoryImpl @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val privateFileManager: PrivateFileManager,
    private val bleConnectionManager: BLEConnectionManager
) : AddFriendRepository {

    private companion object {
        const val TAG = "AddFriendRepository"
    }

    private val contactCache = LruCache<String, Contact>(10)

    override suspend fun handleScannedQRCode(qrContent: String): Result<Contact> =
        withContext(Dispatchers.IO) {
            runCatching {
                val beacon = DiscoveryBeacon.fromByteArray(
                    Base64.decode(
                        qrContent,
                        Base64.NO_WRAP
                    )
                )
                if (!beacon.isValid()) {
                    throw Exception("二维码已过期")
                }

                val myUserId = profileRepository.requireProfile().id
                if (beacon.userIdHashHex == myUserId.toMD5Hex()) {
                    throw Exception("不能添加自己为好友")
                }

                val (profileTransfer, avatarBytes) = bleConnectionManager.readProfile(beacon.userIdHashHex)
                    .getOrThrow()

                parseProfile(profileTransfer, avatarBytes).also { contact ->
                    contactCache.put(contact.id, contact.copy(source = ContactAddSource.QRCode))
                }
            }
        }

    override suspend fun generateMyQRCode(): String = withContext(Dispatchers.IO) {
        val userId = profileRepository.requireUserId()
        val beaconBytes = DiscoveryBeacon.create(userId).toByteArray()
        Base64.encodeToString(beaconBytes, Base64.NO_WRAP)
    }

    override fun getContactFromCache(contactId: String): Contact? = contactCache[contactId]

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

    override suspend fun fetchProfile(userId: String, source: ContactAddSource?): Contact? =
        withContext(Dispatchers.IO) {
            runCatching {
                val userIdHash = userId.toMD5Hex()
                val (transfer, avatarBytes) = bleConnectionManager.readProfile(userIdHash)
                    .getOrThrow()

                parseProfile(transfer, avatarBytes).also { contact ->
                    contactCache.put(contact.id, contact.copy(source = source))
                }
            }.onFailure { e ->
                Log.w(TAG, "获取对方的个人资料失败：userId=$userId, message=${e.message}")
            }.getOrNull()
        }
}