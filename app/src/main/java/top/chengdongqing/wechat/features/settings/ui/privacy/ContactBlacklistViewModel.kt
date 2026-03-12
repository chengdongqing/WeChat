package top.chengdongqing.wechat.features.settings.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import top.chengdongqing.wechat.features.contacts.data.mapper.toListItem
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import javax.inject.Inject

@HiltViewModel
class ContactBlacklistViewModel @Inject constructor(
    contactRepository: ContactRepository
) : ViewModel() {

    val blockedContacts = contactRepository.observeAllContacts(isBlocked = true)
        .map { it.toListItem() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}