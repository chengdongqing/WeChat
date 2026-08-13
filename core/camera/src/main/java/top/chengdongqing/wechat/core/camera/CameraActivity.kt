package top.chengdongqing.wechat.core.camera

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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityOptionsCompat
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.media.model.VisualMediaType
import top.chengdongqing.wechat.core.designsystem.R as DesignR

data class CameraResult(val uri: Uri, val mediaType: VisualMediaType)

internal object CameraProtocol {
    const val EXTRA_MEDIA_TYPE = "top.chengdongqing.wechat.camera.extra.MEDIA_TYPE"
    const val EXTRA_MEDIA_URI = "top.chengdongqing.wechat.camera.extra.MEDIA_URI"
}

class CameraContract : ActivityResultContract<VisualMediaType, CameraResult?>() {
    override fun createIntent(context: Context, input: VisualMediaType) =
        Intent(context, CameraActivity::class.java)
            .putExtra(CameraProtocol.EXTRA_MEDIA_TYPE, input.name)

    override fun parseResult(resultCode: Int, intent: Intent?): CameraResult? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        val uri = intent.parcelableUri(CameraProtocol.EXTRA_MEDIA_URI) ?: return null
        val type = intent.getStringExtra(CameraProtocol.EXTRA_MEDIA_TYPE)
            ?.let { value -> VisualMediaType.entries.find { it.name == value } }
            ?: return null
        return CameraResult(uri, type)
    }
}

@AndroidEntryPoint
class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val type = intent.getStringExtra(CameraProtocol.EXTRA_MEDIA_TYPE)
            ?.let { value -> VisualMediaType.entries.find { it.name == value } }
            ?: run { cancel(); return }

        setContent {
            WeTheme {
                WeCamera(type, onRevoked = ::cancel) { uri, mediaType ->
                    setResult(
                        RESULT_OK,
                        Intent()
                            .putExtra(CameraProtocol.EXTRA_MEDIA_URI, uri)
                            .putExtra(CameraProtocol.EXTRA_MEDIA_TYPE, mediaType.name)
                    )
                    finish()
                }
            }
        }
    }

    private fun cancel() {
        setResult(RESULT_CANCELED); finish()
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                android.R.anim.fade_in,
                DesignR.anim.slide_out_down
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, DesignR.anim.slide_out_down)
        }
    }
}

class CameraLauncher internal constructor(
    private val launcher: ActivityResultLauncher<VisualMediaType>,
    private val options: ActivityOptionsCompat
) {
    fun launch(type: VisualMediaType = VisualMediaType.ImageAndVideo) =
        launcher.launch(type, options)
}

@Composable
fun rememberCameraLauncher(onResult: (CameraResult) -> Unit): CameraLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(CameraContract()) { it?.let(onResult) }
    val options = remember(context) {
        ActivityOptionsCompat.makeCustomAnimation(
            context,
            DesignR.anim.slide_in_up,
            android.R.anim.fade_out
        )
    }
    return remember(launcher, options) { CameraLauncher(launcher, options) }
}

private fun Intent.parcelableUri(key: String): Uri? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelableExtra(
        key,
        Uri::class.java
    )
    else @Suppress("DEPRECATION") getParcelableExtra(key)
