package top.chengdongqing.wechat.feature.profile.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.ScreenRoute
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
    entry<ScreenRoute.Profile> {
        ProfileScreen(
            backStack = backStack,
            onBack = onBack
        )
    }

    // 二维码页
    entry<ScreenRoute.QrCode> {
        WeTheme(isDark = false) {
            QRCodeScreen(
                onBack = onBack,
                onContactDetail = { id ->
                    backStack.add(ScreenRoute.ContactDetail(id))
                },
                onPlainText = { text ->
                    backStack.add(ScreenRoute.PlainText(text))
                },
                onWebView = { url ->
                    backStack.add(ScreenRoute.WebView(url))
                }
            )
        }
    }

    // 编辑页
    entry<ScreenRoute.EditAvatar> { EditAvatarScreen(onBack) }
    entry<ScreenRoute.EditId> { EditIDScreen(onBack) }
    entry<ScreenRoute.EditName> { EditNameScreen(onBack) }
    entry<ScreenRoute.EditSignature> { EditSignatureScreen(onBack) }
    entry<ScreenRoute.EditGender> { EditGenderScreen(onBack) }
    entry<ScreenRoute.Services> {
        ServicesScreen(
            onBack = onBack,
            onPaymentCode = { backStack.add(ScreenRoute.Money) },
            onWallet = { backStack.add(ScreenRoute.Wallet) },
            onBills = { backStack.add(ScreenRoute.PaymentBills) }
        )
    }
    entry<ScreenRoute.Wallet> {
        WalletScreen(
            onBack = onBack,
            onBalance = { backStack.add(ScreenRoute.WalletBalance) },
            onCards = { backStack.add(ScreenRoute.BankCards) },
            onBills = { backStack.add(ScreenRoute.PaymentBills) }
        )
    }
    entry<ScreenRoute.WalletBalance> {
        WalletSubScreen("零钱", "当前余额 ¥0.00，可用于转账和支付。", onBack)
    }
    entry<ScreenRoute.BankCards> {
        WalletSubScreen("银行卡", "尚未添加银行卡。", onBack)
    }
    entry<ScreenRoute.PaymentBills> {
        WalletSubScreen("账单", "暂无支付账单。", onBack)
    }
    entry<ScreenRoute.Money> {
        PaymentCodeScreen(onBack)
    }
}
