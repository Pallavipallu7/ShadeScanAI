package com.example.dental_shade_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var loginErrorMessage by mutableStateOf<String?>(null)
    private var isAuthLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()

        // Check if user is already signed in
        if (auth.currentUser != null) {
            navigateToDashboard()
            return
        }

        val authMessage = intent.getStringExtra("auth_message")
        if (!authMessage.isNullOrEmpty()) {
            loginErrorMessage = authMessage
        }

        val prefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        val savedEmail = prefs.getString("email", "") ?: ""
        val savedPassword = prefs.getString("password", "") ?: ""
        val savedRememberMe = prefs.getBoolean("remember_me", false)

        setContent {
            ShadeScanTheme {
                LoginScreen(
                    initialEmail = savedEmail,
                    initialPassword = savedPassword,
                    initialRememberMe = savedRememberMe,
                    errorMessage = loginErrorMessage,
                    isLoading = isAuthLoading,
                    onLogin = { email, password, rememberMe -> loginUser(email, password, rememberMe) },
                    onGoogleLogin = {},
                    onForgotPassword = { email -> resetPassword(email) },
                    onSignUpClick = { 
                        startActivity(Intent(this, SignUpActivity::class.java))
                    }
                )
            }
        }
    }

    private fun loginUser(email: String, password: String, rememberMe: Boolean) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || password.isEmpty()) {
            loginErrorMessage = "Please enter your email and password."
            return
        }
        loginErrorMessage = null
        isAuthLoading = true

        // Attempt sign-in directly — never auto-redirect to SignUp from here
        auth.signInWithEmailAndPassword(cleanEmail, password)
            .addOnCompleteListener(this) { task ->
                isAuthLoading = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        saveCredentials(cleanEmail, password, rememberMe)
                        navigateToDashboard()
                    }
                } else {
                    val exception = task.exception
                    val msg = exception?.message ?: ""
                    Log.e("LoginActivity", "Login failed - Exception: ${exception?.javaClass?.simpleName}, Message: $msg")
                    when {
                        // In newer Firebase SDK, FirebaseAuthInvalidCredentialsException covers
                        // both "wrong password" and "user not found" — redirect to SignUp for all cases
                        exception is FirebaseAuthInvalidUserException
                            || exception is FirebaseAuthInvalidCredentialsException
                            || msg.contains("user-not-found", true)
                            || msg.contains("no user record", true)
                            || msg.contains("invalid-credential", true)
                            || msg.contains("incorrect", true)
                            || msg.contains("malformed", true)
                            || msg.contains("expired", true) -> {
                            Log.d("LoginActivity", "Redirecting to SignUp - credential issue or user not found")
                            val intent = Intent(this, SignUpActivity::class.java).apply {
                                putExtra("prefill_email", cleanEmail)
                            }
                            startActivity(intent)
                        }
                        msg.contains("wrong-password", true) ->
                            loginErrorMessage = "Incorrect password. Please try again."
                        msg.contains("invalid-email", true) ->
                            loginErrorMessage = "Enter a valid email address."
                        msg.contains("network", true) ->
                            loginErrorMessage = "Check your internet connection and try again."
                        else -> {
                            Log.d("LoginActivity", "Unhandled error - redirecting to SignUp")
                            val intent = Intent(this, SignUpActivity::class.java).apply {
                                putExtra("prefill_email", cleanEmail)
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
    }

    private fun saveCredentials(email: String, pass: String, rememberMe: Boolean) {
        val prefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            if (rememberMe) {
                putString("email", email)
                putString("password", pass)
                putBoolean("remember_me", true)
            } else {
                clear()
            }
            apply()
        }
    }

    private fun resetPassword(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) {
            loginErrorMessage = "Enter your email address to receive reset link."
            return
        }
        auth.sendPasswordResetEmail(cleanEmail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Reset link sent to $cleanEmail.", Toast.LENGTH_LONG).show()
                } else {
                    val msg = task.exception?.message ?: ""
                    loginErrorMessage = if (msg.contains("invalid-email", true)) "Enter a valid email address." else "Error: $msg"
                }
            }
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
