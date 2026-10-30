package com.fdev.nutrimindai.feature.progress.data.repository

import com.fdev.nutrimindai.core.cache.Cached
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.core.di.ApplicationScope
import com.fdev.nutrimindai.feature.progress.data.local.dao.WeightLogDao
import com.fdev.nutrimindai.feature.progress.data.local.entity.WeightLogEntity
import com.fdev.nutrimindai.feature.progress.domain.repository.WeightLogRepository
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

@Singleton
class WeightLogRepositoryImpl @Inject constructor(
    private val weightLogDao: WeightLogDao,
    private val preferenceManager: PreferenceManager,
    @ApplicationScope private val appScope: CoroutineScope
) : WeightLogRepository {

    constructor(
        weightLogDao: WeightLogDao,
        preferenceManager: PreferenceManager
    ) : this(
        weightLogDao = weightLogDao,
        preferenceManager = preferenceManager,
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )

    private val allLogsCache = MutableStateFlow<Cached<List<WeightLogEntity>>>(Cached.NotLoaded)
    private val observerStarted = AtomicBoolean(false)

    @Volatile
    private var hasAllLogsUpstream: Boolean = false

    init {
        ensureObserved()
    }

    private fun ensureObserved() {
        if (observerStarted.get()) return
        val upstream = weightLogDao.getAllWeightLogs() ?: return
        if (observerStarted.compareAndSet(false, true)) {
            hasAllLogsUpstream = true
            appScope.launch(start = CoroutineStart.UNDISPATCHED) {
                upstream.collect { logs ->
                    allLogsCache.value = Cached.Value(logs)
                }
            }
        }
    }

    private suspend fun currentAllLogs(): List<WeightLogEntity> {
        ensureObserved()
        return when (val current = allLogsCache.value) {
            is Cached.Value -> current.data
            Cached.NotLoaded -> {
                val loaded = weightLogDao.getAllWeightLogsDirect()
                allLogsCache.compareAndSet(Cached.NotLoaded, Cached.Value(loaded))
                (allLogsCache.value as? Cached.Value)?.data ?: loaded
            }
        }
    }

    override fun getAllWeightLogs(): Flow<List<WeightLogEntity>> {
        ensureObserved()
        return allLogsCache
            .filterIsInstance<Cached.Value<List<WeightLogEntity>>>()
            .map { it.data }
            .distinctUntilChanged()
    }

    override suspend fun getAllWeightLogsDirect(): List<WeightLogEntity> {
        return currentAllLogs()
    }

    override fun getWeightLogByDate(date: String): Flow<WeightLogEntity?> {
        ensureObserved()
        return if (hasAllLogsUpstream || allLogsCache.value is Cached.Value) {
            getAllWeightLogs()
                .map { logs -> logs.filter { it.date == date }.maxByOrNull { it.timestamp } }
                .distinctUntilChanged()
        } else {
            weightLogDao.getWeightLogByDate(date)
        }
    }

    override suspend fun getWeightLogByDateDirect(date: String): WeightLogEntity? {
        ensureObserved()
        return when (val current = allLogsCache.value) {
            is Cached.Value -> current.data.filter { it.date == date }.maxByOrNull { it.timestamp }
            Cached.NotLoaded -> weightLogDao.getWeightLogByDateDirect(date)
        }
    }

    override fun getLatestWeightLog(): Flow<WeightLogEntity?> {
        ensureObserved()
        return if (hasAllLogsUpstream || allLogsCache.value is Cached.Value) {
            getAllWeightLogs()
                .map { logs ->
                    logs.maxWithOrNull(compareBy<WeightLogEntity> { it.date }.thenBy { it.timestamp })
                }
                .distinctUntilChanged()
        } else {
            weightLogDao.getLatestWeightLog()
        }
    }

    override suspend fun getLatestWeightLogDirect(): WeightLogEntity? {
        ensureObserved()
        return when (val current = allLogsCache.value) {
            is Cached.Value -> current.data.maxWithOrNull(
                compareBy<WeightLogEntity> { it.date }.thenBy { it.timestamp }
            )
            Cached.NotLoaded -> weightLogDao.getLatestWeightLogDirect()
        }
    }

    override suspend fun logWeight(date: String, weightKg: Float, note: String?) {
        val existing = getWeightLogByDateDirect(date)
        val entity = WeightLogEntity(
            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
            date = date,
            weightKg = weightKg,
            note = note?.trim()?.ifBlank { null } ?: existing?.note,
            timestamp = System.currentTimeMillis(),
            isSync = false
        )
        weightLogDao.insertWeightLog(entity)

        val current = (allLogsCache.value as? Cached.Value)?.data
        if (current != null) {
            val updated = current
                .filterNot { it.id == entity.id }
                .plus(entity)
                .sortedWith(compareBy<WeightLogEntity> { it.date }.thenBy { it.timestamp })
            allLogsCache.value = Cached.Value(updated)
        }
    }

    override suspend fun deleteWeightLog(id: String) {
        val existing = getAllWeightLogsDirect().find { it.id == id }
        val docIds = buildList {
            if (id.isNotBlank()) add(id)
            if (existing != null && existing.date.isNotBlank()) {
                add(existing.date)
                add("${existing.date}_${existing.timestamp}_$id")
            }
        }
        weightLogDao.deleteWeightLog(id)

        val current = (allLogsCache.value as? Cached.Value)?.data
        if (current != null) {
            allLogsCache.value = Cached.Value(current.filterNot { it.id == id })
        }

        preferenceManager.queueCloudDeletion("weight_logs", docIds)
    }
}
