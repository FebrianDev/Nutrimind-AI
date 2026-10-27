package com.fdev.nutrimindai.feature.profile.di

import com.fdev.nutrimindai.core.database.NutriMindDatabase
import com.fdev.nutrimindai.feature.profile.data.local.dao.ProfileDao
import com.fdev.nutrimindai.feature.profile.data.repository.ProfileRepositoryImpl
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Dependency Injection module for Profile feature dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository

    companion object {
        @Provides
        @Singleton
        fun provideProfileDao(database: NutriMindDatabase): ProfileDao {
            return database.profileDao()
        }
    }
}
