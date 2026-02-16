//package top.chengdongqing.wechat.features.call.data.repository
//
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.map
//import top.chengdongqing.wechat.data.database.dao.ContactDao
//import top.chengdongqing.wechat.features.call.data.CallType
//import top.chengdongqing.wechat.features.call.data.HangupReason
//import top.chengdongqing.wechat.features.call.data.dao.CallRecordDao
//import top.chengdongqing.wechat.features.call.data.model.CallRecordEntity
//import top.chengdongqing.wechat.features.call.domain.model.CallInfo
//import top.chengdongqing.wechat.features.call.domain.model.CallRecord
//import top.chengdongqing.wechat.features.call.domain.repository.CallRepository
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class CallRepositoryImpl @Inject constructor(
//    private val callRecordDao: CallRecordDao,
//    private val contactDao: ContactDao
//) : CallRepository {
//
//    override suspend fun createRecord(
//        callId: String,
//        peerId: String,
//        callType: CallType,
//        isOutgoing: Boolean
//    ) {
//        callRecordDao.insert(
//            CallRecordEntity(
//                callId = callId,
//                peerId = peerId,
//                callType = callType,
//                isOutgoing = isOutgoing,
//                startTime = System.currentTimeMillis()
//            )
//        )
//    }
//
//    override suspend fun updateResult(callId: String, duration: Int, reason: HangupReason) {
//        callRecordDao.updateResult(
//            callId = callId,
//            duration = duration,
//            endReason = reason.name,
//            endTime = System.currentTimeMillis()
//        )
//    }
//
//    override suspend fun getPeerCallInfo(peerId: String): CallInfo? {
//        val contact = contactDao.getContactById(peerId) ?: return null
//        return CallInfo(
//            peerId = peerId,
//            peerName = contact.remarkName ?: contact.nickname,
//            peerAvatar = contact.avatarPath
//        )
//    }
//
//    override fun observeCallRecords(peerId: String): Flow<List<CallRecord>> {
//        return callRecordDao.observeByPeer(peerId).map { records ->
//            val contact = contactDao.getContactById(peerId)
//            records.map { it.toDomain(contact?.remarkName ?: contact?.nickname ?: peerId, contact?.avatarPath) }
//        }
//    }
//
//    override fun observeAllRecords(): Flow<List<CallRecord>> {
//        return callRecordDao.observeAll().map { records ->
//            records.map { entity ->
//                val contact = contactDao.getContactById(entity.peerId)
//                entity.toDomain(
//                    contact?.remarkName ?: contact?.nickname ?: entity.peerId,
//                    contact?.avatarPath
//                )
//            }
//        }
//    }
//
//    override suspend fun deleteRecords(peerId: String) {
//        callRecordDao.deleteByPeer(peerId)
//    }
//
//    private fun CallRecordEntity.toDomain(peerName: String, peerAvatar: String?) = CallRecord(
//        callId = callId,
//        peerId = peerId,
//        peerName = peerName,
//        peerAvatar = peerAvatar,
//        callType = callType,
//        isOutgoing = isOutgoing,
//        isMissed = isMissed,
//        duration = duration,
//        endReason = endReason,
//        startTime = startTime
//    )
//}