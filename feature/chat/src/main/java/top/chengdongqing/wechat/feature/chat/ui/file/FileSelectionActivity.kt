package top.chengdongqing.wechat.feature.chat.ui.file

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
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
                    onCancel = ::finish,
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
        private const val EXTRA_URIS = "selected_file_uris"

        fun launch(context: Context, launcher: ActivityResultLauncher<Intent>) {
            val options = ActivityOptionsCompat.makeCustomAnimation(
                context, DesignR.anim.slide_in_up, android.R.anim.fade_out
            )
            launcher.launch(Intent(context, FileSelectionActivity::class.java), options)
        }

        fun readResult(intent: Intent?): List<Uri> = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableArrayListExtra(EXTRA_URIS, Uri::class.java).orEmpty()
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableArrayListExtra<Uri>(EXTRA_URIS).orEmpty()
        }
    }
}
