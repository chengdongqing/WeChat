package top.chengdongqing.wechat.core.camera

import android.R
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
import androidx.core.app.ActivityOptionsCompat
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.media.model.VisualMediaType
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@AndroidEntryPoint
class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val type = intent.getStringExtra(EXTRA_MEDIA_TYPE)?.run { VisualMediaType.valueOf(this) }
            ?: VisualMediaType.ImageAndVideo

        setContent {
            WeTheme {
                fun finishWithMedia(uri: Uri, mediaType: VisualMediaType) {
                    val result = Intent().apply {
                        putExtra(EXTRA_MEDIA_URI, uri)
                        putExtra(EXTRA_MEDIA_TYPE, mediaType.name)
                    }
                    setResult(RESULT_OK, result)
                    finish()
                }

                WeCamera(
                    type = type,
                    onRevoked = ::finish,
                    onCapture = ::finishWithMedia
                )
            }
        }
    }

    override fun finish() {
        super.finish()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.fade_in,
                DesignR.anim.slide_out_down
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, DesignR.anim.slide_out_down)
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
            val intent = result.data

            val uri = intent?.photoUri
            val type = intent?.getStringExtra(CameraActivity.EXTRA_MEDIA_TYPE)
                ?.let { VisualMediaType.valueOf(it) }

            if (uri != null && type != null) {
                onChange(uri, type)
            }
        }
    }

    return {
        val intent = CameraActivity.newIntent(context).apply {
            putExtra(CameraActivity.EXTRA_MEDIA_TYPE, it.toString())
        }

        val options = ActivityOptionsCompat.makeCustomAnimation(
            context,
            DesignR.anim.slide_in_up,
            R.anim.fade_out
        )

        launcher.launch(intent, options)
    }
}

private val Intent.photoUri: Uri?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(CameraActivity.EXTRA_MEDIA_URI, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(CameraActivity.EXTRA_MEDIA_URI)
    }
