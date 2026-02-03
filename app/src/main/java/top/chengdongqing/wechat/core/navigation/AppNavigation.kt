package top.chengdongqing.wechat.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import top.chengdongqing.wechat.features.chat.navigation.chatNavGraph
import top.chengdongqing.wechat.features.contacts.navigation.contactsNavGraph
import top.chengdongqing.wechat.features.home.ui.HomeScreen
import top.chengdongqing.wechat.features.me.navigation.meNavGraph
import top.chengdongqing.wechat.features.me.ui.setup.ProfileSetupScreen
import top.chengdongqing.wechat.features.startup.WelcomeScreen

object Screen {
    const val WELCOME = "welcome"
    const val PROFILE_SETUP = "profile_setup"
    const val HOME = "home"
}

@Composable
fun AppNavigation(
    startDestination: String = Screen.WELCOME,
    navController: NavHostController = rememberNavController()
) {
    // 页面返回
    val goBack: () -> Unit = {
        navController.popBackStack()
    }

    WeNavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 欢迎页
        composable(Screen.WELCOME) {
            WelcomeScreen(onNavigateToSetup = {
                navController.navigate(Screen.PROFILE_SETUP)
            })
        }

        // 资料设置页
        composable(
            route = Screen.PROFILE_SETUP,
            exitTransition = {
                if (targetState.destination.route == Screen.HOME) {
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
                navController.navigate(Screen.HOME) {
                    popUpTo(Screen.WELCOME) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            })
        }

        // 主页
        composable(
            route = Screen.HOME,
            enterTransition = {
                if (initialState.destination.route == Screen.PROFILE_SETUP) {
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
    }
}