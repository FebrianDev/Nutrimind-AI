package com.fdev.nutrimindai.core.sync

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthManager @Inject constructor() {

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Timber.w("FirebaseAuthManager: Firebase is not initialized. Please ensure google-services.json is placed in app/.")
            null
        }
    }

    val currentUserId: String?
        get() = auth?.currentUser?.uid

    val isAuthenticated: Boolean
        get() = auth?.currentUser != null

    suspend fun getOrCreateUserId(): String {
        val firebaseAuth = auth ?: return "local_user"
        val user = firebaseAuth.currentUser
        if (user != null) {
            return user.uid
        }
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val newUid = authResult.user?.uid ?: "local_user"
            Timber.d("FirebaseAuthManager: Signed in anonymously with UID=$newUid")
            newUid
        } catch (e: Exception) {
            Timber.e(e, "FirebaseAuthManager: Anonymous sign-in failed")
            "local_user"
        }
    }
}
