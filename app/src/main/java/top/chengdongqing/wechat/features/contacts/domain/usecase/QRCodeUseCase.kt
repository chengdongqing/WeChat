package top.chengdongqing.wechat.features.contacts.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.features.contacts.data.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 二维码相关用例
 * 统一管理二维码的生成和扫描逻辑
 */
@Singleton
class QRCodeUseCase @Inject constructor(
    private val contactP2PRepository: ContactP2PRepository
) {

    /**
     * 生成我的二维码
     */
    suspend fun generateMyQRCode(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val qrCode = contactP2PRepository.generateMyQRCode()
                Result.success(qrCode)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 扫描二维码添加好友
     */
    suspend fun scanQRCodeToAddFriend(qrContent: String): Result<Contact> {
        return contactP2PRepository.handleScannedQRCode(qrContent)
    }
}