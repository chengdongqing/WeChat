package top.chengdongqing.wechat.feature.profile.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.NavigationKey
import top.chengdongqing.wechat.feature.profile.ui.profile.ProfileScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.edit.EditAvatarScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.edit.EditGenderScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.edit.EditIDScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.edit.EditNameScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.edit.EditSignatureScreen
import top.chengdongqing.wechat.feature.profile.ui.qrcode.QRCodeScreen

fun EntryProviderScope<NavKey>.meNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    // 个人资料页
    entry<NavigationKey.Profile> {
        ProfileScreen(
            backStack = backStack,
            onBack = onBack
        )
    }

    // 二维码页
    entry<NavigationKey.QrCode> {
        WeTheme(isDark = false) {
            QRCodeScreen(
                onBack = onBack,
                onNavigateToContactDetail = { id ->
                    backStack.add(NavigationKey.ContactDetail(id))
                },
                onNavigateToPlainText = { text ->
                    backStack.add(NavigationKey.PlainText(text))
                },
                onNavigateToWebView = { url ->
                    backStack.add(NavigationKey.WebView(url))
                }
            )
        }
    }

    // 编辑页
    entry<NavigationKey.EditAvatar> { EditAvatarScreen(onBack) }
    entry<NavigationKey.EditId> { EditIDScreen(onBack) }
    entry<NavigationKey.EditName> { EditNameScreen(onBack) }
    entry<NavigationKey.EditSignature> { EditSignatureScreen(onBack) }
    entry<NavigationKey.EditGender> { EditGenderScreen(onBack) }
}
