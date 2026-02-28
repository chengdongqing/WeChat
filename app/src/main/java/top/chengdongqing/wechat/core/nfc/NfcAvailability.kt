package top.chengdongqing.wechat.core.nfc

import android.nfc.NfcAdapter
import android.nfc.NfcManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * NFC 可用性状态。
 *
 * 检测链：无芯片 → 未开启 → 可用。
 * HCE 能力（被扫描模式）不在这里判断：只要 NFC 已开启，HCE 硬件在
 * Android 4.4+ 的主流设备上均支持，运行时通过 setPreferredService() 保障路由即可。
 */
sealed class NfcAvailability {
    /** 设备无 NFC 芯片 */
    data object NotSupported : NfcAvailability()

    /** 硬件支持但用户未开启 NFC */
    data object Disabled : NfcAvailability()

    /** NFC 已开启，功能可用 */
    data object Enabled : NfcAvailability()
}

/**
 * 检测并订阅 NFC 可用性。
 *
 * 每次回到前台（onResume）重新检测，确保用户从系统设置开关 NFC 后立即生效。
 * NFC 权限（android.permission.NFC）是 normal permission，无需运行时申请。
 */
@Composable
fun rememberNfcAvailability(): NfcAvailability {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val nfcAdapter = remember {
        context.getSystemService(NfcManager::class.java)?.defaultAdapter
    }

    var availability by remember {
        mutableStateOf(nfcAdapter.toAvailability())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                availability = nfcAdapter.toAvailability()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return availability
}

private fun NfcAdapter?.toAvailability(): NfcAvailability = when {
    this == null -> NfcAvailability.NotSupported
    !isEnabled -> NfcAvailability.Disabled
    else -> NfcAvailability.Enabled
}