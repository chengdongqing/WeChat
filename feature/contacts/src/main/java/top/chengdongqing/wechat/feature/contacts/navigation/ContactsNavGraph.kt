package top.chengdongqing.wechat.feature.contacts.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import top.chengdongqing.wechat.core.common.navigation.ChatRoute
import top.chengdongqing.wechat.core.common.navigation.ContactsRoute
import top.chengdongqing.wechat.core.common.navigation.Screen
import top.chengdongqing.wechat.feature.contacts.ui.add.AddContactScreen
import top.chengdongqing.wechat.feature.contacts.ui.add.nfc.NfcAddContactScreen
import top.chengdongqing.wechat.feature.contacts.ui.add.pincode.PinCodeGroupScreen
import top.chengdongqing.wechat.feature.contacts.ui.add.radar.RadarScanScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.ContactDetailScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.profile.ContactProfileScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.profile.edit.EditContactProfileScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.setting.ContactSettingScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.NewContactsScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.request.RequestAddScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.verify.AcceptVerifyScreen

fun NavGraphBuilder.contactsNavGraph(navController: NavHostController, onBack: () -> Unit) {
    composable(ContactsRoute.AddContact.route) {
        AddContactScreen(
            onBack = onBack,
            onNavigateToNFC = {
                navController.navigate(ContactsRoute.NFC.route)
            },
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
    composable(ContactsRoute.NFC.route) {
        NfcAddContactScreen(
            onBack = onBack,
            onNavigateToContact = { id ->
                navController.navigate(ContactsRoute.Detail.createRoute(id))
            }
        )
    }
    composable(ContactsRoute.RadarScan.route) {
        RadarScanScreen(
            onBack = onBack,
            onNavigateToContact = { id ->
                navController.navigate(ContactsRoute.Detail.createRoute(id))
            }
        )
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
            onNavigateToProfile = { id ->
                navController.navigate(ContactsRoute.Profile.createRoute(id))
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
        ContactSettingScreen(
            contactId = contactId,
            onBack = onBack,
            onDelete = {
                navController.popBackStack(Screen.Home.route, inclusive = false)
            },
            onNavigateToContactProfile = {
                navController.navigate(ContactsRoute.ProfileEdit.createRoute(contactId))
            }
        )
    }
    composable(
        route = ContactsRoute.Profile.route,
        arguments = listOf(
            navArgument(ContactsRoute.Profile.ARG_CONTACT_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val contactId =
            backStackEntry.arguments?.getString(ContactsRoute.Profile.ARG_CONTACT_ID) ?: ""
        ContactProfileScreen(
            contactId,
            onBack,
            onNavigateToEdit = {
                navController.navigate(ContactsRoute.ProfileEdit.createRoute(contactId))
            }
        )
    }
    composable(
        route = ContactsRoute.ProfileEdit.route,
        arguments = listOf(
            navArgument(ContactsRoute.ProfileEdit.ARG_CONTACT_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val contactId =
            backStackEntry.arguments?.getString(ContactsRoute.ProfileEdit.ARG_CONTACT_ID) ?: ""
        EditContactProfileScreen(contactId, onBack)
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
        RequestAddScreen(
            contactId = contactId,
            onBack = onBack,
            onSuccess = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        )
    }
    composable(
        route = ContactsRoute.AcceptVerify.route,
        arguments = listOf(
            navArgument(ContactsRoute.AcceptVerify.ARG_REQUEST_ID) {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val contactId = backStackEntry.arguments?.getString(
            ContactsRoute.AcceptVerify.ARG_REQUEST_ID
        ) ?: ""
        AcceptVerifyScreen(contactId, onBack, onBack)
    }

    composable(ContactsRoute.NewFriends.route) {
        NewContactsScreen(
            onBack = onBack,
            onNavigateToAdd = {
                navController.navigate(ContactsRoute.AddContact.route)
            },
            onNavigateToVerify = { id ->
                navController.navigate(ContactsRoute.AcceptVerify.createRoute(id))
            }
        )
    }
}