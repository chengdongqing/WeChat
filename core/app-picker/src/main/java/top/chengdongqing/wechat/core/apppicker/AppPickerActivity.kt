package top.chengdongqing.wechat.core.apppicker

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import top.chengdongqing.wechat.core.apppicker.model.AppResult
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.R as DesignR

data class AppPickerRequest(val maxSelection: Int = 9) {
    init {
        require(maxSelection in 1..99)
    }
}

internal object AppPickerProtocol {
    const val EXTRA_COUNT = "top.chengdongqing.wechat.apppicker.extra.COUNT"
    const val EXTRA_RESULT = "top.chengdongqing.wechat.apppicker.extra.RESULT"
}

class AppPickerContract : ActivityResultContract<AppPickerRequest, List<AppResult>?>() {
    override fun createIntent(context: Context, input: AppPickerRequest) =
        Intent(context, AppPickerActivity::class.java).putExtra(
            AppPickerProtocol.EXTRA_COUNT,
            input.maxSelection
        )

    override fun parseResult(resultCode: Int, intent: Intent?): List<AppResult>? =
        if (resultCode == Activity.RESULT_OK) intent?.appResults?.toList() else null
}

@AndroidEntryPoint
class AppPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val count = intent.getIntExtra(AppPickerProtocol.EXTRA_COUNT, 0)
        if (count !in 1..99) {
            cancel(); return
        }
        setContent {
            WeTheme {
                AppPicker(count, onCancel = ::cancel) { apps ->
                    setResult(RESULT_OK, Intent().putExtra(AppPickerProtocol.EXTRA_RESULT, apps))
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

class AppPickerLauncher internal constructor(
    private val launcher: ActivityResultLauncher<AppPickerRequest>,
    private val options: ActivityOptionsCompat
) {
    fun launch(request: AppPickerRequest) = launcher.launch(request, options)
}

@Composable
fun rememberAppPickerLauncher(onResult: (List<AppResult>) -> Unit): AppPickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(AppPickerContract()) { it?.let(onResult) }
    val options = remember(context) {
        ActivityOptionsCompat.makeCustomAnimation(
            context,
            DesignR.anim.slide_in_up,
            android.R.anim.fade_out
        )
    }
    return remember(launcher, options) { AppPickerLauncher(launcher, options) }
}

private val Intent.appResults: Array<AppResult>?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayExtra(AppPickerProtocol.EXTRA_RESULT, AppResult::class.java)
    } else @Suppress("DEPRECATION", "UNCHECKED_CAST")
    (getParcelableArrayExtra(AppPickerProtocol.EXTRA_RESULT) as? Array<AppResult>)
