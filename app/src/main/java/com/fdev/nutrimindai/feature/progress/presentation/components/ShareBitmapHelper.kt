package com.fdev.nutrimindai.feature.progress.presentation.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.core.content.FileProvider
import com.fdev.nutrimindai.feature.progress.presentation.screen.DailyReviewDetailData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Utility helper to handle Composable graphics layer export to PNG file
 * and system share intents.
 */
object ShareBitmapHelper {

    suspend fun saveBitmapToCache(
        context: Context,
        graphicsLayer: GraphicsLayer,
        filename: String
    ): File = withContext(Dispatchers.IO) {
        val imageBitmap: ImageBitmap = graphicsLayer.toImageBitmap()
        val androidBitmap: Bitmap = imageBitmap.asAndroidBitmap()

        val cacheFolder = File(context.cacheDir, "share_images")
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs()
        }

        val targetFile = File(cacheFolder, "$filename.png")
        FileOutputStream(targetFile).use { out ->
            androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        targetFile
    }

    fun shareImage(
        context: Context,
        imageFile: File,
        chooserTitle: String,
        captionText: String = ""
    ) {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            if (captionText.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, captionText)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    fun shareText(
        context: Context,
        text: String,
        chooserTitle: String
    ) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    fun formatShareText(detail: DailyReviewDetailData): String {
        return buildString {
            appendLine("🌟 NutriMind AI - Daily Review")
            if (detail.userName.isNotBlank()) {
                appendLine("👤 ${detail.userName}'s Health Journey")
            }
            appendLine("📅 Date: ${detail.displayDate.ifBlank { detail.dateStr }}")
            appendLine("🏆 Health Score: ${detail.score}/100 (${detail.status})")
            appendLine()
            appendLine("📊 Daily Nutrition & Activity:")
            appendLine("• Calories: ${String.format(Locale.US, "%,d", detail.calories)} / ${detail.targetCalories} kcal")
            appendLine("• Protein: ${detail.protein.toInt()}g / ${detail.targetProtein.toInt()}g")
            appendLine("• Carbs: ${detail.carbs.toInt()}g / ${detail.targetCarbs.toInt()}g")
            appendLine("• Fat: ${detail.fat.toInt()}g / ${detail.targetFat.toInt()}g")
            appendLine("• Water: ${detail.water}ml / ${detail.targetWater}ml")
            appendLine("• Exercise: ${detail.exerciseMinutes} min / ${detail.targetExerciseMinutes} min")
            if (detail.aiSummary.isNotBlank()) {
                appendLine()
                appendLine("🤖 Meta AI Coach Evaluation:")
                appendLine("\"${detail.aiSummary}\"")
            }
            appendLine()
            appendLine("🌿 Powered by NutriMind AI & Meta AI (Muse Spark & Llama 3)")
            appendLine("#NutriMindAI #HealthJourney")
        }
    }
}
