package com.fdev.nutrimindai.feature.progress

import com.fdev.nutrimindai.feature.progress.presentation.components.ShareBitmapHelper
import com.fdev.nutrimindai.feature.progress.presentation.screen.DailyReviewDetailData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareHelperTest {

    @Test
    fun `formatShareText includes all critical metrics, branding, and hashtags`() {
        val detail = DailyReviewDetailData(
            dateStr = "2026-10-27",
            displayDate = "October 27, 2026",
            userName = "Sarah",
            score = 92,
            status = "Excellent",
            calories = 1950,
            targetCalories = 2100,
            protein = 138.0,
            targetProtein = 140.0,
            carbs = 210.0,
            targetCarbs = 230.0,
            fat = 58.0,
            targetFat = 65.0,
            water = 2600,
            targetWater = 2500,
            exerciseMinutes = 45,
            targetExerciseMinutes = 30,
            aiSummary = "Outstanding consistency on protein and hydration today!"
        )

        val formattedText = ShareBitmapHelper.formatShareText(detail)

        // Assert core elements are present in share text
        assertTrue(formattedText.contains("NutriMind AI - Daily Review"))
        assertTrue(formattedText.contains("Sarah's Health Journey"))
        assertTrue(formattedText.contains("October 27, 2026"))
        assertTrue(formattedText.contains("92/100 (Excellent)"))
        assertTrue(formattedText.contains("1,950 / 2100 kcal"))
        assertTrue(formattedText.contains("138g / 140g"))
        assertTrue(formattedText.contains("2600ml / 2500ml"))
        assertTrue(formattedText.contains("45 min / 30 min"))
        assertTrue(formattedText.contains("Meta AI Coach Evaluation:"))
        assertTrue(formattedText.contains("Outstanding consistency on protein and hydration today!"))
        assertTrue(formattedText.contains("#NutriMindAI"))
    }

    @Test
    fun `formatShareText handles empty AI summary gracefully without throwing`() {
        val detail = DailyReviewDetailData(
            dateStr = "2026-10-27",
            userName = "Alex",
            score = 75,
            status = "Good",
            calories = 1800,
            aiSummary = ""
        )

        val formattedText = ShareBitmapHelper.formatShareText(detail)

        assertTrue(formattedText.contains("NutriMind AI"))
        assertTrue(formattedText.contains("75/100 (Good)"))
        // Empty AI summary shouldn't render evaluation header
        assertFalse(formattedText.contains("Meta AI Coach Evaluation:"))
        assertTrue(formattedText.contains("#NutriMindAI"))
    }

    @Test
    fun `formatShareText with blank userName skips user journey line`() {
        val detail = DailyReviewDetailData(
            dateStr = "2026-10-27",
            userName = "",
            score = 80,
            status = "Good"
        )

        val formattedText = ShareBitmapHelper.formatShareText(detail)

        assertFalse(formattedText.contains("Health Journey"))
        assertTrue(formattedText.contains("80/100 (Good)"))
    }
}
