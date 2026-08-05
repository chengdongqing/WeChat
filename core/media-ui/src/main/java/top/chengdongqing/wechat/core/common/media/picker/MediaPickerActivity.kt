package top.chengdongqing.wechat.core.common.media.picker

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
import top.chengdongqing.wechat.core.common.media.model.MediaItem
import top.chengdongqing.wechat.core.common.media.model.VisualMediaType
import top.chengdongqing.wechat.core.common.media.picker.MediaPickerActivity.Companion.EXTRA_MEDIA_LIST
import top.chengdongqing.wechat.core.common.media.picker.MediaPickerActivity.Companion.EXTRA_MERGE_MEDIA
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.window.StatusBarAppearanceEffect
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@AndroidEntryPoint
class MediaPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val type = intent.getStringExtra(EXTRA_PICK_TYPE)?.let { VisualMediaType.valueOf(it) }
            ?: VisualMediaType.ImageAndVideo
        val count = intent.getIntExtra(EXTRA_PICK_COUNT, 99)
        val enableMerge = intent.getBooleanExtra(EXTRA_ENABLE_MERGE, false)

        setContent {
            StatusBarAppearanceEffect(isDark = false)
            WeTheme(isDark = true) {
                WeMediaPicker(
                    type,
                    count,
                    enableMerge,
                    onCancel = { finish() }) { medias, merge, original ->
                    val intent = Intent().apply {
                        putExtra(EXTRA_MEDIA_LIST, medias)
                        putExtra(EXTRA_MERGE_MEDIA, merge)
                        putExtra(EXTRA_ORIGINAL_MEDIA, original)
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
        const val EXTRA_PICK_TYPE = "extra_pick_type"
        const val EXTRA_PICK_COUNT = "extra_pick_count"
        const val EXTRA_MEDIA_LIST = "extra_media_list"
        const val EXTRA_MERGE_MEDIA = "extra_merge_media"
        const val EXTRA_ORIGINAL_MEDIA = "extra_original_media"
        const val EXTRA_ENABLE_MERGE = "extra_enable_merge"

        fun newIntent(context: Context) = Intent(context, MediaPickerActivity::class.java)
    }
}

@Composable
fun rememberMediaPickerLauncher(
    enableMerge: Boolean = false,
    onResult: (medias: Array<MediaItem>, merge: Boolean, original: Boolean) -> Unit
): (type: VisualMediaType, count: Int) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                data.mediaResults?.let {
                    onResult(
                        it,
                        data.getBooleanExtra(EXTRA_MERGE_MEDIA, false),
                        data.getBooleanExtra(MediaPickerActivity.EXTRA_ORIGINAL_MEDIA, false)
                    )
                }
            }
        }
    }

    return { type, count ->
        val intent = MediaPickerActivity.newIntent(context).apply {
            putExtra(MediaPickerActivity.EXTRA_PICK_TYPE, type.toString())
            putExtra(MediaPickerActivity.EXTRA_PICK_COUNT, count)
            putExtra(MediaPickerActivity.EXTRA_ENABLE_MERGE, enableMerge)
        }

        val options = ActivityOptionsCompat.makeCustomAnimation(
            context,
            DesignR.anim.slide_in_up,
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
