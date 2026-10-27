package com.fdev.nutrimindai.feature.progress.di

import com.fdev.nutrimindai.feature.progress.data.repository.WeightLogRepositoryImpl
import com.fdev.nutrimindai.feature.progress.domain.repository.WeightLogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProgressModule {

    @Binds
    @Singleton
    abstract fun bindWeightLogRepository(
        impl: WeightLogRepositoryImpl
    ): WeightLogRepository
}
