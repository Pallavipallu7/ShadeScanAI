package com.example.dental_shade_app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
    errorMessage: String? = null,
    isLoading: Boolean = false,
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
        Spacer(modifier = Modifier.height(28.dp))
        
        // App Logo
        Surface(
            modifier = Modifier.size(88.dp),
            shape = RoundedCornerShape(28.dp),
            color = PortalAccentLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = "ShadeScan AI Tooth Logo",
                    tint = PortalAccent,
                    modifier = Modifier.size(52.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Welcome Message
        Text(
            text = "ShadeScan AI",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PortalTextMain
        )
        Text(
            text = "Clinical Tooth Shade Intelligence Portal",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = PortalTextMuted,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = PortalCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Welcome Back, Doctor",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PortalTextMain
                )
                Text(
                    text = "Sign in with your email and password",
                    fontSize = 12.sp,
                    color = PortalTextMuted
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                // Error Banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFCDD2))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(it, color = Color(0xFFD32F2F), fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp)
                            }
                        }
                    }
                }
                
                // Secondary: Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PortalTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PortalAccent,
                        unfocusedBorderColor = PortalDivider
                    )
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Secondary: Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
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
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            enabled = !isLoading,
                            colors = CheckboxDefaults.colors(checkedColor = PortalAccent)
                        )
                        Text("Remember Me", fontSize = 13.sp, color = PortalTextMuted, fontWeight = FontWeight.Medium)
                    }
                    TextButton(
                        onClick = { onForgotPassword(email) },
                        enabled = !isLoading
                    ) {
                        Text("Forgot Password?", color = PortalAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Primary Sign In Button
                Button(
                    onClick = { onLogin(email, password, rememberMe) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PortalAccent,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = if (isLoading) "Signing In..." else "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Create Account Link
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("New to ShadeScan AI?", color = PortalTextMuted, fontSize = 14.sp)
            TextButton(
                onClick = onSignUpClick,
                enabled = !isLoading
            ) {
                Text("Create Account", color = PortalAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SignUpScreen(
    initialEmail: String = "",
    onBack: () -> Unit,
    onSignUp: (String, String, String) -> Unit,
    onLoginClick: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            text = "Sign up with your email and password",
            fontSize = 15.sp,
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
                
                // Error Banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFCDD2))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(it, color = Color(0xFFD32F2F), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Full Name Field
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { 
                        fullName = it
                        errorMessage = null
                    },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PortalTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent, unfocusedBorderColor = PortalDivider)
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PortalTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent, unfocusedBorderColor = PortalDivider)
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password (min 6 characters)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PortalTextMuted) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = PortalTextMuted)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent, unfocusedBorderColor = PortalDivider)
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { 
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirm Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PortalTextMuted) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent, unfocusedBorderColor = PortalDivider)
                )
                
                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { 
                        if (email.isBlank()) {
                            errorMessage = "Please enter your email address."
                        } else if (password.length < 6) {
                            errorMessage = "Password must be at least 6 characters."
                        } else if (password != confirmPassword) {
                            errorMessage = "Passwords do not match."
                        } else {
                            onSignUp(fullName, email, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PortalAccent, contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
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
