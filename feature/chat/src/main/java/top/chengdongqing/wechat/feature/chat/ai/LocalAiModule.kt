package top.chengdongqing.wechat.feature.chat.ai

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalAiModule {
    @Binds
    abstract fun bindLocalAiEngine(implementation: LlamaCppLocalAiEngine): LocalAiEngine
}
