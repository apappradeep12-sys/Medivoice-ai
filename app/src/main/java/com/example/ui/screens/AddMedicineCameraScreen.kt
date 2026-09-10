package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MedicineCategory
import com.example.data.model.MedicineEntity
import com.example.service.MedicineOcrScanner
import com.example.service.OcrScanResult
import com.example.service.SampleMedicineScan
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineCameraScreen(
    scanResult: OcrScanResult?,
    isScanning: Boolean,
    capturedBitmap: Bitmap?,
    onScanImage: (Bitmap?) -> Unit,
    onScanPreset: (SampleMedicineScan) -> Unit,
    onClearScan: () -> Unit,
    onSaveMedicine: (MedicineEntity) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Camera Scanner, 1: Manual Form

    // Form fields
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val defaultEnd = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 7)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tablet") }
    var dosage by remember { mutableStateOf("1 tablet (500mg)") }
    var frequency by remember { mutableStateOf("Once Daily") }
    var reminderTime by remember { mutableStateOf("08:00 AM") }
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(defaultEnd) }
    var instructions by remember { mutableStateOf("Take after meal with warm water") }
    var formError by remember { mutableStateOf<String?>(null) }
    var hasOcrAutofilled by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onScanImage(bitmap)
        }
    }

    // React to OCR scan result
    LaunchedEffect(scanResult) {
        if (scanResult != null) {
            name = scanResult.medicineName
            category = scanResult.category
            dosage = scanResult.dosage
            frequency = scanResult.frequency
            reminderTime = scanResult.reminderTime
            startDate = scanResult.startDate
            endDate = scanResult.endDate
            instructions = scanResult.instructions
            hasOcrAutofilled = true
            selectedTab = 0
        }
    }

    val categories = MedicineCategory.entries.map { it.displayName }
    val frequencies = listOf("Once Daily", "Twice Daily", "Thrice Daily", "Every 8 Hours", "As Needed", "Once Weekly")
    val commonTimes = listOf("07:00 AM", "08:00 AM", "01:00 PM", "02:00 PM", "08:00 PM", "09:30 PM")

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("add_medicine_camera_screen")
    ) {
        Text(
            text = "Add Prescription Medicine",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Use Camera OCR to auto-fill strip details or enter manually",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera Scanner")
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manual Form")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab 0: Camera Scanner & OCR Presets
        if (selectedTab == 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("camera_scanner_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Optical Character Recognition (OCR)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (scanResult != null || capturedBitmap != null) {
                            TextButton(
                                onClick = {
                                    onClearScan()
                                    hasOcrAutofilled = false
                                }
                            ) {
                                Text("Clear")
                            }
                        }
                    }

                    Text(
                        text = "Point camera at medicine strip, blister pack, or syrup bottle to automatically extract details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Captured image preview / scanning HUD
                    if (capturedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, TealPrimary, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = capturedBitmap.asImageBitmap(),
                                contentDescription = "Captured Medicine Strip",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Scanner Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch() },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("open_camera_button")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Camera", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                // Simulate high-res image upload scan
                                onScanPreset(MedicineOcrScanner.samplePresets.first())
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("upload_image_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Image")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Instant Sample Medicine Presets for testing OCR
                    Text(
                        text = "Test OCR with sample medicine strips & bottles:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(MedicineOcrScanner.samplePresets) { preset ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .clickable { onScanPreset(preset) }
                                    .testTag("preset_${preset.title.lowercase().replace(" ", "_")}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = TealPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Scanning indicator
                    if (isScanning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE0F7FA))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Processing image with Python OCR model...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TealDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // OCR Result Banner
                    if (scanResult != null && !isScanning) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F5E9))
                                .border(1.dp, GreenSuccess, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .testTag("ocr_success_banner")
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "OCR Recognized: ${scanResult.medicineName} (${(scanResult.confidence * 100).toInt()}% confidence)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenSuccess
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Form auto-filled below! Review and confirm or edit manually before saving.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1B5E20)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = scanResult.detectedRawText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color(0xFF37474F)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Add Medicine Form (Editable & Auto-filled)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_medicine_form_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prescription Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (hasOcrAutofilled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "✨ OCR Auto-filled",
                                style = MaterialTheme.typography.labelSmall,
                                color = GreenSuccess,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 1. Medicine Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Name *") },
                    placeholder = { Text("e.g. Amoxicillin 500mg, Paracetamol") },
                    leadingIcon = {
                        Icon(Icons.Default.Medication, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medicine_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // 2. Category Chips
                Column {
                    Text(
                        text = "Medicine Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("category_chip_$cat")
                            )
                        }
                    }
                }

                // 3. Dosage & Frequency Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Dosage *") },
                        placeholder = { Text("e.g. 500mg, 1 tablet, 10ml") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("medicine_dosage_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frequency") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("medicine_frequency_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 4. Reminder Time (Normal AM/PM)
                Column {
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        label = { Text("Reminder Time (Normal AM/PM) *") },
                        placeholder = { Text("e.g. 08:00 AM, 02:00 PM") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_time_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    // Quick Time Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(commonTimes) { t ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (reminderTime == t) TealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { reminderTime = t }
                            ) {
                                Text(
                                    text = t,
                                    fontSize = 11.sp,
                                    color = if (reminderTime == t) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Start Date & End Date Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_date_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("end_date_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 6. Instructions / Meal Advice
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Meal Instructions / Notes") },
                    placeholder = { Text("e.g. Take after lunch with warm water") },
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("instructions_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (formError != null) {
                    Text(
                        text = formError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            formError = "Please enter medicine name."
                            return@Button
                        }
                        if (dosage.isBlank()) {
                            formError = "Please specify dosage strength."
                            return@Button
                        }
                        if (reminderTime.isBlank()) {
                            formError = "Please specify reminder time."
                            return@Button
                        }

                        val newMed = MedicineEntity(
                            name = name.trim(),
                            category = category,
                            dosage = dosage.trim(),
                            frequency = frequency.trim(),
                            reminderTime = reminderTime.trim(),
                            startDate = startDate.trim(),
                            endDate = endDate.trim(),
                            instructions = instructions.trim()
                        )
                        onSaveMedicine(newMed)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_medicine_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm & Save Medicine", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel")
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
