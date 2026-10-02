package top.chengdongqing.wechat.feature.favorites.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.navigation.ScreenRoute
import top.chengdongqing.wechat.feature.favorites.ui.FavoriteEditorScreen
import top.chengdongqing.wechat.feature.favorites.ui.FavoritesScreen

fun EntryProviderScope<NavKey>.favoritesNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<ScreenRoute.Favorites> {
        FavoritesScreen(
            targetChatId = it.targetChatId,
            onBack = onBack,
            onCreate = { backStack.add(ScreenRoute.FavoriteEditor()) },
            onOpen = { id -> backStack.add(ScreenRoute.FavoriteEditor(id)) }
        )
    }
    entry<ScreenRoute.FavoriteEditor> {
        FavoriteEditorScreen(it.favoriteId, onBack)
    }
}
