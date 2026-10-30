package com.fdev.nutrimindai.feature.profile.data.repository

import com.fdev.nutrimindai.core.cache.Cached
import com.fdev.nutrimindai.core.di.ApplicationScope
import com.fdev.nutrimindai.feature.profile.data.local.dao.ProfileDao
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [ProfileRepository] backed by Room database DAO
 * with reactive in-memory caching and write-through consistency.
 *
 * @property profileDao The data access object for profile database transactions.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    @param:ApplicationScope private val appScope: CoroutineScope
) : ProfileRepository {

    constructor(profileDao: ProfileDao) : this(
        profileDao = profileDao,
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )

    private val cache = MutableStateFlow<Cached<ProfileEntity?>>(Cached.NotLoaded)
    private val observerStarted = AtomicBoolean(false)

    private fun ensureObserved() {
        if (observerStarted.get()) return
        val upstream = profileDao.getProfile() ?: return
        if (observerStarted.compareAndSet(false, true)) {
            appScope.launch(start = CoroutineStart.UNDISPATCHED) {
                upstream.collect { profile ->
                    cache.value = Cached.Value(profile)
                }
            }
        }
    }

    override suspend fun saveProfile(profile: ProfileEntity) {
        ensureObserved()
        profileDao.insertProfile(profile)
        cache.value = Cached.Value(profile)
    }

    override suspend fun updateProfile(profile: ProfileEntity) {
        ensureObserved()
        profileDao.updateProfile(profile)
        cache.value = Cached.Value(profile)
    }

    override fun getProfile(): Flow<ProfileEntity?> {
        ensureObserved()
        return cache
            .filterIsInstance<Cached.Value<ProfileEntity?>>()
            .map { it.data }
            .distinctUntilChanged()
    }

    override suspend fun getProfileDirect(): ProfileEntity? {
        ensureObserved()
        return when (val current = cache.value) {
            is Cached.Value -> current.data
            Cached.NotLoaded -> {
                val loaded = profileDao.getProfileDirect()
                cache.compareAndSet(Cached.NotLoaded, Cached.Value(loaded))
                (cache.value as? Cached.Value)?.data ?: loaded
            }
        }
    }

    override suspend fun deleteProfile() {
        ensureObserved()
        profileDao.deleteProfile()
        cache.value = Cached.Value(null)
    }
}
