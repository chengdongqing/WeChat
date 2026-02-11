package top.chengdongqing.wechat.features.contacts.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import top.chengdongqing.wechat.core.navigation.Screen
import top.chengdongqing.wechat.features.chat.navigation.ChatRoute
import top.chengdongqing.wechat.features.contacts.ui.add.AddContactScreen
import top.chengdongqing.wechat.features.contacts.ui.add.newcontacts.NewContactsScreen
import top.chengdongqing.wechat.features.contacts.ui.add.newcontacts.request.RequestAddScreen
import top.chengdongqing.wechat.features.contacts.ui.add.newcontacts.verify.AcceptVerifyScreen
import top.chengdongqing.wechat.features.contacts.ui.add.pincode.PinCodeGroupScreen
import top.chengdongqing.wechat.features.contacts.ui.add.radar.RadarScanScreen
import top.chengdongqing.wechat.features.contacts.ui.detail.ContactDetailScreen
import top.chengdongqing.wechat.features.contacts.ui.detail.profile.ContactProfileScreen
import top.chengdongqing.wechat.features.contacts.ui.detail.setting.ContactSettingScreen

sealed class ContactsRoute(val route: String) {
    object AddContact : ContactsRoute("contacts/add")
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

    object Profile : ContactsRoute("contacts/{contactId}/profile") {
        const val ARG_CONTACT_ID = "contactId"

        fun createRoute(contactId: String) = "contacts/${contactId}/profile"
    }

    object RequestAdd : ContactsRoute("contacts/{contactId}/request_add") {
        const val ARG_CONTACT_ID = "contactId"

        fun createRoute(contactId: String) = "contacts/${contactId}/request_add"
    }

    object AcceptVerify : ContactsRoute("contacts/{requestId}/accept_verify") {
        const val ARG_REQUEST_ID = "requestId"

        fun createRoute(requestId: String) = "contacts/${requestId}/accept_verify"
    }

    object New : ContactsRoute("contacts/new")
}

fun NavGraphBuilder.contactsNavGraph(navController: NavHostController, onBack: () -> Unit) {
    composable(ContactsRoute.AddContact.route) {
        AddContactScreen(
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
        ContactProfileScreen(contactId, onBack)
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

    composable(
        route = ContactsRoute.New.route,
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "wechat://contacts/new"
            }
        )
    ) {
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