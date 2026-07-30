package top.chengdongqing.wechat.feature.contacts.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.model.LocalAiAssistant
import top.chengdongqing.wechat.core.navigation.NavigationKey
import top.chengdongqing.wechat.feature.contacts.ui.add.AddFriendScreen
import top.chengdongqing.wechat.feature.contacts.ui.add.nfc.NFCAddFriendScreen
import top.chengdongqing.wechat.feature.contacts.ui.add.pincode.PinCodeCreateGroupScreen
import top.chengdongqing.wechat.feature.contacts.ui.add.radar.RadarScanAddFriendScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.ContactDetailScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.ContactDetailViewModel
import top.chengdongqing.wechat.feature.contacts.ui.detail.profile.ContactProfileScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.profile.edit.EditContactProfileScreen
import top.chengdongqing.wechat.feature.contacts.ui.detail.setting.ContactSettingScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.NewFriendsScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.request.RequestAddFriendScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.request.RequestAddFriendViewModel
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.verify.AcceptFriendRequestScreen
import top.chengdongqing.wechat.feature.contacts.ui.friendrequest.verify.AcceptFriendRequestViewModel
import top.chengdongqing.wechat.feature.contacts.ui.group.GroupListScreen
import top.chengdongqing.wechat.feature.contacts.ui.tags.ContactTagEditorScreen
import top.chengdongqing.wechat.feature.contacts.ui.tags.ContactTagPickerScreen
import top.chengdongqing.wechat.feature.contacts.ui.tags.ContactTagsScreen

fun EntryProviderScope<NavKey>.contactsNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    // 添加好友相关
    entry<NavigationKey.AddFriend> {
        AddFriendScreen(
            onBack = onBack,
            onNavigateToNFC = { backStack.add(NavigationKey.NFCAddFriend) },
            onNavigateToRadar = { backStack.add(NavigationKey.RadarScanAddFriend) },
            onNavigateToGroup = { backStack.add(NavigationKey.PinCodeCreateGroup) },
            onNavigateToContactDetail = { backStack.add(NavigationKey.ContactDetail(it)) },
            onNavigateToPlainText = { backStack.add(NavigationKey.PlainText(it)) },
            onNavigateToWebView = { backStack.add(NavigationKey.WebView(it)) }
        )
    }
    entry<NavigationKey.NFCAddFriend> {
        NFCAddFriendScreen(
            onBack = onBack,
            onNavigateToContact = { backStack.add(NavigationKey.ContactDetail(it)) }
        )
    }
    entry<NavigationKey.RadarScanAddFriend> {
        RadarScanAddFriendScreen(
            onBack = onBack,
            onNavigateToContact = { backStack.add(NavigationKey.ContactDetail(it)) }
        )
    }
    entry<NavigationKey.PinCodeCreateGroup> {
        PinCodeCreateGroupScreen(onBack)
    }
    entry<NavigationKey.GroupList> {
        GroupListScreen(
            onBack = onBack,
            onOpenGroup = { groupId -> backStack.add(NavigationKey.ChatSession(groupId)) }
        )
    }

    // 详情与资料
    entry<NavigationKey.ContactDetail> {
        val id = it.contactId

        ContactDetailScreen(
            onBack = onBack,
            onNavigateToChat = {
                backStack.removeIf { key -> key is NavigationKey.ChatSession }
                backStack.add(NavigationKey.ChatSession(id))
            },
            onNavigateToSetting = { backStack.add(NavigationKey.ContactSetting(id)) },
            onNavigateToProfile = { backStack.add(NavigationKey.ContactProfile(id)) },
            onNavigateToRequestAdd = { backStack.add(NavigationKey.RequestAddFriend(id)) },
            isLocalAi = id == LocalAiAssistant.ID,
            viewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
                factory.create(id)
            }
        )
    }
    entry<NavigationKey.ContactSetting> {
        val id = it.contactId

        ContactSettingScreen(
            onBack = onBack,
            onDelete = {
                backStack.clear()
                backStack.add(NavigationKey.Home)
            },
            onNavigateToContactProfile = { backStack.add(NavigationKey.EditContactProfile(id)) },
            viewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
                factory.create(id)
            }
        )
    }
    entry<NavigationKey.ContactProfile> {
        val id = it.contactId

        ContactProfileScreen(
            onBack = onBack,
            onNavigateToEdit = { backStack.add(NavigationKey.EditContactProfile(id)) },
            viewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
                factory.create(id)
            }
        )
    }
    entry<NavigationKey.EditContactProfile> {
        EditContactProfileScreen(
            contactId = it.contactId,
            onBack = onBack,
            onManageTags = { backStack.add(NavigationKey.ManageContactTags(it.contactId)) }
        )
    }

    entry<NavigationKey.ContactTags> {
        ContactTagsScreen(
            onBack = onBack,
            onCreate = { backStack.add(NavigationKey.EditContactTag()) },
            onEdit = { backStack.add(NavigationKey.EditContactTag(it)) }
        )
    }
    entry<NavigationKey.EditContactTag> {
        ContactTagEditorScreen(tagId = it.tagId, onBack = onBack)
    }
    entry<NavigationKey.ManageContactTags> {
        val contactId = it.contactId
        ContactTagPickerScreen(
            contactId = contactId,
            onBack = onBack,
            onCreate = { backStack.add(NavigationKey.EditContactTag()) }
        )
    }

    // 请求与验证
    entry<NavigationKey.RequestAddFriend> {
        RequestAddFriendScreen(
            onBack = onBack,
            onSuccess = { backStack.add(NavigationKey.Home) },
            viewModel = hiltViewModel { factory: RequestAddFriendViewModel.Factory ->
                factory.create(it.contactId)
            }
        )
    }
    entry<NavigationKey.AcceptFriendRequest> {
        AcceptFriendRequestScreen(
            onBack = onBack,
            onSuccess = onBack,
            viewModel = hiltViewModel { factory: AcceptFriendRequestViewModel.Factory ->
                factory.create(it.requestId)
            }
        )
    }
    entry<NavigationKey.NewFriends> {
        NewFriendsScreen(
            onBack = onBack,
            onNavigateToAdd = { backStack.add(NavigationKey.AddFriend) },
            onNavigateToVerify = { backStack.add(NavigationKey.AcceptFriendRequest(it)) }
        )
    }
}
