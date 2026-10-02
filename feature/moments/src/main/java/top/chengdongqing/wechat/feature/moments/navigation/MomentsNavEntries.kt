package top.chengdongqing.wechat.feature.moments.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.navigation.ScreenRoute
import top.chengdongqing.wechat.feature.moments.ui.cover.ChangeMomentCoverScreen
import top.chengdongqing.wechat.feature.moments.ui.cover.PhotographerWorksScreen
import top.chengdongqing.wechat.feature.moments.ui.list.MomentsScreen
import top.chengdongqing.wechat.feature.moments.ui.post.PostMomentRoute

fun EntryProviderScope<NavKey>.momentsNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<ScreenRoute.Moments> {
        MomentsScreen(
            onBack = onBack,
            onPost = { backStack.add(ScreenRoute.PostMoment) },
            onCover = { backStack.add(ScreenRoute.ChangeMomentCover) }
        )
    }
    entry<ScreenRoute.PostMoment> {
        PostMomentRoute(onBack)
    }
    entry<ScreenRoute.ChangeMomentCover> {
        ChangeMomentCoverScreen(
            onBack = onBack,
            onPhotographerWorks = { backStack.add(ScreenRoute.PhotographerCovers) }
        )
    }
    entry<ScreenRoute.PhotographerCovers> {
        PhotographerWorksScreen(
            onBack = onBack,
            onChanged = {
                repeat(2) { onBack() }
            }
        )
    }
}
