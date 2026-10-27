package com.fdev.nutrimindai.feature.onboarding.di

import com.fdev.nutrimindai.core.database.NutriMindDatabase
import com.fdev.nutrimindai.feature.onboarding.data.dao.ProfileDao
import com.fdev.nutrimindai.feature.onboarding.data.repository.ProfileRepository
import com.fdev.nutrimindai.feature.onboarding.data.repository.ProfileRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Dependency Injection module for Onboarding data layer dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingDataModule {

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
