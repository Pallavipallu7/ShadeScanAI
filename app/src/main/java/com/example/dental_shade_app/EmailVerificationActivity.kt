package com.example.dental_shade_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val DB_URL = "https://shadescan-ai-default-rtdb.firebaseio.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()
        database = try { FirebaseDatabase.getInstance(DB_URL) } catch (e: Exception) { FirebaseDatabase.getInstance() }

        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            ShadeScanTheme {
                EmailVerificationScreen(
                    userEmail = user.email ?: "",
                    onCheckVerified = { checkEmailVerification() },
                    onResendEmail = { resendVerificationEmail() },
                    onReturnToLogin = {
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    private fun checkEmailVerification() {
        val user = auth.currentUser ?: return
        user.reload().addOnCompleteListener { task ->
            if (task.isSuccessful && user.isEmailVerified) {
                // Email is verified -> Create/Update user profile in DB and navigate to Dashboard
                val prefs = getSharedPreferences("pending_registration", Context.MODE_PRIVATE)
                val fullName = prefs.getString("fullName", user.displayName ?: "Doctor") ?: "Doctor"
                val username = prefs.getString("username", user.email?.split("@")?.get(0) ?: "user") ?: "user"
                val age = prefs.getString("age", "") ?: ""
                val gender = prefs.getString("gender", "Male") ?: "Male"
                val mobile = prefs.getString("mobile", "") ?: ""

                val userMap = mapOf<String, Any>(
                    "uid" to user.uid,
                    "name" to fullName,
                    "fullName" to fullName,
                    "username" to username,
                    "email" to (user.email ?: ""),
                    "mobile" to mobile,
                    "age" to age,
                    "gender" to gender,
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "provider" to "email",
                    "registrationCompleted" to true,
                    "accountStatus" to "active",
                    "createdAt" to System.currentTimeMillis(),
                    "lastLogin" to System.currentTimeMillis()
                )

                database.getReference("Users").child(user.uid).setValue(userMap)
                    .addOnCompleteListener { dbTask ->
                        prefs.edit().clear().apply()
                        Toast.makeText(this, "Email verified successfully! Welcome to ShadeScan AI.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
            } else {
                Toast.makeText(this, "Email is not verified yet. Please check your inbox and click the link.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resendVerificationEmail() {
        val user = auth.currentUser ?: return
        user.sendEmailVerification().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Verification email resent to ${user.email}.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun EmailVerificationScreen(
    userEmail: String,
    onCheckVerified: () -> Unit,
    onResendEmail: () -> Unit,
    onReturnToLogin: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = RoundedCornerShape(28.dp),
            color = PortalAccentLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = PortalAccent,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verify Your Email Address",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PortalTextMain,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Verification email has been sent to:\n$userEmail\n\nPlease verify your email before continuing.",
            fontSize = 14.sp,
            color = PortalTextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PortalCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = {
                        isLoading = true
                        onCheckVerified()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PortalAccent)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("I Have Verified My Email", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onResendEmail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PortalAccent)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = PortalAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resend Verification Email", color = PortalAccent, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(onClick = onReturnToLogin) {
                    Text("Change Email / Return to Login", color = PortalTextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}
