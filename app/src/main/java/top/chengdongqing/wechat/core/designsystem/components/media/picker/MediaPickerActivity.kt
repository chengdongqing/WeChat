package top.chengdongqing.wechat.core.designsystem.components.media.picker

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
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaItem
import top.chengdongqing.wechat.core.designsystem.components.media.model.VisualMediaType
import top.chengdongqing.wechat.core.designsystem.components.media.picker.MediaPickerActivity.Companion.EXTRA_MEDIA_LIST
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.StatusBarAppearanceEffect

@AndroidEntryPoint
class MediaPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val type = intent.getStringExtra(EXTRA_PICK_TYPE)?.let { VisualMediaType.valueOf(it) }
            ?: VisualMediaType.ImageAndVideo
        val count = intent.getIntExtra(EXTRA_PICK_COUNT, 99)

        setContent {
            StatusBarAppearanceEffect(isDark = false)
            WeTheme(isDark = true) {
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

    override fun finish() {
        super.finish()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                android.R.anim.fade_in,
                R.anim.slide_out_down
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, R.anim.slide_out_down)
        }
    }

    companion object {
        const val EXTRA_PICK_TYPE = "extra_pick_type"
        const val EXTRA_PICK_COUNT = "extra_pick_count"
        const val EXTRA_MEDIA_LIST = "extra_media_list"

        fun newIntent(context: Context) = Intent(context, MediaPickerActivity::class.java)
    }
}

@Composable
fun rememberPickMediasLauncher(onResult: (Array<MediaItem>) -> Unit): (type: VisualMediaType, count: Int) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.mediaResults?.let { onResult(it) }
        }
    }

    return { type, count ->
        val intent = MediaPickerActivity.newIntent(context).apply {
            putExtra(MediaPickerActivity.EXTRA_PICK_TYPE, type.toString())
            putExtra(MediaPickerActivity.EXTRA_PICK_COUNT, count)
        }

        val options = ActivityOptionsCompat.makeCustomAnimation(
            context,
            R.anim.slide_in_up,
            android.R.anim.fade_out
        )
        launcher.launch(intent, options)
    }
}

private val Intent.mediaResults: Array<MediaItem>?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayExtra(
            EXTRA_MEDIA_LIST,
            MediaItem::class.java
        )
    } else {
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        getParcelableArrayExtra(EXTRA_MEDIA_LIST) as? Array<MediaItem>
    }