package top.chengdongqing.wechat.ui.components.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import top.chengdongqing.wechat.data.model.VisualMediaType
import top.chengdongqing.wechat.ui.theme.WeChatTheme

class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val type = intent.getStringExtra(EXTRA_MEDIA_TYPE)?.run { VisualMediaType.valueOf(this) }
            ?: VisualMediaType.ImageAndVideo

        setContent {
            WeChatTheme {
                WeCamera(type, onRevoked = { finish() }) { uri, type ->
                    val intent = Intent().apply {
                        putExtra(EXTRA_MEDIA_URI, uri)
                        putExtra(EXTRA_MEDIA_TYPE, type.name)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_MEDIA_TYPE = "extra_media_type"
        const val EXTRA_MEDIA_URI = "extra_media_uri"

        fun newIntent(context: Context) = Intent(context, CameraActivity::class.java)
    }
}

@Composable
fun rememberCameraLauncher(onChange: (Uri, VisualMediaType) -> Unit): (type: VisualMediaType) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(CameraActivity.EXTRA_MEDIA_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                (result.data?.getParcelableExtra(CameraActivity.EXTRA_MEDIA_URI))
            }
            val type =
                result.data?.getStringExtra(CameraActivity.EXTRA_MEDIA_TYPE)?.let { typeName ->
                    VisualMediaType.valueOf(typeName)
                }

            uri?.let { type }?.let { type ->
                onChange(uri, type)
            }
        }
    }

    return {
        val intent = CameraActivity.newIntent(context).apply {
            putExtra(CameraActivity.EXTRA_MEDIA_TYPE, it.toString())
        }
        launcher.launch(intent)
    }
}