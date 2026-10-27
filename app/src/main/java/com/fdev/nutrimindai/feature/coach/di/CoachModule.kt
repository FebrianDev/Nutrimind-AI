package com.fdev.nutrimindai.feature.coach.di

import com.fdev.nutrimindai.feature.coach.data.repository.CoachRepositoryImpl
import com.fdev.nutrimindai.feature.coach.domain.repository.CoachRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoachModule {

    @Binds
    @Singleton
    abstract fun bindCoachRepository(
        coachRepositoryImpl: CoachRepositoryImpl
    ): CoachRepository
}
