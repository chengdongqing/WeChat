package top.chengdongqing.wechat.ui.components.qrcode.scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.ui.theme.WeChatTheme

class QrCodeScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WeChatTheme {
                WeQrCodeScanner(
                    onRevoked = { finish() }
                ) { codes ->
                    val intent = Intent().apply {
                        putExtra("codes", codes.map { it.rawValue }.toTypedArray())
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, QrCodeScannerActivity::class.java)
    }
}

@Composable
fun rememberScanCodeLauncher(onChange: (Array<String>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            SoundTipPlayer.play(R.raw.qrcode_completed) // 播放提示音
            result.data?.getStringArrayExtra("codes")?.let(onChange)
        }
    }

    return {
        launcher.launch(QrCodeScannerActivity.newIntent(context))
    }
}