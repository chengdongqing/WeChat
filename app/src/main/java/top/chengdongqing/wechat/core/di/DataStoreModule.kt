package top.chengdongqing.wechat.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.android.datatransport.runtime.dagger.multibindings.IntoSet
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

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ConnectionSettingsDataStore

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
    @IntoSet
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

    @Provides
    @Singleton
    @ConnectionSettingsDataStore
    fun provideConnectionSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("connection_settings") }
        )

    @Provides
    @Singleton
    fun provideAllDataStores(
        @ProfileDataStore profile: DataStore<Preferences>,
        @ChatSettingsDataStore chat: DataStore<Preferences>,
        @NotificationSettingsDataStore notification: DataStore<Preferences>,
        @PrivacySettingsDataStore privacy: DataStore<Preferences>,
        @ConnectionSettingsDataStore connection: DataStore<Preferences>
    ): Set<DataStore<Preferences>> = setOf(
        profile, chat, notification, privacy, connection
    )
}