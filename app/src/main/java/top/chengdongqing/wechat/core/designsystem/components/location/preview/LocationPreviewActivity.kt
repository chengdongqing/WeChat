package top.chengdongqing.wechat.core.designsystem.components.location.preview

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.location.model.LocationPreviewInfo
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

class LocationPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_LOCATION, LocationPreviewInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_LOCATION)
        }!!

        setContent {
            WeTheme {
                WeLocationPreview(location) {
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

        fun newIntent(context: Context) = Intent(context, LocationPreviewActivity::class.java)
    }
}

fun Context.previewLocation(location: LocationPreviewInfo) {
    val intent = LocationPreviewActivity.newIntent(this).apply {
        putExtra(LocationPreviewActivity.EXTRA_LOCATION, location)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
    }
    startActivity(intent)
}