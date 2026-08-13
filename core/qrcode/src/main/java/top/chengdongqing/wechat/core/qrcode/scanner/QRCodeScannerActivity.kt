package top.chengdongqing.wechat.core.qrcode.scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.playback.rememberSoundTipPlayer
import top.chengdongqing.wechat.core.playback.R as PlaybackR

internal const val EXTRA_QR_CODES = "top.chengdongqing.wechat.qrcode.extra.CODES"

class QrCodeScannerContract : ActivityResultContract<Unit, List<String>?>() {
    override fun createIntent(context: Context, input: Unit) =
        Intent(context, QRCodeScannerActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): List<String>? =
        if (resultCode == Activity.RESULT_OK) intent?.getStringArrayExtra(EXTRA_QR_CODES)
            ?.toList() else null
}

@AndroidEntryPoint
class QRCodeScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeTheme {
                WeQRCodeScanner(onRevoked = ::cancel) { codes ->
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(
                            EXTRA_QR_CODES,
                            codes.mapNotNull { it.rawValue }.toTypedArray()
                        )
                    )
                    finish()
                }
            }
        }
    }

    private fun cancel() {
        setResult(RESULT_CANCELED); finish()
    }
}

class QrCodeScannerLauncher internal constructor(private val launcher: ActivityResultLauncher<Unit>) {
    fun launch() = launcher.launch(Unit)
}

@Composable
fun rememberQrCodeScannerLauncher(onResult: (List<String>) -> Unit): QrCodeScannerLauncher {
    val soundTipPlayer = rememberSoundTipPlayer()
    val launcher = rememberLauncherForActivityResult(QrCodeScannerContract()) { result ->
        result?.let { soundTipPlayer.play(PlaybackR.raw.tip_qrcode_completed); onResult(it) }
    }
    return QrCodeScannerLauncher(launcher)
}
