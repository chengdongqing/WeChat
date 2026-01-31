package top.chengdongqing.wechat.ui.components.media.preview

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.MediaItem
import top.chengdongqing.wechat.ui.theme.WeChatTheme

class MediaPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val medias = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(EXTRA_MEDIA_LIST, MediaItem::class.java)
        } else {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            intent.getParcelableArrayExtra(EXTRA_MEDIA_LIST) as? Array<MediaItem>
        } ?: emptyArray()
        val current = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)

        setContent {
            WeChatTheme {
                WeMediaPreview(medias, current) {
                    finish()
                }
            }
        }
    }

    override fun finish() {
        super.finish()

        // 设置退出动画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, R.anim.fade_out)
        }
    }

    companion object {
        const val EXTRA_MEDIA_LIST = "extra_media_list"
        const val EXTRA_CURRENT_INDEX = "extra_current_index"

        fun newIntent(context: Context) = Intent(context, MediaPreviewActivity::class.java)
    }
}

fun Context.previewMedias(medias: List<MediaItem>, current: Int = 0) {
    val intent = MediaPreviewActivity.newIntent(this).apply {
        putExtra(MediaPreviewActivity.EXTRA_MEDIA_LIST, medias.toTypedArray())
        putExtra(MediaPreviewActivity.EXTRA_CURRENT_INDEX, current)
        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
    startActivity(intent)
}