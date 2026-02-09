package top.chengdongqing.wechat.features.contacts.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import top.chengdongqing.wechat.core.navigation.Screen
import top.chengdongqing.wechat.features.chat.navigation.ChatRoute
import top.chengdongqing.wechat.features.contacts.ui.addfriend.AddFriendScreen
import top.chengdongqing.wechat.features.contacts.ui.addfriend.newfirends.AcceptVerifyScreen
import top.chengdongqing.wechat.features.contacts.ui.addfriend.newfirends.NewFriendsScreen
import top.chengdongqing.wechat.features.contacts.ui.addfriend.newfirends.RequestAddScreen
import top.chengdongqing.wechat.features.contacts.ui.addfriend.pincode.PinCodeGroupScreen
import top.chengdongqing.wechat.features.contacts.ui.addfriend.radar.RadarScanScreen
import top.chengdongqing.wechat.features.contacts.ui.detail.ContactDetailScreen
import top.chengdongqing.wechat.features.contacts.ui.detail.setting.ContactSettingScreen

sealed class ContactsRoute(val route: String) {
    object AddFriend : ContactsRoute("contacts/add")
    object RadarScan : ContactsRoute("contacts/add/radar_scan")
    object PinCodeGroup : ContactsRoute("contacts/add/pin_code_group")

    object Detail : ContactsRoute("contacts/{contactId}") {
        const val ARG_CONTACT_ID = "contactId"

        fun createRoute(contactId: String) = "contacts/${contactId}"
    }

    object Setting : ContactsRoute("contacts/{contactId}/setting") {
        const val ARG_CONTACT_ID = "contactId"

        fun createRoute(contactId: String) = "contacts/${contactId}/setting"
    }

    object RequestAdd : ContactsRoute("contacts/{contactId}/request_add") {
        const val ARG_CONTACT_ID = "contactId"

        fun createRoute(contactId: String) = "contacts/${contactId}/request_add"
    }

    object AcceptVerify : ContactsRoute("contacts/{contactId}/accept_verify") {
        const val ARG_CONTACT_ID = "contactId"

        fun createRoute(contactId: String) = "contacts/${contactId}/accept_verify"
    }

    object NewFriends : ContactsRoute("contacts/new_friends")
}

fun NavGraphBuilder.contactsNavGraph(navController: NavHostController, onBack: () -> Unit) {
    composable(ContactsRoute.AddFriend.route) {
        AddFriendScreen(
            onBack = onBack,
            onNavigateToRadar = {
                navController.navigate(ContactsRoute.RadarScan.route)
            },
            onNavigateToGroup = {
                navController.navigate(ContactsRoute.PinCodeGroup.route)
            },
            onNavigateToContactDetail = { id ->
                navController.navigate(ContactsRoute.Detail.createRoute(id))
            },
            onNavigateToPlainText = { text ->
                navController.navigate(Screen.PlainText.createRoute(text))
            },
            onNavigateToWebView = { url ->
                navController.navigate(Screen.WebView.createRoute(url))
            }
        )
    }
    composable(ContactsRoute.RadarScan.route) {
        RadarScanScreen(onBack)
    }
    composable(ContactsRoute.PinCodeGroup.route) {
        PinCodeGroupScreen(onBack)
    }

    composable(
        route = ContactsRoute.Detail.route,
        arguments = listOf(
            navArgument(ContactsRoute.Detail.ARG_CONTACT_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val contactId =
            backStackEntry.arguments?.getString(ContactsRoute.Detail.ARG_CONTACT_ID) ?: ""
        ContactDetailScreen(
            contactId = contactId,
            onBack = onBack,
            onNavigateToChat = { id ->
                navController.navigate(ChatRoute.ChatSession.createRoute(id))
            },
            onNavigateToSetting = { id ->
                navController.navigate(ContactsRoute.Setting.createRoute(id))
            },
            onNavigateToRequestAdd = { id ->
                navController.navigate(ContactsRoute.RequestAdd.createRoute(id))
            }
        )
    }
    composable(
        route = ContactsRoute.Setting.route,
        arguments = listOf(
            navArgument(ContactsRoute.Setting.ARG_CONTACT_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val contactId =
            backStackEntry.arguments?.getString(ContactsRoute.Setting.ARG_CONTACT_ID) ?: ""
        ContactSettingScreen(contactId, onBack)
    }
    composable(
        route = ContactsRoute.RequestAdd.route,
        arguments = listOf(
            navArgument(ContactsRoute.RequestAdd.ARG_CONTACT_ID) {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val contactId = backStackEntry.arguments?.getString(
            ContactsRoute.RequestAdd.ARG_CONTACT_ID
        ) ?: ""
        RequestAddScreen(contactId, onBack)
    }
    composable(
        route = ContactsRoute.AcceptVerify.route,
        arguments = listOf(
            navArgument(ContactsRoute.AcceptVerify.ARG_CONTACT_ID) {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val contactId = backStackEntry.arguments?.getString(
            ContactsRoute.AcceptVerify.ARG_CONTACT_ID
        ) ?: ""
        AcceptVerifyScreen(contactId, onBack)
    }

    composable(ContactsRoute.NewFriends.route) {
        NewFriendsScreen(
            onBack = onBack,
            onNavigateToAdd = {
                navController.navigate(ContactsRoute.AddFriend.route)
            },
            onNavigateToVerify = {
                navController.navigate(ContactsRoute.AcceptVerify.route)
            }
        )
    }
}