package top.chengdongqing.wechat.core.designsystem.components.location.picker

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
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.location.model.LocationInfo
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

class LocationPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WeTheme {
                WeLocationPicker(onCancel = { finish() }) { location ->
                    val intent = Intent().apply {
                        putExtra(EXTRA_LOCATION, location)
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
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, R.anim.fade_out)
        }
    }

    companion object {
        const val EXTRA_LOCATION = "extra_location"

        fun newIntent(context: Context) = Intent(context, LocationPickerActivity::class.java)
    }
}

@Composable
fun rememberPickLocationLauncher(onResult: (LocationInfo) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getParcelableExtra(
                        LocationPickerActivity.EXTRA_LOCATION,
                        LocationInfo::class.java
                    )?.let(onResult)
                } else {
                    @Suppress("DEPRECATION")
                    (getParcelableExtra(LocationPickerActivity.EXTRA_LOCATION) as? LocationInfo)
                        ?.let(onResult)
                }
            }
        }
    }

    return {
        val intent = LocationPickerActivity.newIntent(context).apply {
            flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        }

        launcher.launch(intent)
    }
}