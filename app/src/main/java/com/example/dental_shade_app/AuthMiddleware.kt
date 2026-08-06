package com.example.dental_shade_app

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

object AuthMiddleware {

    fun checkAuthorizationAndNavigate(context: Context, onVerified: () -> Unit = {}) {
        try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            if (currentUser == null) {
                Log.d("[AUTH]", "AuthMiddleware: No active session -> launching LoginActivity")
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
                return
            }

            Log.d("[AUTH]", "AuthMiddleware: Authenticated user (${currentUser.uid}) -> proceeding to Dashboard")
            onVerified()
        } catch (e: Exception) {
            Log.e("[AUTH]", "Exception during auth middleware -> launching LoginActivity", e)
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }
}
