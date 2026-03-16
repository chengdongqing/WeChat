package top.chengdongqing.wechat.features.contacts.ui.add.nfc.util

import android.content.ComponentName
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import top.chengdongqing.wechat.core.nfc.NfcHceService

/**
 * 将 [NfcHceService] 设置为前台优先 HCE 服务。
 */
@Composable
fun HcePreferredService() {
    val context = LocalContext.current
    val activity = LocalActivity.current ?: return

    DisposableEffect(Unit) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        val cardEmulation = nfcAdapter?.let { CardEmulation.getInstance(it) }
        val componentName = ComponentName(context, NfcHceService::class.java)

        cardEmulation?.setPreferredService(activity, componentName)

        onDispose {
            cardEmulation?.unsetPreferredService(activity)
        }
    }
}