package top.chengdongqing.wechat.feature.contacts.ui.picker

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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityOptionsCompat
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.ContactResult
import top.chengdongqing.wechat.core.designsystem.R as DesignR

data class ContactPickerRequest(
    val maxSelection: Int = 99,
    val excludeSelf: Boolean = false
) { init {
    require(maxSelection in 1..99)
}
}

internal object ContactPickerProtocol {
    const val EXTRA_COUNT = "top.chengdongqing.wechat.contacts.picker.extra.COUNT"
    const val EXTRA_EXCLUDE_SELF = "top.chengdongqing.wechat.contacts.picker.extra.EXCLUDE_SELF"
    const val EXTRA_RESULT = "top.chengdongqing.wechat.contacts.picker.extra.RESULT"
}

class ContactPickerContract : ActivityResultContract<ContactPickerRequest, List<ContactResult>?>() {
    override fun createIntent(context: Context, input: ContactPickerRequest) =
        Intent(context, ContactPickerActivity::class.java)
            .putExtra(ContactPickerProtocol.EXTRA_COUNT, input.maxSelection)
            .putExtra(ContactPickerProtocol.EXTRA_EXCLUDE_SELF, input.excludeSelf)

    override fun parseResult(resultCode: Int, intent: Intent?): List<ContactResult>? =
        if (resultCode == Activity.RESULT_OK) intent?.contactResults?.toList() else null
}

@AndroidEntryPoint
class ContactPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        val count = intent.getIntExtra(ContactPickerProtocol.EXTRA_COUNT, 0)
        if (count !in 1..99) {
            cancel(); return
        }
        val excludeSelf = intent.getBooleanExtra(ContactPickerProtocol.EXTRA_EXCLUDE_SELF, false)
        setContent {
            WeTheme {
                ContactPicker(count, excludeSelf, onCancel = ::cancel) { contacts ->
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(ContactPickerProtocol.EXTRA_RESULT, contacts)
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
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                android.R.anim.fade_in,
                DesignR.anim.slide_out_down
            )
        } else {
            @Suppress("DEPRECATION") overridePendingTransition(
                android.R.anim.fade_in,
                DesignR.anim.slide_out_down
            )
        }
    }
}

class ContactPickerLauncher internal constructor(
    private val launcher: ActivityResultLauncher<ContactPickerRequest>,
    private val options: ActivityOptionsCompat
) {
    fun launch(request: ContactPickerRequest) = launcher.launch(request, options)
}

@Composable
fun rememberContactPickerLauncher(onResult: (List<ContactResult>) -> Unit): ContactPickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ContactPickerContract()) { it?.let(onResult) }
    val options = remember(context) {
        ActivityOptionsCompat.makeCustomAnimation(
            context,
            DesignR.anim.slide_in_up,
            android.R.anim.fade_out
        )
    }
    return remember(launcher, options) { ContactPickerLauncher(launcher, options) }
}

private val Intent.contactResults: Array<ContactResult>?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayExtra(ContactPickerProtocol.EXTRA_RESULT, ContactResult::class.java)
    } else @Suppress("DEPRECATION", "UNCHECKED_CAST")
    (getParcelableArrayExtra(ContactPickerProtocol.EXTRA_RESULT) as? Array<ContactResult>)
