package top.chengdongqing.wechat.features.me.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.features.me.domain.model.Gender
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_profile"
)

class ProfileRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json
) : ProfileRepository {

    private val dataStore = context.profileDataStore

    private companion object {
        val PROFILE_KEY = stringPreferencesKey("current_profile")
    }

    override fun getCurrentProfile(): Flow<UserProfile?> {
        return dataStore.data.map { preferences ->
            preferences[PROFILE_KEY]?.toProfile()
        }
    }

    override suspend fun getCurrentProfileOnce(): UserProfile? {
        return getCurrentProfile().first()
    }

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[PROFILE_KEY] = profile.toJson()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        nickname: String?,
        gender: Gender?,
        signature: String?,
        avatarPath: String?
    ): Result<Unit> {
        return try {
            val currentProfile = getCurrentProfileOnce()
                ?: return Result.failure(IllegalStateException("Profile not found"))

            val updatedProfile = currentProfile.copyWithUpdate(
                userName = nickname,
                gender = gender,
                signature = signature,
                avatarPath = avatarPath
            )

            saveProfile(updatedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasProfile(): Boolean {
        return getCurrentProfileOnce() != null
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