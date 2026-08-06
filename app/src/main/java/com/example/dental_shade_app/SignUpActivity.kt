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
                        u.sendEmailVerification()
                        val profileData = mapOf(
                            "uid" to u.uid,
                            "email" to cleanEmail,
                            "fullName" to cleanName,
                            "provider" to "email",
                            "accountStatus" to "active",
                            "registrationCompleted" to true,
                            "createdAt" to System.currentTimeMillis()
                        )

                        database.getReference("Users").child(u.uid).setValue(profileData).addOnCompleteListener {
                            database.getReference("doctors").child(u.uid).setValue(profileData).addOnCompleteListener {
                                Toast.makeText(this, "Account Created Successfully! Verification email sent.", Toast.LENGTH_LONG).show()
                                val intent = Intent(this, DashboardActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
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
