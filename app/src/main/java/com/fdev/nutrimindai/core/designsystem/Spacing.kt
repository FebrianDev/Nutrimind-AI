package com.fdev.nutrimindai.core.designsystem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard 8-point grid spacing system for NutriMind AI.
 * Follows Material Design 3 spacing guidelines.
 */
@Immutable
data class Spacing(
    val default: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,   // 4dp: icon-text gap, compact badge padding
    val small: Dp = 8.dp,        // 8dp: chips, small gaps, compact item spacing
    val semiMedium: Dp = 12.dp,  // 12dp: compact card content, tag horizontal padding
    val medium: Dp = 16.dp,      // 16dp: STANDARD (screen horizontal margin & standard card internal padding)
    val prominent: Dp = 20.dp,   // 20dp: prominent card / hero card padding
    val large: Dp = 24.dp,       // 24dp: major section gap, auth/onboarding screen margin
    val extraLarge: Dp = 32.dp,  // 32dp: hero header / bottom sheet clearance
    val huge: Dp = 48.dp         // 48dp: empty state illustration spacer
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current

/**
 * Global AppSpacing constants accessible everywhere in non-composable code
 * (e.g. `Arrangement.spacedBy(AppSpacing.s)`, `PaddingValues(AppSpacing.m)`).
 */
object AppSpacing {
    val default: Dp = 0.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val sm: Dp = 12.dp
    val m: Dp = 16.dp
    val l: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val huge: Dp = 48.dp
}

// =========================================================================
// Semantic Modifier Extensions (Option 2 - Clean & Standardized)
// =========================================================================

/** Standard screen-level horizontal padding (16dp). */
fun Modifier.screenPadding(): Modifier = this.padding(horizontal = 16.dp)

/** Isolated / onboarding / auth screen-level horizontal padding (24dp). */
fun Modifier.authScreenPadding(): Modifier = this.padding(horizontal = 24.dp)

/** Standard card / container internal padding (16dp). */
fun Modifier.cardPadding(): Modifier = this.padding(16.dp)

/** Auth / Onboarding card internal padding (24dp). */
fun Modifier.authCardPadding(): Modifier = this.padding(24.dp)

/** Compact card / list item internal padding (12dp horizontal, 8dp vertical). */
fun Modifier.compactCardPadding(): Modifier = this.padding(horizontal = 12.dp, vertical = 8.dp)

/** Badge / status pill / chip internal padding (8dp horizontal, 4dp vertical). */
fun Modifier.badgePadding(): Modifier = this.padding(horizontal = 8.dp, vertical = 4.dp)

// =========================================================================
// Semantic Spacer Helpers
// =========================================================================

/** Standard Section vertical spacing spacer (16dp). */
@Composable
fun SectionSpacer() = Spacer(modifier = Modifier.height(16.dp))

/** Small gap spacer (8dp vertical). */
@Composable
fun SmallVerticalSpacer() = Spacer(modifier = Modifier.height(8.dp))

/** Medium gap spacer (16dp vertical). */
@Composable
fun MediumVerticalSpacer() = Spacer(modifier = Modifier.height(16.dp))

/** Large gap spacer (24dp vertical). */
@Composable
fun LargeVerticalSpacer() = Spacer(modifier = Modifier.height(24.dp))

/** Small gap spacer (8dp horizontal). */
@Composable
fun SmallHorizontalSpacer() = Spacer(modifier = Modifier.width(8.dp))

/** Medium gap spacer (16dp horizontal). */
@Composable
fun MediumHorizontalSpacer() = Spacer(modifier = Modifier.width(16.dp))
