package top.chengdongqing.wechat.ui.components.location.preview

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import top.chengdongqing.wechat.data.model.LocationPreviewItem
import top.chengdongqing.wechat.ui.theme.WeChatTheme

class LocationPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("location", LocationPreviewItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("location")
        }!!

        setContent {
            WeChatTheme {
                WeLocationPreview(location) {
                    finish()
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, LocationPreviewActivity::class.java)
    }
}

fun Context.previewLocation(location: LocationPreviewItem) {
    val intent = LocationPreviewActivity.newIntent(this).apply {
        putExtra("location", location)
    }
    startActivity(intent)
}