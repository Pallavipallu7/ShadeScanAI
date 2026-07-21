package com.example.dental_shade_app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
import java.util.Date
import java.util.Locale
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
        database = try {
            FirebaseDatabase.getInstance(DB_URL)
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }
        
        database.goOnline()
        shadeClassifier = ToothShadeClassifier(this)
        historyManager = LocalScanHistoryManager(this)
        pdfGenerator = PdfReportGenerator(this)

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
                
                var patientList by remember { mutableStateOf<List<Patient>>(emptyList()) }
                var localHistory by remember { mutableStateOf(historyManager.getHistory()) }
                
                var isConnected by remember { mutableStateOf(false) }
                
                // AI Results
                var analysisResultShade by remember { mutableStateOf("") }
                var analysisConfidence by remember { mutableStateOf("") }
                var analysisPredictions by remember { mutableStateOf<List<Prediction>>(emptyList()) }

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
                        database.getReference("Reports").child(uid).push().setValue(reportData)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Report Saved Successfully!", Toast.LENGTH_SHORT).show()
                                    navController.navigate("scan_history") { popUpTo("home") }
                                } else {
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
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
                            onNewScan = { 
                                selectedPatient = null 
                                navController.navigate("scan_selection") 
                            },
                            onAddPatient = { navController.navigate("add_patient") },
                            onShadeAnalysis = { navController.navigate("scan_history") },
                            onViewReports = { navController.navigate("scan_history") },
                            navController = navController
                        )
                    }
                    composable("scan_selection") {
                        ScanSelectionScreen(
                            onBack = { navController.popBackStack() },
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
                            onClose = { navController.popBackStack() },
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
                            onHome = { navController.navigate("home") { popUpTo("home") { inclusive = true } } }
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
                        var showOverride by remember { mutableStateOf(false) }
                        if (showOverride) {
                            AlertDialog(
                                onDismissRequest = { },
                                title = { Text("Detection Inconclusive") },
                                text = { Text("AI could not verify dental structures with high confidence. Do you want to continue anyway?") },
                                confirmButton = {
                                    Button(onClick = {
                                        scope.launch {
                                            capturedUri?.let { uri ->
                                                val bitmap = loadBitmapFromUri(context, uri)
                                                if (bitmap != null) {
                                                    val results = withContext(Dispatchers.Default) { shadeClassifier.classify(bitmap) }
                                                    if (results.isNotEmpty()) {
                                                        analysisPredictions = results
                                                        analysisResultShade = results[0].label
                                                        analysisConfidence = "${(results[0].confidence * 100).toInt()}%"
                                                        
                                                        val localUri = saveImageToInternalStorage(uri)
                                                        historyManager.saveResult(ScanResult(
                                                            id = UUID.randomUUID().toString(),
                                                            dateTime = System.currentTimeMillis(),
                                                            predictedShade = analysisResultShade,
                                                            confidence = analysisConfidence,
                                                            imageUri = localUri.toString()
                                                        ))
                                                        localHistory = historyManager.getHistory()
                                                        navController.navigate("result")
                                                    }
                                                }
                                            }
                                        }
                                    }) { Text("CONTINUE ANYWAY") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { navController.popBackStack() }) { Text("RETAKE") }
                                }
                            )
                        }
                        ProcessingScreen(
                            onComplete = {
                                scope.launch {
                                    val uri = capturedUri ?: return@launch
                                    val bitmap = loadBitmapFromUri(context, uri)
                                    if (bitmap == null) {
                                        Toast.makeText(context, "Error reading image", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                        return@launch
                                    }

                                    val image = InputImage.fromBitmap(bitmap, 0)
                                    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                                    
                                    try {
                                        val labels = labeler.process(image).await()
                                        val primaryKeywords = listOf("Tooth", "Teeth", "Dentistry", "Dental", "Organ", "Bone", "Enamel")
                                        val secondaryKeywords = listOf("Mouth", "Human mouth", "Smile", "Lip", "Jaw")
                                        
                                        val hasPrimary = labels.any { label -> primaryKeywords.any { kw -> label.text.contains(kw, ignoreCase = true) } && label.confidence > 0.35f }
                                        val hasContext = labels.any { label -> secondaryKeywords.any { kw -> label.text.contains(kw, ignoreCase = true) } && label.confidence > 0.5f }
                                        
                                        val nonDentalKeywords = listOf("Laptop", "Computer", "Keyboard", "Furniture", "Table", "Car", "Building", "Phone", "Mobile phone")
                                        val isClearNonDental = labels.any { label -> nonDentalKeywords.any { kw -> label.text.contains(kw, ignoreCase = true) } && label.confidence > 0.6f }

                                        if (isClearNonDental) {
                                            Toast.makeText(context, "No dental tooth detected. Please capture a valid dental image.", Toast.LENGTH_LONG).show()
                                            navController.popBackStack()
                                        } else if (!hasPrimary && !hasContext) {
                                            showOverride = true
                                        } else {
                                            val results = withContext(Dispatchers.Default) { shadeClassifier.classify(bitmap) }
                                            if (results.isNotEmpty()) {
                                                analysisPredictions = results
                                                analysisResultShade = results[0].label
                                                analysisConfidence = "${(results[0].confidence * 100).toInt()}%"
                                                
                                                val localUri = saveImageToInternalStorage(uri)
                                                historyManager.saveResult(ScanResult(
                                                    id = UUID.randomUUID().toString(),
                                                    dateTime = System.currentTimeMillis(),
                                                    predictedShade = analysisResultShade,
                                                    confidence = analysisConfidence,
                                                    imageUri = localUri.toString()
                                                ))
                                                localHistory = historyManager.getHistory()
                                                navController.navigate("result")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "AI Analysis failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        navController.popBackStack()
                                    }
                                }
                            },
                            onFail = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("result") {
                        ResultScreen(
                            shade = analysisResultShade,
                            confidence = analysisConfidence,
                            imageUri = capturedUri,
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
                                navController.navigate("scan_selection") 
                            },
                            onDashboard = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
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
                                historyManager.deleteResult(id)
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
                            ReportScreen(
                                report = result,
                                patient = null,
                                onBack = { navController.popBackStack() },
                                onHome = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                                onExportPdf = { 
                                    val pdfFile = pdfGenerator.generateReport(result, null)
                                    if (pdfFile != null) {
                                        sharePdf(pdfFile)
                                    } else {
                                        Toast.makeText(context, "Failed to generate report", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
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
                            onHome = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                            onSave = { patient ->
                                val uid = auth.currentUser?.uid ?: return@AddPatientScreen
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
                        )
                    }
                    composable("add_patient") {
                        AddPatientScreen(
                            onBack = { navController.popBackStack() },
                            onHome = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                            onSave = { patient ->
                                val uid = auth.currentUser?.uid ?: return@AddPatientScreen
                                val patientData = mapOf("name" to patient.name, "age" to patient.age, "gender" to patient.gender, "phone" to patient.phone, "notes" to patient.notes, "timestamp" to System.currentTimeMillis())
                                database.getReference("Patients").child(uid).push().setValue(patientData)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(context, "Patient Saved!", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        } else {
                                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
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
                                    val uid = auth.currentUser?.uid ?: return@EditPatientScreen
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
                                reports = localHistory,
                                onBack = { navController.popBackStack() },
                                onEdit = { navController.navigate("edit_patient/$patientId") },
                                navController = navController
                            )
                        }
                    }
                    composable("patients") {
                        PatientsScreen(
                            patients = patientList,
                            onBack = { navController.popBackStack() },
                            onAddNew = { navController.navigate("add_patient") },
                            navController = navController
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onLogout = {
                                auth.signOut()
                                val intent = Intent(this@DashboardActivity, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            },
                            onEditProfile = { navController.navigate("edit_profile") },
                            onVitaGuide = { navController.navigate("vita_shade_guide") },
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { 
                                isDarkTheme = it
                                prefs.edit().putBoolean("dark_theme", it).apply()
                            },
                            isNotificationsEnabled = isNotificationsEnabled,
                            onNotificationsToggle = {
                                isNotificationsEnabled = it
                                prefs.edit().putBoolean("notifications", it).apply()
                            },
                            isPrivacyEnabled = isPrivacyEnabled,
                            onPrivacyToggle = {
                                isPrivacyEnabled = it
                                prefs.edit().putBoolean("privacy", it).apply()
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
                                val uid = auth.currentUser?.uid ?: return@EditProfileScreen
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

    override fun onDestroy() {
        super.onDestroy()
        if (::shadeClassifier.isInitialized) {
            shadeClassifier.close()
        }
    }
}

class LocalScanHistoryManager(private val context: Context) {
    private val historyFile = File(context.filesDir, "scan_history.json")

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
                    imageUri = obj.getString("imageUri")
                ))
            }
            list.sortedByDescending { it.dateTime }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveResult(result: ScanResult) {
        val list = getHistory().toMutableList()
        list.add(0, result) // Add to top
        saveList(list)
    }

    fun deleteResult(id: String) {
        val list = getHistory().toMutableList()
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.id == id) {
                // Delete image file as well
                try {
                    val file = File(Uri.parse(item.imageUri).path!!)
                    if (file.exists()) file.delete()
                } catch (e: Exception) { }
                iterator.remove()
                break
            }
        }
        saveList(list)
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
            array.put(obj)
        }
        historyFile.writeText(array.toString())
    }
}
