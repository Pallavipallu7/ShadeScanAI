package com.example.dental_shade_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.ByteArrayOutputStream

class ToothValidationManager(private val context: Context) {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    fun validateImage(bitmap: Bitmap, callback: (ValidationResult) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        labeler.process(image)
            .addOnSuccessListener { labels ->
                val toothKeywords = listOf("tooth", "teeth", "mouth", "dentist", "dentistry", "smile", "lip", "human mouth")
                val found = labels.filter { label -> 
                    toothKeywords.any { target -> label.text.lowercase().contains(target) }
                }.maxByOrNull { it.confidence }

                if (found != null && found.confidence >= 0.85f) {
                    callback(ValidationResult.Valid(found.confidence))
                } else {
                    callback(ValidationResult.Invalid("No teeth detected in the image. Please rescan or upload a proper tooth image."))
                }
            }
            .addOnFailureListener {
                callback(ValidationResult.Invalid("Validation error: ${it.message}"))
            }
    }
}

sealed class ValidationResult {
    data class Valid(val confidence: Float) : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

object ImageQualityChecker {
    fun checkQuality(imageProxy: ImageProxy): QualityResult {
        val brightness = BrightnessAnalyzer.analyze(imageProxy)
        if (brightness < 50) return QualityResult.LowLight
        if (brightness > 240) return QualityResult.TooBright
        
        val isBlurry = BlurDetectionHelper.isBlurry(imageProxy)
        if (isBlurry) return QualityResult.Blurry
        
        return QualityResult.Good
    }

    fun checkBitmapQuality(bitmap: Bitmap?): DetailedQualityResult {
        if (bitmap == null) {
            return DetailedQualityResult.Invalid("Please capture or upload a clear tooth image before analysis.")
        }

        val width = bitmap.width
        val height = bitmap.height

        if (width < 150 || height < 150) {
            return DetailedQualityResult.Invalid("No tooth detected. Please upload or capture a clear image of the patient's teeth.")
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalLum = 0L
        var toothPixels = 0
        var prevGray = 0

        var diffSum = 0L
        var edgeCount = 0
        val luminances = mutableListOf<Double>()

        val step = 4
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val p = pixels[y * width + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                val brightness = (r + g + b) / 3.0
                totalLum += brightness.toLong()
                luminances.add(brightness)

                val isIvory = r >= g && g >= (b - 30) && brightness in 85.0..248.0
                val isLowSat = Math.abs(r - g) < 50 && Math.abs(g - b) < 60
                if (isIvory && isLowSat) {
                    toothPixels++
                }

                diffSum += Math.abs(brightness.toLong() - prevGray)
                prevGray = brightness.toInt()
                edgeCount++
            }
        }

        val totalSampled = luminances.size
        val avgBrightness = if (totalSampled > 0) totalLum.toDouble() / totalSampled else 128.0
        val enamelRatio = if (totalSampled > 0) toothPixels.toDouble() / totalSampled else 0.0
        val sharpness = if (edgeCount > 0) diffSum.toDouble() / edgeCount else 10.0

        var varianceSum = 0.0
        for (lum in luminances) {
            varianceSum += Math.pow(lum - avgBrightness, 2.0)
        }
        val stdDev = if (totalSampled > 0) Math.sqrt(varianceSum / totalSampled) else 0.0

        // 1. Blank / Solid Color / Black or White Image Check
        if (stdDev < 3.0) {
            return DetailedQualityResult.Invalid("No tooth detected. Please upload or capture a clear image of the patient's teeth.")
        }

        // 2. Tooth Detection Pre-Check (Random object / non-tooth image)
        if (enamelRatio < 0.12) {
            return DetailedQualityResult.Invalid("No tooth detected. Please upload or capture a clear image of the patient's teeth.")
        }

        // 3. Image Quality Validation (Blur / Dark / Bright)
        if (avgBrightness < 70.0 || avgBrightness > 230.0 || sharpness < 2.2) {
            return DetailedQualityResult.Invalid("Image quality is insufficient. Please capture a clearer tooth image.")
        }

        val qualityScore = (Math.min(100.0, sharpness * 10) * 0.4 + (1.0 - Math.abs(avgBrightness - 140) / 140.0) * 60).toInt().coerceIn(70, 99)
        val lightingScore = (100 - (Math.abs(avgBrightness - 135) / 135.0 * 50)).toInt().coerceIn(65, 98)

        return DetailedQualityResult.Valid(qualityScore, lightingScore, avgBrightness, sharpness)
    }
}

sealed class DetailedQualityResult {
    data class Valid(val qualityScore: Int, val lightingScore: Int, val avgBrightness: Double, val sharpness: Double) : DetailedQualityResult()
    data class Invalid(val reason: String) : DetailedQualityResult()
}

enum class QualityResult {
    Good, LowLight, TooBright, Blurry, TooFar
}

object BrightnessAnalyzer {
    fun analyze(imageProxy: ImageProxy): Double {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        var total = 0.0
        for (byte in data) {
            total += (byte.toInt() and 0xFF)
        }
        return total / data.size
    }
}

object BlurDetectionHelper {
    fun isBlurry(imageProxy: ImageProxy): Boolean {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        var diffSum = 0L
        val sampleStep = 20 // Increase step for performance
        var samples = 0

        for (y in 0 until height - sampleStep step sampleStep) {
            for (x in 0 until width - sampleStep step sampleStep) {
                val current = buffer.get(y * rowStride + x * pixelStride).toInt() and 0xFF
                val nextX = buffer.get(y * rowStride + (x + 1) * pixelStride).toInt() and 0xFF
                val nextY = buffer.get((y + 1) * rowStride + x * pixelStride).toInt() and 0xFF
                
                diffSum += Math.abs(current - nextX)
                diffSum += Math.abs(current - nextY)
                samples += 2
            }
        }
        
        val averageDiff = if (samples > 0) diffSum.toDouble() / samples else 0.0
        return averageDiff < 3.0 // Threshold for blur
    }
}

fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer 
    val uBuffer = planes[1].buffer 
    val vBuffer = planes[2].buffer 

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
