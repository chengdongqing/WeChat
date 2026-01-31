package top.chengdongqing.wechat.ui.components.qrcode.scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.media.rememberSoundTipPlayer
import top.chengdongqing.wechat.ui.theme.WeChatTheme

class QRCodeScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WeChatTheme {
                WeQRCodeScanner(
                    onRevoked = { finish() }
                ) { codes ->
                    val intent = Intent().apply {
                        putExtra(EXTRA_QR_CODES, codes.map { it.rawValue }.toTypedArray())
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_QR_CODES = "extra_qr_codes"

        fun newIntent(context: Context) = Intent(context, QRCodeScannerActivity::class.java)
    }
}

@Composable
fun rememberScanCodeLauncher(onChange: (Array<String>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val soundTipPlayer = rememberSoundTipPlayer()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            soundTipPlayer.play(R.raw.qrcode_completed) // 播放提示音
            result.data?.getStringArrayExtra(QRCodeScannerActivity.EXTRA_QR_CODES)?.let(onChange)
        }
    }

    return {
        launcher.launch(QRCodeScannerActivity.newIntent(context))
    }
}