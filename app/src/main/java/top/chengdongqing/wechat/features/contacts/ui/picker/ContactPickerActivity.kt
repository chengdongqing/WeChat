package top.chengdongqing.wechat.features.contacts.ui.picker

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
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@AndroidEntryPoint
class ContactPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val count = intent.getIntExtra(EXTRA_CONTACT_COUNT, 99)

        setContent {
            WeTheme {
                ContactPicker(count, onCancel = ::finish) { chatIds, isGroupChat ->
                    val intent = Intent().apply {
                        putExtra(EXTRA_CHAT_IDS, ArrayList(chatIds))
                        putExtra(EXTRA_IS_GROUP_CHAT, isGroupChat)
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
        const val EXTRA_CHAT_IDS = "extra_chat_ids"
        const val EXTRA_IS_GROUP_CHAT = "extra_is_group_chat"
        const val EXTRA_CONTACT_COUNT = "extra_contact_count"

        fun newIntent(context: Context) = Intent(context, ContactPickerActivity::class.java)
    }
}

@Composable
fun rememberPickContactLauncher(onResult: (chatIds: Set<String>, isGroupChat: Boolean) -> Unit): (count: Int) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val chatIds =
                data?.getStringArrayListExtra(ContactPickerActivity.EXTRA_CHAT_IDS)
            val isGroupChat =
                data?.getBooleanExtra(ContactPickerActivity.EXTRA_IS_GROUP_CHAT, false) ?: false

            if (chatIds != null) {
                onResult(chatIds.toSet(), isGroupChat)
            }
        }
    }

    return { count ->
        val intent = ContactPickerActivity.newIntent(context).apply {
            putExtra(ContactPickerActivity.EXTRA_CONTACT_COUNT, count)
        }

        val options = ActivityOptionsCompat.makeCustomAnimation(
            context,
            R.anim.slide_in_up,
            android.R.anim.fade_out
        )

        launcher.launch(intent, options)
    }
}