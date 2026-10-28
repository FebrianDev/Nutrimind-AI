package com.fdev.nutrimindai.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.fdev.nutrimindai.feature.progress.presentation.screen.ProgressData
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Android PDF Report Generator for Nutrition, Weight Trends, and Dietitian Summaries.
 */
object NutritionPdfReportGenerator {

    fun generateReport(
        context: Context,
        data: ProgressData,
        userName: String = "User"
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (72 dpi)
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 110, 80)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 110, 120)
            textSize = 10f
        }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 40, 50)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(50, 60, 70)
            textSize = 11f
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 40, 50)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 40f

        // 1. Top Decorative Brand Bar
        paint.color = Color.rgb(20, 110, 80)
        canvas.drawRect(0f, 0f, 595f, 10f, paint)

        // 2. Header
        y += 20f
        canvas.drawText("NUTRIMINDAI - NUTRITION & HEALTH REPORT", 40f, y, titlePaint)
        y += 16f
        val currentDateStr = SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $currentDateStr | Client / User: $userName", 40f, y, subPaint)
        y += 8f
        canvas.drawText("Report Period: ${data.selectedFilter.getLabel()} | Goal: ${data.goalData.goalTitle}", 40f, y, subPaint)

        y += 18f
        paint.color = Color.rgb(220, 225, 230)
        paint.strokeWidth = 1f
        canvas.drawLine(40f, y, 555f, y, paint)

        // 3. Goal & Weight Progress Card
        y += 20f
        val cardRect = RectF(40f, y, 555f, y + 100f)
        paint.color = Color.rgb(245, 248, 250)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cardRect, 12f, 12f, paint)
        paint.color = Color.rgb(210, 220, 230)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(cardRect, 12f, 12f, paint)
        paint.style = Paint.Style.FILL

        canvas.drawText("BODY WEIGHT & GOAL PROGRESS", 56f, y + 26f, headingPaint)

        val weightStatus = if (data.goalData.isUsingProjection) "AI Metabolic Projection (Dynamic)" else "Verified Physical Scale"
        val curWeightStr = String.format(Locale.US, "%.1f kg", data.goalData.currentWeightKg)
        val targetWeightStr = String.format(Locale.US, "%.1f kg", data.goalData.targetWeightKg)
        val progressPercentInt = (data.goalData.progressPercent * 100).toInt()

        canvas.drawText("Current Weight: $curWeightStr ($weightStatus)", 56f, y + 50f, bodyPaint)
        canvas.drawText("Target Goal Weight: $targetWeightStr | Estimated Days Remaining: ${data.goalData.remainingDays} days", 56f, y + 70f, bodyPaint)
        canvas.drawText("Overall Progress: $progressPercentInt% achieved", 56f, y + 90f, boldBodyPaint)

        y += 120f

        // 4. Period Nutrition Summary Box
        canvas.drawText("DAILY AVERAGE NUTRITION METRICS (${data.selectedFilter.getLabel()})", 40f, y, headingPaint)
        y += 12f

        val nutritionBox = RectF(40f, y, 555f, y + 110f)
        paint.color = Color.rgb(250, 252, 255)
        canvas.drawRoundRect(nutritionBox, 10f, 10f, paint)
        paint.color = Color.rgb(215, 225, 235)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(nutritionBox, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        // Row 1: Calories & Protein
        val col1X = 56f
        val col2X = 300f
        var rowY = y + 28f

        canvas.drawText("Average Daily Intake:", col1X, rowY, bodyPaint)
        canvas.drawText("${data.avgCalories} kcal / day", col1X + 130f, rowY, boldBodyPaint)

        canvas.drawText("Target Calories:", col2X, rowY, bodyPaint)
        canvas.drawText("${data.todayNutrition.goalCalories} kcal / day", col2X + 110f, rowY, boldBodyPaint)

        rowY += 24f
        canvas.drawText("Hydration Target Achieved:", col1X, rowY, bodyPaint)
        canvas.drawText("${data.waterGoalPercent}%", col1X + 160f, rowY, boldBodyPaint)

        canvas.drawText("Today Hydration:", col2X, rowY, bodyPaint)
        canvas.drawText("${data.todayNutrition.water} / ${data.todayNutrition.goalWater} ml", col2X + 110f, rowY, boldBodyPaint)

        rowY += 24f
        canvas.drawText("Daily Mission Adherence:", col1X, rowY, bodyPaint)
        canvas.drawText("${data.missionPercent}% completed", col1X + 160f, rowY, boldBodyPaint)

        canvas.drawText("Today Active Burn:", col2X, rowY, bodyPaint)
        canvas.drawText("${data.todayNutrition.exerciseCalories} kcal burned", col2X + 110f, rowY, boldBodyPaint)

        rowY += 24f
        canvas.drawText("Logging Consistency:", col1X, rowY, bodyPaint)
        canvas.drawText("${data.completedDays} of ${data.totalDays} days tracked", col1X + 130f, rowY, boldBodyPaint)

        y += 135f

        // 5. Macro Breakdown for Today
        canvas.drawText("MACRONUTRIENT DISTRIBUTION", 40f, y, headingPaint)
        y += 16f
        canvas.drawText(
            "Protein: ${data.todayNutrition.protein.toInt()}g | Carbs: ${data.todayNutrition.carbs.toInt()}g | Fat: ${data.todayNutrition.fat.toInt()}g | Fiber: ${data.todayNutrition.fiber.toInt()}g",
            40f, y, bodyPaint
        )

        y += 30f

        // 6. Clinical & Behavioral AI Dietitian Assessment
        canvas.drawText("AI DIETITIAN & METABOLIC COACH EVALUATION", 40f, y, headingPaint)
        y += 12f

        val aiCard = RectF(40f, y, 555f, y + 130f)
        paint.color = Color.rgb(240, 248, 244)
        canvas.drawRoundRect(aiCard, 10f, 10f, paint)
        paint.color = Color.rgb(180, 220, 200)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(aiCard, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        // Green accent left bar
        paint.color = Color.rgb(20, 110, 80)
        canvas.drawRoundRect(RectF(40f, y, 46f, y + 130f), 4f, 4f, paint)

        val insight = data.aiInsight.takeIf { it.isNotBlank() }
            ?: "The user shows steady meal tracking adherence. Daily hydration and caloric targets are in healthy alignment with current metabolic benchmarks."

        // Multi-line draw helper
        val words = insight.split(" ")
        var line = ""
        var textY = y + 26f
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (bodyPaint.measureText(testLine) > 480f) {
                canvas.drawText(line, 58f, textY, bodyPaint)
                line = word
                textY += 18f
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, 58f, textY, bodyPaint)
        }

        // 7. Footer
        paint.color = Color.rgb(220, 225, 230)
        canvas.drawLine(40f, 790f, 555f, 790f, paint)
        canvas.drawText("NutrimindAI Clinical & Lifestyle Progress Report • Confidential • Personal Health Record", 40f, 810f, subPaint)

        document.finishPage(page)

        // Write to Cache / Files directory
        val fileName = "NutrimindAI_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        val file = File(context.cacheDir, fileName)

        return try {
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            file
        } catch (e: Exception) {
            Timber.e(e, "Failed to write PDF document")
            document.close()
            null
        }
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "NutrimindAI Health & Nutrition Report")
                putExtra(Intent.EXTRA_TEXT, "Here is my nutrition and wellness progress report generated by NutrimindAI.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Nutrition Report via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch PDF share intent")
        }
    }
}
