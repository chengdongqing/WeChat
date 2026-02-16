//package top.chengdongqing.wechat.features.call.domain.repository
//
//import kotlinx.coroutines.flow.Flow
//import top.chengdongqing.wechat.features.call.data.HangupReason
//import top.chengdongqing.wechat.features.call.domain.model.CallInfo
//import top.chengdongqing.wechat.features.call.domain.model.CallRecord
//
///**
// * 通话数据仓库接口（domain 层）
// */
//interface CallRepository {
//
//    /** 保存通话开始记录 */
//    suspend fun createRecord(
//        callId: String,
//        peerId: String,
//        callType: top.chengdongqing.wechat.features.call.data.CallType,
//        isOutgoing: Boolean
//    )
//
//    /** 更新通话结果 */
//    suspend fun updateResult(callId: String, duration: Int, reason: HangupReason)
//
//    /** 获取通话对方信息 */
//    suspend fun getPeerCallInfo(peerId: String): CallInfo?
//
//    /** 观察与某人的通话记录 */
//    fun observeCallRecords(peerId: String): Flow<List<CallRecord>>
//
//    /** 观察所有通话记录 */
//    fun observeAllRecords(): Flow<List<CallRecord>>
//
//    /** 删除与某人的通话记录 */
//    suspend fun deleteRecords(peerId: String)
//}