package top.chengdongqing.wechat.core.location.preview

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.location.model.LocationPreviewInfo
import top.chengdongqing.wechat.core.location.preview.LocationPreviewActivity.Companion.EXTRA_LOCATION
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@AndroidEntryPoint
class LocationPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WeTheme {
                intent.previewLocation?.let { location ->
                    WeLocationPreview(location) {
                        finish()
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, DesignR.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, DesignR.anim.fade_out)
        }
    }

    companion object {
        const val EXTRA_LOCATION = "extra_location"

        fun newIntent(context: Context) = Intent(context, LocationPreviewActivity::class.java)
    }
}

fun Context.previewLocation(location: LocationPreviewInfo) {
    val intent = LocationPreviewActivity.newIntent(this).apply {
        putExtra(EXTRA_LOCATION, location)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
    }
    startActivity(intent)
}

private val Intent.previewLocation: LocationPreviewInfo?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(EXTRA_LOCATION, LocationPreviewInfo::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(EXTRA_LOCATION)
    }