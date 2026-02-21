package top.chengdongqing.wechat.features.contacts.ui.add.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import top.chengdongqing.wechat.core.nfc.NfcHceService

private const val TAG = "NfcReader"

@Composable
fun NfcReaderDispatch(
    isReaderMode: Boolean, // 外部控制是否为读卡器
    onUserIdRead: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(isReaderMode) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)

        if (isReaderMode && nfcAdapter != null) {
            Log.d("NfcReader", "🚀 开启读卡器模式")
            nfcAdapter.enableReaderMode(
                activity,
                { tag ->
                    val userId = readUserIdFromTag(tag)
                    if (userId != null) {
                        mainHandler.post { onUserIdRead(userId) }
                    }
                },
                NfcAdapter.FLAG_READER_NFC_A,
                null
            )
        } else {
            Log.d("NfcReader", "😴 关闭读卡器模式，让路给 HCE")
            nfcAdapter?.disableReaderMode(activity)
        }

        onDispose {
            nfcAdapter?.disableReaderMode(activity)
        }
    }
}

private fun readUserIdFromTag(tag: Tag): String? {
    val isoDep = IsoDep.get(tag) ?: run {
        Log.e(TAG, "❌ 不是 IsoDep Tag")
        return null
    }

    return try {
        isoDep.connect()
        isoDep.timeout = 5000

        val selectApdu = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00) +
                byteArrayOf(NfcHceService.AID.size.toByte()) +
                NfcHceService.AID

        val response = isoDep.transceive(selectApdu)
        Log.d(TAG, "📥 响应: ${response.toHexString()}")

        if (response.size < 3) return null
        if (response[response.size - 2] != 0x90.toByte() ||
            response[response.size - 1] != 0x00.toByte()
        ) return null

        val len = response[0].toInt() and 0xFF
        String(response, 1, len, Charsets.UTF_8).ifBlank { null }
    } catch (e: Exception) {
        Log.e(TAG, "❌ 读取异常: ${e.message}")
        null
    } finally {
        runCatching { isoDep.close() }
    }
}