package com.example.dental_shade_app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(32.dp),
            color = PortalAccentLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.SettingsSuggest,
                    contentDescription = null,
                    tint = PortalAccent,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "ShadeScan AI",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = PortalTextMain
        )
        
        Text(
            text = "Precision Dental Shade Analysis\nPowered by Artificial Intelligence",
            fontSize = 16.sp,
            color = PortalTextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.weight(1.2f))
        
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PortalDark)
        ) {
            Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun LoginScreen(
    initialEmail: String,
    initialPassword: String,
    initialRememberMe: Boolean,
    onLogin: (String, String, Boolean) -> Unit,
    onGoogleLogin: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignUpClick: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf(initialPassword) }
    var rememberMe by remember { mutableStateOf(initialRememberMe) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = PortalAccentLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.SettingsSuggest,
                    contentDescription = null,
                    tint = PortalAccent,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "ShadeScan AI",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PortalTextMain
        )
        Text(
            text = "Clinical Portal Login",
            fontSize = 14.sp,
            color = PortalTextMuted
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PortalCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Welcome Back",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PortalTextMain
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PortalTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PortalAccent,
                        unfocusedBorderColor = PortalDivider
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PortalTextMuted) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = PortalTextMuted
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PortalAccent,
                        unfocusedBorderColor = PortalDivider
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = PortalAccent)
                        )
                        Text("Remember Me", fontSize = 14.sp, color = PortalTextMuted)
                    }
                    TextButton(onClick = { onForgotPassword(email) }) {
                        Text("Forgot?", color = PortalAccent, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { onLogin(email, password, rememberMe) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PortalDark)
                ) {
                    Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = PortalDivider)
            Text("  OR  ", color = PortalTextMuted, fontSize = 12.sp)
            HorizontalDivider(modifier = Modifier.weight(1f), color = PortalDivider)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedButton(
            onClick = onGoogleLogin,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PortalDivider),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PortalTextMain)
        ) {
            Icon(Icons.Default.AccountCircle, null, tint = PortalAccent)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Sign in with Google", fontWeight = FontWeight.Medium)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            Text("New to ShadeScan?", color = PortalTextMuted)
            TextButton(onClick = onSignUpClick) {
                Text("Create Account", color = PortalAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignUp: (String, String, String, String, String, String, String) -> Unit,
    onLoginClick: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var username by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Email Verification State
    var isEmailVerified by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding()) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = PortalDark)
        }
        
        Text(
            text = "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PortalTextMain,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Complete your clinical profile",
            fontSize = 16.sp,
            color = PortalTextMuted
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PortalCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Personal Information", fontWeight = FontWeight.Bold, color = PortalTextMain, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent, unfocusedBorderColor = PortalDivider)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent, unfocusedBorderColor = PortalDivider)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PortalBg)
                            .clickable { gender = if (gender == "Male") "Female" else "Male" }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("Gender: $gender", color = PortalTextMain, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent, unfocusedBorderColor = PortalDivider)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent, unfocusedBorderColor = PortalDivider)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Account Access", fontWeight = FontWeight.Bold, color = PortalTextMain, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { 
                                email = it
                                isEmailVerified = false
                                emailError = null
                            },
                            label = { Text("Email Address") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            isError = emailError != null,
                            enabled = !isEmailVerified && !isVerifying,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent, unfocusedBorderColor = PortalDivider)
                        )
                        
                        Button(
                            onClick = {
                                if (email.isBlank()) {
                                    emailError = "Email cannot be empty"
                                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    emailError = "Invalid email format"
                                } else {
                                    scope.launch {
                                        isVerifying = true
                                        delay(2000) // Simulation delay
                                        isVerifying = false
                                        isEmailVerified = true
                                    }
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isEmailVerified && !isVerifying && email.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEmailVerified) Color(0xFF10B981) else PortalAccent
                            )
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(if (isEmailVerified) "Verified" else "Verify")
                            }
                        }
                    }
                    
                    if (emailError != null) {
                        Text(
                            text = emailError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                    
                    if (isEmailVerified) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verified", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verification email sent successfully.", color = PortalTextMuted, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (6+ chars)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = PortalTextMuted)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent, unfocusedBorderColor = PortalDivider)
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onSignUp(fullName, age, gender, username, mobile, email, password) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PortalDark),
                    enabled = fullName.isNotBlank() && isEmailVerified && password.length >= 6
                ) {
                    Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account?", color = PortalTextMuted)
            TextButton(onClick = onLoginClick) {
                Text("Login", color = PortalAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}
