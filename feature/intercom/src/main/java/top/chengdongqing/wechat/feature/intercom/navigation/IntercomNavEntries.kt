package top.chengdongqing.wechat.feature.intercom.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.navigation.NavigationKey
import top.chengdongqing.wechat.feature.intercom.ui.IntercomLobbyScreen
import top.chengdongqing.wechat.feature.intercom.ui.IntercomRoomScreen

fun EntryProviderScope<NavKey>.intercomNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<NavigationKey.IntercomLobby> {
        IntercomLobbyScreen(
            onBack = onBack,
            onJoinChannel = { channel ->
                backStack.add(NavigationKey.IntercomRoom(channel))
            }
        )
    }
    entry<NavigationKey.IntercomRoom> {
        IntercomRoomScreen(
            channel = it.channel,
            onBack = onBack
        )
    }
}
