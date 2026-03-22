package top.chengdongqing.wechat.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import top.chengdongqing.wechat.features.chat.data.repository.ChatSessionRepositoryImpl
import top.chengdongqing.wechat.features.chat.data.repository.MessageRepositoryImpl
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import top.chengdongqing.wechat.features.contacts.data.repository.AddFriendRepositoryImpl
import top.chengdongqing.wechat.features.contacts.data.repository.ContactRepositoryImpl
import top.chengdongqing.wechat.features.contacts.data.repository.FriendRequestRepositoryImpl
import top.chengdongqing.wechat.features.contacts.data.repository.RadarDiscoveryRepositoryImpl
import top.chengdongqing.wechat.features.contacts.domain.repository.AddFriendRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.RadarDiscoveryRepository
import top.chengdongqing.wechat.features.profile.data.repository.ProfileRepositoryImpl
import top.chengdongqing.wechat.features.profile.domain.repository.ProfileRepository
import top.chengdongqing.wechat.features.settings.data.repository.ChatSettingsRepositoryImpl
import top.chengdongqing.wechat.features.settings.data.repository.ConnectionSettingsRepositoryImpl
import top.chengdongqing.wechat.features.settings.data.repository.DisplaySettingsRepositoryImpl
import top.chengdongqing.wechat.features.settings.data.repository.NotificationSettingsRepositoryImpl
import top.chengdongqing.wechat.features.settings.data.repository.PrivacySettingsRepositoryImpl
import top.chengdongqing.wechat.features.settings.domain.repository.ChatSettingsRepository
import top.chengdongqing.wechat.features.settings.domain.repository.ConnectionSettingsRepository
import top.chengdongqing.wechat.features.settings.domain.repository.DisplaySettingsRepository
import top.chengdongqing.wechat.features.settings.domain.repository.NotificationSettingsRepository
import top.chengdongqing.wechat.features.settings.domain.repository.PrivacySettingsRepository

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    fun bindContactRepository(
        impl: ContactRepositoryImpl
    ): ContactRepository

    @Binds
    @Singleton
    fun bindAddFriendRepository(
        impl: AddFriendRepositoryImpl
    ): AddFriendRepository

    @Binds
    @Singleton
    fun bindFriendRequestRepository(
        impl: FriendRequestRepositoryImpl
    ): FriendRequestRepository

    @Binds
    @Singleton
    fun bindChatSessionRepository(
        impl: ChatSessionRepositoryImpl
    ): ChatSessionRepository

    @Binds
    @Singleton
    fun bindMessageRepository(
        impl: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    @Singleton
    fun bindRadarRepository(
        impl: RadarDiscoveryRepositoryImpl
    ): RadarDiscoveryRepository

    @Binds
    @Singleton
    fun bindDisplaySettingsRepository(
        impl: DisplaySettingsRepositoryImpl
    ): DisplaySettingsRepository

    @Binds
    @Singleton
    fun bindChatSettingsRepository(
        impl: ChatSettingsRepositoryImpl
    ): ChatSettingsRepository

    @Binds
    @Singleton
    fun bindNotificationSettingsRepository(
        impl: NotificationSettingsRepositoryImpl
    ): NotificationSettingsRepository

    @Binds
    @Singleton
    fun bindPrivacySettingsRepository(
        impl: PrivacySettingsRepositoryImpl
    ): PrivacySettingsRepository

    @Binds
    @Singleton
    fun bindConnectionModeSettingsRepository(
        impl: ConnectionSettingsRepositoryImpl
    ): ConnectionSettingsRepository
}