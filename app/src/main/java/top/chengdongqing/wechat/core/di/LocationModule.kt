package top.chengdongqing.wechat.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import top.chengdongqing.wechat.core.location.geocoding.GeocodingDataSource
import top.chengdongqing.wechat.core.location.geocoding.amap.AMapGeocodingDataSource

@Module
@InstallIn(SingletonComponent::class)
interface LocationModule {

    @Binds
    @Singleton
    fun bindGeocodingDataSource(
        impl: AMapGeocodingDataSource
    ): GeocodingDataSource
}
