package com.fdev.nutrimindai.feature.onboarding.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fdev.nutrimindai.feature.onboarding.data.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing operations on [ProfileEntity].
 */
@Dao
interface ProfileDao {

    /**
     * Inserts a user profile into the database. Replaces existing profile if conflict occurs.
     *
     * @param profile The profile entity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    /**
     * Updates an existing profile in the database.
     *
     * @param profile The updated profile entity.
     */
    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    /**
     * Retrieves the single user profile as a reactive [Flow].
     *
     * @param id The profile ID to query. Defaults to "user_profile".
     * @return A flow emitting the current [ProfileEntity] or null if non-existent.
     */
    @Query("SELECT * FROM profile_table WHERE id = :id LIMIT 1")
    fun getProfile(id: String = ProfileEntity.DEFAULT_PROFILE_ID): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile_table WHERE id = :id LIMIT 1")
    suspend fun getProfileDirect(id: String = ProfileEntity.DEFAULT_PROFILE_ID): ProfileEntity?

    /**
     * Retrieves unsynced profiles.
     */
    @Query("SELECT * FROM profile_table WHERE isSync = 0")
    suspend fun getUnsyncedProfiles(): List<ProfileEntity>

    /**
     * Marks profiles as synced.
     */
    @Query("UPDATE profile_table SET isSync = 1 WHERE id IN (:ids)")
    suspend fun markProfilesSynced(ids: List<String>)

    /**
     * Deletes all profiles from the database.
     */
    @Query("DELETE FROM profile_table")
    suspend fun deleteProfile()
}
