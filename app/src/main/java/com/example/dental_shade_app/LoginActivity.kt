package com.example.dental_shade_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()

        val prefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        val savedEmail = prefs.getString("email", "") ?: ""
        val savedPassword = prefs.getString("password", "") ?: ""
        val savedRememberMe = prefs.getBoolean("remember_me", false)

        // Configure Google Sign-In
        val webClientId = "543866248276-v1l2u3.apps.googleusercontent.com" 
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId) 
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        setContent {
            ShadeScanTheme {
                LoginScreen(
                    initialEmail = savedEmail,
                    initialPassword = savedPassword,
                    initialRememberMe = savedRememberMe,
                    onLogin = { email, password, rememberMe -> loginUser(email, password, rememberMe) },
                    onGoogleLogin = { signInWithGoogle() },
                    onForgotPassword = { email -> resetPassword(email) },
                    onSignUpClick = { 
                        startActivity(Intent(this, SignUpActivity::class.java))
                    }
                )
            }
        }
    }

    private fun loginUser(email: String, password: String, rememberMe: Boolean) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        saveCredentials(email, password, rememberMe)
                        navigateToDashboard()
                    } else if (user != null) {
                        Toast.makeText(this, "Please verify your email first. A link was sent to ${user.email}.", Toast.LENGTH_LONG).show()
                        auth.signOut()
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthInvalidUserException) {
                        Toast.makeText(this, "Account not found. Please Sign Up first.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Login Failed: ${exception?.message}", Toast.LENGTH_LONG).show()
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

    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToDashboard()
                } else {
                    Toast.makeText(this, "Google Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun resetPassword(email: String) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email to receive reset link", Toast.LENGTH_SHORT).show()
            return
        }
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Reset link sent to $email. Please check your inbox.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
