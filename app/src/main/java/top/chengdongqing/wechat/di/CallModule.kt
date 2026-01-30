package top.chengdongqing.wechat.di

//@Module
//@InstallIn(SingletonComponent::class)
//object CallModule {
//
//    @Provides
//    @Singleton
//    fun provideCallEngine(): CallEngine {
//        return WebRTCEngine() // 或其他实现
//    }
//
//    @Provides
//    @Singleton
//    fun provideCallManager(
//        callEngine: CallEngine,
//        signalingClient: SignalingClient
//    ): CallManager {
//        return CallManager(callEngine, signalingClient)
//    }
//
//    @Provides
//    fun provideStartCallUseCase(
//        repository: CallRepository
//    ): StartCallUseCase {
//        return StartCallUseCase(repository)
//    }
//}