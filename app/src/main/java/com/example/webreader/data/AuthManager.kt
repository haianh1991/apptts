package com.example.webreader.data

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class AuthManager(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val analytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun getGoogleSignInClient(): GoogleSignInClient {
        val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val webClientId = if (resourceId != 0) context.getString(resourceId) else ""
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("User is null after signing in")
            
            // Save to Firestore
            saveUserToFirestore(user)
            
            // Log Analytics Event
            logLoginSuccess(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthManager", "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            getGoogleSignInClient().signOut().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign out failed", e)
            Result.failure(e)
        }
    }

    private suspend fun saveUserToFirestore(user: FirebaseUser) {
        try {
            val userRef = firestore.collection("users").document(user.uid)
            val userData = hashMapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "lastLogin" to Date()
            )
            userRef.set(userData).await()
            Log.d("AuthManager", "User metadata saved to Firestore successfully")
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed to save user metadata to Firestore", e)
        }
    }

    private fun logLoginSuccess(user: FirebaseUser) {
        try {
            val bundle = android.os.Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, "google")
                putString("user_id", user.uid)
                putString("email", user.email ?: "")
                putString("name", user.displayName ?: "")
            }
            analytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
            analytics.logEvent("login_google_success", bundle)
            Log.d("AuthManager", "Logged login analytics event successfully")
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed to log analytics event", e)
        }
    }
}
