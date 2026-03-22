package top.chengdongqing.wechat.features.profile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.di.ProfileDataStore
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.features.profile.domain.model.Gender
import top.chengdongqing.wechat.features.profile.domain.model.UserProfile
import top.chengdongqing.wechat.features.profile.domain.repository.ProfileRepository
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val json: Json,
    private val chatSessionDao: ChatSessionDao,
    @param:IoScope private val scope: CoroutineScope,
    @param:ProfileDataStore private val dataStore: DataStore<Preferences>
) : ProfileRepository {

    private companion object {
        val PROFILE_KEY = stringPreferencesKey("current_profile")
    }

    val profileFlow = dataStore.data.map { it[PROFILE_KEY] }

    private val profileState = profileFlow.map { it?.toProfile() }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly, // App 启动就加载并保持最新
            initialValue = null
        )

    override fun observeProfile(): Flow<UserProfile?> = profileState

    override fun getProfile(): UserProfile? = profileState.value

    override fun requireProfile(): UserProfile = getProfile() ?: throw Exception("未找到个人资料")

    override fun requireUserId(): String = requireProfile().id

    override suspend fun saveProfile(profile: UserProfile) {
        dataStore.edit { it[PROFILE_KEY] = profile.toJson() }
    }

    override suspend fun updateProfile(
        nickname: String?,
        gender: Gender?,
        signature: String?,
        avatarPath: String?
    ) {
        val updatedProfile = requireProfile().copyWithUpdate(
            nickname = nickname,
            gender = gender,
            signature = signature,
            avatarPath = avatarPath,
        )

        saveProfile(updatedProfile)

        // 更新和自己的会话
        chatSessionDao.update(updatedProfile.id) {
            it.copy(
                contactName = updatedProfile.nickname,
                contactAvatar = updatedProfile.avatarPath
            )
        }
    }

    override suspend fun hasProfile(): Boolean {
        return profileFlow.first() != null
    }

    private fun String.toProfile(): UserProfile? = runCatching {
        json.decodeFromString<UserProfile>(this)
    }.getOrNull()

    private fun UserProfile.toJson(): String = json.encodeToString(this)
}