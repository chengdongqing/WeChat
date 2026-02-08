package top.chengdongqing.wechat.features.contacts.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import top.chengdongqing.wechat.features.chat.navigation.ChatRoute
import top.chengdongqing.wechat.features.contacts.ui.addfriend.AddFriendScreen
import top.chengdongqing.wechat.features.contacts.ui.addfriend.pincode.PinCodeGroupScreen
import top.chengdongqing.wechat.features.contacts.ui.addfriend.radar.RadarScanScreen
import top.chengdongqing.wechat.features.contacts.ui.detail.ContactDetailScreen
import top.chengdongqing.wechat.features.contacts.ui.detail.setting.ContactSettingScreen
import top.chengdongqing.wechat.features.contacts.ui.newfirends.NewFriendsScreen
import top.chengdongqing.wechat.features.contacts.ui.requestadd.RequestAddScreen

sealed class ContactsRoute(val route: String) {
    object AddFriend : ContactsRoute("contacts/add")
    object RadarScan : ContactsRoute("contacts/add/radar_scan")
    object PinCodeGroup : ContactsRoute("contacts/add/pin_code_group")

    object ContactDetail : ContactsRoute("contacts/{contactId}") {
        const val ARG_CONTACT_ID = "contactId"

        fun createRoute(contactId: String) = "contacts/${contactId}"
    }

    object ContactSetting : ContactsRoute("contacts/{contactId}/setting") {
        const val ARG_CONTACT_ID = "contactId"

        fun createRoute(contactId: String) = "contacts/${contactId}/setting"
    }

    object ContactRequestAdd : ContactsRoute("contacts/{contactId}/request_add") {
        const val ARG_CONTACT_ID = "contactId"

        fun createRoute(contactId: String) = "contacts/${contactId}/request_add"
    }

    object NewFriends : ContactsRoute("contacts/new_friends")
}

fun NavGraphBuilder.contactsNavGraph(navController: NavHostController, onBack: () -> Unit) {
    composable(ContactsRoute.AddFriend.route) {
        AddFriendScreen(
            onNavigateToRadar = {
                navController.navigate(ContactsRoute.RadarScan.route)
            },
            onNavigateToGroup = {
                navController.navigate(ContactsRoute.PinCodeGroup.route)
            },
            onBack = onBack
        )
    }
    composable(ContactsRoute.RadarScan.route) {
        RadarScanScreen(onBack)
    }
    composable(ContactsRoute.PinCodeGroup.route) {
        PinCodeGroupScreen(onBack)
    }

    composable(
        route = ContactsRoute.ContactDetail.route,
        arguments = listOf(
            navArgument(ContactsRoute.ContactDetail.ARG_CONTACT_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val contactId =
            backStackEntry.arguments?.getString(ContactsRoute.ContactDetail.ARG_CONTACT_ID) ?: ""
        ContactDetailScreen(
            contactId = contactId,
            onBack = onBack,
            onNavigateToChat = { id ->
                navController.navigate(ChatRoute.ChatSession.createRoute(id))
            },
            onNavigateToSetting = { id ->
                navController.navigate(ContactsRoute.ContactSetting.createRoute(id))
            },
            onNavigateToRequestAdd = { id ->
                navController.navigate(ContactsRoute.ContactRequestAdd.createRoute(id))
            }
        )
    }
    composable(
        route = ContactsRoute.ContactSetting.route,
        arguments = listOf(
            navArgument(ContactsRoute.ContactSetting.ARG_CONTACT_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val contactId =
            backStackEntry.arguments?.getString(ContactsRoute.ContactSetting.ARG_CONTACT_ID) ?: ""
        ContactSettingScreen(contactId, onBack)
    }
    composable(
        route = ContactsRoute.ContactRequestAdd.route,
        arguments = listOf(
            navArgument(ContactsRoute.ContactRequestAdd.ARG_CONTACT_ID) {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val contactId = backStackEntry.arguments?.getString(
            ContactsRoute.ContactRequestAdd.ARG_CONTACT_ID
        ) ?: ""
        RequestAddScreen(contactId, onBack)
    }

    composable(ContactsRoute.NewFriends.route) {
        NewFriendsScreen(
            onBack = onBack,
            onNavigateToAdd = {
                navController.navigate(ContactsRoute.AddFriend.route)
            },
            onNavigateToVerify = {

            }
        )
    }
}