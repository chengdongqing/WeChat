package top.chengdongqing.wechat.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.NavDisplay
import top.chengdongqing.wechat.feature.common.PlainTextScreen
import top.chengdongqing.wechat.feature.common.WebViewScreen
import top.chengdongqing.wechat.feature.profile.ui.login.LoginScreen
import top.chengdongqing.wechat.feature.startup.GuideScreen
import top.chengdongqing.wechat.feature.startup.SplashScreen

internal fun EntryProviderScope<NavKey>.commonNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    // 启动页
    entry<NavigationKey.Splash>(
        metadata = NavDisplay.transitionSpec {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) {
        SplashScreen(
            onNavigateToHome = {
                backStack.clear()
                backStack.add(NavigationKey.Home)
            },
            onNavigateToWelcome = {
                backStack.clear()
                backStack.add(NavigationKey.Guide)
            }
        )
    }

    // 欢迎页
    entry<NavigationKey.Guide>(
        metadata = NavDisplay.transitionSpec {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) {
        GuideScreen(
            onNavigateToSetup = {
                backStack.add(NavigationKey.Login)
            },
            onNavigateToLanguage = {
                backStack.add(NavigationKey.LanguageSettings)
            }
        )
    }

    // 登录页
    entry<NavigationKey.Login> {
        LoginScreen(
            onBack = onBack,
            onSetupComplete = {
                backStack.clear()
                backStack.add(NavigationKey.Home)
            }
        )
    }

    // 首页
    entry<NavigationKey.Home>(
        metadata = metadata {
            put(NavDisplay.TransitionKey) {
                (fadeIn(animationSpec = tween(300)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(300)
                        )) togetherWith ExitTransition.KeepUntilTransitionsFinished
            }
        }
    ) {
        HomeDestination(backStack)
    }

    // 文本预览
    entry<NavigationKey.PlainText> {
        PlainTextScreen(it.text, onBack)
    }

    // 网页预览
    entry<NavigationKey.WebView> {
        WebViewScreen(it.url, onBack)
    }
}
