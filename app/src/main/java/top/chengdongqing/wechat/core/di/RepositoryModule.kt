package top.chengdongqing.wechat.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import top.chengdongqing.wechat.features.contacts.data.repository.ContactP2PRepositoryImpl
import top.chengdongqing.wechat.features.contacts.data.repository.ContactRepositoryImpl
import top.chengdongqing.wechat.features.contacts.data.repository.FriendRequestRepositoryImpl
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.me.data.repository.ProfileRepositoryImpl
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    fun bindContactRepository(
        impl: ContactRepositoryImpl
    ): ContactRepository

    @Binds
    @Singleton
    fun bindContactP2PRepository(
        impl: ContactP2PRepositoryImpl
    ): ContactP2PRepository

    @Binds
    @Singleton
    fun bindFriendRequestRepository(
        impl: FriendRequestRepositoryImpl
    ): FriendRequestRepository
}