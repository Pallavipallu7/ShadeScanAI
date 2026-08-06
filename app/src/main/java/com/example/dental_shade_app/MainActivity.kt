package com.example.dental_shade_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ShadeScanTheme {
                var isLoading by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    // Always direct to LoginActivity so user is prompted for login credentials
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(28.dp),
                color = PortalAccentLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = PortalAccent,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "ShadeScan AI",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PortalTextMain
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = PortalAccent, strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Verifying Practitioner Credentials...",
                fontSize = 12.sp,
                color = PortalTextMuted
            )
        }
    }
}
