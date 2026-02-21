package top.chengdongqing.wechat.core.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * NFC HCE 服务（全链路日志版本）
 */
@AndroidEntryPoint
class NfcHceService : HostApduService() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    companion object {
        val AID = byteArrayOf(
            0xF0.toByte(), 0x57, 0x65, 0x43,
            0x68, 0x61, 0x74, 0x4E, 0x46, 0x43
        )
        private val SELECT_APDU_HEADER = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)
        private val SUCCESS_SW = byteArrayOf(0x90.toByte(), 0x00)
        private val FAILURE_SW = byteArrayOf(0x6F, 0x00)
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        Log.d("NfcHce", "📥 收到 APDU: ${commandApdu.toHexString()}")

        if (!isSelectAidApdu(commandApdu)) {
            Log.w("NfcHce", "⚠️ 非 SELECT AID，忽略")
            return byteArrayOf(0x00, 0x00)
        }

        val userId = getUserIdBlocking()
        Log.d("NfcHce", "📋 userId: $userId")

        if (userId == null) {
            Log.e("NfcHce", "❌ userId 为 null")
            return FAILURE_SW
        }

        val bytes = userId.toByteArray(Charsets.UTF_8)
        val response = byteArrayOf(bytes.size.toByte()) + bytes + SUCCESS_SW
        Log.d("NfcHce", "📤 返回: ${response.toHexString()}")
        return response
    }

    override fun onDeactivated(reason: Int) {
        Log.d("NfcHce", "🔵 onDeactivated: $reason")
    }

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

    private fun getUserIdBlocking(): String? = try {
        runBlocking {
            profileRepository.getCurrentProfile().firstOrNull()?.id
        }
    } catch (e: Exception) {
        Log.e("NfcHce", "❌ 获取 userId 异常: ${e.message}", e)
        null
    }
}

private fun ByteArray.toHexString() = joinToString(" ") { "%02X".format(it) }