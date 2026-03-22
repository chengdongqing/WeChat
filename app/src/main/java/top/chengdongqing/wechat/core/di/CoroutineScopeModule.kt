package top.chengdongqing.wechat.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Qualifier
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainScope // 处理 UI 相关任务

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultScope // 处理 CPU 密集型任务（加解密、复杂逻辑）

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoScope // 处理 I/O 任务（网络、数据库、文件等）

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    @Provides
    @Singleton
    @MainScope
    fun provideMainScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Provides
    @Singleton
    @DefaultScope
    fun provideDefaultScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    @IoScope
    fun provideIoScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}