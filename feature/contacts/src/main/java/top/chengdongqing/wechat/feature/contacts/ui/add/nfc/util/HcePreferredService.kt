package top.chengdongqing.wechat.feature.contacts.ui.add.nfc.util

import android.content.ComponentName
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import top.chengdongqing.wechat.core.common.nfc.NfcConstants

/**
 * 将 HCE 服务设置为前台优先 HCE 服务。
 */
@Composable
fun HcePreferredService() {
    val context = LocalContext.current
    val activity = LocalActivity.current ?: return

    DisposableEffect(Unit) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        val cardEmulation = nfcAdapter?.let { CardEmulation.getInstance(it) }
        val componentName = ComponentName(context.packageName, NfcConstants.HCE_SERVICE_CLASS)

        cardEmulation?.setPreferredService(activity, componentName)

        onDispose {
            cardEmulation?.unsetPreferredService(activity)
        }
    }
}