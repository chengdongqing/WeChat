package top.chengdongqing.wechat.core.nfc

import android.content.ComponentName
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * 将 [NfcHceService] 设置为前台优先 HCE 服务。
 *
 * 作用：在本页面活跃期间，若有读卡器扫描，系统优先路由到我们的 HCE 服务，
 * 而不是弹出 AID 冲突选择对话框。
 *
 * 生命周期：随 Composable 进入/离开 Composition 自动注册/注销。
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