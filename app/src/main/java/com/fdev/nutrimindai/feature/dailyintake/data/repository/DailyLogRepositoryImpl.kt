package com.fdev.nutrimindai.feature.dailyintake.data.repository

import com.fdev.nutrimindai.core.cache.Cached
import com.fdev.nutrimindai.core.di.ApplicationScope
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalDailyLogDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalExerciseDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalIntakeDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalWaterDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.DailyLogEntity
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toDomain
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDailyLogDataSource,
    private val intakeDataSource: LocalIntakeDataSource,
    private val waterDataSource: LocalWaterDataSource,
    private val exerciseDataSource: LocalExerciseDataSource,
    @param:ApplicationScope private val appScope: CoroutineScope
) : DailyLogRepository {

    constructor(
        localDataSource: LocalDailyLogDataSource,
        intakeDataSource: LocalIntakeDataSource,
        waterDataSource: LocalWaterDataSource,
        exerciseDataSource: LocalExerciseDataSource
    ) : this(
        localDataSource = localDataSource,
        intakeDataSource = intakeDataSource,
        waterDataSource = waterDataSource,
        exerciseDataSource = exerciseDataSource,
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )

    private val logsCache = MutableStateFlow<Cached<List<DailyLog>>>(Cached.NotLoaded)
    private val dateToIdCache = ConcurrentHashMap<String, String>()
    private val observerStarted = AtomicBoolean(false)
    private val createMutex = Mutex()
    private val autoCompleteMutex = Mutex()

    @Volatile
    private var lastAutoCompleteDate: String? = null

    private fun ensureObserved() {
        if (observerStarted.get()) return
        val upstream = localDataSource.getAllDailyLogs() ?: return
        if (observerStarted.compareAndSet(false, true)) {
            appScope.launch(start = CoroutineStart.UNDISPATCHED) {
                upstream.collect { entities ->
                    val domainList = entities.map { it.toDomain() }
                    logsCache.value = Cached.Value(domainList)
                    val currentMap = entities.associate { it.date to it.id }
                    dateToIdCache.keys.retainAll(currentMap.keys)
                    dateToIdCache.putAll(currentMap)
                    val today = todayString()
                    if (entities.isEmpty() || entities.any { it.date < today && it.status == DailyLogStatus.DRAFT }) {
                        lastAutoCompleteDate = null
                    }
                }
            }
        }
    }

    private fun upsertCachedLog(log: DailyLog) {
        dateToIdCache[log.date] = log.id
        val current = logsCache.value
        if (current is Cached.Value) {
            val updated = current.data
                .filterNot { it.id == log.id || it.date == log.date }
                .plus(log)
                .sortedByDescending { it.date }
            logsCache.value = Cached.Value(updated)
        }
    }

    override fun getDailyLogByDate(date: String): Flow<DailyLog?> {
        ensureObserved()
        return getAllDailyLogs()
            .map { list -> list.firstOrNull { it.date == date } }
            .distinctUntilChanged()
    }

    override suspend fun getDailyLogByDateDirect(date: String): DailyLog? {
        ensureObserved()
        val cached = (logsCache.value as? Cached.Value)?.data?.firstOrNull { it.date == date }
        if (cached != null) return cached
        return localDataSource.getDailyLogByDateDirect(date)?.toDomain()?.also {
            upsertCachedLog(it)
        }
    }

    override fun getAllDailyLogs(): Flow<List<DailyLog>> {
        ensureObserved()
        return logsCache
            .filterIsInstance<Cached.Value<List<DailyLog>>>()
            .map { it.data }
            .distinctUntilChanged()
    }

    override suspend fun getAllDailyLogsDirect(): List<DailyLog> {
        ensureObserved()
        return when (val current = logsCache.value) {
            is Cached.Value -> current.data
            Cached.NotLoaded -> {
                val loaded = localDataSource.getAllDailyLogsDirect().map { it.toDomain() }
                loaded.forEach { dateToIdCache[it.date] = it.id }
                logsCache.compareAndSet(Cached.NotLoaded, Cached.Value(loaded))
                (logsCache.value as? Cached.Value)?.data ?: loaded
            }
        }
    }

    override fun getDailyLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyLog>> {
        ensureObserved()
        return getAllDailyLogs()
            .map { list -> list.filter { it.date in startDate..endDate } }
            .distinctUntilChanged()
    }

    override suspend fun getDailyLogsBetweenDatesDirect(startDate: String, endDate: String): List<DailyLog> {
        ensureObserved()
        return when (val current = logsCache.value) {
            is Cached.Value -> current.data.filter { it.date in startDate..endDate }
            Cached.NotLoaded -> localDataSource.getDailyLogsBetweenDatesDirect(startDate, endDate).map { it.toDomain() }
        }
    }

    override suspend fun autoCompletePastLogs() {
        val currentDate = todayString()
        val pastDrafts = localDataSource.getPastDraftLogs(currentDate)

        for (log in pastDrafts) {
            val intakeCount = intakeDataSource.getIntakeCount(log.id)
            val waterCount = waterDataSource.getWaterCount(log.id)
            val exerciseCount = exerciseDataSource.getExerciseCount(log.id)

            val newStatus = if (intakeCount > 0 || waterCount > 0 || exerciseCount > 0) {
                DailyLogStatus.COMPLETED
            } else {
                DailyLogStatus.MISSED
            }

            val updated = log.copy(
                status = newStatus,
                completedAt = if (newStatus == DailyLogStatus.COMPLETED) System.currentTimeMillis() else null,
                isSync = false
            )
            localDataSource.updateDailyLog(updated)
            upsertCachedLog(updated.toDomain())
        }
        lastAutoCompleteDate = currentDate
    }

    private suspend fun ensurePastLogsCompleted() {
        val today = todayString()
        if (lastAutoCompleteDate == today) return
        autoCompleteMutex.withLock {
            if (lastAutoCompleteDate == today) return
            autoCompletePastLogs()
        }
    }

    override suspend fun getOrCreateDailyLog(date: String): String {
        ensureObserved()
        ensurePastLogsCompleted()

        dateToIdCache[date]?.let { return it }

        return createMutex.withLock {
            dateToIdCache[date]?.let { return@withLock it }

            val existing = (logsCache.value as? Cached.Value)?.data?.firstOrNull { it.date == date }
                ?: localDataSource.getDailyLogByDateDirect(date)?.toDomain()
            if (existing != null) {
                upsertCachedLog(existing)
                return@withLock existing.id
            }

            val newLog = DailyLogEntity(date = date)
            localDataSource.insertDailyLog(newLog)
            val finalEntity = localDataSource.getDailyLogByDateDirect(date) ?: newLog
            upsertCachedLog(finalEntity.toDomain())
            finalEntity.id
        }
    }

    override suspend fun completeDailyLog(date: String, aiSummary: String?) {
        ensureObserved()
        val existing = localDataSource.getDailyLogByDateDirect(date)
        if (existing != null) {
            val updated = existing.copy(
                status = DailyLogStatus.COMPLETED,
                completedAt = System.currentTimeMillis(),
                aiSummary = aiSummary ?: existing.aiSummary,
                isSync = false
            )
            localDataSource.updateDailyLog(updated)
            upsertCachedLog(updated.toDomain())
        }
    }

    override suspend fun completeToday(aiSummary: String?) {
        val currentDate = todayString()
        getOrCreateDailyLog(currentDate)
        completeDailyLog(currentDate, aiSummary)
    }

    override suspend fun saveAiSummary(dailyLogId: String, aiSummary: String) {
        ensureObserved()
        localDataSource.updateAiSummary(dailyLogId, aiSummary)
        val current = logsCache.value
        if (current is Cached.Value) {
            val updated = current.data.map { log ->
                if (log.id == dailyLogId) log.copy(aiSummary = aiSummary) else log
            }
            logsCache.value = Cached.Value(updated)
        }
    }

    override suspend fun reopenToday() {
        val currentDate = todayString()
        getOrCreateDailyLog(currentDate)
        reopenDailyLog(currentDate)
    }

    override suspend fun reopenDailyLog(date: String) {
        ensureObserved()
        val existing = localDataSource.getDailyLogByDateDirect(date)
        if (existing != null) {
            val updated = existing.copy(
                status = DailyLogStatus.DRAFT,
                completedAt = null,
                isSync = false
            )
            localDataSource.updateDailyLog(updated)
            upsertCachedLog(updated.toDomain())
        }
    }

    override suspend fun getDailyLogCount(): Int {
        ensureObserved()
        return when (val current = logsCache.value) {
            is Cached.Value -> current.data.size
            Cached.NotLoaded -> localDataSource.getDailyLogCount()
        }
    }

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
