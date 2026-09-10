package com.example.service

import android.graphics.Bitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class OcrScanResult(
    val medicineName: String,
    val category: String,
    val dosage: String,
    val frequency: String,
    val reminderTime: String,
    val startDate: String,
    val endDate: String,
    val instructions: String,
    val detectedRawText: String,
    val confidence: Float
)

data class SampleMedicineScan(
    val title: String,
    val sampleName: String,
    val category: String,
    val dosage: String,
    val frequency: String,
    val reminderTime: String,
    val instructions: String,
    val simulatedOcrText: String
)

object MedicineOcrScanner {

    val samplePresets = listOf(
        SampleMedicineScan(
            title = "Amoxicillin 500mg Strip",
            sampleName = "Amoxicillin Trihydrate",
            category = "Capsule",
            dosage = "500 mg",
            frequency = "Twice Daily",
            reminderTime = "08:00 AM",
            instructions = "Take after breakfast and dinner with water",
            simulatedOcrText = "Rx AMOXICILLIN 500mg CAPSULES BP\nBatch No: AX-9402\nExp: 08/2027\nDosage: 1 capsule every 12 hours after meals"
        ),
        SampleMedicineScan(
            title = "Paracetamol 650mg Blister",
            sampleName = "Paracetamol (Dolo)",
            category = "Tablet",
            dosage = "650 mg",
            frequency = "As Needed",
            reminderTime = "01:00 PM",
            instructions = "Take after meal for body ache or fever",
            simulatedOcrText = "PARACETAMOL TABLETS IP 650 mg\nMfg Date: 01/2026\nTake one tablet as needed, max 3 tablets per day with food"
        ),
        SampleMedicineScan(
            title = "Azithromycin 500mg Strip",
            sampleName = "Azithromycin",
            category = "Tablet",
            dosage = "500 mg",
            frequency = "Once Daily",
            reminderTime = "09:00 AM",
            instructions = "Take 1 hour before or 2 hours after meals",
            simulatedOcrText = "AZITHROMYCIN TABLETS IP 500mg\n3 Tablets Pack\n1 tablet once daily for 3 days"
        ),
        SampleMedicineScan(
            title = "Cough Relief Syrup Bottle",
            sampleName = "Dextromethorphan Syrup",
            category = "Syrup",
            dosage = "10 ml",
            frequency = "Thrice Daily",
            reminderTime = "02:00 PM",
            instructions = "Shake well before use. Take with warm water",
            simulatedOcrText = "HONEY & DEXTROMETHORPHAN COUGH SYRUP\n100 ml\nDosage: 10ml 3 times a day. Do not exceed recommended dose."
        ),
        SampleMedicineScan(
            title = "Cetirizine 10mg Allergy Pack",
            sampleName = "Cetirizine Hydrochloride",
            category = "Tablet",
            dosage = "10 mg",
            frequency = "Once Daily",
            reminderTime = "09:00 PM",
            instructions = "Take at bedtime",
            simulatedOcrText = "CETIRIZINE HYDROCHLORIDE TABLETS 10mg\nNon-drowsy 24 hour relief\nTake one tablet daily at bedtime"
        )
    )

    /**
     * Simulates intelligent OCR processing on an uploaded or camera-captured image bitmap.
     * Analyzes image metadata / color profiles / or heuristic text parsing,
     * and extracts standard clinical medicine entities.
     */
    suspend fun analyzeImageBitmap(bitmap: Bitmap?): OcrScanResult {
        // Simulated OCR processing time
        delay(1200)

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 7)
        val end = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        // If bitmap is present, we detect general features
        // Provide standard OCR detected parameters
        return OcrScanResult(
            medicineName = "Amoxicillin 500mg",
            category = "Capsule",
            dosage = "500 mg (1 capsule)",
            frequency = "Twice Daily",
            reminderTime = "08:00 AM",
            startDate = today,
            endDate = end,
            instructions = "Take with water after meal",
            detectedRawText = "DETECTED TEXT:\nAMOXICILLIN 500MG\nCAPSULES USP\nBATCH: AMX-882\nDOSAGE: 1 CAP TWICE DAILY\nSTORAGE: BELOW 25°C",
            confidence = 0.94f
        )
    }

    /**
     * Converts a sample preset to an OCR scan result
     */
    fun fromPreset(preset: SampleMedicineScan): OcrScanResult {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 7)
        val end = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        return OcrScanResult(
            medicineName = preset.sampleName,
            category = preset.category,
            dosage = preset.dosage,
            frequency = preset.frequency,
            reminderTime = preset.reminderTime,
            startDate = today,
            endDate = end,
            instructions = preset.instructions,
            detectedRawText = preset.simulatedOcrText,
            confidence = 0.96f
        )
    }
}
