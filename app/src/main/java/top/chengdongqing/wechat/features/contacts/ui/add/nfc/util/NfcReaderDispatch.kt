package top.chengdongqing.wechat.features.contacts.ui.add.nfc.util

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
import top.chengdongqing.wechat.core.util.toHexByte

private const val TAG = "NfcReader"

/**
 * 根据 [isReaderMode] 动态启停 NFC 读卡器模式。
 *
 * - 读卡器模式（isReaderMode = true）：主动扫描对方手机的 HCE 服务，读取 userId。
 * - 被扫描模式（isReaderMode = false）：关闭读卡器，让路给本机 HCE，等待对方扫描。
 *
 * 每次 [isReaderMode] 变更时，DisposableEffect 会先调用 onDispose 关闭当前读卡器，
 * 再重新执行 effect 块以开启或保持关闭，避免两台手机同时作为读卡器时的冲突。
 *
 * @param isReaderMode 是否启用读卡器模式
 * @param onUserIdRead 成功读取到对方 userId 时的回调（主线程）
 */
@Composable
fun NfcReaderDispatch(
    isReaderMode: Boolean,
    onUserIdRead: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    // Handler 用于将 NFC 回调（子线程）切换到主线程
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(isReaderMode) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)

        if (isReaderMode && nfcAdapter != null) {
            nfcAdapter.enableReaderMode(
                activity,
                { tag -> handleTag(tag, mainHandler, onUserIdRead) },
                NfcAdapter.FLAG_READER_NFC_A,
                null
            )
        } else {
            // 关闭读卡器，确保本机 HCE 服务可以被对方扫描
            nfcAdapter?.disableReaderMode(activity)
        }

        onDispose {
            nfcAdapter?.disableReaderMode(activity)
        }
    }
}

/**
 * 在 NFC 回调线程中处理 [tag]，成功读取后在主线程回调。
 */
private fun handleTag(
    tag: Tag,
    mainHandler: Handler,
    onUserIdRead: (String) -> Unit
) {
    val userId = readUserIdFromTag(tag)
    if (userId != null) {
        mainHandler.post { onUserIdRead(userId) }
    }
}

/**
 * 通过 ISO-DEP（ISO 7816-4 APDU）协议读取对方 HCE 服务中存储的 userId。
 *
 * APDU SELECT 指令格式：
 *   CLA  INS  P1  P2  Lc  AID...
 *   00   A4   04  00  len  <AID bytes>
 *
 * 响应格式（HCE 端写入）：
 *   [len: 1 byte] [userId: len bytes] [SW1=90 SW2=00]
 *
 * @return 读取到的 userId，失败返回 null
 */
private fun readUserIdFromTag(tag: Tag): String? {
    val isoDep = IsoDep.get(tag) ?: run {
        Log.e(TAG, "Tag 不支持 IsoDep")
        return null
    }

    return try {
        isoDep.connect()
        isoDep.timeout = 5_000 // ms，避免长时间阻塞

        // 构造 SELECT AID 指令
        val apdu = buildSelectApdu(NfcHceService.Companion.AID)
        val response = isoDep.transceive(apdu)
        Log.d(TAG, "响应原始数据: ${response.toHexString()}")

        parseUserIdFromResponse(response)
    } catch (e: Exception) {
        Log.e(TAG, "APDU 通信异常: ${e.message}")
        null
    } finally {
        runCatching { isoDep.close() }
    }
}

/**
 * 构造 SELECT AID APDU 指令。
 *
 * 格式：CLA(00) INS(A4) P1(04) P2(00) Lc(aid.size) AID
 */
private fun buildSelectApdu(aid: ByteArray): ByteArray =
    byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid

/**
 * 从 HCE 响应中解析 userId。
 *
 * 期望响应：[len(1)] [userId bytes] [0x90 0x00]
 * 最小长度：3 字节（len + SW1 + SW2）
 */
private fun parseUserIdFromResponse(response: ByteArray): String? {
    if (response.size < 3) {
        Log.e(TAG, "响应长度不足: ${response.size}")
        return null
    }

    val sw1 = response[response.size - 2]
    val sw2 = response[response.lastIndex]
    if (sw1 != 0x90.toByte() || sw2 != 0x00.toByte()) {
        Log.e(TAG, "响应状态码异常: ${sw1.toHexByte()} ${sw2.toHexByte()}")
        return null
    }

    val len = response[0].toInt() and 0xFF
    val userId = String(response, 1, len, Charsets.UTF_8)
    return userId.ifBlank { null }
}