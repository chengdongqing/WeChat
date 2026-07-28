package top.chengdongqing.wechat.feature.profile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import top.chengdongqing.wechat.feature.profile.data.repository.ProfileRepositoryImpl
import top.chengdongqing.wechat.feature.profile.domain.repository.ProfileRepository

@Module
@InstallIn(SingletonComponent::class)
interface ProfileRepositoryModule {
    @Binds @Singleton fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
