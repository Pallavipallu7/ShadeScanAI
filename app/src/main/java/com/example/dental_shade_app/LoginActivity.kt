package com.example.dental_shade_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
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

        // First check if email exists in Firebase Authentication
        auth.fetchSignInMethodsForEmail(cleanEmail)
            .addOnCompleteListener { fetchTask ->
                if (fetchTask.isSuccessful) {
                    val signInMethods = fetchTask.result?.signInMethods
                    if (signInMethods == null || signInMethods.isEmpty()) {
                        // Email does NOT exist in Firebase Auth -> Redirect to Sign Up page automatically
                        isAuthLoading = false
                        val intent = Intent(this, SignUpActivity::class.java).apply {
                            putExtra("prefill_email", cleanEmail)
                        }
                        startActivity(intent)
                        return@addOnCompleteListener
                    }
                }

                // Email exists (or fetch fallback), attempt sign in with password
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
                            if (exception is FirebaseAuthInvalidUserException || msg.contains("user-not-found", true) || msg.contains("no user record", true)) {
                                // Redirect to Sign Up page pre-filling email
                                val intent = Intent(this, SignUpActivity::class.java).apply {
                                    putExtra("prefill_email", cleanEmail)
                                }
                                startActivity(intent)
                            } else if (exception is FirebaseAuthInvalidCredentialsException || msg.contains("wrong-password", true) || msg.contains("invalid-credential", true)) {
                                loginErrorMessage = "Incorrect password. Please try again."
                            } else if (msg.contains("invalid-email", true)) {
                                loginErrorMessage = "Enter a valid email address."
                            } else if (msg.contains("network", true)) {
                                loginErrorMessage = "Check your internet connection and try again."
                            } else {
                                loginErrorMessage = "Incorrect password. Please try again."
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
