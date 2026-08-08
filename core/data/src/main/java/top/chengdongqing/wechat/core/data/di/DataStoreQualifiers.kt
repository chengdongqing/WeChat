package top.chengdongqing.wechat.core.common.di

import jakarta.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ProfileDataStore

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DisplaySettingsDataStore

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ChatSettingsDataStore

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class NotificationSettingsDataStore

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PrivacySettingsDataStore

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ConnectionSettingsDataStore
