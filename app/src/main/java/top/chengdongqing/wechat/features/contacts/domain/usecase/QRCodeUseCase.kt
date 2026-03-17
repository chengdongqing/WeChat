package top.chengdongqing.wechat.features.contacts.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.qrcode.QRCodeFormat
import top.chengdongqing.wechat.core.qrcode.QRCodeResult
import top.chengdongqing.wechat.core.qrcode.QRCodeType
import top.chengdongqing.wechat.features.contacts.domain.repository.AddFriendRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 二维码相关用例
 * 统一管理二维码的生成和扫描逻辑
 */
@Singleton
class QRCodeUseCase @Inject constructor(
    private val addFriendRepository: AddFriendRepository
) {

    /**
     * 生成我的二维码
     */
    suspend fun generateMyQRCode(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val beaconBase64 = addFriendRepository.generateMyQRCode()
            QRCodeFormat.generateAddFriendQRCode(beaconBase64)
        }
    }

    /**
     * 处理扫描到的二维码
     */
    suspend fun handleScannedQRCode(qrContent: String): QRCodeResult =
        when (val type = QRCodeFormat.parseQRCode(qrContent)) {
            is QRCodeType.AddFriend -> {
                addFriendRepository.handleScannedQRCode(type.beaconBase64).fold(
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