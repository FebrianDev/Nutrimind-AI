package com.fdev.nutrimindai.feature.profile.data.repository

import com.fdev.nutrimindai.feature.profile.data.local.dao.ProfileDao
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [ProfileRepository] backed by Room database DAO.
 *
 * @property profileDao The data access object for profile database transactions.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao
) : ProfileRepository {

    override suspend fun saveProfile(profile: ProfileEntity) {
        profileDao.insertProfile(profile)
    }

    override suspend fun updateProfile(profile: ProfileEntity) {
        profileDao.updateProfile(profile)
    }

    override fun getProfile(): Flow<ProfileEntity?> {
        return profileDao.getProfile()
    }

    override suspend fun getProfileDirect(): ProfileEntity? {
        return profileDao.getProfileDirect()
    }

    override suspend fun deleteProfile() {
        profileDao.deleteProfile()
    }
}
