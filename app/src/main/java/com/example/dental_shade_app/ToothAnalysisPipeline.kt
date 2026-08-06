package com.example.dental_shade_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs

enum class PipelineStage(val stageName: String) {
    DETECTING_TOOTH("Detecting tooth..."),
    CROPPING_TOOTH("Cropping tooth..."),
    CHECKING_QUALITY("Checking image quality..."),
    CORRECTING_LIGHTING("Correcting lighting..."),
    RUNNING_AI("Running AI..."),
    COMPARING_VITA("Comparing with VITA shades..."),
    CONFIDENCE_SCORE("Confidence score...")
}

enum class StepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

data class PipelineStepState(
    val stage: PipelineStage,
    val status: StepStatus = StepStatus.PENDING,
    val detailMessage: String = ""
)

data class PipelineFinalResult(
    val predictedShade: String,
    val confidencePercent: Float,
    val imageQualityText: String,
    val qualityScore: Int,
    val croppedBitmap: Bitmap,
    val correctedBitmap: Bitmap,
    val topPredictions: List<Prediction>
)

sealed class PipelineState {
    data class Progress(val steps: List<PipelineStepState>, val currentStageIndex: Int) : PipelineState()
    data class Success(val result: PipelineFinalResult, val steps: List<PipelineStepState>) : PipelineState()
    data class Error(val errorMessage: String, val steps: List<PipelineStepState>, val failedStage: PipelineStage) : PipelineState()
}

class ToothAnalysisPipeline(private val context: Context) {

    private val classifier = ToothShadeClassifier(context)
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.Builder()
        .setConfidenceThreshold(0.5f)
        .build())

    fun runPipeline(inputBitmap: Bitmap): Flow<PipelineState> = flow {
        val steps = PipelineStage.values().map { stage ->
            PipelineStepState(stage = stage, status = StepStatus.PENDING)
        }.toMutableList()

        fun updateStep(index: Int, status: StepStatus, msg: String = "") {
            steps[index] = steps[index].copy(status = status, detailMessage = msg)
        }

        suspend fun emitProgress(activeIdx: Int) {
            emit(PipelineState.Progress(steps.toList(), activeIdx))
        }

        // STEP 0: ML Kit hard gate — reject non-tooth images before any pixel analysis
        updateStep(0, StepStatus.RUNNING, "Checking if image contains teeth...")
        emitProgress(0)

        val toothLabels = setOf(
            "tooth", "teeth", "mouth", "dentistry", "dentist",
            "smile", "human mouth", "jaw", "gums", "oral",
            "incisor", "molar", "canine", "enamel", "crown"
        )

        val mlKitResult = suspendCancellableCoroutine<Pair<Boolean, String>> { cont ->
            val image = InputImage.fromBitmap(inputBitmap, 0)
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val labelMap = labels.associate { it.text.lowercase() to it.confidence }

                    // Check for any tooth-related label
                    val toothMatch = labelMap.entries.firstOrNull { entry ->
                        toothLabels.any { kw -> entry.key.contains(kw) } && entry.value >= 0.55f
                    }

                    // Face detected without teeth = always invalid
                    val faceLabels = setOf("face", "nose", "eye", "ear", "forehead", "cheek",
                        "skin", "hair", "person", "selfie", "portrait", "head", "neck", "lip")
                    val hasFace = labelMap.entries.any { entry ->
                        faceLabels.any { kw -> entry.key.contains(kw) } && entry.value >= 0.55f
                    }

                    // Check for other strong reject labels (non-face non-tooth objects)
                    val otherRejectLabels = setOf(
                        "laptop", "computer", "keyboard", "table", "desk",
                        "furniture", "electronics", "food", "plant", "vehicle",
                        "sky", "building", "animal", "cat", "dog", "hand",
                        "finger", "wall", "floor", "ceiling",
                        "paper", "book", "phone", "bottle", "cup", "cloth",
                        "shirt", "glasses"
                    )
                    val hasOtherReject = labelMap.entries.any { entry ->
                        otherRejectLabels.any { kw -> entry.key.contains(kw) } && entry.value >= 0.60f
                    }

                    val allLabelsText = labels.take(5).joinToString { "${it.text}(${String.format("%.0f", it.confidence * 100)}%)" }
                    android.util.Log.d("ToothGate", "ML Kit labels: $allLabelsText | hasFace=$hasFace | toothMatch=${toothMatch?.key}")

                    when {
                        // Tooth detected — always allow regardless of face
                        toothMatch != null -> cont.resume(Pair(true, "Teeth detected: ${toothMatch.key} (${String.format("%.0f", toothMatch.value * 100)}%)"))
                        // Face without teeth — ALWAYS reject
                        hasFace -> cont.resume(Pair(false, "No teeth visible. Please open your mouth and show your teeth clearly for accurate shade analysis."))
                        // Non-tooth object (laptop, table, etc.)
                        hasOtherReject -> cont.resume(Pair(false, "Image does not contain teeth. Please capture a clear photo of the patient's teeth."))
                        // No recognizable label — let pixel analysis decide
                        else -> cont.resume(Pair(true, "No strong label — proceeding to pixel analysis"))
                    }
                }
                .addOnFailureListener {
                    // If ML Kit fails, allow pipeline to continue with pixel check
                    cont.resume(Pair(true, "ML Kit unavailable, continuing with pixel analysis"))
                }
        }

        if (!mlKitResult.first) {
            updateStep(0, StepStatus.FAILED, mlKitResult.second)
            emit(PipelineState.Error(
                errorMessage = mlKitResult.second,
                steps = steps.toList(),
                failedStage = PipelineStage.DETECTING_TOOTH
            ))
            return@flow
        }
        updateStep(0, StepStatus.COMPLETED, mlKitResult.second)

        // 1. Pixel-based tooth bounding box detection
        emitProgress(0)
        kotlinx.coroutines.delay(200)

        val toothRect = detectToothBoundingBox(inputBitmap)
        if (toothRect == null || toothRect.width() < 30 || toothRect.height() < 30) {
            updateStep(0, StepStatus.FAILED, "Tooth region not locatable in frame")
            emit(PipelineState.Error(
                errorMessage = "Could not locate tooth region. Please align the tooth clearly in the frame and try again.",
                steps = steps.toList(),
                failedStage = PipelineStage.DETECTING_TOOTH
            ))
            return@flow
        }
        updateStep(0, StepStatus.COMPLETED, "Tooth region located")

        // 2. Cropping tooth...
        updateStep(1, StepStatus.RUNNING, "Extracting tooth region...")
        emitProgress(1)
        kotlinx.coroutines.delay(300)

        val croppedBitmap = cropBitmapSafely(inputBitmap, toothRect)
        if (croppedBitmap == null || croppedBitmap.width < 20 || croppedBitmap.height < 20) {
            updateStep(1, StepStatus.FAILED, "Invalid crop area")
            emit(PipelineState.Error(
                errorMessage = "Failed to crop tooth region due to invalid dimensions.",
                steps = steps.toList(),
                failedStage = PipelineStage.CROPPING_TOOTH
            ))
            return@flow
        }
        updateStep(1, StepStatus.COMPLETED, "Tooth region extracted (${croppedBitmap.width}x${croppedBitmap.height}px)")

        // 3. Checking image quality...
        updateStep(2, StepStatus.RUNNING, "Evaluating brightness, sharpness & enamel visibility...")
        emitProgress(2)
        kotlinx.coroutines.delay(300)

        val qualityResult = checkCroppedQuality(croppedBitmap)
        if (!qualityResult.isValid) {
            updateStep(2, StepStatus.FAILED, qualityResult.reason)
            emit(PipelineState.Error(
                errorMessage = qualityResult.reason,
                steps = steps.toList(),
                failedStage = PipelineStage.CHECKING_QUALITY
            ))
            return@flow
        }
        updateStep(2, StepStatus.COMPLETED, "Quality score: ${qualityResult.qualityScore}% (${qualityResult.qualityLabel})")

        // 4. Correcting lighting...
        updateStep(3, StepStatus.RUNNING, "Applying white-balance and luminance normalization...")
        emitProgress(3)
        kotlinx.coroutines.delay(300)

        val correctedBitmap = correctLightingAndColor(croppedBitmap)
        updateStep(3, StepStatus.COMPLETED, "Lighting and shadows normalized")

        // 5. Running AI...
        updateStep(4, StepStatus.RUNNING, "Executing TensorFlow Lite inference (shade_model.tflite)...")
        emitProgress(4)
        kotlinx.coroutines.delay(300)

        val predictions = try {
            classifier.classify(correctedBitmap)
        } catch (e: Exception) {
            updateStep(4, StepStatus.FAILED, "Model execution error: ${e.localizedMessage}")
            emit(PipelineState.Error(
                errorMessage = "TensorFlow Lite model inference failed: ${e.localizedMessage}",
                steps = steps.toList(),
                failedStage = PipelineStage.RUNNING_AI
            ))
            return@flow
        }

        if (predictions.isEmpty()) {
            updateStep(4, StepStatus.FAILED, "Model output empty")
            emit(PipelineState.Error(
                errorMessage = "Unexpected model output: Empty prediction list received.",
                steps = steps.toList(),
                failedStage = PipelineStage.RUNNING_AI
            ))
            return@flow
        }
        updateStep(4, StepStatus.COMPLETED, "Model output obtained (${predictions.size} classes evaluated)")

        // 6. Comparing with VITA shades...
        updateStep(5, StepStatus.RUNNING, "Mapping to VITA Classical standard categories...")
        emitProgress(5)
        kotlinx.coroutines.delay(300)

        val topPrediction = predictions.first()
        val predictedShade = topPrediction.label
        updateStep(5, StepStatus.COMPLETED, "Top match: VITA $predictedShade")

        // 7. Confidence score...
        updateStep(6, StepStatus.RUNNING, "Computing model output probability...")
        emitProgress(6)
        kotlinx.coroutines.delay(300)

        val rawConf = (topPrediction.confidence * 100f).coerceIn(15f, 98.5f)
        val formattedConf = String.format("%.1f", rawConf).toFloat()
        updateStep(6, StepStatus.COMPLETED, "Confidence score: $formattedConf%")

        val finalResult = PipelineFinalResult(
            predictedShade = predictedShade,
            confidencePercent = formattedConf,
            imageQualityText = qualityResult.qualityLabel,
            qualityScore = qualityResult.qualityScore,
            croppedBitmap = croppedBitmap,
            correctedBitmap = correctedBitmap,
            topPredictions = predictions
        )

        emit(PipelineState.Success(finalResult, steps.toList()))
    }.flowOn(Dispatchers.Default)

    private fun detectToothBoundingBox(bitmap: Bitmap): Rect? {
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0
        var toothPixelCount = 0
        var totalSampledCount = 0

        val step = 2
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val brightness = (r + g + b) / 3
                totalSampledCount++

                // Tightened: must be bright ivory/white, low saturation, not reddish skin tone
                val isIvory = r >= g && g >= (b - 20) && brightness in 100..250
                val isLowSat = abs(r - g) < 35 && abs(g - b) < 40 && abs(r - b) < 50
                // Exclude reddish/pinkish skin tones: skin has high R, lower B
                // Also exclude lips (high R-B difference even at lower brightness)
                val isSkin = (r > 140 && (r - b) > 30 && g < 210) ||
                             (r > 120 && (r - b) > 50)  // lips / darker skin

                if (isIvory && isLowSat && !isSkin) {
                    toothPixelCount++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val enamelRatio = if (totalSampledCount > 0) toothPixelCount.toFloat() / totalSampledCount else 0f

        // Raised threshold to 0.22 — face/skin rarely reach this without actual teeth
        if (enamelRatio >= 0.22f) {
            val padX = ((maxX - minX) * 0.10f).toInt().coerceAtLeast(10)
            val padY = ((maxY - minY) * 0.10f).toInt().coerceAtLeast(10)

            val left = (minX - padX).coerceAtLeast(0)
            val top = (minY - padY).coerceAtLeast(0)
            val right = (maxX + padX).coerceAtMost(width)
            val bottom = (maxY + padY).coerceAtMost(height)

            return Rect(left, top, right, bottom)
        }

        return null
    }

    private fun cropBitmapSafely(bitmap: Bitmap, rect: Rect): Bitmap? {
        return try {
            val left = rect.left.coerceIn(0, bitmap.width - 1)
            val top = rect.top.coerceIn(0, bitmap.height - 1)
            val width = (rect.width()).coerceIn(1, bitmap.width - left)
            val height = (rect.height()).coerceIn(1, bitmap.height - top)
            Bitmap.createBitmap(bitmap, left, top, width, height)
        } catch (e: Exception) {
            null
        }
    }

    private data class QualityEvaluation(
        val isValid: Boolean,
        val qualityScore: Int,
        val qualityLabel: String,
        val reason: String
    )

    private fun checkCroppedQuality(bitmap: Bitmap): QualityEvaluation {
        val width = bitmap.width
        val height = bitmap.height

        if (width < 30 || height < 30) {
            return QualityEvaluation(false, 0, "Too Small", "Image resolution is too low. Please capture another image.")
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalLum = 0L
        var enamelCount = 0
        var prevGray = 0
        var diffSum = 0L
        var sampleCount = 0

        val step = 2
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val p = pixels[y * width + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                val brightness = (r + g + b) / 3
                totalLum += brightness

                // Tightened enamel detection — must be bright ivory, low sat, not skin-tone
                val isIvory = r >= g && g >= (b - 20) && brightness in 100..250
                val isLowSat = abs(r - g) < 35 && abs(g - b) < 40
                val isSkin = r > 150 && (r - b) > 40 && g < 200
                if (isIvory && isLowSat && !isSkin) {
                    enamelCount++
                }

                diffSum += abs(brightness - prevGray)
                prevGray = brightness
                sampleCount++
            }
        }

        val avgBrightness = if (sampleCount > 0) totalLum.toDouble() / sampleCount else 128.0
        val enamelRatio = if (sampleCount > 0) enamelCount.toDouble() / sampleCount else 0.5
        val sharpness = if (sampleCount > 0) diffSum.toDouble() / sampleCount else 10.0

        if (avgBrightness < 60) {
            return QualityEvaluation(false, (avgBrightness / 2.5).toInt(), "Low Light", "Image is too dark. Please capture under better lighting.")
        }
        if (avgBrightness > 242) {
            return QualityEvaluation(false, 40, "Overexposed", "Image is overexposed with glare. Reduce flash or direct light.")
        }
        if (sharpness < 2.0) {
            return QualityEvaluation(false, 45, "Blurry", "Image is too blurry. Hold steady and focus clearly on the tooth.")
        }
        // Raised threshold from 0.06 to 0.10
        if (enamelRatio < 0.10) {
            return QualityEvaluation(false, 30, "No Tooth Detected", "Tooth region is not visible clearly. Please align the central tooth inside the camera frame.")
        }

        val qualityScore = ((sharpness * 6).coerceAtMost(40.0) + (1.0 - abs(avgBrightness - 140) / 140.0) * 60).toInt().coerceIn(70, 98)
        val label = when {
            qualityScore >= 90 -> "Optimal ($qualityScore%)"
            qualityScore >= 80 -> "Good ($qualityScore%)"
            else -> "Acceptable ($qualityScore%)"
        }

        return QualityEvaluation(true, qualityScore, label, "")
    }

    private fun correctLightingAndColor(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var count = 0

        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val brightness = (r + g + b) / 3

            if (brightness in 60..245) {
                totalR += r
                totalG += g
                totalB += b
                count++
            }
        }

        if (count == 0) return bitmap

        val avgR = (totalR / count).toFloat().coerceAtLeast(1f)
        val avgG = (totalG / count).toFloat().coerceAtLeast(1f)
        val avgB = (totalB / count).toFloat().coerceAtLeast(1f)

        val targetLum = 135f
        val gainR = (targetLum / avgR).coerceIn(0.7f, 1.4f)
        val gainG = (targetLum / avgG).coerceIn(0.7f, 1.4f)
        val gainB = (targetLum / avgB).coerceIn(0.7f, 1.4f)

        val outputPixels = IntArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p shr 24) and 0xFF
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF

            val newR = (r * gainR).toInt().coerceIn(0, 255)
            val newG = (g * gainG).toInt().coerceIn(0, 255)
            val newB = (b * gainB).toInt().coerceIn(0, 255)

            outputPixels[i] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
        }

        return Bitmap.createBitmap(outputPixels, width, height, Bitmap.Config.ARGB_8888)
    }

    fun close() {
        classifier.close()
        labeler.close()
    }
}
