package top.chengdongqing.wechat.core.designsystem.components.media.preview

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaItem
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@AndroidEntryPoint
class MediaPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val medias = MediaPreviewDataHolder.mediaList ?: emptyList()
        val current = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)

        setContent {
            WeTheme {
                WeMediaPreview(medias, current) {
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
        const val EXTRA_CURRENT_INDEX = "extra_current_index"

        fun newIntent(context: Context) = Intent(context, MediaPreviewActivity::class.java)
    }
}

fun Context.previewMedias(mediaList: List<MediaItem>, current: Int = 0) {
    MediaPreviewDataHolder.mediaList = mediaList
    val intent = MediaPreviewActivity.newIntent(this).apply {
        putExtra(MediaPreviewActivity.EXTRA_CURRENT_INDEX, current)
        flags = Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(intent)
}

private object MediaPreviewDataHolder {
    var mediaList: List<MediaItem>? = null
}