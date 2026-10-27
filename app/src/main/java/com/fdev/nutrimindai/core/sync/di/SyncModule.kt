package com.fdev.nutrimindai.core.sync.di

import com.fdev.nutrimindai.core.sync.FirebaseAuthManager
import com.fdev.nutrimindai.core.sync.FirestoreSyncRepository
import com.fdev.nutrimindai.core.sync.SyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    // Singletons are provided via @Inject constructor
}
