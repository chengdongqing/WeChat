package top.chengdongqing.wechat.feature.moments.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.navigation.NavigationKey
import top.chengdongqing.wechat.feature.moments.ui.cover.ChangeMomentCoverScreen
import top.chengdongqing.wechat.feature.moments.ui.cover.PhotographerWorksScreen
import top.chengdongqing.wechat.feature.moments.ui.list.MomentsScreen
import top.chengdongqing.wechat.feature.moments.ui.post.CreateMomentScreen

fun EntryProviderScope<NavKey>.momentsNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<NavigationKey.Moments> {
        MomentsScreen(
            onBack = onBack,
            onNavigateToPost = { backStack.add(NavigationKey.CreateMoment) },
            onNavigateToCover = { backStack.add(NavigationKey.ChangeMomentCover) }
        )
    }
    entry<NavigationKey.CreateMoment> {
        CreateMomentScreen(onBack)
    }
    entry<NavigationKey.ChangeMomentCover> {
        ChangeMomentCoverScreen(
            onBack = onBack,
            onNavigateToPhotographerWorks = { backStack.add(NavigationKey.PhotographerCovers) }
        )
    }
    entry<NavigationKey.PhotographerCovers> {
        PhotographerWorksScreen(
            onBack = onBack,
            onChanged = {
                repeat(2) { onBack() }
            }
        )
    }
}
