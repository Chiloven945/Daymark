package top.chiloven.daymark.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import top.chiloven.daymark.core.data.widget.WidgetRefreshScheduler
import top.chiloven.daymark.core.model.DateCalculator
import top.chiloven.daymark.core.model.SystemTimeProvider
import top.chiloven.daymark.core.model.TimeProvider

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider =
        SystemTimeProvider()

    @Provides
    @Singleton
    fun provideDateCalculator(timeProvider: TimeProvider): DateCalculator =
        DateCalculator(timeProvider)

    @Provides
    @Singleton
    fun provideWidgetRefreshScheduler(): WidgetRefreshScheduler =
        NoOpWidgetRefreshScheduler

}

private object NoOpWidgetRefreshScheduler : WidgetRefreshScheduler {

    override fun refreshAll() =
        Unit

    override fun refresh(eventId: Long) =
        Unit

}
