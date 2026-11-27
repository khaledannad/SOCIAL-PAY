package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "SocialPayAuth"

object CredentialAuthHelper {

  fun authStateFlow(auth: FirebaseAuth): Flow<FirebaseUser?> = callbackFlow {
    trySend(auth.currentUser)
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  /**
   * Silent startup auto sign-in using GetGoogleIdOption in a single-option request.
   * Returns null silently when no authorized account is cached.
   */
  suspend fun attemptSilentGoogleSignIn(
    context: Context,
    auth: FirebaseAuth
  ): Result<FirebaseUser?> {
    return try {
      val existing = auth.currentUser
      if (existing != null) return Result.success(existing)

      val serverClientId = context.getString(R.string.default_web_client_id)
      val credentialManager = CredentialManager.create(context)
      val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(serverClientId)
        .setAutoSelectEnabled(true)
        .build()

      val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

      val response = credentialManager.getCredential(context = context, request = request)
      val user = exchangeCredentialForFirebaseUser(response.credential, auth)
      Result.success(user)
    } catch (e: CancellationException) {
      throw e
    } catch (_: NoCredentialException) {
      Result.success(null)
    } catch (e: GetCredentialCancellationException) {
      Log.w(AUTH_TAG, "Silent Google Sign-In cancelled: ${e.message}", e)
      Result.success(null)
    } catch (e: Exception) {
      Log.w(AUTH_TAG, "Silent auto sign-in unavailable: ${e.message}", e)
      Result.success(null)
    }
  }

  /**
   * Interactive Google Sign-In via GetSignInWithGoogleOption in its own single-option request.
   */
  suspend fun signInWithGoogleInteractive(
    context: Context,
    auth: FirebaseAuth
  ): Result<FirebaseUser> {
    return try {
      val serverClientId = context.getString(R.string.default_web_client_id)
      val credentialManager = CredentialManager.create(context)
      val signInOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
      val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

      val response = credentialManager.getCredential(context = context, request = request)
      val user = exchangeCredentialForFirebaseUser(response.credential, auth)
      Result.success(user)
    } catch (e: CancellationException) {
      throw e
    } catch (e: GetCredentialCancellationException) {
      Log.w(AUTH_TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
      Result.failure(e)
    } catch (e: Exception) {
      Log.e(AUTH_TAG, "Interactive Google Sign-In failed: ${e.message}", e)
      Result.failure(e)
    }
  }

  private suspend fun exchangeCredentialForFirebaseUser(
    credential: androidx.credentials.Credential,
    auth: FirebaseAuth
  ): FirebaseUser {
    if (credential is CustomCredential &&
      credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
      val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
      val authResult = auth.signInWithCredential(firebaseCredential).await()
      return authResult.user
        ?: throw IllegalStateException("Firebase authentication completed without a user record.")
    }
    throw IllegalStateException("Unsupported credential type: ${credential.type}")
  }

  /**
   * Detaches callers/cleanup and clears Credential Manager state under NonCancellable
   * before signing out of FirebaseAuth so active listeners do not race into PERMISSION_DENIED.
   */
  suspend fun signOutWithCredentialClear(
    context: Context,
    auth: FirebaseAuth,
    beforeAuthSignOut: suspend () -> Unit = {}
  ) {
    withContext(NonCancellable) {
      try {
        beforeAuthSignOut()
      } catch (e: Exception) {
        Log.w(AUTH_TAG, "Pre-signout cleanup error: ${e.message}", e)
      }
      try {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}", e)
      }
      try {
        auth.signOut()
      } catch (e: Exception) {
        Log.w(AUTH_TAG, "FirebaseAuth signOut error: ${e.message}", e)
      }
    }
  }
}
