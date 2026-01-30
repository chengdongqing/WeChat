package top.chengdongqing.wechat.ui.components.media.picker

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
import top.chengdongqing.wechat.data.model.MediaItem
import top.chengdongqing.wechat.data.model.VisualMediaType
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.utils.SetupStatusBarStyle

class MediaPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val type = intent.getStringExtra(EXTRA_MEDIA_TYPE)?.run { VisualMediaType.valueOf(this) }
            ?: VisualMediaType.IMAGE_AND_VIDEO
        val count = intent.getIntExtra(EXTRA_MEDIA_COUNT, 99)

        setContent {
            SetupStatusBarStyle(isDark = false)
            WeChatTheme(darkTheme = true) {
                WeMediaPicker(type, count, onCancel = { finish() }) { medias ->
                    val intent = Intent().apply {
                        putExtra(EXTRA_MEDIA_LIST, medias)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_MEDIA_TYPE = "extra_extra_media_type"
        const val EXTRA_MEDIA_COUNT = "extra_extra_media_count"
        const val EXTRA_MEDIA_LIST = "extra_extra_media_list"

        fun newIntent(context: Context) = Intent(context, MediaPickerActivity::class.java)
    }
}

@Composable
fun rememberPickMediasLauncher(onChange: (Array<MediaItem>) -> Unit): (type: VisualMediaType, count: Int) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getParcelableArrayExtra(
                        MediaPickerActivity.EXTRA_MEDIA_LIST,
                        MediaItem::class.java
                    )
                } else {
                    @Suppress("DEPRECATION", "UNCHECKED_CAST")
                    (getParcelableArrayExtra(MediaPickerActivity.EXTRA_MEDIA_LIST) as? Array<MediaItem>)
                }?.let(onChange)
            }
        }
    }

    return { type, count ->
        val intent = MediaPickerActivity.newIntent(context).apply {
            putExtra(MediaPickerActivity.EXTRA_MEDIA_TYPE, type.toString())
            putExtra(MediaPickerActivity.EXTRA_MEDIA_COUNT, count)
        }
        launcher.launch(intent)
    }
}