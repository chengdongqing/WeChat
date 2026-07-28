package top.chengdongqing.wechat.feature.contacts.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import top.chengdongqing.wechat.feature.contacts.data.repository.AddFriendRepositoryImpl
import top.chengdongqing.wechat.feature.contacts.data.repository.ContactRepositoryImpl
import top.chengdongqing.wechat.feature.contacts.data.repository.FriendRequestRepositoryImpl
import top.chengdongqing.wechat.feature.contacts.data.repository.RadarDiscoveryRepositoryImpl
import top.chengdongqing.wechat.feature.contacts.domain.repository.AddFriendRepository
import top.chengdongqing.wechat.feature.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.feature.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.feature.contacts.domain.repository.RadarDiscoveryRepository

@Module
@InstallIn(SingletonComponent::class)
interface ContactsRepositoryModule {
    @Binds @Singleton fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository
    @Binds @Singleton fun bindAddFriendRepository(impl: AddFriendRepositoryImpl): AddFriendRepository
    @Binds @Singleton fun bindFriendRequestRepository(impl: FriendRequestRepositoryImpl): FriendRequestRepository
    @Binds @Singleton fun bindRadarRepository(impl: RadarDiscoveryRepositoryImpl): RadarDiscoveryRepository
}
