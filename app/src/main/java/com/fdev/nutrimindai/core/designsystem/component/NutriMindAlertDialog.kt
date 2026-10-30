package com.fdev.nutrimindai.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fdev.nutrimindai.R

/**
 * Reusable Confirmation / Alert Dialog for NutriMind AI.
 * Provides consistent surface styling, corner radius, and standard/danger action buttons.
 */
@Composable
fun NutriMindConfirmationDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButtonText: String = stringResource(R.string.food_dialog_ok),
    dismissButtonText: String? = stringResource(R.string.dialog_cancel),
    onDismissClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    isDanger: Boolean = false,
    confirmButtonEnabled: Boolean = true,
    dismissButtonEnabled: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = iconTint ?: if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = text ?: description?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmButtonEnabled,
                shape = RoundedCornerShape(12.dp),
                colors = if (isDanger) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(
                    text = confirmButtonText,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = dismissButtonText?.let { btnText ->
            {
                TextButton(
                    onClick = { (onDismissClick ?: onDismissRequest)() },
                    enabled = dismissButtonEnabled
                ) {
                    Text(
                        text = btnText,
                        color = if (isDanger) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}

/**
 * Reusable Information Dialog (single OK/Close button)
 */
@Composable
fun NutriMindInfoDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    text: (@Composable () -> Unit)? = null,
    buttonText: String = stringResource(R.string.food_dialog_ok),
    icon: ImageVector? = null,
    iconTint: Color? = null
) {
    NutriMindConfirmationDialog(
        title = title,
        description = description,
        text = text,
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        confirmButtonText = buttonText,
        dismissButtonText = null,
        icon = icon,
        iconTint = iconTint,
        modifier = modifier
    )
}
