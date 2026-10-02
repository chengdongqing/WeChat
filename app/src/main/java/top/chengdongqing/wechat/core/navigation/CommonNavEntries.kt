package top.chengdongqing.wechat.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import top.chengdongqing.wechat.feature.auth.ui.LoginRoute
import top.chengdongqing.wechat.feature.common.PlainTextScreen
import top.chengdongqing.wechat.feature.common.WebViewScreen
import top.chengdongqing.wechat.feature.startup.GuideRoute
import top.chengdongqing.wechat.feature.startup.LaunchRoute
import top.chengdongqing.wechat.home.HomeRoute

internal fun EntryProviderScope<NavKey>.commonNavEntries(
    onBack: () -> Unit
) {
    // 启动页
    entry<ScreenRoute.Launch>(
        metadata = NavDisplay.transitionSpec {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) {
        LaunchRoute()
    }

    // 欢迎页
    entry<ScreenRoute.Guide>(
        metadata = NavDisplay.transitionSpec {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) {
        GuideRoute()
    }

    // 登录页
    entry<ScreenRoute.Login> {
        LoginRoute()
    }

    // 应用主框架
    entry<ScreenRoute.Home>(
        metadata = NavDisplay.transitionSpec {
            (fadeIn(animationSpec = tween(300)) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(300)
                    )) togetherWith ExitTransition.KeepUntilTransitionsFinished
        }
    ) {
        HomeRoute()
    }

    // 文本预览
    entry<ScreenRoute.PlainText> {
        PlainTextScreen(it.text, onBack)
    }

    // 网页预览
    entry<ScreenRoute.WebView> {
        WebViewScreen(it.url, onBack)
    }
}
