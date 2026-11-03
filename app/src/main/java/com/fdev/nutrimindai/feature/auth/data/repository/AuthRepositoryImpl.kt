package com.fdev.nutrimindai.feature.auth.data.repository

import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.core.health.HealthConnectManager
import com.fdev.nutrimindai.feature.auth.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val preferenceManager: PreferenceManager,
    private val healthConnectManager: HealthConnectManager
) : AuthRepository {

    override suspend fun loginWithEmail(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            healthConnectManager.disconnect()
            firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "AuthRepository: Login failed")
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    override suspend fun registerWithEmail(name: String, email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            healthConnectManager.disconnect()
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user

            // Set user display name if provided
            if (user != null && name.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name.trim())
                    .build()
                user.updateProfile(profileUpdates).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "AuthRepository: Registration failed")
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    override suspend fun loginAnonymously(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            healthConnectManager.disconnect()
            firebaseAuth.signInAnonymously().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "AuthRepository: Anonymous login failed")
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            healthConnectManager.disconnect()
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "AuthRepository: Google sign in failed")
            Result.failure(Exception(mapAuthError(e)))
        }
    }


    override suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "AuthRepository: Password reset failed")
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null || preferenceManager.getDemoUserEmailBlocking() != null
    }

    override fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid ?: preferenceManager.getDemoUserEmailBlocking()
    }

    override fun isAnonymousUser(): Boolean {
        return firebaseAuth.currentUser?.isAnonymous == true
    }

    override suspend fun linkAnonymousWithEmail(
        email: String,
        password: String,
        displayName: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser
                ?: return@withContext Result.failure(Exception("No active user session found."))

            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email.trim(), password)
            val linkResult = user.linkWithCredential(credential).await()

            if (!displayName.isNullOrBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
                user.updateProfile(profileUpdates).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "AuthRepository: Link email account failed")
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    override suspend fun linkAnonymousWithGoogle(
        idToken: String,
        displayName: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser
                ?: return@withContext Result.failure(Exception("No active user session found."))

            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val linkResult = user.linkWithCredential(credential).await()

            if (!displayName.isNullOrBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
                user.updateProfile(profileUpdates).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "AuthRepository: Link Google account failed")
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            healthConnectManager.disconnect()
            firebaseAuth.signOut()
            preferenceManager.setDemoUserEmail(null)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "AuthRepository: Sign out failed")
            Result.failure(e)
        }
    }

    private fun mapAuthError(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> "No account found with this email."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password. Please try again."
            is FirebaseAuthUserCollisionException -> "This email is already registered. Please log in or use a different email."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
            else -> {
                val msg = exception.message ?: ""
                when {
                    msg.contains("network", ignoreCase = true) -> "Failed to connect to server. Please check your internet connection."
                    msg.contains("badly formatted", ignoreCase = true) -> "Invalid email format."
                    msg.contains("blocked", ignoreCase = true) -> "Too many failed attempts. Please try again later."
                    else -> "Authentication error: ${exception.localizedMessage ?: "Please try again."}"
                }
            }
        }
    }
}
