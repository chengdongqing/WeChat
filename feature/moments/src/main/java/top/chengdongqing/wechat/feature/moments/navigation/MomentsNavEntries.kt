package top.chengdongqing.wechat.feature.moments.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.navigation.NavigationKey
import top.chengdongqing.wechat.feature.moments.ui.cover.ChangeMomentCoverScreen
import top.chengdongqing.wechat.feature.moments.ui.cover.PhotographerCoversScreen
import top.chengdongqing.wechat.feature.moments.ui.create.CreateMomentScreen
import top.chengdongqing.wechat.feature.moments.ui.feed.MomentsScreen

fun EntryProviderScope<NavKey>.momentsNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<NavigationKey.Moments> {
        MomentsScreen(
            onBack = onBack,
            onCreate = { backStack.add(NavigationKey.CreateMoment) },
            onChangeCover = { backStack.add(NavigationKey.ChangeMomentCover) }
        )
    }
    entry<NavigationKey.CreateMoment> { CreateMomentScreen(onBack) }
    entry<NavigationKey.ChangeMomentCover> {
        ChangeMomentCoverScreen(
            onBack = onBack,
            onChanged = { backStack.removeLastOrNull() },
            onPhotographerWorks = { backStack.add(NavigationKey.PhotographerCovers) }
        )
    }
    entry<NavigationKey.PhotographerCovers> {
        PhotographerCoversScreen(
            onBack = onBack,
            onChanged = {
                backStack.removeLastOrNull()
                backStack.removeLastOrNull()
            }
        )
    }
}
