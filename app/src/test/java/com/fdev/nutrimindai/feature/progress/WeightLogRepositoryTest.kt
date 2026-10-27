package com.fdev.nutrimindai.feature.progress

import com.fdev.nutrimindai.feature.progress.data.local.dao.WeightLogDao
import com.fdev.nutrimindai.feature.progress.data.local.entity.WeightLogEntity
import com.fdev.nutrimindai.feature.progress.data.repository.WeightLogRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class WeightLogRepositoryTest {

    private val weightLogDao = mock(WeightLogDao::class.java)
    private val repository = WeightLogRepositoryImpl(weightLogDao)

    @Test
    fun `getLatestWeightLog returns latest logged weight`() = runTest {
        val sampleLog = WeightLogEntity(
            id = 1L,
            date = "2026-08-08",
            weightKg = 69.5f,
            note = "Morning post-workout"
        )
        `when`(weightLogDao.getLatestWeightLog()).thenReturn(flowOf(sampleLog))

        val result = repository.getLatestWeightLog().first()

        assertNotNull(result)
        assertEquals(69.5f, result?.weightKg)
        assertEquals("Morning post-workout", result?.note)
    }

    @Test
    fun `getAllWeightLogs returns all logged entries in chronological order`() = runTest {
        val logs = listOf(
            WeightLogEntity(1L, "2026-08-01", 72.0f, null),
            WeightLogEntity(2L, "2026-08-08", 70.2f, "Weekly check-in")
        )
        `when`(weightLogDao.getAllWeightLogs()).thenReturn(flowOf(logs))

        val result = repository.getAllWeightLogs().first()

        assertEquals(2, result.size)
        assertEquals(72.0f, result[0].weightKg)
        assertEquals(70.2f, result[1].weightKg)
    }
}
