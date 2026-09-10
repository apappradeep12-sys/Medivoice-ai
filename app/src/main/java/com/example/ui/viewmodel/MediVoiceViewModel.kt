package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.DoseLogEntity
import com.example.data.model.MedicineEntity
import com.example.data.model.UserProfileEntity
import com.example.data.repository.MedicineRepository
import com.example.service.AssistantActionType
import com.example.service.AssistantMessage
import com.example.service.AssistantSender
import com.example.service.MedicineOcrScanner
import com.example.service.OcrScanResult
import com.example.service.ReminderAlertManager
import com.example.service.SampleMedicineScan
import com.example.service.VoiceAssistantProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediVoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = MedicineRepository(
        database.medicineDao(),
        database.doseLogDao(),
        database.userProfileDao()
    )

    val reminderAlertManager = ReminderAlertManager(application)

    val currentUser: StateFlow<UserProfileEntity?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allMedicines: StateFlow<List<MedicineEntity>> = repository.allMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMedicines: StateFlow<List<MedicineEntity>> = repository.activeMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<DoseLogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _todayLogs = MutableStateFlow<List<DoseLogEntity>>(emptyList())
    val todayLogs: StateFlow<List<DoseLogEntity>> = _todayLogs.asStateFlow()

    // Live Digital Clock State
    private val _liveTime = MutableStateFlow("")
    val liveTime: StateFlow<String> = _liveTime.asStateFlow()

    private val _liveDate = MutableStateFlow("")
    val liveDate: StateFlow<String> = _liveDate.asStateFlow()

    // Active Reminder Alert state (Banner / Notification overlay)
    private val _activeAlertMedicine = MutableStateFlow<MedicineEntity?>(null)
    val activeAlertMedicine: StateFlow<MedicineEntity?> = _activeAlertMedicine.asStateFlow()

    // Camera & OCR Scanner State
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResult = MutableStateFlow<OcrScanResult?>(null)
    val scanResult: StateFlow<OcrScanResult?> = _scanResult.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    // Voice Assistant Conversation State
    private val _assistantMessages = MutableStateFlow<List<AssistantMessage>>(emptyList())
    val assistantMessages: StateFlow<List<AssistantMessage>> = _assistantMessages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // Auth OTP State
    private val _generatedOtp = MutableStateFlow("482910")
    val generatedOtp: StateFlow<String> = _generatedOtp.asStateFlow()

    private val _pendingAuthEmail = MutableStateFlow("")
    val pendingAuthEmail: StateFlow<String> = _pendingAuthEmail.asStateFlow()

    private val _isOtpStep = MutableStateFlow(false)
    val isOtpStep: StateFlow<Boolean> = _isOtpStep.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        startLiveClock()
        refreshTodayLogs()
        initAssistantGreeting()
    }

    private fun startLiveClock() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val now = Date()
                _liveTime.value = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(now)
                _liveDate.value = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(now)

                // Check for exact minute scheduled reminders
                checkScheduledReminders(now)

                delay(1000)
            }
        }
    }

    private var lastCheckedMinute = ""
    private fun checkScheduledReminders(now: Date) {
        val currentMinute = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
        if (currentMinute != lastCheckedMinute) {
            lastCheckedMinute = currentMinute
            val currentMeds = activeMedicines.value
            val match = currentMeds.firstOrNull { it.reminderTime.equals(currentMinute, ignoreCase = true) }
            if (match != null && _activeAlertMedicine.value == null) {
                triggerReminderAlert(match)
            }
        }
    }

    fun triggerReminderAlert(medicine: MedicineEntity) {
        _activeAlertMedicine.value = medicine
        reminderAlertManager.playReminderSound()
        reminderAlertManager.speakVoiceAlert(medicine.name, medicine.dosage, medicine.instructions)
    }

    fun dismissAlert() {
        _activeAlertMedicine.value = null
        reminderAlertManager.stopSpeaking()
    }

    fun refreshTodayLogs() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            repository.getLogsForDate(today).collect { logs ->
                _todayLogs.value = logs
            }
        }
    }

    // Dose actions
    fun markDoseTaken(medicine: MedicineEntity) {
        viewModelScope.launch {
            repository.logDoseAction(medicine, "TAKEN")
            dismissAlert()
            refreshTodayLogs()
            reminderAlertManager.speak("Great! Marked ${medicine.name} as taken.")
        }
    }

    fun markDoseSkipped(medicine: MedicineEntity) {
        viewModelScope.launch {
            repository.logDoseAction(medicine, "SKIPPED")
            dismissAlert()
            refreshTodayLogs()
            reminderAlertManager.speak("Noted. Skipped dose for ${medicine.name}.")
        }
    }

    fun snoozeDose(medicine: MedicineEntity, minutes: Int = 15) {
        viewModelScope.launch {
            repository.logDoseAction(medicine, "SNOOZED")
            dismissAlert()
            refreshTodayLogs()
            reminderAlertManager.speak("Snoozed reminder for ${medicine.name} by $minutes minutes.")
        }
    }

    // Medicine CRUD
    fun addMedicine(medicine: MedicineEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertMedicine(medicine)
            clearScan()
            onComplete()
        }
    }

    fun updateMedicine(medicine: MedicineEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateMedicine(medicine)
            onComplete()
        }
    }

    fun deleteMedicine(medicine: MedicineEntity) {
        viewModelScope.launch {
            repository.deleteMedicine(medicine)
        }
    }

    fun deleteMedicineById(id: Long) {
        viewModelScope.launch {
            repository.deleteMedicineById(id)
        }
    }

    // Camera & OCR Scanner
    fun scanImage(bitmap: Bitmap?) {
        _capturedBitmap.value = bitmap
        _isScanning.value = true
        viewModelScope.launch {
            val result = MedicineOcrScanner.analyzeImageBitmap(bitmap)
            _scanResult.value = result
            _isScanning.value = false
            reminderAlertManager.speak("Medicine detected: ${result.medicineName}, dosage ${result.dosage}.")
        }
    }

    fun scanSamplePreset(preset: SampleMedicineScan) {
        _isScanning.value = true
        viewModelScope.launch {
            delay(800)
            val result = MedicineOcrScanner.fromPreset(preset)
            _scanResult.value = result
            _isScanning.value = false
            reminderAlertManager.speak("Preset scanned: ${result.medicineName}, ${result.dosage}.")
        }
    }

    fun clearScan() {
        _scanResult.value = null
        _capturedBitmap.value = null
        _isScanning.value = false
    }

    // Auth flows
    fun requestOtp(email: String, password: String): Boolean {
        _authError.value = null
        if (email.isBlank() || !email.contains("@")) {
            _authError.value = "Please enter a valid email address."
            return false
        }
        if (password.length < 6) {
            _authError.value = "Password must be at least 6 characters."
            return false
        }
        _pendingAuthEmail.value = email.trim()
        // Generate random 6-digit OTP
        val otp = (100000..999999).random().toString()
        _generatedOtp.value = otp
        _isOtpStep.value = true
        return true
    }

    fun verifyOtp(enteredOtp: String): Boolean {
        if (enteredOtp.trim() == _generatedOtp.value) {
            viewModelScope.launch {
                val email = _pendingAuthEmail.value.ifBlank { "user@medivoice.ai" }
                val name = email.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
                repository.saveUser(email, name)
                _isOtpStep.value = false
                _authError.value = null
            }
            return true
        } else {
            _authError.value = "Invalid OTP code. Please try again."
            return false
        }
    }

    fun quickDemoLogin() {
        viewModelScope.launch {
            repository.saveUser("alex.mercer@medivoice.ai", "Alex Mercer")
            _isOtpStep.value = false
            _authError.value = null
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isOtpStep.value = false
        }
    }

    // Voice Assistant conversation
    private fun initAssistantGreeting() {
        val greeting = AssistantMessage(
            sender = AssistantSender.ASSISTANT,
            text = "Hello! I am your MediVoice AI Assistant. I can track your reminders, add medicines, scan labels, and answer your medication questions. How can I help you today?",
            timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        )
        _assistantMessages.value = listOf(greeting)
    }

    fun sendVoiceQuery(query: String) {
        if (query.isBlank()) return

        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val userMsg = AssistantMessage(
            sender = AssistantSender.USER,
            text = query,
            timestamp = nowTime
        )
        val updated = _assistantMessages.value.toMutableList()
        updated.add(userMsg)
        _assistantMessages.value = updated

        // Process response
        val response = VoiceAssistantProcessor.processQuery(
            query = query,
            medicines = allMedicines.value,
            todayLogs = todayLogs.value
        )

        val assistantMsg = AssistantMessage(
            sender = AssistantSender.ASSISTANT,
            text = response.displayText,
            timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            actionType = response.actionType,
            actionPayload = response.actionPayload
        )
        updated.add(assistantMsg)
        _assistantMessages.value = updated

        // Speak response aloud!
        reminderAlertManager.speak(response.speech)
    }

    override fun onCleared() {
        super.onCleared()
        reminderAlertManager.release()
    }
}
