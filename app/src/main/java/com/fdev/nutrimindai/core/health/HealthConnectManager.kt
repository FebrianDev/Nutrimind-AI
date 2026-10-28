package com.fdev.nutrimindai.core.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class HealthMetrics(
    val steps: Long = 0L,
    val activeCalories: Int = 0
)

@Singleton
class HealthConnectManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        const val HEALTH_CONNECT_EXERCISE_TYPE = "Health Connect (Smartwatch & Langkah)"
    }

    val isAvailable: Boolean
        get() = try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (isAvailable) HealthConnectClient.getOrCreate(context) else null
        } catch (e: Exception) {
            Timber.e(e, "Failed to create HealthConnectClient")
            null
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            permissions.all { it in granted }
        } catch (e: Exception) {
            Timber.e(e, "Error checking Health Connect permissions")
            false
        }
    }

    suspend fun readTodayMetrics(): HealthMetrics {
        val client = healthConnectClient ?: return HealthMetrics()
        return try {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val now = Instant.now()
            val timeRange = TimeRangeFilter.between(startOfDay, now)

            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL
                    ),
                    timeRangeFilter = timeRange
                )
            )

            val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
            val activeEnergy = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
            val totalEnergy = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
            val parsedCalories = activeEnergy?.inKilocalories?.toInt()
                ?: totalEnergy?.inKilocalories?.toInt()
                ?: 0

            // If calories is 0 but steps > 0 (common when sync tools only sync StepsRecord),
            // calculate active walking calories burned (~0.04 kcal/step)
            val activeCalories = if (parsedCalories > 0) {
                parsedCalories
            } else if (steps > 0) {
                (steps * 0.04).toInt().coerceAtLeast(1)
            } else {
                0
            }

            HealthMetrics(steps = steps, activeCalories = activeCalories)
        } catch (e: Exception) {
            Timber.e(e, "Error reading Health Connect metrics")
            HealthMetrics()
        }
    }
}
