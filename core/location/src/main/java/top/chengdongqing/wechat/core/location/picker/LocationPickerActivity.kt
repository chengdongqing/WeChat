package top.chengdongqing.wechat.core.location.picker

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
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.location.model.LocationInfo
import top.chengdongqing.wechat.core.location.repository.LocationRepository
import top.chengdongqing.wechat.core.designsystem.R as DesignR

internal const val EXTRA_LOCATION_RESULT = "top.chengdongqing.wechat.location.extra.RESULT"

class LocationPickerContract : ActivityResultContract<Unit, LocationInfo?>() {
    override fun createIntent(context: Context, input: Unit) =
        Intent(
            context,
            LocationPickerActivity::class.java
        ).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)

    override fun parseResult(resultCode: Int, intent: Intent?): LocationInfo? =
        if (resultCode == Activity.RESULT_OK) intent?.locationResult else null
}

@AndroidEntryPoint
class LocationPickerActivity : ComponentActivity() {
    @Inject
    lateinit var locationRepository: LocationRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeTheme {
                WeLocationPicker(locationRepository, onCancel = ::cancel) { location ->
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(EXTRA_LOCATION_RESULT, location)
                    ); finish()
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
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, DesignR.anim.fade_out)
        } else {
            @Suppress("DEPRECATION") overridePendingTransition(0, DesignR.anim.fade_out)
        }
    }
}

class LocationPickerLauncher internal constructor(private val launcher: ActivityResultLauncher<Unit>) {
    fun launch() = launcher.launch(Unit)
}

@Composable
fun rememberLocationPickerLauncher(onResult: (LocationInfo) -> Unit): LocationPickerLauncher =
    LocationPickerLauncher(rememberLauncherForActivityResult(LocationPickerContract()) {
        it?.let(
            onResult
        )
    })

private val Intent.locationResult: LocationInfo?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(EXTRA_LOCATION_RESULT, LocationInfo::class.java)
    } else @Suppress("DEPRECATION") getParcelableExtra(EXTRA_LOCATION_RESULT)
