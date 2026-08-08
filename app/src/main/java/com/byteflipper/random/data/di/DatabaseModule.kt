package com.byteflipper.random.data.di

import android.content.Context
import androidx.room.Room
import com.byteflipper.random.data.db.AppDatabase
import com.byteflipper.random.data.db.AppMigrations
import com.byteflipper.random.data.person.PeopleRepository
import com.byteflipper.random.data.person.PersonDao
import com.byteflipper.random.data.preset.ListPresetDao
import com.byteflipper.random.data.preset.ListPresetRepository
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.data.team.TeamPresetDao
import com.byteflipper.random.data.team.TeamPresetRepository
import com.byteflipper.random.utils.Constants.DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(*AppMigrations.ALL)
            .build()
    }

    @Provides
    @Singleton
    fun provideListPresetDao(database: AppDatabase): ListPresetDao {
        return database.listPresetDao()
    }

    @Provides
    @Singleton
    fun provideListPresetRepository(dao: ListPresetDao): ListPresetRepository {
        return ListPresetRepository(dao)
    }

    @Provides
    @Singleton
    fun providePersonDao(database: AppDatabase): PersonDao {
        return database.personDao()
    }

    @Provides
    @Singleton
    fun providePeopleRepository(dao: PersonDao): PeopleRepository {
        return PeopleRepository(dao)
    }

    @Provides
    @Singleton
    fun provideTeamPresetDao(database: AppDatabase): TeamPresetDao {
        return database.teamPresetDao()
    }

    @Provides
    @Singleton
    fun provideTeamPresetRepository(dao: TeamPresetDao): TeamPresetRepository {
        return TeamPresetRepository(dao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context.applicationContext)
    }
}
