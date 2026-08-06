package com.example.dental_shade_app

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

data class Prediction(val label: String, val confidence: Float, val deltaE: Float = 0f, val hex: String = "#F0E6D2")

data class VitaTarget(val label: String, val L: Float, val a: Float, val b: Float, val hex: String)

class ToothShadeClassifier(context: Context) {

    private var interpreter: Interpreter? = null
    val labels = listOf("A1", "A2", "A3", "A3.5", "A4", "B1", "B2", "B3", "B4", "C1", "C2", "C3", "C4", "D2", "D3", "D4")
    private val inputImageSize = 224

    private val vitaTargets = listOf(
        VitaTarget("A1", 82.5f, 0.8f, 14.2f, "#F6F2E5"),
        VitaTarget("A2", 78.4f, 1.4f, 16.8f, "#F0E6D2"),
        VitaTarget("A3", 74.2f, 2.1f, 19.5f, "#E6D7BD"),
        VitaTarget("A3.5", 70.1f, 2.8f, 21.8f, "#DAC4A4"),
        VitaTarget("A4", 65.8f, 3.5f, 23.2f, "#CBAF88"),
        VitaTarget("B1", 85.0f, -0.5f, 12.0f, "#F9F6EA"),
        VitaTarget("B2", 80.2f, 0.2f, 15.5f, "#F2EAD8"),
        VitaTarget("B3", 73.8f, 1.2f, 20.4f, "#E5D5B8"),
        VitaTarget("B4", 68.5f, 2.0f, 23.0f, "#D6C09B"),
        VitaTarget("C1", 77.0f, -0.2f, 11.8f, "#ECE7DB"),
        VitaTarget("C2", 72.5f, 0.5f, 14.8f, "#DFD5C4"),
        VitaTarget("C3", 67.2f, 1.1f, 17.2f, "#CEBFAB"),
        VitaTarget("C4", 62.0f, 1.8f, 19.0f, "#BEAB94"),
        VitaTarget("D2", 76.2f, 0.8f, 13.5f, "#ECE4D4"),
        VitaTarget("D3", 71.0f, 1.6f, 16.5f, "#DDD0BC"),
        VitaTarget("D4", 66.5f, 2.2f, 18.2f, "#CCBCA5")
    )

    init {
        try {
            val model = FileUtil.loadMappedFile(context, "shade_model.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(model, options)
        } catch (e: Throwable) {
            Log.w("ShadeClassifier", "TFLite model init warning, utilizing CIELAB classifier engine", e)
        }
    }

    fun classify(bitmap: Bitmap): List<Prediction> {
        // Extract central tooth ROI pixels for robust CIELAB color matching
        val sampledLab = extractSampledLab(bitmap)
        val labPredictions = mutableListOf<Prediction>()

        var totalWeight = 0f
        val rawWeights = mutableListOf<Float>()

        for (target in vitaTargets) {
            val dL = sampledLab[0] - target.L
            val da = sampledLab[1] - target.a
            val db = sampledLab[2] - target.b
            val deltaE = kotlin.math.sqrt(dL * dL + da * da + db * db)
            
            val weight = kotlin.math.exp(-deltaE / 3.5f)
            rawWeights.add(weight)
            totalWeight += weight
        }

        for (i in vitaTargets.indices) {
            val target = vitaTargets[i]
            val dL = sampledLab[0] - target.L
            val da = sampledLab[1] - target.a
            val db = sampledLab[2] - target.b
            val deltaE = kotlin.math.sqrt(dL * dL + da * da + db * db)
            val conf = if (totalWeight > 0f) rawWeights[i] / totalWeight else 1f / vitaTargets.size
            labPredictions.add(Prediction(target.label, conf, deltaE, target.hex))
        }

        val interpreter = interpreter
        if (interpreter == null) {
            return labPredictions.sortedByDescending { it.confidence }
        }

        try {
            val inputShape = interpreter.getInputTensor(0).shape()
            val modelWidth = if (inputShape.size >= 3) inputShape[1] else inputImageSize
            val modelHeight = if (inputShape.size >= 3) inputShape[2] else inputImageSize

            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)

            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(modelHeight, modelWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()

            val processedImage = imageProcessor.process(tensorImage)

            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            val numClasses = if (outputShape.size >= 2) outputShape[1] else labels.size

            val outputBuffer = Array(1) { FloatArray(numClasses) }
            interpreter.run(processedImage.buffer, outputBuffer)

            val rawOutputs = outputBuffer[0]
            
            // Determine if outputs are raw logits (contain negative numbers or sum != 1) and apply softmax
            val isLogits = rawOutputs.any { it < 0f } || kotlin.math.abs(rawOutputs.sum() - 1.0f) > 0.15f
            val probs = if (isLogits) {
                val maxLogit = rawOutputs.maxOrNull() ?: 0f
                val exps = rawOutputs.map { kotlin.math.exp(it - maxLogit) }
                val sumExp = exps.sum().coerceAtLeast(1e-6f)
                exps.map { it / sumExp }.toFloatArray()
            } else {
                val sum = rawOutputs.sum().coerceAtLeast(1e-6f)
                rawOutputs.map { it / sum }.toFloatArray()
            }

            val predictions = mutableListOf<Prediction>()
            for (i in probs.indices) {
                val label = labels.getOrElse(i) { "A2" }
                val target = vitaTargets.find { it.label == label } ?: vitaTargets[1]
                val dL = sampledLab[0] - target.L
                val da = sampledLab[1] - target.a
                val db = sampledLab[2] - target.b
                val deltaE = kotlin.math.sqrt(dL * dL + da * da + db * db)
                
                // Hybrid score: combine model probability with CIELAB color closeness
                val labWeight = rawWeights.getOrElse(i) { 0.05f }
                val hybridConf = (probs[i] * 0.70f) + (labWeight / (totalWeight.coerceAtLeast(1e-6f)) * 0.30f)
                predictions.add(Prediction(label, hybridConf, deltaE, target.hex))
            }

            return predictions.sortedByDescending { it.confidence }
        } catch (e: Exception) {
            Log.w("ToothShadeClassifier", "TFLite runtime exception, utilizing CIELAB predictions", e)
            return labPredictions.sortedByDescending { it.confidence }
        }
    }

    private fun extractSampledLab(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height

        val startX = (width * 0.35).toInt()
        val startY = (height * 0.35).toInt()
        val roiWidth = (width * 0.30).toInt().coerceAtLeast(1)
        val roiHeight = (height * 0.30).toInt().coerceAtLeast(1)

        val pixels = IntArray(roiWidth * roiHeight)
        bitmap.getPixels(pixels, 0, roiWidth, startX, startY, roiWidth, roiHeight)

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var count = 0

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val brightness = (r + g + b) / 3
            if (brightness in 90..245) {
                totalR += r
                totalG += g
                totalB += b
                count++
            }
        }

        if (count == 0) return floatArrayOf(78.4f, 1.4f, 16.8f) // Default A2 LAB

        val avgR = (totalR / count).toFloat()
        val avgG = (totalG / count).toFloat()
        val avgB = (totalB / count).toFloat()

        return rgbToLab(avgR, avgG, avgB)
    }

    private fun rgbToLab(r: Float, g: Float, b: Float): FloatArray {
        var rN = r / 255f
        var gN = g / 255f
        var bN = b / 255f

        rN = if (rN > 0.04045f) Math.pow(((rN + 0.055) / 1.055), 2.4).toFloat() else rN / 12.92f
        gN = if (gN > 0.04045f) Math.pow(((gN + 0.055) / 1.055), 2.4).toFloat() else gN / 12.92f
        bN = if (bN > 0.04045f) Math.pow(((bN + 0.055) / 1.055), 2.4).toFloat() else bN / 12.92f

        var x = (rN * 0.4124f + gN * 0.3576f + bN * 0.1805f) / 0.95047f
        var y = (rN * 0.2126f + gN * 0.7152f + bN * 0.0722f) / 1.00000f
        var z = (rN * 0.0193f + gN * 0.1192f + bN * 0.9505f) / 1.08883f

        x = if (x > 0.008856f) Math.pow(x.toDouble(), 1.0 / 3.0).toFloat() else (7.787f * x) + (16f / 116f)
        y = if (y > 0.008856f) Math.pow(y.toDouble(), 1.0 / 3.0).toFloat() else (7.787f * y) + (16f / 116f)
        z = if (z > 0.008856f) Math.pow(z.toDouble(), 1.0 / 3.0).toFloat() else (7.787f * z) + (16f / 116f)

        val L = (116f * y) - 16f
        val a = 500f * (x - y)
        val bVal = 200f * (y - z)

        return floatArrayOf(L, a, bVal)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
