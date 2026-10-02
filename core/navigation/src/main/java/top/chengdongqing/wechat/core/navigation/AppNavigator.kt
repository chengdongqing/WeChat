package top.chengdongqing.wechat.core.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

interface AppNavigator {
    val backStack: NavBackStack<NavKey>
    fun navigateTo(route: ScreenRoute)
    fun back()
    fun clear()
}

val LocalAppNavigator = staticCompositionLocalOf<AppNavigator> {
    error("AppNavigator not provided!")
}