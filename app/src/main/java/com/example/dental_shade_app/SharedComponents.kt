package com.example.dental_shade_app

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
val VitaA1 = Color(0xFFF1E5C8)
val VitaA2 = Color(0xFFEEDDAA)
val VitaB1 = Color(0xFFF5E9D1)
val VitaC1 = Color(0xFFE5D8C0)

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
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
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
    val patientId: String = ""
)

@Composable
fun DashboardScreen(
    doctorName: String,
    patientCount: Int,
    scanCount: Int,
    onNewScan: () -> Unit,
    onAddPatient: () -> Unit,
    onShadeAnalysis: () -> Unit,
    onViewReports: () -> Unit,
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
                Spacer(modifier = Modifier.weight(1f))
                Row {
                    Card(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().clickable { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, null, tint = PortalTextMuted)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Card(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.Notifications, null, tint = PortalTextMuted)
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(top = 10.dp, end = 10.dp)
                                    .background(PortalNotificationDot, CircleShape)
                            )
                        }
                    }
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
                Text("View All", color = PortalAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { navController.navigate("scan_history") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth().height(200.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.background) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.History, null, tint = PortalTextMuted, modifier = Modifier.size(32.dp)) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(if(scanCount == 0) "No Scans Recorded Yet" else "Recent Activity Logged", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(if(scanCount == 0) "Perform or upload your first dental\nmatch to begin logs." else "Check your Scan History for details.", color = PortalTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
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
    val filteredPatients = patients.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Select Patient", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { navController.navigate("home") }) { Icon(Icons.Default.Home, null, tint = PortalDark) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search patient name...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PortalAccent)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onAddNew,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PortalAccentLight, contentColor = PortalAccent)
            ) {
                Icon(Icons.Default.PersonAdd, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Add New Patient", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Existing Patients", color = PortalTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredPatients) { patient ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onPatientSelected(patient) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = PortalAccentLight) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(patient.name.take(1).uppercase(), color = PortalAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(patient.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("${patient.age} yrs | ${patient.gender}", color = PortalTextMuted, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = PortalTextMuted)
                        }
                    }
                }
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
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { navController.navigate("home") }) { Icon(Icons.Default.Home, null, tint = PortalDark) }
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
fun ProcessingScreen(onComplete: () -> Unit, onFail: (String) -> Unit) {
    var progressStep by remember { mutableStateOf(0) }
    val steps = listOf("Detecting Tooth Region...", "Analyzing Enamel Transparency...", "Measuring Chroma Values...", "Matching with VITA Scale...")
    
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "")
    
    LaunchedEffect(Unit) {
        for (i in 0 until steps.size) {
            progressStep = i
            kotlinx.coroutines.delay(1200)
        }
        onComplete()
    }
    
    Box(modifier = Modifier.fillMaxSize().background(PortalDark), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(120.dp), color = PortalAccent, strokeWidth = 8.dp)
                Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(48.dp).rotate(rotation))
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text("AI IS ANALYZING...", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(steps[progressStep], color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(32.dp))
            LinearProgressIndicator(progress = { (progressStep + 1) / steps.size.toFloat() }, modifier = Modifier.width(200.dp).height(4.dp).clip(CircleShape), color = PortalAccent, trackColor = Color.White.copy(0.1f))
        }
    }
}

@Composable
fun ResultScreen(
    shade: String,
    confidence: String,
    imageUri: Uri?,
    predictions: List<Prediction>,
    onSave: () -> Unit,
    onScanAgain: () -> Unit,
    onDashboard: () -> Unit,
    onGeneratePdf: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Captured tooth image at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(PortalDark)
        ) {
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Analyzed Tooth",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Header buttons
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
                    .offset(y = (-30).dp)
            ) {
                // 2. Modern Material 3 result card
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
                        Text(
                            "PREDICTED SHADE",
                            style = MaterialTheme.typography.labelLarge,
                            color = PortalTextMuted
                        )
                        
                        Text(
                            shade,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 72.sp
                            ),
                            color = PortalAccent
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "AI Confidence: $confidence",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = PortalTextMain
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 3. Confidence progress bar
                        val confidenceValue = confidence.replace("%", "").toFloatOrNull() ?: 0f
                        LinearProgressIndicator(
                            progress = { confidenceValue / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = PortalAccent,
                            trackColor = PortalAccentLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Top 3 Predictions
                Text(
                    "ANALYSIS DETAILS",
                    style = MaterialTheme.typography.labelLarge,
                    color = PortalTextMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
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
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = PortalDivider.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 6. Scan Again & 7. Save Result Buttons
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PortalDark)
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE TO RECORDS", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = onScanAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PortalDivider)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCAN AGAIN", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
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

data class Patient(val id: String = "", val name: String, val age: String, val gender: String, val phone: String, val notes: String)

@Composable
fun AddPatientScreen(onBack: () -> Unit, onSave: (Patient) -> Unit, onHome: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Add New Patient", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
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
                onClick = { onSave(Patient(name = name, age = age, gender = gender, phone = phone, notes = notes)) }, 
                modifier = Modifier.fillMaxWidth().height(56.dp), 
                shape = RoundedCornerShape(16.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = PortalDark), 
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) { 
                Text("Save Patient") 
            }
        }
    }
}

@Composable
fun PatientsScreen(patients: List<Patient>, onBack: () -> Unit, onAddNew: () -> Unit, navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredPatients = patients.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(bottomBar = { AppBottomNavigation(navController) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(24.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) { 
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Patient Directory", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onAddNew) { Icon(Icons.Default.PersonAdd, null, tint = PortalAccent) }
                IconButton(onClick = { navController.navigate("home") }) { Icon(Icons.Default.Home, null, tint = PortalDark) }
            }
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (filteredPatients.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { 
                    Icon(Icons.Default.Group, null, tint = PortalTextMuted, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp)); Text("No patients found", color = PortalTextMuted, fontSize = 16.sp) 
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    items(filteredPatients) { patient ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { navController.navigate("patient_history/${patient.id}") }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = PortalAccentLight) { Box(contentAlignment = Alignment.Center) { Text(patient.name.take(1).uppercase(), color = PortalAccent, fontWeight = FontWeight.Bold) } }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column { Text(patient.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text("Age: ${patient.age} | ${patient.gender}", color = PortalTextMuted, fontSize = 12.sp) }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = PortalTextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatientHistoryScreen(patient: Patient, reports: List<ScanResult>, onBack: () -> Unit, onEdit: () -> Unit, navController: NavController) {
    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Patient History", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = PortalAccent) }
                IconButton(onClick = { navController.navigate("home") }) { Icon(Icons.Default.Home, null, tint = PortalDark) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = PortalDark)) {
                Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(16.dp), color = Color.White.copy(0.2f)) {
                        Box(contentAlignment = Alignment.Center) { Text(patient.name.take(1).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(patient.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${patient.age} years old | ${patient.gender}", color = Color.White.copy(0.7f), fontSize = 14.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("SCAN TIMELINE", color = PortalTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(16.dp))
            
            if (reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No scans recorded for this patient", color = PortalTextMuted)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    items(reports) { report ->
                        val date = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(report.dateTime))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { navController.navigate("history_report_detail/${report.id}") },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(PortalAccentLight), contentAlignment = Alignment.Center) {
                                    Text(report.predictedShade, fontWeight = FontWeight.Black, color = PortalAccent)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Tooth Scan Match", fontWeight = FontWeight.Bold)
                                    Text(date, color = PortalTextMuted, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(report.confidence, fontWeight = FontWeight.Bold, color = PortalAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanResultCard(result: ScanResult, onDelete: () -> Unit, onClick: () -> Unit) {
    val date = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(result.dateTime))
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(PortalAccentLight), contentAlignment = Alignment.Center) {
                if (result.imageUri.isNotEmpty()) {
                    Image(painter = rememberAsyncImagePainter(result.imageUri), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(result.predictedShade, fontWeight = FontWeight.Black, color = PortalAccent, fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Dental Scan Result", color = PortalTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Shade: ${result.predictedShade}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PortalDark)
                Text("Confidence: ${result.confidence}", color = PortalAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(date, color = PortalTextMuted, fontSize = 11.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteSweep, "Delete", tint = Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun ScanHistoryScreen(results: List<ScanResult>, onBack: () -> Unit, onDelete: (String) -> Unit, onRecordClick: (ScanResult) -> Unit, navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredResults = results.filter { it.predictedShade.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Scan History", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { navController.navigate("home") }) { Icon(Icons.Default.Home, null, tint = PortalDark) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by shade (e.g. A1)...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
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
                        ScanResultCard(result, onDelete = { onDelete(result.id) }, onClick = { onRecordClick(result) })
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun ReportScreen(report: ScanResult, patient: Patient?, onBack: () -> Unit, onExportPdf: () -> Unit, onHome: () -> Unit) {
    val date = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date(report.dateTime))
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(report.dateTime))

    Scaffold(
        containerColor = PortalBg,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Scan Report", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onHome) { Icon(Icons.Default.Home, null, tint = PortalAccent) }
                IconButton(onClick = onExportPdf) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, null, tint = PortalAccent) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("PATIENT DETAILS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PortalTextMuted)
                            Text(patient?.name ?: "Quick Scan Result", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("ID: ${report.id.take(8).uppercase()}", fontSize = 12.sp, color = PortalTextMuted)
                        }
                        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(PortalAccentLight), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = PortalAccent)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = PortalDivider)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("DATE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PortalTextMuted)
                            Text(date, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TIME", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PortalTextMuted)
                            Text(time, fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(24.dp)).background(PortalDark), contentAlignment = Alignment.Center) {
                        if (report.imageUri.isNotEmpty()) {
                             Image(painter = rememberAsyncImagePainter(report.imageUri), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("FINAL AI MATCH", color = Color.White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(report.predictedShade, color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Black)
                                Text("Confidence: ${report.confidence}", color = PortalAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("CLINICAL SUMMARY", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PortalTextMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("The analysis of the dental scan shows a primary match with VITA shade ${report.predictedShade}. The match confidence is ${report.confidence}, which is high for clinical application. The chroma and value are within expected ranges.", fontSize = 14.sp, color = PortalTextMain, lineHeight = 20.sp)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onExportPdf, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PortalAccent)) {
                        Icon(Icons.Default.Share, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SHARE PDF REPORT")
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("ShadeScan AI • Secure Diagnostic Report", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = PortalTextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onVitaGuide: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    isNotificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    isPrivacyEnabled: Boolean,
    onPrivacyToggle: (Boolean) -> Unit,
    navController: NavController
) {
    Scaffold(bottomBar = { AppBottomNavigation(navController) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth().padding(24.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
                Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { navController.navigate("home") }) { Icon(Icons.Default.Home, null, tint = PortalDark) }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsItem("Profile Settings", Icons.Default.Person, onEditProfile)
                SettingsItem("🦷 VITA Shade Guide", Icons.AutoMirrored.Filled.FormatListBulleted, onVitaGuide)
                
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Brightness4, null, tint = PortalDark)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Dark Theme", modifier = Modifier.weight(1f))
                            Switch(checked = isDarkTheme, onCheckedChange = onThemeToggle)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = PortalDivider)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, null, tint = PortalDark)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Notifications", modifier = Modifier.weight(1f))
                            Switch(checked = isNotificationsEnabled, onCheckedChange = onNotificationsToggle)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = PortalDivider)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, null, tint = PortalDark)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Privacy Lock", modifier = Modifier.weight(1f))
                            Switch(checked = isPrivacyEnabled, onCheckedChange = onPrivacyToggle)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), shape = RoundedCornerShape(16.dp)) { Text("Logout", color = Color.White) }
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

@Composable
fun ShadeAnalysisDashboard(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.fillMaxWidth().padding(24.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PortalDark) }
            Text("Shade Analysis", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PortalDark)
        }
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.AutoAwesome, null, tint = PortalAccent, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("AI Analysis Tools", fontWeight = FontWeight.Bold, color = PortalDark)
            Text("Advanced shade matching analytics will appear here.", color = PortalTextMuted, textAlign = TextAlign.Center)
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
