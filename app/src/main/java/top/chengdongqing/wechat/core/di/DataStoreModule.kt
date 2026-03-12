package top.chengdongqing.wechat.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Qualifier
import jakarta.inject.Singleton

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

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @ProfileDataStore
    fun provideProfileDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("user_profile") }
        )

    @Provides
    @Singleton
    @DisplaySettingsDataStore
    fun provideDisplaySettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("display_settings") }
        )

    @Provides
    @Singleton
    @ChatSettingsDataStore
    fun provideChatSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("chat_settings") }
        )

    @Provides
    @Singleton
    @NotificationSettingsDataStore
    fun provideNotificationSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("notification_settings") }
        )

    @Provides
    @Singleton
    @PrivacySettingsDataStore
    fun providePrivacySettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("privacy_settings") }
        )
}