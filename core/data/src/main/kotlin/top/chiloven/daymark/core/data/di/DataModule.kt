package top.chiloven.daymark.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import top.chiloven.daymark.core.data.db.DaymarkDatabase
import top.chiloven.daymark.core.data.db.EventDao
import top.chiloven.daymark.core.data.reminder.AlarmReminderScheduler
import top.chiloven.daymark.core.data.reminder.ReminderScheduler
import top.chiloven.daymark.core.data.repository.EventRepository
import top.chiloven.daymark.core.data.repository.EventRepositoryImpl
import top.chiloven.daymark.core.data.repository.SettingsRepository
import top.chiloven.daymark.core.data.repository.SettingsRepositoryImpl
import top.chiloven.daymark.core.data.repository.userSettingsDataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DaymarkDatabase =
        Room.databaseBuilder(
            context,
            DaymarkDatabase::class.java,
            "daymark.db",
        )
                .fallbackToDestructiveMigration(true)
                .build()

    @Provides
    fun provideEventDao(database: DaymarkDatabase): EventDao =
        database.eventDao()

}

@Module
@InstallIn(SingletonComponent::class)
object SettingsDataStoreModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.userSettingsDataStore

}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: AlarmReminderScheduler): ReminderScheduler

}
