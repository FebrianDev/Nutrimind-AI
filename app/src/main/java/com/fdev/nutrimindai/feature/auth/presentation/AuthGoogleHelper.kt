package com.fdev.nutrimindai.feature.auth.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

object AuthGoogleHelper {

    fun performGoogleSignIn(
        context: Context,
        coroutineScope: CoroutineScope,
        onIdTokenReceived: (String) -> Unit,
        onOAuthSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val webClientIdResId = context.resources.getIdentifier(
                    "default_web_client_id",
                    "string",
                    context.packageName
                )
                val webClientId = if (webClientIdResId != 0) context.getString(webClientIdResId) else null

                if (!webClientId.isNullOrBlank()) {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(request = request, context = context)
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    onIdTokenReceived(idToken)
                    return@launch
                }
            } catch (_: GetCredentialCancellationException) {
                Timber.i("Google Sign-In was canceled by user")
                return@launch
            } catch (e: GetCredentialException) {
                Timber.w(e, "CredentialManager sign-in failed, attempting OAuthProvider fallback")
            } catch (e: Exception) {
                Timber.w(e, "CredentialManager error, falling back to OAuthProvider")
            }

            // Fallback to Firebase OAuthProvider
            val activity = context.findActivity()
            if (activity != null) {
                val provider = OAuthProvider.newBuilder("google.com")
                val pendingAuthResult = FirebaseAuth.getInstance().pendingAuthResult
                if (pendingAuthResult != null) {
                    pendingAuthResult
                        .addOnSuccessListener { onOAuthSuccess() }
                        .addOnFailureListener { e -> onError(e.localizedMessage ?: "Google Sign-In failed") }
                } else {
                    FirebaseAuth.getInstance().startActivityForSignInWithProvider(activity, provider.build())
                        .addOnSuccessListener { onOAuthSuccess() }
                        .addOnFailureListener { e -> onError(e.localizedMessage ?: "Google Sign-In failed") }
                }
            } else {
                onError("Unable to find Activity context for Google sign-in.")
            }
        }
    }

    private tailrec fun Context.findActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
    }
}
