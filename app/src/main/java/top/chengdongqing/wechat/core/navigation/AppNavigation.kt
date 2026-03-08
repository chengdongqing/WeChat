package top.chengdongqing.wechat.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import top.chengdongqing.wechat.core.util.decode
import top.chengdongqing.wechat.core.util.encode
import top.chengdongqing.wechat.features.chat.navigation.chatNavGraph
import top.chengdongqing.wechat.features.common.PlainTextScreen
import top.chengdongqing.wechat.features.common.WebViewScreen
import top.chengdongqing.wechat.features.contacts.navigation.contactsNavGraph
import top.chengdongqing.wechat.features.home.ui.HomeScreen
import top.chengdongqing.wechat.features.me.navigation.meNavGraph
import top.chengdongqing.wechat.features.me.ui.setup.ProfileSetupScreen
import top.chengdongqing.wechat.features.settings.navigation.settingsNavGraph
import top.chengdongqing.wechat.features.startup.SplashScreen
import top.chengdongqing.wechat.features.startup.WelcomeScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object ProfileSetup : Screen("profile_setup")
    object Home : Screen("home")

    object PlainText : Screen("plain_text/{text}") {
        const val ARG_TEXT = "text"

        fun createRoute(text: String): String {
            return "plain_text/${text.encode()}"
        }
    }

    data object WebView : Screen("webview/{url}") {
        const val ARG_URL = "url"

        fun createRoute(url: String): String {
            return "webview/${url.encode()}"
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    // 页面返回
    val goBack: () -> Unit = {
        navController.navigateUp()
    }

    WeNavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        onboardingNavGraph(navController, goBack)
        homeNavGraph(navController)
        chatNavGraph(navController, goBack)
        contactsNavGraph(navController, goBack)
        meNavGraph(navController, goBack)
        settingsNavGraph(navController, goBack)
        commonNavGraph(goBack)
    }
}

private fun NavGraphBuilder.onboardingNavGraph(
    navController: NavHostController,
    onBack: () -> Unit
) {
    // 启动页
    composable(
        route = Screen.Splash.route,
        exitTransition = { fadeOut(animationSpec = tween(durationMillis = 0)) }
    ) {
        SplashScreen(
            onNavigateToHome = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            onNavigateToWelcome = {
                navController.navigate(Screen.Welcome.route) {
                    popUpTo(Screen.Splash.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        )
    }
    // 欢迎页
    composable(
        route = Screen.Welcome.route,
        enterTransition = { fadeIn(animationSpec = tween(durationMillis = 0)) }
    ) {
        WelcomeScreen(onNavigateToSetup = {
            navController.navigate(Screen.ProfileSetup.route)
        })
    }

    // 资料设置页
    composable(
        route = Screen.ProfileSetup.route,
        exitTransition = {
            if (targetState.destination.route == Screen.Home.route) {
                fadeOut(
                    animationSpec = tween(300)
                ) + scaleOut(
                    targetScale = 1.08f,
                    animationSpec = tween(300)
                )
            } else null
        }
    ) {
        ProfileSetupScreen(onBack = onBack, onSetupComplete = {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Welcome.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        })
    }
}

private fun NavGraphBuilder.homeNavGraph(navController: NavHostController) {
    // 主页
    composable(
        route = Screen.Home.route,
        enterTransition = {
            if (initialState.destination.route == Screen.Splash.route
                || initialState.destination.route == Screen.ProfileSetup.route
            ) {
                fadeIn(animationSpec = tween(300)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(300)
                        )
            } else {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    tween(300)
                )
            }
        }
    ) {
        HomeScreen(navController)
    }
}

private fun NavGraphBuilder.commonNavGraph(onBack: () -> Unit) {
    composable(
        route = Screen.PlainText.route,
        arguments = listOf(
            navArgument(Screen.PlainText.ARG_TEXT) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val text = backStackEntry.arguments?.getString(Screen.PlainText.ARG_TEXT) ?: ""
        PlainTextScreen(text.decode(), onBack)
    }
    composable(
        route = Screen.WebView.route,
        arguments = listOf(
            navArgument(Screen.WebView.ARG_URL) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val url = backStackEntry.arguments?.getString(Screen.WebView.ARG_URL) ?: ""
        WebViewScreen(url.decode(), onBack)
    }
}