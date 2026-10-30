package com.fdev.nutrimindai.feature.home.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.component.NutriMindConfirmationDialog

@Composable
fun ReopenConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NutriMindConfirmationDialog(
        title = stringResource(R.string.home_reopen_dialog_title),
        description = stringResource(R.string.home_reopen_dialog_desc),
        confirmButtonText = stringResource(R.string.home_btn_reopen),
        dismissButtonText = stringResource(R.string.dialog_cancel),
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        modifier = modifier,
    )
}
