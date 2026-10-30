package com.fdev.nutrimindai.feature.dailyintake.data.repository

import com.fdev.nutrimindai.core.cache.Cached
import com.fdev.nutrimindai.core.di.ApplicationScope
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalFoodDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toDomain
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Food
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.FoodRepository
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
class FoodRepositoryImpl @Inject constructor(
    private val localDataSource: LocalFoodDataSource,
    @ApplicationScope private val appScope: CoroutineScope
) : FoodRepository {

    constructor(localDataSource: LocalFoodDataSource) : this(
        localDataSource = localDataSource,
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )

    private val cache = MutableStateFlow<Cached<List<Food>>>(Cached.NotLoaded)
    private val observerStarted = AtomicBoolean(false)

    init {
        ensureObserved()
    }

    private fun ensureObserved() {
        if (observerStarted.get()) return
        val upstream = localDataSource.getAllFoods() ?: return
        if (observerStarted.compareAndSet(false, true)) {
            appScope.launch(start = CoroutineStart.UNDISPATCHED) {
                upstream.collect { entities ->
                    cache.value = Cached.Value(entities.map { it.toDomain() })
                }
            }
        }
    }

    private suspend fun currentFoods(): List<Food> {
        ensureObserved()
        return when (val current = cache.value) {
            is Cached.Value -> current.data
            Cached.NotLoaded -> {
                val loaded = localDataSource.getAllFoodsDirect().map { it.toDomain() }
                cache.compareAndSet(Cached.NotLoaded, Cached.Value(loaded))
                (cache.value as? Cached.Value)?.data ?: loaded
            }
        }
    }

    private fun foodsFlow(): Flow<List<Food>> {
        ensureObserved()
        return cache
            .filterIsInstance<Cached.Value<List<Food>>>()
            .map { it.data }
            .distinctUntilChanged()
    }

    override fun getAllFoods(): Flow<List<Food>> = foodsFlow()

    override suspend fun getAllFoodsDirect(): List<Food> = currentFoods()

    override fun searchFoods(query: String): Flow<List<Food>> {
        val q = query.trim()
        return foodsFlow()
            .map { list ->
                if (q.isEmpty()) list
                else list.filter { it.name.contains(q, ignoreCase = true) }
            }
            .distinctUntilChanged()
    }

    override suspend fun searchFoodsDirect(query: String): List<Food> {
        val q = query.trim()
        val list = currentFoods()
        return if (q.isEmpty()) list else list.filter { it.name.contains(q, ignoreCase = true) }
    }

    override suspend fun findFoodByName(name: String): Food? {
        val key = name.trim()
        return currentFoods().firstOrNull { it.name.trim().equals(key, ignoreCase = true) }
    }

    override suspend fun insertFoods(foods: List<Food>) {
        if (foods.isEmpty()) return
        val existingList = currentFoods()
        val byName = existingList.associateBy { it.name.trim().lowercase() }.toMutableMap()
        val mergedDomain = existingList.associateBy { it.id }.toMutableMap()

        val nonDuplicateEntities = foods.map { food ->
            val key = food.name.trim().lowercase()
            val existing = byName[key]
            val finalFood = if (existing != null) food.copy(id = existing.id) else food
            byName[key] = finalFood
            mergedDomain[finalFood.id] = finalFood
            finalFood.toEntity()
        }
        localDataSource.insertFoods(nonDuplicateEntities)
        cache.value = Cached.Value(mergedDomain.values.sortedBy { it.name })
    }

    override suspend fun insertFood(food: Food): String {
        val existing = findFoodByName(food.name)
        val finalFood = if (existing != null) food.copy(id = existing.id) else food
        localDataSource.insertFood(finalFood.toEntity())

        val current = (cache.value as? Cached.Value)?.data
        if (current != null) {
            val updated = current
                .filterNot { it.id == finalFood.id || it.name.trim().equals(finalFood.name.trim(), ignoreCase = true) }
                .plus(finalFood)
                .sortedBy { it.name }
            cache.value = Cached.Value(updated)
        }
        return finalFood.id
    }

    override suspend fun getFoodCount(): Int {
        ensureObserved()
        return when (val current = cache.value) {
            is Cached.Value -> current.data.size
            Cached.NotLoaded -> localDataSource.getFoodCount()
        }
    }
}
