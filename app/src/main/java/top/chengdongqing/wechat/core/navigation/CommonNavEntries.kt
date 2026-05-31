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
import top.chengdongqing.wechat.core.common.navigation.CommonKey
import top.chengdongqing.wechat.core.common.navigation.SettingsKey
import top.chengdongqing.wechat.feature.common.PlainTextScreen
import top.chengdongqing.wechat.feature.common.WebViewScreen
import top.chengdongqing.wechat.feature.home.theme.HomeTheme
import top.chengdongqing.wechat.feature.home.ui.HomeScreen
import top.chengdongqing.wechat.feature.profile.ui.setup.ProfileSetupScreen
import top.chengdongqing.wechat.feature.startup.SplashScreen
import top.chengdongqing.wechat.feature.startup.WelcomeScreen

internal fun EntryProviderScope<NavKey>.commonNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    // 启动页
    entry<CommonKey.Splash>(
        metadata = NavDisplay.transitionSpec {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) {
        SplashScreen(
            onNavigateToHome = {
                backStack.clear()
                backStack.add(CommonKey.Home)
            },
            onNavigateToWelcome = {
                backStack.clear()
                backStack.add(CommonKey.Welcome)
            }
        )
    }

    // 欢迎页
    entry<CommonKey.Welcome>(
        metadata = NavDisplay.transitionSpec {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) {
        WelcomeScreen(
            onNavigateToSetup = {
                backStack.add(CommonKey.Setup)
            },
            onNavigateToLanguage = {
                backStack.add(SettingsKey.Language)
            }
        )
    }

    // 资料设置页
    entry<CommonKey.Setup> {
        ProfileSetupScreen(
            onBack = onBack,
            onSetupComplete = {
                backStack.clear()
                backStack.add(CommonKey.Home)
            }
        )
    }

    // 首页
    entry<CommonKey.Home>(
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
        HomeTheme {
            HomeScreen(backStack)
        }
    }

    // 文本预览
    entry<CommonKey.PlainText> {
        PlainTextScreen(it.text, onBack)
    }

    // 网页预览
    entry<CommonKey.WebView> {
        WebViewScreen(it.url, onBack)
    }
}