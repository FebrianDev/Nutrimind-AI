package com.fdev.nutrimindai.feature.progress.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.BrandPrimary
import com.fdev.nutrimindai.feature.progress.presentation.screen.DailyReviewDetailData
import kotlinx.coroutines.launch

/**
 * Bottom sheet displaying a real-time preview of the ShareableHealthCard
 * with options to share as an image or formatted text to external apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSummaryBottomSheet(
    detail: DailyReviewDetailData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var isExporting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val chooserTitle = stringResource(R.string.daily_review_share_title)
    val captionText = stringResource(R.string.share_caption_template, detail.score)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.daily_review_share_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.share_dialog_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.dialog_cancel)
                    )
                }
            }

            // Card Preview with recording graphicsLayer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    }
            ) {
                ShareableHealthCard(detail = detail)
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action: Share as Image
                Button(
                    onClick = {
                        if (!isExporting) {
                            coroutineScope.launch {
                                isExporting = true
                                errorMessage = null
                                try {
                                    val filename = "nutrimind_summary_${detail.dateStr.ifBlank { "today" }}"
                                    val file = ShareBitmapHelper.saveBitmapToCache(
                                        context = context,
                                        graphicsLayer = graphicsLayer,
                                        filename = filename
                                    )
                                    ShareBitmapHelper.shareImage(
                                        context = context,
                                        imageFile = file,
                                        chooserTitle = chooserTitle,
                                        captionText = captionText
                                    )
                                    onDismiss()
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: context.getString(R.string.share_image_error)
                                } finally {
                                    isExporting = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.share_generating_image),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.share_btn_image),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Secondary Action: Share as Text
                OutlinedButton(
                    onClick = {
                        val shareText = ShareBitmapHelper.formatShareText(detail)
                        ShareBitmapHelper.shareText(
                            context = context,
                            text = shareText,
                            chooserTitle = chooserTitle
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isExporting
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.share_btn_text),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
