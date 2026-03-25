package top.chengdongqing.wechat.core.common.app

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import top.chengdongqing.wechat.core.common.app.model.AppResult
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@AndroidEntryPoint
class AppPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val count = intent.getIntExtra(EXTRA_PICK_COUNT, 9)

        setContent {
            WeTheme {
                AppPicker(count, onCancel = ::finish) { apps ->
                    val intent = Intent().apply {
                        putExtra(EXTRA_APP_LIST, apps)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
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
            overridePendingTransition(android.R.anim.fade_in, DesignR.anim.slide_out_down)
        }
    }

    companion object {
        const val EXTRA_APP_LIST = "extra_app_list"
        const val EXTRA_PICK_COUNT = "extra_pick_count"

        fun newIntent(context: Context) = Intent(context, AppPickerActivity::class.java)
    }
}

@Composable
fun rememberPickAppLauncher(onResult: (Array<AppResult>) -> Unit): (count: Int) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.appResults?.let(onResult)
        }
    }

    return { count ->
        val intent = AppPickerActivity.newIntent(context).apply {
            putExtra(AppPickerActivity.EXTRA_PICK_COUNT, count)
        }

        val options = ActivityOptionsCompat.makeCustomAnimation(
            context,
            DesignR.anim.slide_in_up,
            android.R.anim.fade_out
        )

        launcher.launch(intent, options)
    }
}

private val Intent.appResults: Array<AppResult>?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayExtra(AppPickerActivity.EXTRA_APP_LIST, AppResult::class.java)
    } else {
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        getParcelableArrayExtra(AppPickerActivity.EXTRA_APP_LIST) as? Array<AppResult>
    }