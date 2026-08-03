package top.chengdongqing.wechat.feature.chat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import top.chengdongqing.wechat.core.data.repository.TemporaryChatRepository
import top.chengdongqing.wechat.feature.chat.data.repository.ChatSessionRepositoryImpl
import top.chengdongqing.wechat.feature.chat.data.repository.MessageRepositoryImpl
import top.chengdongqing.wechat.feature.chat.data.repository.TemporaryChatRepositoryImpl
import top.chengdongqing.wechat.feature.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.feature.chat.domain.repository.MessageRepository

@Module
@InstallIn(SingletonComponent::class)
interface ChatRepositoryModule {
    @Binds @Singleton fun bindChatSessionRepository(impl: ChatSessionRepositoryImpl): ChatSessionRepository
    @Binds @Singleton fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository
    @Binds
    @Singleton
    fun bindTemporaryChatRepository(impl: TemporaryChatRepositoryImpl): TemporaryChatRepository
}
