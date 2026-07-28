package top.chengdongqing.wechat.feature.settings.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import top.chengdongqing.wechat.feature.settings.data.repository.ChatSettingsRepositoryImpl
import top.chengdongqing.wechat.feature.settings.data.repository.ConnectionSettingsRepositoryImpl
import top.chengdongqing.wechat.feature.settings.data.repository.DisplaySettingsRepositoryImpl
import top.chengdongqing.wechat.feature.settings.data.repository.NotificationSettingsRepositoryImpl
import top.chengdongqing.wechat.feature.settings.data.repository.PrivacySettingsRepositoryImpl
import top.chengdongqing.wechat.feature.settings.domain.repository.ChatSettingsRepository
import top.chengdongqing.wechat.feature.settings.domain.repository.ConnectionSettingsRepository
import top.chengdongqing.wechat.feature.settings.domain.repository.DisplaySettingsRepository
import top.chengdongqing.wechat.feature.settings.domain.repository.NotificationSettingsRepository
import top.chengdongqing.wechat.feature.settings.domain.repository.PrivacySettingsRepository

@Module
@InstallIn(SingletonComponent::class)
interface SettingsRepositoryModule {
    @Binds @Singleton fun bindDisplaySettingsRepository(impl: DisplaySettingsRepositoryImpl): DisplaySettingsRepository
    @Binds @Singleton fun bindChatSettingsRepository(impl: ChatSettingsRepositoryImpl): ChatSettingsRepository
    @Binds @Singleton fun bindNotificationSettingsRepository(impl: NotificationSettingsRepositoryImpl): NotificationSettingsRepository
    @Binds @Singleton fun bindPrivacySettingsRepository(impl: PrivacySettingsRepositoryImpl): PrivacySettingsRepository
    @Binds @Singleton fun bindConnectionSettingsRepository(impl: ConnectionSettingsRepositoryImpl): ConnectionSettingsRepository
}
