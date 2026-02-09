package top.chengdongqing.wechat.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import top.chengdongqing.wechat.features.startup.WelcomeScreen

sealed class Screen(val route: String) {
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
    startDestination: String = Screen.Welcome.route,
    navController: NavHostController = rememberNavController()
) {
    // 页面返回
    val goBack: () -> Unit = {
        navController.navigateUp()
    }

    WeNavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 欢迎页
        composable(Screen.Welcome.route) {
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
                        animationSpec = tween(700)
                    ) + scaleOut(
                        targetScale = 1.08f,
                        animationSpec = tween(700)
                    )
                } else null
            }
        ) {
            ProfileSetupScreen(onBack = goBack, onSetupComplete = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Welcome.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            })
        }

        // 主页
        composable(
            route = Screen.Home.route,
            enterTransition = {
                if (initialState.destination.route == Screen.ProfileSetup.route) {
                    fadeIn(animationSpec = tween(700)) +
                            scaleIn(
                                initialScale = 0.92f,
                                animationSpec = tween(700)
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

        chatNavGraph(navController, goBack)
        contactsNavGraph(navController, goBack)
        meNavGraph(navController, goBack)

        composable(
            route = Screen.PlainText.route,
            arguments = listOf(
                navArgument(Screen.PlainText.ARG_TEXT) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val text = backStackEntry.arguments?.getString(Screen.PlainText.ARG_TEXT) ?: ""
            PlainTextScreen(text.decode(), goBack)
        }
        composable(
            route = Screen.WebView.route,
            arguments = listOf(
                navArgument(Screen.WebView.ARG_URL) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString(Screen.WebView.ARG_URL) ?: ""
            WebViewScreen(url.decode(), goBack)
        }
    }
}