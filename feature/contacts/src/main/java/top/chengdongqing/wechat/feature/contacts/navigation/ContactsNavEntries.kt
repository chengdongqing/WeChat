package top.chengdongqing.wechat.feature.contacts.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.common.navigation.ChatKey
import top.chengdongqing.wechat.core.common.navigation.CommonKey
import top.chengdongqing.wechat.core.common.navigation.ContactsKey
import top.chengdongqing.wechat.feature.contacts.ui.add.AddContactScreen
import top.chengdongqing.wechat.feature.contacts.ui.add.nfc.NfcAddContactScreen
import top.chengdongqing.wechat.feature.contacts.ui.add.pincode.PinCodeGroupScreen
import top.chengdongqing.wechat.feature.contacts.ui.add.radar.RadarScanScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.ContactDetailScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.ContactDetailViewModel
import top.chengdongqing.wechat.feature.contacts.ui.detail.profile.ContactProfileScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.profile.edit.EditContactProfileScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.setting.ContactSettingScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.NewContactsScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.request.RequestAddScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.request.RequestAddViewModel
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.verify.AcceptVerifyScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.verify.AcceptVerifyViewModel

fun EntryProviderScope<NavKey>.contactsNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    // 添加联系人相关
    entry<ContactsKey.AddContact> {
        AddContactScreen(
            onBack = onBack,
            onNavigateToNFC = { backStack.add(ContactsKey.NFC) },
            onNavigateToRadar = { backStack.add(ContactsKey.RadarScan) },
            onNavigateToGroup = { backStack.add(ContactsKey.PinCodeGroup) },
            onNavigateToContactDetail = { backStack.add(ContactsKey.Detail(it)) },
            onNavigateToPlainText = { backStack.add(CommonKey.PlainText(it)) },
            onNavigateToWebView = { backStack.add(CommonKey.WebView(it)) }
        )
    }
    entry<ContactsKey.NFC> {
        NfcAddContactScreen(
            onBack = onBack,
            onNavigateToContact = { backStack.add(ContactsKey.Detail(it)) }
        )
    }
    entry<ContactsKey.RadarScan> {
        RadarScanScreen(
            onBack = onBack,
            onNavigateToContact = { backStack.add(ContactsKey.Detail(it)) }
        )
    }
    entry<ContactsKey.PinCodeGroup> {
        PinCodeGroupScreen(onBack)
    }

    // 详情与资料
    entry<ContactsKey.Detail> {
        val id = it.contactId

        ContactDetailScreen(
            onBack = onBack,
            onNavigateToChat = {
                backStack.removeIf { key -> key is ChatKey.ChatSession }
                backStack.add(ChatKey.ChatSession(id))
            },
            onNavigateToSetting = { backStack.add(ContactsKey.Setting(id)) },
            onNavigateToProfile = { backStack.add(ContactsKey.Profile(id)) },
            onNavigateToRequestAdd = { backStack.add(ContactsKey.RequestAdd(id)) },
            viewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
                factory.create(id)
            }
        )
    }
    entry<ContactsKey.Setting> {
        val id = it.contactId

        ContactSettingScreen(
            onBack = onBack,
            onDelete = {
                backStack.add(CommonKey.Home)
            },
            onNavigateToContactProfile = { backStack.add(ContactsKey.EditProfile(id)) },
            viewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
                factory.create(id)
            }
        )
    }
    entry<ContactsKey.Profile> {
        val id = it.contactId

        ContactProfileScreen(
            onBack = onBack,
            onNavigateToEdit = { backStack.add(ContactsKey.EditProfile(id)) },
            viewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
                factory.create(id)
            }
        )
    }
    entry<ContactsKey.EditProfile> {
        EditContactProfileScreen(it.contactId, onBack)
    }

    // 请求与验证
    entry<ContactsKey.RequestAdd> {
        RequestAddScreen(
            onBack = onBack,
            onSuccess = { backStack.add(CommonKey.Home) },
            viewModel = hiltViewModel { factory: RequestAddViewModel.Factory ->
                factory.create(it.contactId)
            }
        )
    }
    entry<ContactsKey.AcceptVerify> {
        AcceptVerifyScreen(
            onBack = onBack,
            onSuccess = onBack,
            viewModel = hiltViewModel { factory: AcceptVerifyViewModel.Factory ->
                factory.create(it.requestId)
            }
        )
    }
    entry<ContactsKey.NewFriends> {
        NewContactsScreen(
            onBack = onBack,
            onNavigateToAdd = { backStack.add(ContactsKey.AddContact) },
            onNavigateToVerify = { backStack.add(ContactsKey.AcceptVerify(it)) }
        )
    }
}