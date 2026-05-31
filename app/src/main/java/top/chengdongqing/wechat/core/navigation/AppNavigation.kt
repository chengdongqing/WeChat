package top.chengdongqing.wechat.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import top.chengdongqing.wechat.feature.chat.navigation.chatNavEntries
import top.chengdongqing.wechat.feature.contacts.navigation.contactsNavEntries
import top.chengdongqing.wechat.feature.profile.navigation.meNavEntries
import top.chengdongqing.wechat.feature.settings.navigation.settingsNavEntries

@Composable
fun AppNavigation(backStack: NavBackStack<NavKey>) {
    val goBack: () -> Unit = {
        backStack.removeLastOrNull()
    }

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
            settingsNavEntries(backStack, goBack)
        }
    )
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