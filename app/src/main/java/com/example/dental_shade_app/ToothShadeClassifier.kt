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

data class Prediction(val label: String, val confidence: Float)

class ToothShadeClassifier(context: Context) {

    private var interpreter: Interpreter? = null
    private val labels = listOf("A1", "A2", "B1", "B2", "C1")
    private val inputImageSize = 224

    init {
        try {
            val model = FileUtil.loadMappedFile(context, "shade_model.tflite")
            val options = Interpreter.Options().apply {
                setUseNNAPI(true)
                setNumThreads(4)
            }
            interpreter = Interpreter(model, options)
        } catch (e: Exception) {
            Log.e("ShadeClassifier", "Error initializing TFLite interpreter", e)
        }
    }

    fun classify(bitmap: Bitmap): List<Prediction> {
        val interpreter = interpreter ?: return emptyList()

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputImageSize, inputImageSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()

        val processedImage = imageProcessor.process(tensorImage)

        val outputBuffer = Array(1) { FloatArray(labels.size) }
        interpreter.run(processedImage.buffer, outputBuffer)

        val result = outputBuffer[0]
        val predictions = mutableListOf<Prediction>()
        for (i in result.indices) {
            predictions.add(Prediction(labels[i], result[i]))
        }

        return predictions.sortedByDescending { it.confidence }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
