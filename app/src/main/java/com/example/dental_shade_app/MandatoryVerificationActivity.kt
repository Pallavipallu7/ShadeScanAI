package com.example.dental_shade_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MandatoryVerificationActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val DB_URL = "https://shadescan-ai-default-rtdb.firebaseio.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()
        database = try {
            FirebaseDatabase.getInstance(DB_URL)
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }

        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val initialName = user.displayName ?: ""
        val initialEmail = user.email ?: ""
        val initialPhoto = user.photoUrl?.toString() ?: ""

        setContent {
            ShadeScanTheme {
                MandatoryVerificationScreen(
                    initialFullName = initialName,
                    initialEmail = initialEmail,
                    initialPhotoUrl = initialPhoto,
                    onSubmit = { profileData ->
                        saveProfileAndProceed(profileData)
                    }
                )
            }
        }
    }

    private fun saveProfileAndProceed(profileData: Map<String, Any>) {
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        val uid = user.uid
        val fullData = profileData.toMutableMap()
        fullData["uid"] = uid
        fullData["email"] = user.email ?: ""
        fullData["registrationCompleted"] = true
        fullData["accountStatus"] = "active"
        fullData["updatedAt"] = System.currentTimeMillis()

        database.getReference("Users").child(uid).setValue(fullData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account verified successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    android.util.Log.w("MandatoryVerification", "RTDB write warning: ${task.exception?.message}, completing registration session.")
                    database.getReference("Users").child(uid).updateChildren(fullData)
                }

                val intent = Intent(this, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
    }
}

@Composable
fun MandatoryVerificationScreen(
    initialFullName: String,
    initialEmail: String,
    initialPhotoUrl: String,
    onSubmit: (Map<String, Any>) -> Unit
) {
    // Required Fields
    var fullName by remember { mutableStateOf(initialFullName) }
    var mobile by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var country by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    var acceptedPrivacy by remember { mutableStateOf(false) }

    // Optional Fields
    var address by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var clinicName by remember { mutableStateOf("") }
    var dentistName by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf(initialPhotoUrl) }

    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = fullName.isNotBlank() &&
            mobile.isNotBlank() &&
            age.isNotBlank() &&
            country.isNotBlank() &&
            state.isNotBlank() &&
            city.isNotBlank() &&
            acceptedTerms &&
            acceptedPrivacy

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // Header
        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(24.dp),
            color = PortalAccentLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = PortalAccent,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Complete Your ShadeScan AI Account",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PortalTextMain,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Text(
            text = "Mandatory Practitioner Verification",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = PortalAccent,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (initialEmail.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PortalAccentLight,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PortalAccent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Google Account: $initialEmail",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PortalTextMain
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your Google account has been recognized. Please complete the required verification/registration steps before accessing ShadeScan AI.",
                        fontSize = 12.sp,
                        color = PortalTextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PortalCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Required Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PortalTextMain
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = PortalTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mobile Number
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = PortalTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Age & Gender
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PortalBg)
                            .clickable { gender = if (gender == "Male") "Female" else "Male" }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("Gender: $gender", color = PortalTextMain, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Location Fields: Country, State, City
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Public, null, tint = PortalTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
                    )

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Optional Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PortalTextMain
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = clinicName,
                    onValueChange = { clinicName = it },
                    label = { Text("Clinic Name (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.LocalHospital, null, tint = PortalTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dentistName,
                    onValueChange = { dentistName = it },
                    label = { Text("Dentist Title / Name (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.MedicalServices, null, tint = PortalTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Home, null, tint = PortalTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("Emergency Contact (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.ContactPhone, null, tint = PortalTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Legal Consents
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = acceptedTerms,
                        onCheckedChange = { acceptedTerms = it },
                        colors = CheckboxDefaults.colors(checkedColor = PortalAccent)
                    )
                    Text("I accept the Terms & Conditions *", fontSize = 13.sp, color = PortalTextMain)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = acceptedPrivacy,
                        onCheckedChange = { acceptedPrivacy = it },
                        colors = CheckboxDefaults.colors(checkedColor = PortalAccent)
                    )
                    Text("I accept the Privacy Policy *", fontSize = 13.sp, color = PortalTextMain)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isLoading = true
                        val data = mapOf(
                            "fullName" to fullName,
                            "mobile" to mobile,
                            "age" to age,
                            "gender" to gender,
                            "country" to country,
                            "state" to state,
                            "city" to city,
                            "address" to address,
                            "emergencyContact" to emergencyContact,
                            "clinicName" to clinicName,
                            "dentistName" to dentistName,
                            "photoUrl" to photoUrl,
                            "acceptedTerms" to acceptedTerms,
                            "acceptedPrivacy" to acceptedPrivacy
                        )
                        onSubmit(data)
                    },
                    enabled = isFormValid && !isLoading,
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
                    Text("Complete Verification & Continue", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
