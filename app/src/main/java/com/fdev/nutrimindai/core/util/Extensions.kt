package com.fdev.nutrimindai.core.util

import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

/**
 * Returns a [Calendar] normalized to midnight (00:00:00.000) representing when the user created their account/profile.
 *
 * Evaluation order:
 * 1. [ProfileEntity.createdAt] (captures when the user completed onboarding / NutriMind journey setup)
 * 2. Firebase Auth metadata creation timestamp (if available and positive)
 * 3. Start of today as fallback
 *
 * Capped at today to prevent any future clock skew issues.
 */
fun getAccountStartCalendar(profile: ProfileEntity?): Calendar {
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val firebaseCreated = try {
        FirebaseAuth.getInstance().currentUser?.metadata?.creationTimestamp?.takeIf { it > 0 }
    } catch (_: Exception) {
        null
    }

    val creationMillis = profile?.createdAt?.takeIf { it > 0 }
        ?: firebaseCreated
        ?: return todayCal

    val accountCal = Calendar.getInstance().apply {
        timeInMillis = creationMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return if (accountCal.after(todayCal)) todayCal else accountCal
}
