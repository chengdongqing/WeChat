package top.chengdongqing.wechat.feature.profile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.feature.profile.data.repository.ProfileRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
interface ProfileRepositoryModule {
    @Binds @Singleton fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
