package com.example.dental_shade_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val DB_URL = "https://shadescan-ai-default-rtdb.firebaseio.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(DB_URL)

        val prefillEmail = intent.getStringExtra("prefill_email") ?: ""

        setContent {
            ShadeScanTheme {
                SignUpScreen(
                    initialEmail = prefillEmail,
                    onBack = { finish() },
                    onSignUp = { fullName, email, password ->
                        handleSignUp(fullName, email, password)
                    },
                    onLoginClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    private fun handleSignUp(fullName: String, email: String, pass: String) {
        val cleanEmail = email.trim()
        val cleanName = fullName.trim()

        auth.createUserWithEmailAndPassword(cleanEmail, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let { u ->
                        // Send verification email first
                        u.sendEmailVerification().addOnCompleteListener {
                            // Save pending registration data to be written after verification
                            val prefs = getSharedPreferences("pending_registration", MODE_PRIVATE)
                            prefs.edit()
                                .putString("fullName", cleanName)
                                .putString("username", cleanEmail.split("@")[0])
                                .apply()

                            Toast.makeText(
                                this,
                                "Verification email sent to $cleanEmail. Please verify to continue.",
                                Toast.LENGTH_LONG
                            ).show()

                            // Go to Email Verification screen — NOT Dashboard
                            val intent = Intent(this, EmailVerificationActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                } else {
                    val exception = task.exception
                    val msg = exception?.message ?: ""
                    if (exception is FirebaseAuthUserCollisionException || msg.contains("email-already-in-use", true) || msg.contains("already in use", true)) {
                        val intent = Intent(this, LoginActivity::class.java).apply {
                            putExtra("auth_message", "An account already exists with this email. Please sign in.")
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Registration Error: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }
}
