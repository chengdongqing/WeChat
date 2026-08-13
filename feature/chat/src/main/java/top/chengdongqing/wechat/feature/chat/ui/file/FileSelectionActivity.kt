package top.chengdongqing.wechat.feature.chat.ui.file

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
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@AndroidEntryPoint
class FileSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeTheme {
                FileSelectionScreen(
                    onCancel = ::cancel,
                    onConfirm = { uris ->
                        setResult(
                            RESULT_OK,
                            Intent().putParcelableArrayListExtra(EXTRA_URIS, ArrayList(uris))
                        )
                        finish()
                    }
                )
            }
        }
    }

    private fun cancel() {
        setResult(RESULT_CANCELED)
        finish()
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
            overridePendingTransition(
                android.R.anim.fade_in,
                DesignR.anim.slide_out_down
            )
        }
    }

    companion object {
        internal const val EXTRA_URIS = "top.chengdongqing.wechat.files.extra.URIS"
    }
}

class FilePickerContract : ActivityResultContract<Unit, List<Uri>?>() {
    override fun createIntent(context: Context, input: Unit) =
        Intent(context, FileSelectionActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri>? {
        if (resultCode != Activity.RESULT_OK) return null
        return if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableArrayListExtra(FileSelectionActivity.EXTRA_URIS, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableArrayListExtra(FileSelectionActivity.EXTRA_URIS)
        }
    }
}

class FilePickerLauncher internal constructor(
    private val launcher: ActivityResultLauncher<Unit>,
    private val options: ActivityOptionsCompat
) {
    fun launch() = launcher.launch(Unit, options)
}

@Composable
fun rememberFilePickerLauncher(onResult: (List<Uri>) -> Unit): FilePickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(FilePickerContract()) { it?.let(onResult) }
    val options = remember(context) {
        ActivityOptionsCompat.makeCustomAnimation(
            context,
            DesignR.anim.slide_in_up,
            android.R.anim.fade_out
        )
    }
    return remember(launcher, options) { FilePickerLauncher(launcher, options) }
}
