package top.chengdongqing.wechat.core.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * NFC HCE（主机卡模拟）服务
 *
 * 将本机模拟成一张 NFC 卡片，当对方设备发起 SELECT AID 指令时，
 * 返回当前用户的 userId，用于扫一扫加好友场景。
 */
@AndroidEntryPoint
class NfcHceService : HostApduService() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    private val myUserId: String
        get() = profileRepository.requireUserId()

    companion object {
        /** 应用标识符（AID），读卡方凭此定位到本应用 */
        val AID = byteArrayOf(
            0xF0.toByte(), 0x57, 0x65, 0x43,
            0x68, 0x61, 0x74, 0x4E, 0x46, 0x43
        )

        /** SELECT APDU 指令头，固定为 CLA=00 INS=A4 P1=04 P2=00 */
        private val SELECT_APDU_HEADER = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)

        /** APDU 成功状态码 */
        private val SUCCESS_SW = byteArrayOf(0x90.toByte(), 0x00)
    }

    /**
     * 处理读卡方发来的 APDU 指令。
     *
     * 仅响应 SELECT AID 指令，响应格式：[userId 长度(1字节)] + [userId UTF-8 字节] + [ 成功状态码]。
     * 非 SELECT AID 指令一律返回空响应忽略。
     */
    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (!isSelectAidApdu(commandApdu)) {
            Log.w("NfcHce", "非 SELECT AID，忽略")
            return byteArrayOf(0x00, 0x00)
        }

        val bytes = myUserId.toByteArray(Charsets.UTF_8)
        return byteArrayOf(bytes.size.toByte()) + bytes + SUCCESS_SW
    }

    /**
     * NFC 连接断开时回调，[reason] 取值见 [HostApduService] 常量。
     */
    override fun onDeactivated(reason: Int) {
        Log.d("NfcHce", "onDeactivated: $reason")
    }

    /**
     * 判断 [apdu] 是否为合法的 SELECT AID 指令。
     *
     * 校验顺序：指令头匹配 → AID 长度匹配 → AID 内容匹配。
     */
    private fun isSelectAidApdu(apdu: ByteArray): Boolean {
        if (apdu.size < SELECT_APDU_HEADER.size + 1) return false
        for (i in SELECT_APDU_HEADER.indices) {
            if (apdu[i] != SELECT_APDU_HEADER[i]) return false
        }
        val aidLen = apdu[SELECT_APDU_HEADER.size].toInt() and 0xFF
        if (aidLen != AID.size) return false
        val apduAid = apdu.copyOfRange(
            SELECT_APDU_HEADER.size + 1,
            SELECT_APDU_HEADER.size + 1 + aidLen
        )
        return apduAid.contentEquals(AID)
    }
}