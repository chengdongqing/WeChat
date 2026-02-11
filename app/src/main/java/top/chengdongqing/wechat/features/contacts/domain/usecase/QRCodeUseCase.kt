package top.chengdongqing.wechat.features.contacts.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.data.model.QRCodeFormat
import top.chengdongqing.wechat.data.model.QRCodeType
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactP2PRepository
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
                val beaconBase64 = contactP2PRepository.generateMyQRCode()
                val qrCode = QRCodeFormat.generateAddFriendQRCode(beaconBase64)
                Result.success(qrCode)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 处理扫描到的二维码
     */
    suspend fun handleScannedQRCode(qrContent: String): QRCodeResult {
        return when (val type = QRCodeFormat.parseQRCode(qrContent)) {
            is QRCodeType.AddFriend -> {
                contactP2PRepository.handleScannedQRCode(type.beaconBase64).fold(
                    onSuccess = { contact ->
                        QRCodeResult.AddFriend(contact)
                    },
                    onFailure = { e ->
                        QRCodeResult.Error(e.message ?: "添加失败")
                    }
                )
            }

            is QRCodeType.JoinGroup -> {
                QRCodeResult.JoinGroup(type.groupId)
            }

            is QRCodeType.WebUrl -> {
                QRCodeResult.OpenUrl(type.url)
            }

            is QRCodeType.PlainText -> {
                QRCodeResult.ShowText(type.text)
            }
        }
    }
}

/**
 * 二维码处理结果
 */
sealed class QRCodeResult {
    /**
     * 添加好友成功
     */
    data class AddFriend(val contact: Contact) : QRCodeResult()

    /**
     * 加入群聊
     */
    data class JoinGroup(val groupId: String) : QRCodeResult()

    /**
     * 打开网页
     */
    data class OpenUrl(val url: String) : QRCodeResult()

    /**
     * 显示文本
     */
    data class ShowText(val text: String) : QRCodeResult()

    /**
     * 错误
     */
    data class Error(val message: String) : QRCodeResult()
}