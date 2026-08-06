package com.example.dental_shade_app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import java.io.ByteArrayOutputStream
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var shadeClassifier: ToothShadeClassifier
    private lateinit var historyManager: LocalScanHistoryManager
    private lateinit var pdfGenerator: PdfReportGenerator
    private val DB_URL = "https://shadescan-ai-default-rtdb.firebaseio.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()
        database = try { FirebaseDatabase.getInstance(DB_URL) } catch (e: Exception) { FirebaseDatabase.getInstance() }

        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        try {
            shadeClassifier = ToothShadeClassifier(this)
            historyManager = LocalScanHistoryManager(this)
            pdfGenerator = PdfReportGenerator(this)
        } catch (e: Throwable) {
            Log.e("DashboardActivity", "Classifier/History init warning", e)
        }

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
            
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
            var isNotificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications", true)) }
            var isPrivacyEnabled by remember { mutableStateOf(prefs.getBoolean("privacy", false)) }

            ShadeScanTheme(isDarkTheme = isDarkTheme) {
                val navController = rememberNavController()
                var doctorName by remember { mutableStateOf("Doctor") }
                var doctorAge by remember { mutableStateOf("") }
                var doctorGender by remember { mutableStateOf("Male") }
                var doctorMobile by remember { mutableStateOf("") }
                
                var capturedUri by remember { mutableStateOf<Uri?>(null) }
                var selectedPatient by remember { mutableStateOf<Patient?>(null) }
                var lastScanId by remember { mutableStateOf<String?>(null) }
                
                var patientList by remember { mutableStateOf<List<Patient>>(emptyList()) }
                var localHistory by remember { mutableStateOf(historyManager.getHistory()) }
                
                var isConnected by remember { mutableStateOf(false) }
                
                // AI Results
                var analysisResultShade by remember { mutableStateOf("") }
                var analysisConfidence by remember { mutableStateOf("") }
                var analysisPredictions by remember { mutableStateOf<List<Prediction>>(emptyList()) }
                var imageQualityText by remember { mutableStateOf("Optimal (92%)") }
                var croppedToothBitmap by remember { mutableStateOf<Bitmap?>(null) }

                val galleryLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        capturedUri = uri
                        navController.navigate("preview")
                    }
                }

                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        navController.navigate("camera")
                    } else {
                        Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
                    }
                }

                // Helper for saving reports
                val performSaveReport = { patient: Patient ->
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val reportData = mapOf(
                            "patientId" to patient.id,
                            "patientName" to patient.name,
                            "shade" to analysisResultShade,
                            "confidence" to analysisConfidence,
                            "timestamp" to System.currentTimeMillis()
                        )
                        
                        // Update local history with patient ID if it's missing
                        lastScanId?.let { scanId ->
                            historyManager.updatePatientId(scanId, patient.id, patient.name)
                            localHistory = historyManager.getHistory()
                        }

                        database.getReference("Reports").child(uid).push().setValue(reportData)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Report Saved Successfully!", Toast.LENGTH_SHORT).show()
                                    localHistory = historyManager.getHistory()
                                    navController.navigate("home") { popUpTo("home") { inclusive = false } }
                                } else {
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Report Saved Locally", Toast.LENGTH_SHORT).show()
                        localHistory = historyManager.getHistory()
                        navController.navigate("home") { popUpTo("home") { inclusive = false } }
                    }
                }

                // Connection Monitor
                LaunchedEffect(Unit) {
                    database.getReference(".info/connected").addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            isConnected = snapshot.getValue(Boolean::class.java) ?: false
                            Log.d("DashboardActivity", "Firebase Connected: $isConnected")
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                LaunchedEffect(auth.currentUser?.uid) {
                    val uid = auth.currentUser?.uid ?: return@LaunchedEffect
                    
                    database.getReference("Users").child(uid).addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.child("fullName").value?.toString()?.let { doctorName = it }
                            snapshot.child("age").value?.toString()?.let { doctorAge = it }
                            snapshot.child("gender").value?.toString()?.let { doctorGender = it }
                            snapshot.child("mobile").value?.toString()?.let { doctorMobile = it }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })

                    database.getReference("Patients").child(uid).addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val patients = mutableListOf<Patient>()
                            for (child in snapshot.children) {
                                patients.add(Patient(
                                    id = child.key ?: "",
                                    name = child.child("name").value?.toString() ?: "Unknown",
                                    age = child.child("age").value?.toString() ?: "",
                                    gender = child.child("gender").value?.toString() ?: "",
                                    phone = child.child("phone").value?.toString() ?: "",
                                    notes = child.child("notes").value?.toString() ?: ""
                                ))
                            }
                            patientList = patients
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        DashboardScreen(
                            doctorName = doctorName,
                            patientCount = patientList.size,
                            scanCount = localHistory.size,
                            recentScans = localHistory,
                            onNewScan = { 
                                selectedPatient = null 
                                navController.navigate("scan_selection") 
                            },
                            onAddPatient = { navController.navigate("add_patient") },
                            onShadeAnalysis = { navController.navigate("scan_history") },
                            onViewReports = { navController.navigate("scan_history") },
                            onScanClick = { result -> navController.navigate("history_report_detail/${result.id}") },
                            navController = navController
                        )
                    }
                    composable("scan_selection") {
                        ScanSelectionScreen(
                            onBack = { navController.navigate("home") { popUpTo("home") { inclusive = false } } },
                            onTakePhoto = { 
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    navController.navigate("camera")
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            onUploadImage = { galleryLauncher.launch("image/*") },
                            navController = navController
                        )
                    }
                    composable("camera") {
                        FullscreenCameraScreen(
                            onClose = { navController.navigate("home") { popUpTo("home") { inclusive = false } } },
                            onCapture = { uri -> 
                                capturedUri = uri
                                navController.navigate("preview") 
                            },
                            onOpenGallery = { galleryLauncher.launch("image/*") }
                        )
                    }
                    composable("preview") {
                        PreviewScreen(
                            capturedUri = capturedUri,
                            onRetake = { navController.popBackStack() },
                            onContinue = { navController.navigate("edit_image") },
                            onHome = { navController.navigate("home") { popUpTo("home") { inclusive = false } } }
                        )
                    }
                    composable("edit_image") {
                        ImageEditingScreen(
                            capturedUri = capturedUri,
                            onBack = { navController.popBackStack() },
                            onProcessed = { updatedUri ->
                                capturedUri = updatedUri
                                navController.navigate("processing")
                            }
                        )
                    }
                    composable("processing") {
                        var inputBitmap by remember { mutableStateOf<Bitmap?>(null) }
                        var isLoadingBitmap by remember { mutableStateOf(true) }

                        // Prevent Back navigation while AI inference is running
                        BackHandler(enabled = true) { }

                        LaunchedEffect(capturedUri) {
                            if (capturedUri != null) {
                                inputBitmap = loadBitmapFromUri(context, capturedUri!!)
                            }
                            isLoadingBitmap = false
                        }

                        if (isLoadingBitmap) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(PortalDark),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PortalAccent)
                            }
                        } else {
                            ProcessingScreen(
                                inputBitmap = inputBitmap,
                                onSuccess = { result ->
                                    analysisPredictions = result.topPredictions
                                    analysisResultShade = result.predictedShade
                                    analysisConfidence = "${result.confidencePercent}%"
                                    imageQualityText = result.imageQualityText
                                    croppedToothBitmap = result.croppedBitmap

                                    scope.launch {
                                        val localUri = capturedUri?.let { saveImageToInternalStorage(it) }
                                        val base64Image = result.croppedBitmap?.let { bitmapToBase64DataUrl(it) } 
                                            ?: localUri.toString()
                                        val scanId = UUID.randomUUID().toString()
                                        lastScanId = scanId

                                        historyManager.saveResult(ScanResult(
                                            id = scanId,
                                            dateTime = System.currentTimeMillis(),
                                            predictedShade = analysisResultShade,
                                            confidence = analysisConfidence,
                                            imageUri = base64Image,
                                            patientId = selectedPatient?.id ?: ""
                                        ))
                                        localHistory = historyManager.getHistory()
                                        // Replace workflow in stack so Back on Result page goes directly to Dashboard
                                        navController.navigate("result") {
                                            popUpTo("home") { inclusive = false }
                                        }
                                    }
                                },
                                onFail = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    navController.navigate("home") { popUpTo("home") { inclusive = false } }
                                }
                            )
                        }
                    }
                    composable("result") {
                        var showDiscardDialog by remember { mutableStateOf(false) }

                        val navigateToDashboard = {
                            capturedUri = null
                            selectedPatient = null
                            navController.navigate("home") { popUpTo("home") { inclusive = false } }
                        }

                        BackHandler {
                            showDiscardDialog = true
                        }

                        if (showDiscardDialog) {
                            AlertDialog(
                                onDismissRequest = { showDiscardDialog = false },
                                title = { Text("Discard this analysis?") },
                                text = { Text("Unsaved shade analysis data will be lost.") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showDiscardDialog = false
                                        navigateToDashboard()
                                    }) {
                                        Text("YES")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDiscardDialog = false }) {
                                        Text("NO")
                                    }
                                }
                            )
                        }

                        ResultScreen(
                            shade = analysisResultShade,
                            confidence = analysisConfidence,
                            qualityResultText = imageQualityText,
                            imageUri = capturedUri,
                            croppedBitmap = croppedToothBitmap,
                            predictions = analysisPredictions,
                            onSave = { 
                                if (selectedPatient == null) {
                                    navController.navigate("link_patient_to_report")
                                } else {
                                    performSaveReport(selectedPatient!!)
                                }
                            },
                            onScanAgain = { 
                                selectedPatient = null
                                navController.navigate("scan_selection") { popUpTo("home") { inclusive = false } }
                            },
                            onDashboard = { showDiscardDialog = true },
                            onGeneratePdf = {
                                val lastResult = localHistory.firstOrNull()
                                if (lastResult != null) {
                                    val pdfFile = pdfGenerator.generateReport(lastResult, selectedPatient)
                                    if (pdfFile != null) {
                                        sharePdf(pdfFile)
                                    } else {
                                        Toast.makeText(context, "Failed to generate report", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                    composable("scan_history") {
                        ScanHistoryScreen(
                            results = localHistory,
                            onBack = { navController.popBackStack() },
                            onDelete = { id -> 
                                historyManager.softDeleteResult(id)
                                localHistory = historyManager.getHistory()
                            },
                            onRecordClick = { report ->
                                navController.navigate("history_report_detail/${report.id}")
                            },
                            navController = navController
                        )
                    }
                    composable("history_report_detail/{resultId}") { backStackEntry ->
                        val resultId = backStackEntry.arguments?.getString("resultId")
                        val result = localHistory.find { it.id == resultId }
                        if (result != null) {
                            val patient = patientList.find { it.id == result.patientId }
                            ReportScreen(
                                report = result,
                                patient = patient,
                                onBack = { navController.popBackStack() },
                                onExportPdf = { 
                                    val pdfFile = pdfGenerator.generateReport(result, patient)
                                    if (pdfFile != null) {
                                        sharePdf(pdfFile)
                                    } else {
                                        Toast.makeText(context, "Failed to generate report", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDeleteScan = {
                                    historyManager.softDeleteResult(result.id)
                                    localHistory = historyManager.getHistory()
                                    val uid = auth.currentUser?.uid
                                    if (uid != null) {
                                        val reportRef = database.getReference("Reports").child(uid).child(result.id)
                                        reportRef.get().addOnSuccessListener { snapshot ->
                                            if (snapshot.exists()) {
                                                val data = snapshot.value
                                                val deletedRef = database.getReference("DeletedReports").child(uid).child(result.id)
                                                deletedRef.setValue(data).addOnCompleteListener {
                                                    deletedRef.child("deletedAt").setValue(System.currentTimeMillis())
                                                    reportRef.removeValue()
                                                }
                                            } else {
                                                val deletedRef = database.getReference("DeletedReports").child(uid).child(result.id)
                                                deletedRef.setValue(mapOf(
                                                    "patientId" to result.patientId,
                                                    "patientName" to (result.patientName ?: ""),
                                                    "shade" to result.predictedShade,
                                                    "confidence" to result.confidence,
                                                    "timestamp" to result.dateTime,
                                                    "deletedAt" to System.currentTimeMillis()
                                                ))
                                            }
                                        }
                                    }
                                    navController.popBackStack()
                                    Toast.makeText(context, "Scan moved to Deleted Scans", Toast.LENGTH_SHORT).show()
                                },
                                onReanalyzeScan = {
                                    navController.navigate("scan_selection")
                                },
                                onSaveNotes = { notes ->
                                    historyManager.updateDoctorNotes(result.id, notes)
                                    localHistory = historyManager.getHistory()
                                    Toast.makeText(context, "Notes saved!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                    composable("deleted_scans") {
                        DeletedScansScreen(
                            deletedScans = historyManager.getDeletedHistory(),
                            onBack = { navController.popBackStack() },
                            onRestore = { id ->
                                historyManager.restoreResult(id)
                                localHistory = historyManager.getHistory()
                                val uid = auth.currentUser?.uid
                                if (uid != null) {
                                    val deletedRef = database.getReference("DeletedReports").child(uid).child(id)
                                    deletedRef.get().addOnSuccessListener { snapshot ->
                                        if (snapshot.exists()) {
                                            val data = snapshot.value
                                            val reportRef = database.getReference("Reports").child(uid).child(id)
                                            reportRef.setValue(data).addOnCompleteListener {
                                                reportRef.child("deletedAt").removeValue()
                                                deletedRef.removeValue()
                                            }
                                        }
                                    }
                                }
                                Toast.makeText(context, "Scan restored!", Toast.LENGTH_SHORT).show()
                            },
                            onPermanentDelete = { id ->
                                historyManager.permanentlyDeleteResult(id)
                                val uid = auth.currentUser?.uid
                                if (uid != null) {
                                    database.getReference("DeletedReports").child(uid).child(id).removeValue()
                                }
                                Toast.makeText(context, "Scan permanently deleted", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteAll = {
                                historyManager.deleteAllDeleted()
                                val uid = auth.currentUser?.uid
                                if (uid != null) {
                                    database.getReference("DeletedReports").child(uid).removeValue()
                                }
                                Toast.makeText(context, "All deleted scans removed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    composable("link_patient_to_report") {
                        PatientSelectionScreen(
                            patients = patientList,
                            onBack = { navController.popBackStack() },
                            onPatientSelected = { patient ->
                                selectedPatient = patient
                                performSaveReport(patient)
                            },
                            onAddNew = { navController.navigate("add_patient_for_report") },
                            navController = navController
                        )
                    }
                    composable("add_patient_for_report") {
                        AddPatientScreen(
                            onBack = { navController.popBackStack() },
                            onSave = { patient ->
                                val uid = auth.currentUser?.uid
                                if (uid != null) {
                                    val patientData = mapOf("name" to patient.name, "age" to patient.age, "gender" to patient.gender, "phone" to patient.phone, "notes" to patient.notes, "timestamp" to System.currentTimeMillis())
                                    val newPatientRef = database.getReference("Patients").child(uid).push()
                                    val newPatientId = newPatientRef.key ?: ""
                                    newPatientRef.setValue(patientData).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val createdPatient = patient.copy(id = newPatientId)
                                            selectedPatient = createdPatient
                                            performSaveReport(createdPatient)
                                        } else {
                                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                    composable("add_patient") {
                        AddPatientScreen(
                            onBack = { navController.popBackStack() },
                            onSave = { patient ->
                                if (patientList.any { it.phone.trim() == patient.phone.trim() && patient.phone.isNotBlank() }) {
                                    Toast.makeText(context, "A patient with this mobile number already exists.", Toast.LENGTH_LONG).show()
                                } else {
                                    val uid = auth.currentUser?.uid
                                    if (uid != null) {
                                        val patientData = mapOf(
                                            "name" to patient.name,
                                            "age" to patient.age,
                                            "gender" to patient.gender,
                                            "phone" to patient.phone,
                                            "notes" to patient.notes,
                                            "email" to patient.email,
                                            "address" to patient.address,
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        database.getReference("Patients").child(uid).push().setValue(patientData)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    Toast.makeText(context, "Patient Profile Created!", Toast.LENGTH_SHORT).show()
                                                    navController.popBackStack()
                                                } else {
                                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                    }
                                }
                            }
                        )
                    }
                    composable("patients") {
                        PatientsScreen(
                            patients = patientList,
                            allScans = localHistory,
                            onBack = { navController.popBackStack() },
                            onAddNew = { navController.navigate("add_patient") },
                            navController = navController
                        )
                    }
                    composable("edit_patient/{patientId}") { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId")
                        val patient = patientList.find { it.id == patientId }
                        if (patient != null) {
                            EditPatientScreen(
                                patient = patient,
                                onBack = { navController.popBackStack() },
                                onSave = { updatedPatient ->
                                    val uid = auth.currentUser?.uid
                                    if (uid != null) {
                                        val updates = mapOf(
                                            "name" to updatedPatient.name,
                                            "age" to updatedPatient.age,
                                            "gender" to updatedPatient.gender,
                                            "phone" to updatedPatient.phone,
                                            "notes" to updatedPatient.notes
                                        )
                                        database.getReference("Patients").child(uid).child(patientId!!).updateChildren(updates)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    Toast.makeText(context, "Patient Updated!", Toast.LENGTH_SHORT).show()
                                                    navController.popBackStack()
                                                } else {
                                                    Toast.makeText(context, "Update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    }
                                },
                                onHome = { navController.navigate("home") { popUpTo("home") { inclusive = true } } }
                            )
                        }
                    }
                    composable("patient_history/{patientId}") { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId")
                        val patient = patientList.find { it.id == patientId }
                        if (patient != null) {
                            PatientHistoryScreen(
                                patient = patient,
                                reports = localHistory.filter { it.patientId == patientId },
                                onBack = { navController.navigate("patients") { popUpTo("home") } },
                                onEdit = { navController.navigate("edit_patient/$patientId") },
                                onStartScan = { p ->
                                    selectedPatient = p
                                    navController.navigate("scan_selection")
                                },
                                onDeleteScan = { scanId ->
                                    historyManager.softDeleteResult(scanId)
                                    localHistory = historyManager.getHistory()
                                },
                                navController = navController
                            )
                        }
                    }

                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onLogout = {
                                auth.signOut()
                                getSharedPreferences("login_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                                val intent = Intent(this@DashboardActivity, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            },
                            onEditProfile = { navController.navigate("edit_profile") },
                            onVitaGuide = { navController.navigate("vita_shade_guide") },
                            onOpenDeletedScans = { navController.navigate("deleted_scans") },
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { enabled ->
                                isDarkTheme = enabled
                                prefs.edit().putBoolean("dark_theme", enabled).apply()
                            },
                            isNotificationsEnabled = isNotificationsEnabled,
                            onNotificationsToggle = { enabled ->
                                isNotificationsEnabled = enabled
                                prefs.edit().putBoolean("notifications", enabled).apply()
                            },
                            isPrivacyEnabled = isPrivacyEnabled,
                            onPrivacyToggle = { enabled ->
                                isPrivacyEnabled = enabled
                                prefs.edit().putBoolean("privacy", enabled).apply()
                            },
                            navController = navController
                        )
                    }
                    composable("vita_shade_guide") {
                        VitaShadeGuideScreen(onBack = { navController.popBackStack() })
                    }
                    composable("edit_profile") {
                        EditProfileScreen(
                            currentName = doctorName,
                            currentAge = doctorAge,
                            currentGender = doctorGender,
                            currentMobile = doctorMobile,
                            onBack = { navController.popBackStack() },
                            onHome = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                            onSave = { name, age, gender, mobile ->
                                val uid = auth.currentUser?.uid
                                if (uid != null) {
                                    val updates = mapOf("fullName" to name, "age" to age, "gender" to gender, "mobile" to mobile)
                                    database.getReference("Users").child(uid).updateChildren(updates)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            } else {
                                                Toast.makeText(context, "Update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun sharePdf(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Clinical Report"))
    }

    private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveImageToInternalStorage(uri: Uri): Uri = withContext(Dispatchers.IO) {
        val inputStream = contentResolver.openInputStream(uri)
        val fileName = "scan_${System.currentTimeMillis()}.jpg"
        val file = File(filesDir, "scans/$fileName")
        file.parentFile?.mkdirs()
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        Uri.fromFile(file)
    }

    private fun bitmapToBase64DataUrl(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    override fun onResume() {
        super.onResume()
        if (::auth.isInitialized && auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::shadeClassifier.isInitialized) {
            shadeClassifier.close()
        }
    }
}

data class DeletedScanRecord(
    val scan: ScanResult,
    val deletedAt: Long
)

class LocalScanHistoryManager(private val context: Context) {
    private val historyFile = File(context.filesDir, "scan_history.json")
    private val deletedFile = File(context.filesDir, "deleted_scans.json")

    fun getHistory(): List<ScanResult> {
        if (!historyFile.exists()) return emptyList()
        return try {
            val json = historyFile.readText()
            val array = JSONArray(json)
            val list = mutableListOf<ScanResult>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(ScanResult(
                    id = obj.getString("id"),
                    dateTime = obj.getLong("dateTime"),
                    predictedShade = obj.getString("predictedShade"),
                    confidence = obj.getString("confidence"),
                    imageUri = obj.getString("imageUri"),
                    patientId = obj.optString("patientId", ""),
                    patientName = obj.optString("patientName", "Quick Scan"),
                    doctorNotes = obj.optString("doctorNotes", "")
                ))
            }
            list.sortedByDescending { it.dateTime }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getDeletedHistory(): List<DeletedScanRecord> {
        if (!deletedFile.exists()) return emptyList()
        return try {
            val json = deletedFile.readText()
            val array = JSONArray(json)
            val list = mutableListOf<DeletedScanRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val scanObj = obj.getJSONObject("scan")
                val scan = ScanResult(
                    id = scanObj.getString("id"),
                    dateTime = scanObj.getLong("dateTime"),
                    predictedShade = scanObj.getString("predictedShade"),
                    confidence = scanObj.getString("confidence"),
                    imageUri = scanObj.getString("imageUri"),
                    patientId = scanObj.optString("patientId", ""),
                    patientName = scanObj.optString("patientName", "Quick Scan"),
                    doctorNotes = scanObj.optString("doctorNotes", "")
                )
                list.add(DeletedScanRecord(scan, obj.getLong("deletedAt")))
            }
            list.sortedByDescending { it.deletedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveResult(result: ScanResult) {
        val list = getHistory().toMutableList()
        list.add(0, result) 
        saveList(list)
    }

    fun updatePatientId(scanId: String, patientId: String, patientName: String = "Quick Scan") {
        val list = getHistory().map { 
            if (it.id == scanId) it.copy(patientId = patientId, patientName = patientName) else it 
        }
        saveList(list)
    }

    fun updateDoctorNotes(scanId: String, notes: String) {
        val list = getHistory().map { 
            if (it.id == scanId) it.copy(doctorNotes = notes) else it 
        }
        saveList(list)
    }

    fun softDeleteResult(id: String) {
        val activeList = getHistory().toMutableList()
        val deletedList = getDeletedHistory().toMutableList()
        val target = activeList.find { it.id == id }
        if (target != null) {
            activeList.remove(target)
            deletedList.add(0, DeletedScanRecord(target, System.currentTimeMillis()))
            saveList(activeList)
            saveDeletedList(deletedList)
        }
    }

    fun restoreResult(id: String) {
        val activeList = getHistory().toMutableList()
        val deletedList = getDeletedHistory().toMutableList()
        val targetRecord = deletedList.find { it.scan.id == id }
        if (targetRecord != null) {
            deletedList.remove(targetRecord)
            activeList.add(0, targetRecord.scan)
            saveList(activeList)
            saveDeletedList(deletedList)
        }
    }

    fun permanentlyDeleteResult(id: String) {
        val deletedList = getDeletedHistory().toMutableList()
        val target = deletedList.find { it.scan.id == id }
        if (target != null) {
            try {
                val file = File(Uri.parse(target.scan.imageUri).path!!)
                if (file.exists()) file.delete()
            } catch (e: Exception) { }
            deletedList.remove(target)
            saveDeletedList(deletedList)
        }
    }

    fun deleteAllDeleted() {
        val deletedList = getDeletedHistory()
        deletedList.forEach { record ->
            try {
                val file = File(Uri.parse(record.scan.imageUri).path!!)
                if (file.exists()) file.delete()
            } catch (e: Exception) { }
        }
        saveDeletedList(emptyList())
    }

    private fun saveList(list: List<ScanResult>) {
        val array = JSONArray()
        list.forEach { result ->
            val obj = JSONObject()
            obj.put("id", result.id)
            obj.put("dateTime", result.dateTime)
            obj.put("predictedShade", result.predictedShade)
            obj.put("confidence", result.confidence)
            obj.put("imageUri", result.imageUri)
            obj.put("patientId", result.patientId)
            obj.put("patientName", result.patientName)
            obj.put("doctorNotes", result.doctorNotes)
            array.put(obj)
        }
        historyFile.writeText(array.toString())
    }

    private fun saveDeletedList(list: List<DeletedScanRecord>) {
        val array = JSONArray()
        list.forEach { record ->
            val obj = JSONObject()
            val scanObj = JSONObject()
            scanObj.put("id", record.scan.id)
            scanObj.put("dateTime", record.scan.dateTime)
            scanObj.put("predictedShade", record.scan.predictedShade)
            scanObj.put("confidence", record.scan.confidence)
            scanObj.put("imageUri", record.scan.imageUri)
            scanObj.put("patientId", record.scan.patientId)
            scanObj.put("patientName", record.scan.patientName)
            scanObj.put("doctorNotes", record.scan.doctorNotes)
            obj.put("scan", scanObj)
            obj.put("deletedAt", record.deletedAt)
            array.put(obj)
        }
        deletedFile.writeText(array.toString())
    }
}
