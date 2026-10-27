package com.fdev.nutrimindai.feature.onboarding.data.repository

import com.fdev.nutrimindai.feature.onboarding.data.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth repository contract for user profile management.
 */
interface ProfileRepository {

    /**
     * Persists a new user profile or replaces an existing profile.
     *
     * @param profile The [ProfileEntity] to be saved.
     */
    suspend fun saveProfile(profile: ProfileEntity)

    /**
     * Updates an existing user profile in storage.
     *
     * @param profile The updated [ProfileEntity].
     */
    suspend fun updateProfile(profile: ProfileEntity)

    /**
     * Observes the active user profile data.
     *
     * @return A [Flow] emitting the latest [ProfileEntity], or null if uninitialized.
     */
    fun getProfile(): Flow<ProfileEntity?>
    suspend fun getProfileDirect(): ProfileEntity?

    /**
     * Purges user profile data from storage.
     */
    suspend fun deleteProfile()
}
