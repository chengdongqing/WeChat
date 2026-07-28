package top.chengdongqing.wechat.core.location.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import top.chengdongqing.wechat.core.location.repository.LocationRepository
import top.chengdongqing.wechat.core.location.repository.LocationRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
interface LocationRepositoryModule {
    @Binds @Singleton fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository
}
