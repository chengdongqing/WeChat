package top.chengdongqing.wechat.feature.profile.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.common.navigation.CommonKey
import top.chengdongqing.wechat.core.common.navigation.ContactsKey
import top.chengdongqing.wechat.core.common.navigation.MeKey
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
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
    entry<MeKey.Profile> {
        ProfileScreen(
            backStack = backStack,
            onBack = onBack
        )
    }

    // 二维码页
    entry<MeKey.QrCode> {
        WeTheme(isDark = false) {
            QRCodeScreen(
                onBack = onBack,
                onNavigateToContactDetail = { id ->
                    backStack.add(ContactsKey.Detail(id))
                },
                onNavigateToPlainText = { text ->
                    backStack.add(CommonKey.PlainText(text))
                },
                onNavigateToWebView = { url ->
                    backStack.add(CommonKey.WebView(url))
                }
            )
        }
    }

    // 编辑页
    entry<MeKey.EditAvatar> { EditAvatarScreen(onBack) }
    entry<MeKey.EditId> { EditIDScreen(onBack) }
    entry<MeKey.EditName> { EditNameScreen(onBack) }
    entry<MeKey.EditSignature> { EditSignatureScreen(onBack) }
    entry<MeKey.EditGender> { EditGenderScreen(onBack) }
}