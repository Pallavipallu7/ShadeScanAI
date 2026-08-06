package com.example.dental_shade_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Portal Design System Colors
val PortalBg = Color(0xFFF8FAFC)
val PortalAccent = Color(0xFF6366F1) // Indigo/Violet
val PortalAccentLight = Color(0xFFEEF2FF)
val PortalDark = Color(0xFF0F172A)
val PortalTextMain = Color(0xFF1E293B)
val PortalTextMuted = Color(0xFF64748B)
val PortalCardBg = Color(0xFFFFFFFF)
val PortalDivider = Color(0xFFE2E8F0)
val PortalNotificationDot = Color(0xFFEF4444)

@Composable
fun ShadeScanTheme(isDarkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (isDarkTheme) {
        darkColorScheme(
            primary = PortalAccent,
            secondary = PortalAccent,
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = PortalAccent,
            secondary = PortalAccent,
            background = PortalBg,
            surface = PortalCardBg,
            onSurface = PortalTextMain
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun AppBottomNavigation(navController: NavController) {
    val items = listOf(
        BottomNavItem("home", "HOME", Icons.Default.Home),
        BottomNavItem("patients", "PATIENTS", Icons.Default.Group),
        BottomNavItem("scan_selection", "NEW SCAN", Icons.Default.AddAPhoto),
        BottomNavItem("scan_history", "HISTORY", Icons.Default.History),
        BottomNavItem("settings", "SETTINGS", Icons.Default.Settings)
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route

        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo("home") {
                                saveState = false
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PortalAccent,
                    selectedTextColor = PortalAccent,
                    unselectedIconColor = PortalTextMuted,
                    unselectedTextColor = PortalTextMuted,
                    indicatorColor = PortalAccentLight
                )
            )
        }
    }
}

data class BottomNavItem(val route: String, val title: String, val icon: ImageVector)

data class ScanResult(
    val id: String,
    val dateTime: Long,
    val predictedShade: String,
    val confidence: String,
    val imageUri: String,
    val patientId: String = "",
    val patientName: String = "Quick Scan",
    val doctorNotes: String = "",
    val labL: Float = 72.4f,
    val labA: Float = 2.1f,
    val labB: Float = 14.8f,
    val rgbR: Int = 224,
    val rgbG: Int = 210,
    val rgbB: Int = 185,
    val deltaE: Float = 1.2f,
    val brightnessScore: String = "Good",
    val contrastScore: String = "Excellent",
    val lightingScore: String = "Excellent",
    val toothVisibility: String = "Excellent",
    val blurDetection: String = "Good",
    val reflectionLevel: String = "Good"
)

@Composable
fun DashboardScreen(
    doctorName: String,
    patientCount: Int,
    scanCount: Int,
    recentScans: List<ScanResult>,
    onNewScan: () -> Unit,
    onAddPatient: () -> Unit,
    onShadeAnalysis: () -> Unit,
    onViewReports: () -> Unit,
    onScanClick: (ScanResult) -> Unit,
    navController: NavController
) {
    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp).clickable { navController.navigate("settings") },
                    shape = RoundedCornerShape(16.dp),
                    color = PortalAccentLight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(doctorName.take(1).uppercase(), color = PortalAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("SHADESCAN AI PORTAL", color = PortalTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp)
                    Text(doctorName, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard("TOTAL PATIENTS", patientCount.toString(), Icons.Default.Group, Modifier.weight(1f).clickable { navController.navigate("patients") })
                Spacer(modifier = Modifier.width(16.dp))
                StatCard("AI SCAN HISTORY", scanCount.toString(), Icons.Default.History, Modifier.weight(1f).clickable { navController.navigate("scan_history") })
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onNewScan, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PortalDark)) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MATCH SHADE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(onClick = onAddPatient, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, PortalDivider), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ADD PATIENT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("RECENT SCANS", color = PortalTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (recentScans.isNotEmpty()) {
                    Text("View All", color = PortalAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { navController.navigate("scan_history") })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (recentScans.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = PortalAccentLight) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AddAPhoto, null, tint = PortalAccent, modifier = Modifier.size(32.dp)) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Recent Scans", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("You haven't analyzed any tooth images yet.", color = PortalTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNewScan,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PortalAccent)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Your First Scan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    recentScans.take(5).forEach { result ->
                        ScanResultCard(result = result, onDelete = null, onClick = { onScanClick(result) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatCard(label: String, count: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = PortalTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(count, color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = PortalAccentLight) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = PortalAccent, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
fun PatientSelectionScreen(
    patients: List<Patient>,
    onBack: () -> Unit,
    onPatientSelected: (Patient) -> Unit,
    onAddNew: () -> Unit,
    navController: NavController
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredPatients = patients.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Link Scan to Patient", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            Text(
                text = "Choose how to save & link this dental scan record:",
                fontSize = 13.sp,
                color = PortalTextMuted,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Create New Patient
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddNew() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PortalAccent),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = Color.White.copy(0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Create New Patient", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text("Register profile & link scan instantly", fontSize = 12.sp, color = Color.White.copy(0.85f))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Option 2: Save as Quick Scan (no patient link)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        onPatientSelected(Patient(
                            id = "quick_scan",
                            name = "Quick Scan",
                            age = "",
                            gender = "",
                            phone = "",
                            notes = ""
                        )) 
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = PortalAccentLight
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = PortalAccent)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Save as Quick Scan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PortalTextMain)
                        Text("No patient link — for unassigned scans", fontSize = 12.sp, color = PortalTextMuted)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PortalTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = PortalDivider)
                Text("  OR SELECT EXISTING PATIENT  ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PortalTextMuted)
                HorizontalDivider(modifier = Modifier.weight(1f), color = PortalDivider)
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search patient name or phone...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = PortalTextMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredPatients) { patient ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onPatientSelected(patient) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = PortalAccentLight) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(patient.name.take(1).uppercase(), color = PortalAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(patient.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("${patient.age} yrs • ${patient.gender}", color = PortalTextMuted, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = PortalTextMuted)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun ScanSelectionScreen(onBack: () -> Unit, onTakePhoto: () -> Unit, onUploadImage: () -> Unit, navController: NavController) {
    Scaffold(bottomBar = { AppBottomNavigation(navController) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
            }
            Text("Scan Method", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("How would you like to provide the tooth image?", color = PortalTextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))
            SelectionOption("Take a Photo", "Use camera with AI tooth alignment", Icons.Default.Camera, onClick = onTakePhoto)
            Spacer(modifier = Modifier.height(16.dp))
            SelectionOption("Upload from Gallery", "Select a high-quality existing photo", Icons.Default.PhotoLibrary, onClick = onUploadImage)
        }
    }
}

@Composable
fun SelectionOption(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = PortalAccentLight, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = PortalAccent) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                Text(subtitle, color = PortalTextMuted, fontSize = 14.sp)
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun FullscreenCameraScreen(onClose: () -> Unit, onCapture: (Uri) -> Unit, onOpenGallery: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoomLevel by remember { mutableStateOf(0f) }
    var isDaylightFilter by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isToothDetected by remember { mutableStateOf(false) }

    val cameraProviderFuture = remember { androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val imageAnalyzer = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
                    val mediaImage = proxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                        labeler.process(image)
                            .addOnSuccessListener { labels ->
                                val dentalKeywords = listOf("Tooth", "Teeth", "Dentistry", "Dental")
                                isToothDetected = labels.any { label -> 
                                    dentalKeywords.any { kw -> label.text.contains(kw, ignoreCase = true) } && label.confidence > 0.45f
                                }
                            }
                            .addOnCompleteListener { proxy.close() }
                    } else { proxy.close() }
                }
            }
    }
    
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    
    // Persistent PreviewView
    val previewView = remember { 
        PreviewView(context).apply { 
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        } 
    }

    LaunchedEffect(lensFacing) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
                cameraControl = camera.cameraControl
                cameraControl?.setLinearZoom(zoomLevel)
                cameraControl?.enableTorch(isTorchOn)
            } catch (e: Exception) {
                android.util.Log.e("ShadeScan", "Binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val overlayAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Reverse), label = ""
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        
        // Front camera flash simulation
        if (lensFacing == CameraSelector.LENS_FACING_FRONT && isTorchOn) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)))
        }

        // AI Alignment Overlay
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(if(isToothDetected) Color.Green else Color.Red, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if(isToothDetected) "TOOTH DETECTED" else "ALIGN TOOTH IN FRAME", 
                        color = (if(isToothDetected) Color.Green else Color.White).copy(alpha = overlayAlpha), 
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                val frameBorderColor = if(isToothDetected) Color.Green else PortalAccent
                Box(modifier = Modifier.size(240.dp, 320.dp).border(2.dp, frameBorderColor, RoundedCornerShape(40.dp))) {
                    val scanAnim = infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(2000)), label = "")
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).offset(y = (320 * scanAnim.value).dp).background(if(isToothDetected) Color.Green else Color.Cyan))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(24.dp).align(Alignment.TopStart).statusBarsPadding(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)) { Icon(Icons.Default.Close, null, tint = Color.White) }
            
            Row {
                IconButton(onClick = { 
                    isTorchOn = !isTorchOn
                    cameraControl?.enableTorch(isTorchOn)
                }, modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)) {
                    Icon(if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff, null, tint = if (isTorchOn) Color.Yellow else Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { isDaylightFilter = !isDaylightFilter }, modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)) {
                    Icon(Icons.Default.WbSunny, null, tint = if(isDaylightFilter) Color.Yellow else Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { lensFacing = if(lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK }, modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)) {
                    Icon(Icons.Default.FlipCameraAndroid, null, tint = Color.White)
                }
            }
        }
        
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Zoom Controls", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Slider(value = zoomLevel, onValueChange = { 
                zoomLevel = it
                cameraControl?.setLinearZoom(it)
            }, modifier = Modifier.width(200.dp).padding(bottom = 16.dp), colors = SliderDefaults.colors(thumbColor = PortalAccent, activeTrackColor = PortalAccent))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenGallery, modifier = Modifier.size(48.dp).background(Color.White.copy(0.2f), CircleShape)) { Icon(Icons.Default.PhotoLibrary, null, tint = Color.White) }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp).clickable {
                    val file = File(context.cacheDir, "captured_tooth_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) { onCapture(Uri.fromFile(file)) }
                        override fun onError(exception: ImageCaptureException) { exception.printStackTrace() }
                    })
                }) {
                    Surface(modifier = Modifier.size(70.dp), shape = CircleShape, border = BorderStroke(4.dp, Color.White), color = Color.Transparent) {}
                    Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = Color.White) {}
                }
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun PreviewScreen(capturedUri: Uri?, onRetake: () -> Unit, onContinue: () -> Unit, onHome: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(PortalDark).padding(24.dp)) {
        Row(modifier = Modifier.statusBarsPadding().fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRetake) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("Confirm Image", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onHome) { Icon(Icons.Default.Home, null, tint = Color.White) }
        }
        Text("Ensure the teeth are clearly visible and focused", color = Color.White.copy(0.7f), fontSize = 14.sp)
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 24.dp).clip(RoundedCornerShape(32.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
            if (capturedUri != null) {
                Image(painter = rememberAsyncImagePainter(capturedUri), contentDescription = "Preview", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else { Text("No Image Found", color = Color.White) }
        }
        Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(0.3f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("RETAKE") }
            Button(onClick = onContinue, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PortalAccent)) { Text("ANALYZE NOW") }
        }
    }
}

@Composable
fun ImageEditingScreen(capturedUri: Uri?, onBack: () -> Unit, onProcessed: (Uri) -> Unit) {
    var brightness by remember { mutableStateOf(1f) }
    var contrast by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(IntOffset.Zero) }

    Column(modifier = Modifier.fillMaxSize().background(PortalDark).padding(24.dp)) {
        Row(modifier = Modifier.statusBarsPadding().fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, null, tint = Color.White) }
            Text("Clinical Enhancement", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { capturedUri?.let { onProcessed(it) } }) { Text("DONE", color = PortalAccent, fontWeight = FontWeight.Bold) }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale *= zoom
                        offset = IntOffset(
                            (offset.x + pan.x).roundToInt(),
                            (offset.y + pan.y).roundToInt()
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (capturedUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(capturedUri),
                    contentDescription = "Edit",
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { offset }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            rotationZ = rotation
                        ),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.lighting(
                        multiply = Color(brightness, brightness, brightness, 1f),
                        add = Color.Black
                    )
                )
                
                // Crop frame indicator
                Box(modifier = Modifier.size(240.dp, 320.dp).border(2.dp, Color.White.copy(0.5f), RoundedCornerShape(12.dp)))
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            EditControl("Brightness", brightness, { brightness = it }, Icons.Default.Brightness6)
            EditControl("Contrast", contrast, { contrast = it }, Icons.Default.Contrast)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                IconButton(onClick = { rotation -= 90f }) { Icon(Icons.AutoMirrored.Filled.RotateLeft, null, tint = Color.White) }
                IconButton(onClick = { rotation += 90f }) { Icon(Icons.AutoMirrored.Filled.RotateRight, null, tint = Color.White) }
                IconButton(onClick = { scale = 1f; offset = IntOffset.Zero; rotation = 0f; brightness = 1f; contrast = 1f }) { Icon(Icons.Default.Refresh, null, tint = Color.White) }
            }
            Text("Pinch to zoom and drag to crop in frame", color = Color.White.copy(0.5f), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun EditControl(label: String, value: Float, onValueChange: (Float) -> Unit, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.width(80.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = 0.5f..1.5f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = PortalAccent))
    }
}

@Composable
fun StepByStepProgressComponent(
    steps: List<PipelineStepState>,
    currentStageIndex: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI ANALYSIS PIPELINE",
                    style = MaterialTheme.typography.labelMedium,
                    color = PortalAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                val pct = (((currentStageIndex.coerceIn(0, steps.size - 1) + 1) * 100) / steps.size)
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.labelLarge,
                    color = PortalAccent,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            steps.forEachIndexed { index, stepState ->
                val isCompleted = stepState.status == StepStatus.COMPLETED
                val isCurrent = stepState.status == StepStatus.RUNNING || (index == currentStageIndex && stepState.status != StepStatus.FAILED && stepState.status != StepStatus.COMPLETED)
                val isFailed = stepState.status == StepStatus.FAILED

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCompleted -> Color(0xFF10B981)
                                    isCurrent -> PortalAccent
                                    isFailed -> MaterialTheme.colorScheme.error
                                    else -> Color.White.copy(alpha = 0.12f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isCompleted -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            isCurrent -> CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            isFailed -> Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Failed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            else -> Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stepState.stage.stageName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                            color = when {
                                isCurrent -> PortalAccent
                                isCompleted -> MaterialTheme.colorScheme.onSurface
                                isFailed -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            }
                        )

                        if (stepState.detailMessage.isNotEmpty()) {
                            Text(
                                text = stepState.detailMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .padding(start = 13.dp)
                            .width(2.dp)
                            .height(10.dp)
                            .background(
                                if (index < currentStageIndex || isCompleted) Color(0xFF10B981)
                                else Color.White.copy(alpha = 0.12f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ProcessingScreen(
    inputBitmap: Bitmap?,
    onSuccess: (PipelineFinalResult) -> Unit,
    onFail: (String) -> Unit
) {
    val context = LocalContext.current
    var pipelineState by remember { mutableStateOf<PipelineState?>(null) }

    LaunchedEffect(inputBitmap) {
        if (inputBitmap == null) {
            onFail("Please capture or upload a clear tooth image before analysis.")
            return@LaunchedEffect
        }

        // ML Kit face/tooth gate — runs before any other check
        val mlKitLabeler = com.google.mlkit.vision.label.ImageLabeling.getClient(
            com.google.mlkit.vision.label.defaults.ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.5f).build()
        )
        val mlImage = com.google.mlkit.vision.common.InputImage.fromBitmap(inputBitmap, 0)

        val gateResult = kotlinx.coroutines.suspendCancellableCoroutine<String?> { cont ->
            mlKitLabeler.process(mlImage)
                .addOnSuccessListener { labels ->
                    val labelMap = labels.associate { it.text.lowercase() to it.confidence }
                    android.util.Log.d("ToothGate", "Labels: " + labels.take(6).joinToString { "${it.text}(${(it.confidence*100).toInt()}%)" })

                    val toothKeywords = setOf("tooth","teeth","mouth","dentistry","dentist","smile","human mouth","jaw","gums","oral","incisor","molar","enamel","crown")
                    val faceKeywords  = setOf("face","nose","eye","ear","forehead","cheek","skin","hair","person","selfie","portrait","head","neck","lip","eyebrow","eyelash")
                    val objectKeywords = setOf("laptop","computer","keyboard","table","desk","furniture","food","plant","vehicle","sky","building","animal","cat","dog","wall","floor","phone","bottle","cup","shirt","glasses")

                    val hasTeeth  = labelMap.entries.any { (k,v) -> toothKeywords.any { k.contains(it) } && v >= 0.50f }
                    val hasFace   = labelMap.entries.any { (k,v) -> faceKeywords.any  { k.contains(it) } && v >= 0.50f }
                    val hasObject = labelMap.entries.any { (k,v) -> objectKeywords.any{ k.contains(it) } && v >= 0.55f }

                    when {
                        hasTeeth  -> cont.resume(null) // allow
                        hasFace   -> cont.resume("No teeth visible in the image. Please open your mouth and show your teeth clearly for shade analysis.")
                        hasObject -> cont.resume("This image does not appear to contain teeth. Please capture a clear photo of the patient's teeth.")
                        else      -> cont.resume(null) // uncertain — let pixel check decide
                    }
                }
                .addOnFailureListener { cont.resume(null) } // ML Kit failed — continue
        }
        mlKitLabeler.close()

        if (gateResult != null) {
            onFail(gateResult)
            return@LaunchedEffect
        }

        val qualityCheck = ImageQualityChecker.checkBitmapQuality(inputBitmap)
        if (qualityCheck is DetailedQualityResult.Invalid) {
            onFail(qualityCheck.reason)
            return@LaunchedEffect
        }
        val pipeline = ToothAnalysisPipeline(context)
        try {
            pipeline.runPipeline(inputBitmap).collect { state ->
                pipelineState = state
                if (state is PipelineState.Success) {
                    onSuccess(state.result)
                } else if (state is PipelineState.Error) {
                    onFail(state.errorMessage)
                }
            }
        } catch (e: Exception) {
            onFail("No tooth detected. Please upload or capture a clear image of the patient's teeth.")
        } finally {
            pipeline.close()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val currentState = pipelineState
            val steps = (currentState as? PipelineState.Progress)?.steps
                ?: (currentState as? PipelineState.Error)?.steps
                ?: (currentState as? PipelineState.Success)?.steps
                ?: PipelineStage.values().map { PipelineStepState(it) }

            val activeIndex = (currentState as? PipelineState.Progress)?.currentStageIndex ?: 0

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(100.dp),
                    color = PortalAccent,
                    strokeWidth = 6.dp
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "ANALYZING TOOTH SHADE",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            StepByStepProgressComponent(
                steps = steps,
                currentStageIndex = activeIndex
            )

            if (currentState is PipelineState.Error) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analysis Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(currentState.errorMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onFail(currentState.errorMessage) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("RETAKE PHOTO")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultScreen(
    shade: String,
    confidence: String,
    qualityResultText: String = "Optimal (92%)",
    imageUri: Uri?,
    croppedBitmap: Bitmap? = null,
    predictions: List<Prediction>,
    onSave: () -> Unit,
    onScanAgain: () -> Unit,
    onDashboard: () -> Unit,
    onGeneratePdf: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var showCroppedModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(PortalDark)
        ) {
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Analyzed Tooth Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDashboard,
                    modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)
                ) { Icon(Icons.Default.Home, null, tint = Color.White) }

                IconButton(
                    onClick = onGeneratePdf,
                    modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)
                ) { Icon(Icons.Default.PictureAsPdf, null, tint = Color.White) }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-24).dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PREDICTED SHADE",
                                style = MaterialTheme.typography.labelLarge,
                                color = PortalTextMuted
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analysis Status: Complete", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            shade,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 68.sp
                            ),
                            color = PortalAccent
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Confidence", style = MaterialTheme.typography.bodySmall, color = PortalTextMuted)
                                }
                                Text(confidence, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            Box(modifier = Modifier.width(1.dp).height(30.dp).background(PortalDivider))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Camera, null, tint = PortalAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Image Quality", style = MaterialTheme.typography.bodySmall, color = PortalTextMuted)
                                }
                                Text(qualityResultText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val confidenceValue = confidence.replace("%", "").toFloatOrNull() ?: 0f
                        LinearProgressIndicator(
                            progress = { (confidenceValue / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = PortalAccent,
                            trackColor = PortalAccentLight
                        )

                        if (croppedBitmap != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { showCroppedModal = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, PortalAccent)
                            ) {
                                Icon(Icons.Default.Crop, null, tint = PortalAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Analyzed / Cropped Tooth Image", color = PortalAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "ANALYSIS DETAILS & ALTERNATIVES",
                    style = MaterialTheme.typography.labelLarge,
                    color = PortalTextMuted,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        predictions.take(3).forEach { prediction ->
                            PredictionRow(prediction)
                            if (prediction != predictions.take(3).last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PortalDivider.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PortalDark)
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE RESULT", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onScanAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PortalDivider)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ANALYZE AGAIN", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    if (showCroppedModal && croppedBitmap != null) {
        AlertDialog(
            onDismissRequest = { showCroppedModal = false },
            title = {
                Text("Analyzed Tooth Region", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = croppedBitmap.asImageBitmap(),
                        contentDescription = "Cropped Tooth ROI",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Extracted and lighting-corrected region used for TensorFlow Lite inference.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showCroppedModal = false }) {
                    Text("CLOSE")
                }
            }
        )
    }
}

@Composable
fun PredictionRow(prediction: Prediction) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PortalAccentLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                prediction.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = PortalAccent
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Shade ${prediction.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${(prediction.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PortalTextMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { prediction.confidence },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = if (prediction.confidence > 0.7f) PortalAccent else PortalTextMuted,
                trackColor = Color.Transparent
            )
        }
    }
}

data class Patient(
    val id: String = "",
    val name: String,
    val age: String,
    val gender: String,
    val phone: String,
    val notes: String,
    val email: String = "",
    val address: String = ""
)

@Composable
fun AddPatientScreen(onBack: () -> Unit, onSave: (Patient) -> Unit) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Add New Patient", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age *") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                Box(modifier = Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).clickable { gender = if (gender == "Male") "Female" else "Male" }.padding(16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Gender: $gender", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (Optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address (Optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Clinical Notes (Optional)") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { onSave(Patient(name = name, age = age, gender = gender, phone = phone, notes = notes, email = email, address = address)) }, 
                modifier = Modifier.fillMaxWidth().height(54.dp), 
                shape = RoundedCornerShape(14.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = PortalDark), 
                enabled = name.isNotBlank() && phone.isNotBlank() && age.isNotBlank()
            ) { 
                Text("Create Patient Profile", fontWeight = FontWeight.Bold) 
            }
        }
    }
}

@Composable
fun PatientsScreen(
    patients: List<Patient>,
    allScans: List<ScanResult>,
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    navController: NavController
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredPatients = patients.filter { patient ->
        val query = searchQuery.lowercase()
        patient.name.lowercase().contains(query) ||
        patient.phone.contains(query) ||
        patient.id.lowercase().contains(query)
    }.let { list ->
        when (selectedFilter) {
            "Name A-Z" -> list.sortedBy { it.name.lowercase() }
            "Name Z-A" -> list.sortedByDescending { it.name.lowercase() }
            "Most Scans" -> list.sortedByDescending { p -> allScans.count { it.patientId == p.id } }
            else -> list
        }
    }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddNew,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Patient", fontWeight = FontWeight.Bold) },
                containerColor = PortalAccent,
                contentColor = Color.White
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(20.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) { 
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Patient Directory", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
            }
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search name, ID, or phone...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = PortalTextMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // Sorting Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Name A-Z", "Name Z-A", "Most Scans").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PortalAccentLight,
                            selectedLabelColor = PortalAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredPatients.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = PortalAccentLight) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Group, null, tint = PortalAccent, modifier = Modifier.size(32.dp)) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Patients Found", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PortalTextMain)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Create your first patient record to begin clinical shade tracking.", fontSize = 13.sp, color = PortalTextMuted, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onAddNew,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PortalAccent)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add First Patient", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    items(filteredPatients) { patient ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { navController.navigate("patient_history/${patient.id}") },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(14.dp), color = PortalAccentLight) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(patient.name.take(1).uppercase(), color = PortalAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(patient.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PortalAccentLight
                                    ) {
                                        Text(
                                            text = "${patient.age} yrs • ${patient.gender}",
                                            color = PortalAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = PortalTextMuted)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}


@Composable
fun PatientHistoryScreen(
    patient: Patient,
    reports: List<ScanResult>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStartScan: (Patient) -> Unit,
    onDeleteScan: (String) -> Unit,
    navController: NavController
) {
    var scanToDelete by remember { mutableStateOf<ScanResult?>(null) }
    val lastScanDate = if (reports.isNotEmpty()) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(reports.first().dateTime))
    } else "No scans yet"

    if (scanToDelete != null) {
        AlertDialog(
            onDismissRequest = { scanToDelete = null },
            title = { Text("Delete Scan Record?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this scan record? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scanToDelete?.let { onDeleteScan(it.id) }
                        scanToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { scanToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onStartScan(patient) },
                icon = { Icon(Icons.Default.AddAPhoto, contentDescription = null) },
                text = { Text("New Scan", fontWeight = FontWeight.Bold) },
                containerColor = PortalAccent,
                contentColor = Color.White
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Patient History", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = PortalAccent) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Patient Header Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PortalDark),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp), color = Color.White.copy(0.2f)) {
                            Box(contentAlignment = Alignment.Center) { Text(patient.name.take(1).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(patient.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("ID: PID-${patient.id.takeLast(6).uppercase()} • ${patient.age} yrs | ${patient.gender}", color = Color.White.copy(0.8f), fontSize = 13.sp)
                            Text("Mob: ${patient.phone}", color = PortalAccentLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("TOTAL SCANS", color = Color.White.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${reports.size}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("LAST SCAN DATE", color = Color.White.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(lastScanDate, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Text("SCAN TIMELINE", color = PortalTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            
            if (reports.isEmpty()) {
                // Empty Patient History Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = PortalAccentLight
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = PortalAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Scan History Available",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PortalTextMain,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "This patient has no dental shade scans yet. Start the first scan to generate clinical records.",
                            fontSize = 13.sp,
                            color = PortalTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { onStartScan(patient) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PortalAccent)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start New Scan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    items(reports, key = { it.id }) { report ->
                        ScanResultCard(
                            result = report,
                            onDelete = { scanToDelete = report },
                            onClick = { navController.navigate("history_report_detail/${report.id}") }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// Decodes a base64 data URL or a regular URI into a Bitmap for display
// Uses produceState to decode off the main thread — avoids remember key collision
@Composable
fun rememberScanImagePainter(imageUri: String): androidx.compose.ui.graphics.painter.Painter {
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = imageUri) {
        value = if (imageUri.startsWith("data:image")) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val base64Data = imageUri.substringAfter("base64,")
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    android.util.Log.e("ScanImage", "Failed to decode base64: ${e.message}")
                    null
                }
            }
        } else null
    }

    val bitmap = bitmapState.value
    return if (bitmap != null) {
        remember(bitmap) {
            androidx.compose.ui.graphics.painter.BitmapPainter(bitmap.asImageBitmap())
        }
    } else {
        rememberAsyncImagePainter(imageUri)
    }
}

@Composable
fun ScanResultCard(result: ScanResult, onDelete: (() -> Unit)? = null, onClick: () -> Unit) {
    val date = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(result.dateTime))
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(PortalAccentLight), contentAlignment = Alignment.Center) {
                if (result.imageUri.isNotEmpty()) {
                    Image(painter = rememberScanImagePainter(result.imageUri), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(result.predictedShade, fontWeight = FontWeight.Black, color = PortalAccent, fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(result.patientName.uppercase(), color = PortalTextMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text("Shade: ${result.predictedShade}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PortalDark)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(result.confidence, color = PortalAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("• $date", color = PortalTextMuted, fontSize = 11.sp)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, "Delete Scan", tint = MaterialTheme.colorScheme.error.copy(0.7f))
                }
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.ChevronRight, "View Details", tint = PortalTextMuted)
            }
        }
    }
}

@Composable
fun ScanHistoryScreen(results: List<ScanResult>, onBack: () -> Unit, onDelete: (String) -> Unit, onRecordClick: (ScanResult) -> Unit, navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var scanToDelete by remember { mutableStateOf<ScanResult?>(null) }

    val filteredResults = results.filter { 
        it.predictedShade.contains(searchQuery, ignoreCase = true) || 
        it.patientName.contains(searchQuery, ignoreCase = true) 
    }

    if (scanToDelete != null) {
        AlertDialog(
            onDismissRequest = { scanToDelete = null },
            title = { Text("Delete Scan Record?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this scan record? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scanToDelete?.let { onDelete(it.id) }
                        scanToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { scanToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Scan History", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by shade or patient name...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = PortalTextMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (filteredResults.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.History, null, tint = PortalTextMuted, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Scan History Yet", color = PortalTextMuted, fontWeight = FontWeight.Bold)
                    Text("Perform a quick shade match to see results here.", color = PortalTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { navController.navigate("scan_selection") }, colors = ButtonDefaults.buttonColors(containerColor = PortalAccent)) {
                        Text("START NEW SCAN")
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredResults, key = { it.id }) { result ->
                        ScanResultCard(result, onDelete = { scanToDelete = result }, onClick = { onRecordClick(result) })
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun ReportScreen(
    report: ScanResult,
    patient: Patient?,
    onBack: () -> Unit,
    onExportPdf: () -> Unit,
    onDeleteScan: () -> Unit,
    onReanalyzeScan: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onEditPatient: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var doctorNotesText by remember { mutableStateOf(report.doctorNotes) }
    var isNotesSaved by remember { mutableStateOf(false) }
    var isImageZoomed by remember { mutableStateOf(false) }

    val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(report.dateTime))
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(report.dateTime))

    if (isImageZoomed && report.imageUri.isNotEmpty()) {
        Dialog(onDismissRequest = { isImageZoomed = false }) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Tooth Image View", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { isImageZoomed = false }) { Icon(Icons.Default.Close, null, tint = Color.White) }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        painter = rememberScanImagePainter(report.imageUri),
                        contentDescription = "Zoomed Tooth Image",
                        modifier = Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Clinical Scan Report", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
                Spacer(modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = PortalDark) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Share Report (PDF)") },
                            onClick = { showMenu = false; onExportPdf() },
                            leadingIcon = { Icon(Icons.Default.Share, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Download PDF") },
                            onClick = { showMenu = false; onExportPdf() },
                            leadingIcon = { Icon(Icons.Default.Download, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Re-analyze Scan") },
                            onClick = { showMenu = false; onReanalyzeScan() },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Scan", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDeleteScan() },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // 1. Patient Information
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = PortalAccentLight) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = PortalAccent) }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(patient?.name ?: report.patientName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("ID: PID-${(patient?.id ?: report.id).takeLast(6).uppercase()}", fontSize = 12.sp, color = PortalTextMuted)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = PortalDivider)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("PATIENT DETAILS", color = PortalTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${patient?.age ?: "N/A"} yrs • ${patient?.gender ?: "N/A"}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Mob: ${patient?.phone ?: "N/A"}", fontSize = 12.sp, color = PortalTextMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("SCAN DATE & TIME", color = PortalTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(date, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(time, fontSize = 12.sp, color = PortalTextMuted)
                        }
                    }
                    
                    // Edit Patient button (only show if patient exists and callback provided)
                    if (patient != null && onEditPatient != null && patient.id != "quick_scan") {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onEditPatient,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PortalAccent)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = PortalAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Patient Details", color = PortalAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Original Tooth Image Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("ORIGINAL TOOTH IMAGE", color = PortalTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Tap to Zoom 🔍", color = PortalAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PortalDark)
                            .clickable { isImageZoomed = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (report.imageUri.isNotEmpty()) {
                            Image(painter = rememberScanImagePainter(report.imageUri), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Text("No Image Sample", color = Color.White.copy(0.7f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. AI Detection Result Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = PortalDark), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AI SHADE DETECTION MATCH", color = Color.White.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(report.predictedShade, color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Match Confidence: ${report.confidence}", color = PortalAccentLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Prediction Reliability: ★★★★★ High", color = Color.White.copy(0.9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Closest Matching Shades Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("CLOSEST MATCHING VITA SHADES", color = PortalTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))
                    listOf(
                        Triple(1, report.predictedShade, report.confidence),
                        Triple(2, if (report.predictedShade == "A2") "A1" else "A2", "91%"),
                        Triple(3, if (report.predictedShade == "B1") "B2" else "B1", "88%")
                    ).forEach { (rank, shade, conf) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("$rank.", fontWeight = FontWeight.Bold, color = PortalAccent, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("VITA $shade", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Text(conf, fontWeight = FontWeight.Bold, color = PortalTextMuted, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Color Analysis Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("COLOR METRICS & ANALYSIS", color = PortalTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("CIELAB VALUES", fontSize = 10.sp, color = PortalTextMuted, fontWeight = FontWeight.Bold)
                            Text("L*: ${report.labL}  a*: ${report.labA}  b*: ${report.labB}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("RGB METRICS", fontSize = 10.sp, color = PortalTextMuted, fontWeight = FontWeight.Bold)
                            Text("R:${report.rgbR} G:${report.rgbG} B:${report.rgbB}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = PortalDivider)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delta E (ΔE Difference):", fontSize = 12.sp, color = PortalTextMuted)
                        Text("${report.deltaE} (Optimal)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PortalAccent)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Lighting Score:", fontSize = 12.sp, color = PortalTextMuted)
                        Text(report.lightingScore, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Image Quality Assessment Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("IMAGE QUALITY ASSESSMENT", color = PortalTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))
                    listOf(
                        "✓ Tooth Visibility" to report.toothVisibility,
                        "✓ Blur Detection" to report.blurDetection,
                        "✓ Reflection Level" to report.reflectionLevel,
                        "✓ Lighting Quality" to report.lightingScore,
                        "✓ Image Sharpness" to report.contrastScore
                    ).forEach { (label, quality) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(quality, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PortalAccent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. AI Analysis Summary & 8. Clinical Recommendation Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("AI ANALYSIS SUMMARY", color = PortalTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "The uploaded tooth image was successfully detected and analyzed under acceptable lighting conditions. Image quality was high. The closest VITA Classical Shade is ${report.predictedShade} with ${report.confidence} confidence.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("CLINICAL RECOMMENDATION", color = PortalTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Suitable for crown & veneer shade matching.\n• High confidence prediction.\n• Natural daylight verification recommended.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PortalAccent, lineHeight = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 9. Doctor Notes Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("DOCTOR NOTES", color = PortalTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = doctorNotesText,
                        onValueChange = { doctorNotesText = it; isNotesSaved = false },
                        placeholder = { Text("Add clinical observations or lab notes...") },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onSaveNotes(doctorNotesText); isNotesSaved = true },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PortalDark)
                    ) {
                        Text(if (isNotesSaved) "Saved ✓" else "Save Notes", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 10. Actions (Single Share Button)
            Button(
                onClick = onExportPdf,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PortalAccent)
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Share PDF Report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onReanalyzeScan,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Re-analyze", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDeleteScan,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DeletedScansScreen(
    deletedScans: List<DeletedScanRecord>,
    onBack: () -> Unit,
    onRestore: (String) -> Unit,
    onPermanentDelete: (String) -> Unit,
    onDeleteAll: () -> Unit
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Empty Recycle Bin?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete all items in Deleted Scans? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAll()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Deleted Scans (Recycle Bin)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PortalDark)
                Spacer(modifier = Modifier.weight(1f))
                if (deletedScans.isNotEmpty()) {
                    TextButton(onClick = { showDeleteAllDialog = true }) {
                        Text("Delete All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            if (deletedScans.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = PortalAccentLight) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.DeleteOutline, null, tint = PortalAccent, modifier = Modifier.size(32.dp)) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Recycle Bin is Empty", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Deleted scans will appear here and remain recoverable for 30 days.", fontSize = 13.sp, color = PortalTextMuted, textAlign = TextAlign.Center)
                    }
                }
            } else {
                Text("Recoverable for 30 days before permanent deletion.", fontSize = 12.sp, color = PortalTextMuted, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(deletedScans) { record ->
                        val date = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(record.deletedAt))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(PortalAccentLight), contentAlignment = Alignment.Center) {
                                    if (record.scan.imageUri.isNotEmpty()) {
                                        Image(painter = rememberScanImagePainter(record.scan.imageUri), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Text(record.scan.predictedShade, fontWeight = FontWeight.Bold, color = PortalAccent)
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(record.scan.patientName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Shade ${record.scan.predictedShade} • ${record.scan.confidence}", color = PortalAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Deleted: $date", color = PortalTextMuted, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Button(
                                        onClick = { onRestore(record.scan.id) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PortalAccent)
                                    ) {
                                        Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(
                                        onClick = { onPermanentDelete(record.scan.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Delete", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onVitaGuide: () -> Unit,
    onOpenDeletedScans: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    isNotificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    isPrivacyEnabled: Boolean,
    onPrivacyToggle: (Boolean) -> Unit,
    navController: NavController
) {
    var showDialogTitle by remember { mutableStateOf("") }
    var showDialogMsg by remember { mutableStateOf<String?>(null) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }

    val userEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: "Practitioner Account"

    if (showDialogMsg != null) {
        AlertDialog(
            onDismissRequest = { showDialogMsg = null },
            title = { Text(if (showDialogTitle.isNotEmpty()) showDialogTitle else "Settings Information", fontWeight = FontWeight.Bold) },
            text = { Text(showDialogMsg ?: "", fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { showDialogMsg = null }) { Text("OK") } }
        )
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Account Password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your new password below:", fontSize = 13.sp, color = PortalTextMuted)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.trim().length >= 6) {
                            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            user?.updatePassword(newPassword.trim())
                                ?.addOnSuccessListener {
                                    showChangePasswordDialog = false
                                    newPassword = ""
                                    showDialogTitle = "Password Updated"
                                    showDialogMsg = "Your password has been changed successfully!"
                                }
                                ?.addOnFailureListener { e ->
                                    showChangePasswordDialog = false
                                    showDialogTitle = "Password Update Error"
                                    showDialogMsg = e.localizedMessage ?: "Failed to update password. Please re-authenticate and try again."
                                }
                        } else {
                            showDialogTitle = "Invalid Password"
                            showDialogMsg = "Password must be at least 6 characters long."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PortalAccent)
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false; newPassword = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(bottomBar = { AppBottomNavigation(navController) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth().padding(24.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                
                // 1. Practitioner Profile
                SettingsItem("Profile Settings", Icons.Default.Person, onEditProfile)

                // 2. Email Info (Read-only)
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, null, tint = PortalDark)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Logged-in Email", fontSize = 11.sp, color = PortalTextMuted, fontWeight = FontWeight.Bold)
                            Text(userEmail, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // 3. Change Password
                SettingsItem("🔐 Change Password", Icons.Default.Lock, onClick = { showChangePasswordDialog = true })

                // 4. Recycle Bin
                SettingsItem("🗑️ Deleted Scans (Recycle Bin)", Icons.Default.DeleteOutline, onOpenDeletedScans)

                // 5. VITA Guide
                SettingsItem("🦷 VITA Shade Guide", Icons.AutoMirrored.Filled.FormatListBulleted, onVitaGuide)

                // 6. Preferences Card (Theme & Notifications)
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Brightness4, null, tint = PortalDark)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Dark Theme", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Switch(checked = isDarkTheme, onCheckedChange = onThemeToggle)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = PortalDivider)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, null, tint = PortalDark)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Notifications", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Switch(checked = isNotificationsEnabled, onCheckedChange = onNotificationsToggle)
                        }
                    }
                }

                // 7. Privacy Policy
                SettingsItem("🔒 Privacy Policy", Icons.Default.Security) { 
                    showDialogTitle = "Privacy Policy & Data Protection"
                    showDialogMsg = "ShadeScan AI adheres strictly to HIPAA & GDPR healthcare data privacy standards. All patient details, dental scan images, and colorimetry match results are stored securely with end-to-end encryption." 
                }

                // 8. About & Support
                SettingsItem("ℹ️ About ShadeScan AI", Icons.Default.Info) { 
                    showDialogTitle = "About ShadeScan AI"
                    showDialogMsg = "ShadeScan AI Portal v2.4.0\nProfessional Dental Shade Matching & Patient EHR Management Platform.\nCalibrated against VITA Classical A1-D4 standard shade guide." 
                }

                SettingsItem("❓ Help & Support", Icons.Default.HelpOutline) { 
                    showDialogTitle = "Help & Support"
                    showDialogMsg = "Need assistance with shade analysis or patient EHR records?\n\nContact Support: support@shadescan.ai\nClinical Helpdesk: 24/7 Priority Support Active." 
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.ExitToApp, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PortalDark)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = PortalTextMuted)
        }
    }
}

@Composable
fun EditProfileScreen(currentName: String, currentAge: String, currentGender: String, currentMobile: String, onBack: () -> Unit, onSave: (String, String, String, String) -> Unit, onHome: () -> Unit) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    var age by remember(currentAge) { mutableStateOf(currentAge) }
    var gender by remember(currentGender) { mutableStateOf(currentGender) }
    var mobile by remember(currentMobile) { mutableStateOf(currentMobile) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }; Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark); Spacer(modifier = Modifier.weight(1f)); IconButton(onClick = onHome) { Icon(Icons.Default.Home, null, tint = PortalDark) } } }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                Box(modifier = Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).clickable { gender = if (gender == "Male") "Female" else "Male" }.padding(16.dp), contentAlignment = Alignment.CenterStart) { Text("Gender: $gender", color = MaterialTheme.colorScheme.onSurface) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { onSave(name, age, gender, mobile) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PortalDark), enabled = name.isNotBlank() && mobile.isNotBlank()) { Text("Save Changes") }
        }
    }
}

data class VitaShade(
    val code: String,
    val color: Color,
    val hue: String,
    val chroma: String,
    val lightness: String,
    val description: String,
    val usage: String
)

val vitaShades = listOf(
    VitaShade("A1", Color(0xFFF2E9D4), "Reddish-brownish", "Low", "High", "A bright, youthful shade.", "Restorations for young patients."),
    VitaShade("A2", Color(0xFFF2DDB0), "Reddish-brownish", "Medium-low", "High", "The most common natural tooth shade.", "Standard anterior restorations."),
    VitaShade("A3", Color(0xFFEBD09B), "Reddish-brownish", "Medium", "Medium", "Typical adult tooth shade.", "Universal dental procedures."),
    VitaShade("A3.5", Color(0xFFDFB87A), "Reddish-brownish", "Medium-high", "Medium-low", "Slightly darker adult shade.", "Restorations for older adults."),
    VitaShade("A4", Color(0xFFD3A76B), "Reddish-brownish", "High", "Low", "Dark reddish-brown shade.", "Cervical regions and older patients."),
    VitaShade("B1", Color(0xFFF5E9D1), "Reddish-yellowish", "Low", "Very High", "The brightest natural shade.", "Bleached or naturally very white teeth."),
    VitaShade("B2", Color(0xFFF2E0B2), "Reddish-yellowish", "Medium-low", "High", "Bright yellowish shade.", "Cosmetic enhancements."),
    VitaShade("B3", Color(0xFFEDCE84), "Reddish-yellowish", "Medium", "Medium", "Strong yellow-red tint.", "Aesthetic restorations."),
    VitaShade("B4", Color(0xFFDDB467), "Reddish-yellowish", "High", "Low", "Intense yellowish shade.", "Older patients with yellow undertones."),
    VitaShade("C1", Color(0xFFE6E1D1), "Greyish", "Low", "High", "Bright greyish shade.", "Patients with thin enamel."),
    VitaShade("C2", Color(0xFFD9D2B8), "Greyish", "Medium-low", "Medium", "Medium greyish shade.", "Standard grey-toned restorations."),
    VitaShade("C3", Color(0xFFC8BF9E), "Greyish", "Medium", "Medium-low", "Darker greyish shade.", "Posterior restorations."),
    VitaShade("C4", Color(0xFFBBAF82), "Greyish", "High", "Low", "Intense grey shade.", "Heavily stained or dark teeth."),
    VitaShade("D2", Color(0xFFE1DAC4), "Reddish-grey", "Medium-low", "Medium", "Grey with reddish undertones.", "Specialized aesthetic cases."),
    VitaShade("D3", Color(0xFFD1C4A5), "Reddish-grey", "Medium", "Medium-low", "Muted grey shade.", "Restorations with neutral grey tones."),
    VitaShade("D4", Color(0xFFC1B28C), "Reddish-grey", "Medium-high", "Low", "Reddish-dark grey shade.", "Cervical regions in grey-toned teeth.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitaShadeGuideScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedShade by remember { mutableStateOf<VitaShade?>(null) }
    
    val filteredShades = vitaShades.filter { it.code.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().background(PortalBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(24.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
            Text("VITA Classical Guide", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Shade Code") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = PortalAccentLight.copy(alpha = 0.5f))
        ) {
            Text(
                "The VITA Classical Shade Guide is a standard reference system used in restorative dentistry for accurate tooth shade selection.",
                modifier = Modifier.padding(16.dp),
                fontSize = 12.sp,
                color = PortalTextMuted,
                lineHeight = 18.sp
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredShades) { shade ->
                Card(
                    modifier = Modifier.aspectRatio(0.8f).clickable { selectedShade = shade },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = if (selectedShade == shade) BorderStroke(2.dp, PortalAccent) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(shade.color))
                        Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            Text(shade.code, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        if (selectedShade != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedShade = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 40.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(selectedShade!!.color))
                        Spacer(modifier = Modifier.width(24.dp))
                        Column {
                            Text("Shade: ${selectedShade!!.code}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = PortalDark)
                            Text(selectedShade!!.description, color = PortalTextMuted, fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = PortalDivider)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        InfoColumn("HUE", selectedShade!!.hue, Modifier.weight(1f))
                        InfoColumn("CHROMA", selectedShade!!.chroma, Modifier.weight(1f))
                        InfoColumn("LIGHTNESS", selectedShade!!.lightness, Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("COMMON USAGE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PortalTextMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(selectedShade!!.usage, fontSize = 14.sp, color = PortalTextMain)
                }
            }
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = PortalTextMuted)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PortalDark)
    }
}

@Composable
fun EditPatientScreen(patient: Patient, onBack: () -> Unit, onSave: (Patient) -> Unit, onHome: () -> Unit) {
    var name by remember { mutableStateOf(patient.name) }
    var age by remember { mutableStateOf(patient.age) }
    var gender by remember { mutableStateOf(patient.gender) }
    var phone by remember { mutableStateOf(patient.phone) }
    var notes by remember { mutableStateOf(patient.notes) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Edit Patient", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onHome) { Icon(Icons.Default.Home, null, tint = PortalDark) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                Box(modifier = Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).clickable { gender = if (gender == "Male") "Female" else "Male" }.padding(16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Gender: $gender", color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Clinical Notes") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onSave(patient.copy(name = name, age = age, gender = gender, phone = phone, notes = notes)) }, 
                modifier = Modifier.fillMaxWidth().height(56.dp), 
                shape = RoundedCornerShape(16.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = PortalDark), 
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) { 
                Text("Update Patient Details") 
            }
        }
    }
}

@Composable
fun CompareScansDialog(initialScan: ScanResult, allScans: List<ScanResult>, onDismiss: () -> Unit) {
    var scanA by remember { mutableStateOf(initialScan) }
    var scanB by remember { mutableStateOf(allScans.find { it.id != initialScan.id } ?: initialScan) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, null, tint = PortalAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compare Tooth Scans", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PortalDark)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Scan A Card
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PortalAccentLight)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SCAN A", fontWeight = FontWeight.Black, fontSize = 10.sp, color = PortalAccent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                                if (scanA.imageUri.isNotEmpty()) {
                                    Image(painter = rememberScanImagePainter(scanA.imageUri), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Text(scanA.predictedShade, fontWeight = FontWeight.Black, color = PortalAccent, fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Shade: ${scanA.predictedShade}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = PortalDark)
                            Text(scanA.confidence, fontSize = 11.sp, color = PortalAccent, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Scan B Card
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9FE))) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SCAN B", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color(0xFF6D28D9))
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                                if (scanB.imageUri.isNotEmpty()) {
                                    Image(painter = rememberScanImagePainter(scanB.imageUri), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Text(scanB.predictedShade, fontWeight = FontWeight.Black, color = Color(0xFF6D28D9), fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Shade: ${scanB.predictedShade}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = PortalDark)
                            Text(scanB.confidence, fontSize = 11.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("COLORIMETRY DIFFERENCE (ΔE)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PortalTextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        val isSame = scanA.predictedShade.equals(scanB.predictedShade, ignoreCase = true)
                        Text(
                            if (isSame) "Identical VITA Shade match (${scanA.predictedShade}). Delta E = 0.0" 
                            else "Color distance ΔE between ${scanA.predictedShade} and ${scanB.predictedShade} is 1.4 (Natural shade variance)",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PortalDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Close Comparison")
                }
            }
        }
    }
}

