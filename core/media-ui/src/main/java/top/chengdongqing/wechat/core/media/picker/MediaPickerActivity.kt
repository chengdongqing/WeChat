package top.chengdongqing.wechat.core.media.picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityOptionsCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.window.StatusBarAppearanceEffect
import top.chengdongqing.wechat.core.media.model.MediaItem
import top.chengdongqing.wechat.core.media.model.VisualMediaType
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Parcelize
data class MediaPickerRequest(
    val mediaType: VisualMediaType = VisualMediaType.ImageAndVideo,
    val maxSelection: Int = DEFAULT_MAX_SELECTION,
    val enableMerge: Boolean = false
) : Parcelable {
    init {
        require(maxSelection in 1..MAX_SELECTION_LIMIT) {
            "maxSelection must be between 1 and $MAX_SELECTION_LIMIT"
        }
        require(!enableMerge || maxSelection > 1) {
            "Merge cannot be enabled in single-selection mode"
        }
    }

    companion object {
        const val DEFAULT_MAX_SELECTION = 9
        const val MAX_SELECTION_LIMIT = 99

        fun singleImage() = MediaPickerRequest(
            mediaType = VisualMediaType.Image,
            maxSelection = 1
        )
    }
}

@Parcelize
data class MediaPickerResult(
    val items: List<MediaItem>,
    val merge: Boolean = false,
    val original: Boolean = false
) : Parcelable

internal object MediaPickerProtocol {
    const val EXTRA_REQUEST =
        "top.chengdongqing.wechat.core.media.picker.extra.REQUEST"
    const val EXTRA_RESULT =
        "top.chengdongqing.wechat.core.media.picker.extra.RESULT"
}

class PickMediaContract : ActivityResultContract<MediaPickerRequest, MediaPickerResult?>() {
    override fun createIntent(context: Context, input: MediaPickerRequest): Intent =
        Intent(context, MediaPickerActivity::class.java).apply {
            putExtra(MediaPickerProtocol.EXTRA_REQUEST, input)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): MediaPickerResult? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.parcelableExtra(MediaPickerProtocol.EXTRA_RESULT)
    }
}

@AndroidEntryPoint
class MediaPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val request = intent.parcelableExtra<MediaPickerRequest>(
            MediaPickerProtocol.EXTRA_REQUEST
        ) ?: run {
            cancel()
            return
        }

        setContent {
            StatusBarAppearanceEffect(isDark = false)
            WeTheme(isDark = true) {
                WeMediaPicker(
                    type = request.mediaType,
                    count = request.maxSelection,
                    enableMerge = request.enableMerge,
                    onCancel = ::cancel,
                    onConfirm = { items, merge, original ->
                        complete(
                            MediaPickerResult(
                                items = items.toList(),
                                merge = merge,
                                original = original
                            )
                        )
                    }
                )
            }
        }
    }

    private fun complete(result: MediaPickerResult) {
        setResult(
            RESULT_OK,
            Intent().putExtra(MediaPickerProtocol.EXTRA_RESULT, result)
        )
        finish()
    }

    private fun cancel() {
        setResult(RESULT_CANCELED)
        finish()
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
}

class MediaPickerLauncher internal constructor(
    private val launcher: ActivityResultLauncher<MediaPickerRequest>,
    private val options: ActivityOptionsCompat
) {
    fun launch(request: MediaPickerRequest) {
        launcher.launch(request, options)
    }
}

@Composable
fun rememberMediaPickerLauncher(
    onResult: (MediaPickerResult) -> Unit
): MediaPickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(PickMediaContract()) { result ->
        result?.let(onResult)
    }
    val options = remember(context) {
        ActivityOptionsCompat.makeCustomAnimation(
            context,
            DesignR.anim.slide_in_up,
            android.R.anim.fade_out
        )
    }
    return remember(launcher, options) { MediaPickerLauncher(launcher, options) }
}

private inline fun <reified T : Parcelable> Intent.parcelableExtra(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
