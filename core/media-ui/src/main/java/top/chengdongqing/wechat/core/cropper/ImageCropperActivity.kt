package top.chengdongqing.wechat.core.cropper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.window.ImmersiveSystemBars

internal const val EXTRA_CROPPER_URI = "top.chengdongqing.wechat.cropper.extra.URI"

class ImageCropperContract : ActivityResultContract<Uri, Uri?>() {
    override fun createIntent(context: Context, input: Uri) =
        Intent(context, ImageCropperActivity::class.java).putExtra(EXTRA_CROPPER_URI, input)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.croppingUri else null
}

@AndroidEntryPoint
class ImageCropperActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.croppingUri ?: run { cancel(); return }
        setContent {
            ImmersiveSystemBars()
            WeTheme(isDark = true) {
                WeImageCropper(source, onCancel = ::cancel) { result ->
                    setResult(RESULT_OK, Intent().putExtra(EXTRA_CROPPER_URI, result)); finish()
                }
            }
        }
    }

    private fun cancel() {
        setResult(RESULT_CANCELED); finish()
    }
}

class ImageCropperLauncher internal constructor(private val launcher: ActivityResultLauncher<Uri>) {
    fun launch(uri: Uri) = launcher.launch(uri)
}

@Composable
fun rememberImageCropperLauncher(onResult: (Uri) -> Unit): ImageCropperLauncher =
    ImageCropperLauncher(rememberLauncherForActivityResult(ImageCropperContract()) {
        it?.let(
            onResult
        )
    })

private val Intent.croppingUri: Uri?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(EXTRA_CROPPER_URI, Uri::class.java)
    } else @Suppress("DEPRECATION") getParcelableExtra(EXTRA_CROPPER_URI)
