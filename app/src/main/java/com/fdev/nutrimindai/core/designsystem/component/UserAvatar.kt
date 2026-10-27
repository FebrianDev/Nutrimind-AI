package com.fdev.nutrimindai.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.BrandPrimary
import timber.log.Timber
import java.util.Locale

@Composable
fun UserAvatar(
    photoUrl: String?,
    userName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showEditBadge: Boolean = false,
    isUploading: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val initialLetter = userName.trim()
        .ifEmpty { "User" }
        .take(1)
        .uppercase(Locale.getDefault())

    val clickableModifier = if (onClick != null && !isUploading) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .listener(
                        onError = { _, result ->
                            Timber.e(result.throwable, "UserAvatar failed to load photo: $photoUrl")
                        }
                    )
                    .build(),
                contentDescription = stringResource(R.string.avatar_cd_profile_picture),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                loading = {
                    InitialAvatarBox(
                        initialLetter = initialLetter,
                        fontSizeSp = (size.value * 0.45f)
                    )
                },
                error = {
                    InitialAvatarBox(
                        initialLetter = initialLetter,
                        fontSizeSp = (size.value * 0.45f)
                    )
                }
            )
        } else {
            InitialAvatarBox(
                initialLetter = initialLetter,
                fontSizeSp = (size.value * 0.45f)
            )
        }

        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(size * 0.4f)
                )
            }
        }

        if (showEditBadge && !isUploading) {
            val badgeSize = (size.value * 0.32f).coerceAtLeast(18f).dp
            val iconSize = (badgeSize.value * 0.6f).dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(BrandPrimary)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = stringResource(R.string.avatar_cd_change_profile_picture),
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
private fun InitialAvatarBox(
    initialLetter: String,
    fontSizeSp: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(BrandPrimary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialLetter,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.Bold,
            color = BrandPrimary
        )
    }
}
