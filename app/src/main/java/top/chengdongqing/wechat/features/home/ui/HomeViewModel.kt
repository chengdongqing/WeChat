package top.chengdongqing.wechat.features.home.ui

//@HiltViewModel
//class HomeViewModel @Inject constructor(
//    private val chatRepository: ChatRepository,
//    private val contactRepository: ContactRepository
//) : ViewModel() {
//
//    // ⭐ 各个 Tab 的未读消息数
//    val unreadCounts: StateFlow<Map<HomeTab, Int>> = combine(
//        chatRepository.getTotalUnreadCount(),
//        contactRepository.getNewFriendRequestCount()
//    ) { chatUnread, contactUnread ->
//        mapOf(
//            HomeTab.Chats to chatUnread,
//            HomeTab.Contacts to contactUnread,
//            HomeTab.Discovery to 0,
//            HomeTab.Me to 0
//        )
//    }.stateIn(
//        viewModelScope,
//        SharingStarted.WhileSubscribed(5000),
//        emptyMap()
//    )
//}