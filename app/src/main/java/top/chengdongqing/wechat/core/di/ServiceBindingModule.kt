package top.chengdongqing.wechat.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import top.chengdongqing.wechat.core.network.messaging.SignalingDispatcher
import top.chengdongqing.wechat.core.network.service.call.CallServiceModule
import top.chengdongqing.wechat.core.network.service.notification.NotificationServiceModule
import top.chengdongqing.wechat.feature.call.manager.SignalingManager
import top.chengdongqing.wechat.service.call.CallProtocolHandler
import top.chengdongqing.wechat.service.notification.NotificationHandler

@Module
@InstallIn(SingletonComponent::class)
interface ServiceBindingModule {

    @Binds
    @Singleton
    fun bindSignalingDispatcher(impl: SignalingManager): SignalingDispatcher

    @Binds
    @Singleton
    fun bindCallServiceModule(impl: CallProtocolHandler): CallServiceModule

    @Binds
    @Singleton
    fun bindNotificationServiceModule(impl: NotificationHandler): NotificationServiceModule
}
