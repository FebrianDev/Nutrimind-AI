package com.fdev.nutrimindai.feature.auth.domain.repository

import com.google.firebase.auth.FirebaseUser

/**
 * Interface contract for authentication operations via Firebase Auth.
 */
interface AuthRepository {

    /**
     * Signs in an existing user using email and password.
     */
    suspend fun loginWithEmail(email: String, password: String): Result<Unit>

    /**
     * Registers a new user with email, password, and display name.
     */
    suspend fun registerWithEmail(name: String, email: String, password: String): Result<Unit>

    /**
     * Signs in as a guest / anonymous user.
     */
    suspend fun loginAnonymously(): Result<Unit>

    /**
     * Sends password reset email.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /**
     * Checks if a user is currently logged in.
     */
    fun isUserLoggedIn(): Boolean

    /**
     * Gets the currently authenticated user.
     */
    fun getCurrentUser(): FirebaseUser?

    /**
     * Gets current user ID or fallback identifier.
     */
    fun getCurrentUserId(): String?

    /**
     * Checks if the currently authenticated user is anonymous.
     */
    fun isAnonymousUser(): Boolean

    /**
     * Links an anonymous account to permanent email and password credentials.
     */
    suspend fun linkAnonymousWithEmail(email: String, password: String, displayName: String? = null): Result<Unit>

    /**
     * Links an anonymous account to Google credentials.
     */
    suspend fun linkAnonymousWithGoogle(idToken: String, displayName: String? = null): Result<Unit>

    /**
     * Signs out the current user.
     */
    suspend fun logout(): Result<Unit>
}
