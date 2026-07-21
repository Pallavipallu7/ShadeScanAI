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
