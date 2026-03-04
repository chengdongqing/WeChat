package top.chengdongqing.wechat.features.me.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.features.me.domain.model.Gender
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_profile"
)

class ProfileRepositoryImpl @Inject constructor(
    private val json: Json,
    @param:IoScope private val scope: CoroutineScope,
    @param:ApplicationContext private val context: Context
) : ProfileRepository {

    private val dataStore = context.profileDataStore

    private companion object {
        val PROFILE_KEY = stringPreferencesKey("current_profile")
    }

    private val profileState = dataStore.data
        .map { preferences ->
            preferences[PROFILE_KEY]?.toProfile()
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly, // App 启动就加载并保持最新
            initialValue = null
        )

    // 实时流
    override fun observeProfile(): Flow<UserProfile?> = profileState

    // 内存快照
    override fun getProfile(): UserProfile? = profileState.value

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> = runCatching {
        dataStore.edit { preferences ->
            preferences[PROFILE_KEY] = profile.toJson()
        }
    }

    override suspend fun updateProfile(
        nickname: String?,
        gender: Gender?,
        signature: String?,
        avatarPath: String?
    ): Result<Unit> = runCatching {
        val currentProfile = getProfile()
            ?: throw IllegalStateException("Profile not found")

        val updatedProfile = currentProfile.copyWithUpdate(
            userName = nickname,
            gender = gender,
            signature = signature,
            avatarPath = avatarPath
        )

        saveProfile(updatedProfile)
    }

    override suspend fun hasProfile(): Boolean {
        return getProfile() != null
    }

    override suspend fun clearProfile(): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences.remove(PROFILE_KEY)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun String.toProfile(): UserProfile? = runCatching {
        json.decodeFromString<UserProfile>(this)
    }.getOrNull()

    private fun UserProfile.toJson(): String = json.encodeToString(this)
}