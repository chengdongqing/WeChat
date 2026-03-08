package top.chengdongqing.wechat.core.designsystem.components.cropper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.ImmersiveSystemBars

@AndroidEntryPoint
class ImageCropperActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_URI)
            }!!

            ImmersiveSystemBars()
            WeTheme(darkTheme = true) {
                WeImageCropper(uri, onCancel = { finish() }) {
                    val intent = Intent().apply {
                        putExtra(EXTRA_URI, it)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_URI = "extra_uri"

        fun newIntent(context: Context) = Intent(context, ImageCropperActivity::class.java)
    }
}


@Composable
fun rememberImageCropperLauncher(onChange: (Uri) -> Unit): (uri: Uri) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getParcelableExtra(ImageCropperActivity.EXTRA_URI, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    (getParcelableExtra(ImageCropperActivity.EXTRA_URI) as? Uri)
                }?.let(onChange)
            }
        }
    }

    return {
        val intent = ImageCropperActivity.newIntent(context).apply {
            putExtra(ImageCropperActivity.EXTRA_URI, it)
        }
        launcher.launch(intent)
    }
}