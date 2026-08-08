package top.chengdongqing.wechat.core.navigation

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import top.chengdongqing.wechat.core.call.ui.rememberCallLauncher
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.core.model.ContactResult
import top.chengdongqing.wechat.core.notification.CallNotificationPermissionManager
import top.chengdongqing.wechat.feature.call.ui.startCall
import top.chengdongqing.wechat.feature.chat.navigation.chatNavEntries
import top.chengdongqing.wechat.feature.contacts.navigation.contactsNavEntries
import top.chengdongqing.wechat.feature.contacts.ui.picker.rememberPickContactLauncher
import top.chengdongqing.wechat.feature.favorites.navigation.favoritesNavEntries
import top.chengdongqing.wechat.feature.intercom.navigation.intercomNavEntries
import top.chengdongqing.wechat.feature.moments.navigation.momentsNavEntries
import top.chengdongqing.wechat.feature.profile.navigation.meNavEntries
import top.chengdongqing.wechat.feature.settings.navigation.settingsNavEntries

@Composable
fun AppNavigation(backStack: NavBackStack<NavKey>) {
    val goBack: () -> Unit = {
        backStack.removeLastOrNull()
    }

    val context = LocalContext.current
    val callLauncher = remember(context) { AppCallLauncher(context) }
    CompositionLocalProvider(
        LocalContactPickerLauncher provides AppContactPickerLauncher,
        LocalCallLauncher provides callLauncher
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = goBack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            transitionSpec = { createEnterTransition() },
            popTransitionSpec = { createExitTransition() },
            predictivePopTransitionSpec = { createExitTransition() },
            entryProvider = entryProvider {
                commonNavEntries(backStack, goBack)
                chatNavEntries(backStack, goBack)
                contactsNavEntries(backStack, goBack)
                meNavEntries(backStack, goBack)
                momentsNavEntries(backStack, goBack)
                intercomNavEntries(backStack, goBack)
                favoritesNavEntries(backStack, goBack)
                settingsNavEntries(backStack, goBack)
            }
        )
    }
}

private object AppContactPickerLauncher : ContactPickerLauncher {
    @Composable
    override fun rememberLauncher(
        excludeSelf: Boolean,
        onResult: (Array<ContactResult>) -> Unit
    ) = rememberPickContactLauncher(excludeSelf, onResult)
}

private class AppCallLauncher(private val context: Context) : CallLauncher {
    @Composable
    override fun rememberLauncher(peerId: String): (CallType) -> Unit {
        var showRationale by remember { mutableStateOf(false) }
        var pendingCall by remember { mutableStateOf<Pair<String, CallType>?>(null) }

        val continueCall = {
            pendingCall?.let { (id, type) -> context.startCall(id, type) }
            pendingCall = null
        }
        val miuiPermissionSettings = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            continueCall()
        }
        val overlaySettings = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (CallNotificationPermissionManager.needsMiuiCallPermissions()) {
                miuiPermissionSettings.launch(
                    CallNotificationPermissionManager.miuiPermissionSettingsIntent(context)
                )
            } else {
                continueCall()
            }
        }
        val fullScreenSettings = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (!CallNotificationPermissionManager.canDisplayOverOtherApps(context)) {
                overlaySettings.launch(
                    CallNotificationPermissionManager.overlaySettingsIntent(context)
                )
            } else if (CallNotificationPermissionManager.needsMiuiCallPermissions()) {
                miuiPermissionSettings.launch(
                    CallNotificationPermissionManager.miuiPermissionSettingsIntent(context)
                )
            } else {
                continueCall()
            }
        }

        val requestSpecialCallPermissions = {
            when {
                !CallNotificationPermissionManager.canUseFullScreenIntent(context) ->
                    fullScreenSettings.launch(
                        CallNotificationPermissionManager
                            .fullScreenIntentSettingsIntent(context)
                    )

                !CallNotificationPermissionManager.canDisplayOverOtherApps(context) ->
                    overlaySettings.launch(
                        CallNotificationPermissionManager.overlaySettingsIntent(context)
                    )

                CallNotificationPermissionManager.needsMiuiCallPermissions() ->
                    miuiPermissionSettings.launch(
                        CallNotificationPermissionManager.miuiPermissionSettingsIntent(context)
                    )

                else -> continueCall()
            }
        }

        val runtimePermissionLauncher = rememberCallLauncher(peerId) { id, type ->
            pendingCall = id to type
            val specialPermissionsReady =
                CallNotificationPermissionManager.canUseFullScreenIntent(context) &&
                    CallNotificationPermissionManager.canDisplayOverOtherApps(context)
            if (specialPermissionsReady) {
                continueCall()
            } else {
                showRationale = true
            }
        }

        if (showRationale) {
            AlertDialog(
                onDismissRequest = {
                    showRationale = false
                    continueCall()
                },
                title = { Text("完善来电提醒") },
                text = {
                    Text("开启全屏通知和后台弹出权限后，锁屏、息屏或使用其他应用时也能及时看到来电。")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showRationale = false
                            requestSpecialCallPermissions()
                        }
                    ) {
                        Text("去开启")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRationale = false
                            continueCall()
                        }
                    ) {
                        Text("暂不")
                    }
                }
            )
        }

        return runtimePermissionLauncher
    }

}

/**
 * 默认动画配置
 */
private const val TRANSITION_DURATION_MILLISECOND = 300
private val TRANSITION_ANIMATION_SPEC = tween<IntOffset>(
    durationMillis = TRANSITION_DURATION_MILLISECOND
)

private fun createEnterTransition() = slideInHorizontally(
    initialOffsetX = { it },
    animationSpec = TRANSITION_ANIMATION_SPEC
) togetherWith slideOutHorizontally(
    targetOffsetX = { -it },
    animationSpec = TRANSITION_ANIMATION_SPEC
)

private fun createExitTransition() = slideInHorizontally(
    initialOffsetX = { -it },
    animationSpec = TRANSITION_ANIMATION_SPEC
) togetherWith slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = TRANSITION_ANIMATION_SPEC
)
