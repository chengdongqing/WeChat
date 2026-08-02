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
import top.chengdongqing.wechat.feature.profile.ui.services.PaymentCodeScreen
import top.chengdongqing.wechat.feature.profile.ui.services.ServicesScreen
import top.chengdongqing.wechat.feature.profile.ui.services.WalletScreen
import top.chengdongqing.wechat.feature.profile.ui.services.WalletSubScreen

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
    entry<NavigationKey.Services> {
        ServicesScreen(
            onBack = onBack,
            onPaymentCode = { backStack.add(NavigationKey.PaymentCode) },
            onWallet = { backStack.add(NavigationKey.Wallet) },
            onBills = { backStack.add(NavigationKey.PaymentBills) }
        )
    }
    entry<NavigationKey.Wallet> {
        WalletScreen(
            onBack = onBack,
            onBalance = { backStack.add(NavigationKey.WalletBalance) },
            onCards = { backStack.add(NavigationKey.BankCards) },
            onBills = { backStack.add(NavigationKey.PaymentBills) }
        )
    }
    entry<NavigationKey.WalletBalance> {
        WalletSubScreen("零钱", "当前余额 ¥0.00，可用于转账和支付。", onBack)
    }
    entry<NavigationKey.BankCards> {
        WalletSubScreen("银行卡", "尚未添加银行卡。", onBack)
    }
    entry<NavigationKey.PaymentBills> {
        WalletSubScreen("账单", "暂无支付账单。", onBack)
    }
    entry<NavigationKey.PaymentCode> {
        PaymentCodeScreen(onBack)
    }
}
