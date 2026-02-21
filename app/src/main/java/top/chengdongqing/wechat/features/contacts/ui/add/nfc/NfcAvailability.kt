package top.chengdongqing.wechat.features.contacts.ui.add.nfc

import android.nfc.NfcManager
import android.util.Log
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

// ==================== NFC 状态枚举 ====================

enum class NfcAvailability {
    /** 硬件不支持 NFC */
    NotSupported,

    /** 硬件支持但用户未开启 */
    Disabled,

    /** 可用 */
    Enabled
}

// ==================== NFC 可用性检测 ====================

/**
 * 检测 NFC 可用性
 *
 * 注意：NFC 权限（android.permission.NFC）是 normal permission，
 * 系统安装时自动授予，无需运行时申请。
 * 此函数只需检测硬件支持 + 开关状态。
 */
@Composable
fun rememberNfcAvailability(): NfcAvailability {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val nfcManager = remember {
        context.getSystemService(NfcManager::class.java)
    }
    val nfcAdapter = remember { nfcManager?.defaultAdapter }

    var availability by remember {
        val initial = when {
            nfcAdapter == null -> NfcAvailability.NotSupported
            nfcAdapter.isEnabled -> NfcAvailability.Enabled
            else -> NfcAvailability.Disabled
        }
        Log.d("NfcScreen", "初始 NFC 状态: $initial，adapter=$nfcAdapter")
        mutableStateOf(initial)
    }

    // 监听生命周期：每次 onResume 重新检测（用户可能从设置页返回后开启了 NFC）
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                availability = when {
                    nfcAdapter == null -> NfcAvailability.NotSupported
                    nfcAdapter.isEnabled -> NfcAvailability.Enabled
                    else -> NfcAvailability.Disabled
                }
                Log.d("NfcScreen", "onResume 重新检测 NFC 状态: $availability")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return availability
}