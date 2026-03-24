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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityOptionsCompat
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.ContactResult

@AndroidEntryPoint
class ContactPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val count = intent.getIntExtra(EXTRA_PICK_COUNT, 99)

        setContent {
            WeTheme {
                ContactPicker(count, onCancel = ::finish) { contacts ->
                    val intent = Intent().apply {
                        putExtra(EXTRA_CONTACTS, contacts)
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
        const val EXTRA_CONTACTS = "extra_contacts"
        const val EXTRA_PICK_COUNT = "extra_pick_count"

        fun newIntent(context: Context) = Intent(context, ContactPickerActivity::class.java)
    }
}

@Composable
fun rememberPickContactLauncher(onResult: (chatIds: Array<ContactResult>) -> Unit): (count: Int) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.contactResults?.let(onResult)
        }
    }

    return { count ->
        val intent = ContactPickerActivity.newIntent(context).apply {
            putExtra(ContactPickerActivity.EXTRA_PICK_COUNT, count)
        }

        val options = ActivityOptionsCompat.makeCustomAnimation(
            context,
            R.anim.slide_in_up,
            android.R.anim.fade_out
        )

        launcher.launch(intent, options)
    }
}

private val Intent.contactResults: Array<ContactResult>?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayExtra(
            ContactPickerActivity.EXTRA_CONTACTS,
            ContactResult::class.java
        )
    } else {
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        getParcelableArrayExtra(ContactPickerActivity.EXTRA_CONTACTS) as? Array<ContactResult>
    }