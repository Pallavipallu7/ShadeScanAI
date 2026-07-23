package com.example.shadescanai

import com.example.dental_shade_app.Prediction
import com.example.dental_shade_app.QualityResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ToothShadeClassifierUnitTest {

    @Test
    fun testPredictionModelCreation() {
        val prediction = Prediction("A2", 0.94f)
        assertNotNull(prediction)
        assertEquals("A2", prediction.label)
        assertEquals(0.94f, prediction.confidence)
    }

    @Test
    fun testQualityResultsEnum() {
        val good = QualityResult.Good
        val blurry = QualityResult.Blurry
        val lowLight = QualityResult.LowLight
        
        assertEquals("Good", good.name)
        assertEquals("Blurry", blurry.name)
        assertEquals("LowLight", lowLight.name)
    }
}
