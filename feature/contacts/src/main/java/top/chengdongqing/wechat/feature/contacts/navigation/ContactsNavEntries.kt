package top.chengdongqing.wechat.feature.contacts.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.model.LocalAiAssistant
import top.chengdongqing.wechat.core.navigation.ScreenRoute
import top.chengdongqing.wechat.feature.contacts.ui.add.AddFriendScreen
import top.chengdongqing.wechat.feature.contacts.ui.add.nfc.NFCAddFriendScreen
import top.chengdongqing.wechat.feature.contacts.ui.add.pincode.PinCodeCreateGroupScreen
import top.chengdongqing.wechat.feature.contacts.ui.add.radar.RadarScanAddFriendScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.ContactDetailScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.ContactDetailViewModel
import top.chengdongqing.wechat.feature.contacts.ui.detail.profile.ContactProfileScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.profile.edit.EditContactProfileScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.setting.ContactSettingRoute
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.NewFriendsScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.request.RequestAddFriendScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.request.RequestAddFriendViewModel
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.verify.AcceptFriendRequestScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.verify.AcceptFriendRequestViewModel
import top.chengdongqing.wechat.feature.contacts.ui.tags.ContactTagEditorScreen
import top.chengdongqing.wechat.feature.contacts.ui.tags.ContactTagPickerScreen
import top.chengdongqing.wechat.feature.contacts.ui.tags.ContactTagsScreen

fun EntryProviderScope<NavKey>.contactsNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    // 添加好友相关
    entry<ScreenRoute.AddFriend> {
        AddFriendScreen(
            onBack = onBack,
            onNFC = { backStack.add(ScreenRoute.NFCAddFriend) },
            onRadar = { backStack.add(ScreenRoute.RadarScanAddFriend) },
            onGroup = { backStack.add(ScreenRoute.PinCodeCreateGroup) },
            onContactDetail = { backStack.add(ScreenRoute.ContactDetail(it)) },
            onPlainText = { backStack.add(ScreenRoute.PlainText(it)) },
            onWebView = { backStack.add(ScreenRoute.WebView(it)) }
        )
    }
    entry<ScreenRoute.NFCAddFriend> {
        NFCAddFriendScreen(
            onBack = onBack,
            onContact = { backStack.add(ScreenRoute.ContactDetail(it)) }
        )
    }
    entry<ScreenRoute.RadarScanAddFriend> {
        RadarScanAddFriendScreen(
            onBack = onBack,
            onContact = { backStack.add(ScreenRoute.ContactDetail(it)) }
        )
    }
    entry<ScreenRoute.PinCodeCreateGroup> {
        PinCodeCreateGroupScreen(onBack)
    }

    // 详情与资料
    entry<ScreenRoute.ContactDetail> {
        val id = it.contactId

        ContactDetailScreen(
            onBack = onBack,
            onChat = {
                backStack.removeIf { key -> key is ScreenRoute.Chat }
                backStack.add(ScreenRoute.Chat(id))
            },
            onSetting = { backStack.add(ScreenRoute.ContactSetting(id)) },
            onProfile = { backStack.add(ScreenRoute.ContactProfile(id)) },
            onRequestAdd = { backStack.add(ScreenRoute.RequestAddFriend(id)) },
            isAiAssistant = id == LocalAiAssistant.ID,
            viewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
                factory.create(id)
            }
        )
    }
    entry<ScreenRoute.ContactSetting> {
        ContactSettingRoute(it.contactId)
    }
    entry<ScreenRoute.ContactProfile> {
        val id = it.contactId

        ContactProfileScreen(
            onBack = onBack,
            onEdit = { backStack.add(ScreenRoute.EditContactProfile(id)) },
            viewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
                factory.create(id)
            }
        )
    }
    entry<ScreenRoute.EditContactProfile> {
        EditContactProfileScreen(
            contactId = it.contactId,
            onBack = onBack,
            onManageTags = { backStack.add(ScreenRoute.ManageContactTags(it.contactId)) }
        )
    }

    entry<ScreenRoute.ContactTags> {
        ContactTagsScreen(
            onBack = onBack,
            onCreate = { backStack.add(ScreenRoute.EditContactTag()) },
            onEdit = { backStack.add(ScreenRoute.EditContactTag(it)) }
        )
    }
    entry<ScreenRoute.EditContactTag> {
        ContactTagEditorScreen(tagId = it.tagId, onBack = onBack)
    }
    entry<ScreenRoute.ManageContactTags> {
        val contactId = it.contactId
        ContactTagPickerScreen(
            contactId = contactId,
            onBack = onBack,
            onCreate = { backStack.add(ScreenRoute.EditContactTag()) }
        )
    }

    // 请求与验证
    entry<ScreenRoute.RequestAddFriend> {
        RequestAddFriendScreen(
            onBack = onBack,
            onSuccess = {
                backStack.clear()
                backStack.add(ScreenRoute.Home)
            },
            viewModel = hiltViewModel { factory: RequestAddFriendViewModel.Factory ->
                factory.create(it.contactId)
            }
        )
    }
    entry<ScreenRoute.AcceptFriendRequest> {
        AcceptFriendRequestScreen(
            onBack = onBack,
            onSuccess = {
                backStack.clear()
                backStack.add(ScreenRoute.Home)
            },
            viewModel = hiltViewModel { factory: AcceptFriendRequestViewModel.Factory ->
                factory.create(it.requestId)
            }
        )
    }
    entry<ScreenRoute.NewFriends> {
        NewFriendsScreen(
            onBack = onBack,
            onAdd = { backStack.add(ScreenRoute.AddFriend) },
            onVerify = { backStack.add(ScreenRoute.AcceptFriendRequest(it)) }
        )
    }
}
