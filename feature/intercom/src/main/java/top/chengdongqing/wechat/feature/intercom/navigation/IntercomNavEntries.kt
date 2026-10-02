package top.chengdongqing.wechat.feature.intercom.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.navigation.ScreenRoute
import top.chengdongqing.wechat.feature.intercom.ui.IntercomLobbyScreen
import top.chengdongqing.wechat.feature.intercom.ui.IntercomRoomScreen

fun EntryProviderScope<NavKey>.intercomNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<ScreenRoute.IntercomLobby> {
        IntercomLobbyScreen(
            onBack = onBack,
            onJoinChannel = { channel ->
                backStack.add(ScreenRoute.IntercomRoom(channel))
            }
        )
    }
    entry<ScreenRoute.IntercomRoom> {
        IntercomRoomScreen(
            channel = it.channel,
            onBack = onBack
        )
    }
}
