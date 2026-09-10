package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AssistantActionType
import com.example.ui.screens.AddMedicineCameraScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DosageHistoryScreen
import com.example.ui.screens.MedicineListScreen
import com.example.ui.screens.SymptomCheckerScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.MediVoiceViewModel

enum class NavDestination(val label: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Home", Icons.Default.Dashboard, "nav_home"),
    MEDICINES("Medicines", Icons.Default.Medication, "nav_medicines"),
    SCANNER("Scan/Add", Icons.Default.CameraAlt, "nav_scan"),
    HISTORY("History", Icons.Default.History, "nav_history"),
    VOICE("Voice AI", Icons.Default.Mic, "nav_voice"),
    SYMPTOMS("Symptoms", Icons.Default.HealthAndSafety, "nav_symptoms")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MediVoiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MediVoiceApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediVoiceApp(viewModel: MediVoiceViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isOtpStep by viewModel.isOtpStep.collectAsState()
    val generatedOtp by viewModel.generatedOtp.collectAsState()
    val pendingEmail by viewModel.pendingAuthEmail.collectAsState()
    val authError by viewModel.authError.collectAsState()

    val liveTime by viewModel.liveTime.collectAsState()
    val liveDate by viewModel.liveDate.collectAsState()

    val medicines by viewModel.allMedicines.collectAsState()
    val todayLogs by viewModel.todayLogs.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val activeAlertMedicine by viewModel.activeAlertMedicine.collectAsState()

    val isScanning by viewModel.isScanning.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()

    val assistantMessages by viewModel.assistantMessages.collectAsState()

    var currentTab by remember { mutableStateOf(NavDestination.DASHBOARD) }

    // If user is not logged in, show AuthScreen (Email, Password, OTP verification)
    if (currentUser == null) {
        AuthScreen(
            isOtpStep = isOtpStep,
            generatedOtp = generatedOtp,
            pendingEmail = pendingEmail,
            errorMessage = authError,
            onRequestOtp = { email, password ->
                viewModel.requestOtp(email, password)
            },
            onVerifyOtp = { otp ->
                viewModel.verifyOtp(otp)
            },
            onQuickDemo = {
                viewModel.quickDemoLogin()
            }
        )
        return
    }

    // Main App Layout
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MediVoice AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavDestination.entries.forEach { dest ->
                    val selected = currentTab == dest
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = dest },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.label
                            )
                        },
                        label = {
                            Text(
                                text = dest.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealDark,
                            indicatorColor = Color(0xFFE0F2F1)
                        ),
                        modifier = Modifier.testTag(dest.tag)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavDestination.DASHBOARD -> {
                    DashboardScreen(
                        liveTime = liveTime,
                        liveDate = liveDate,
                        userName = currentUser?.fullName ?: "Patient",
                        medicines = medicines,
                        todayLogs = todayLogs,
                        activeAlertMedicine = activeAlertMedicine,
                        onTaken = { med -> viewModel.markDoseTaken(med) },
                        onSkip = { med -> viewModel.markDoseSkipped(med) },
                        onSnooze = { med -> viewModel.snoozeDose(med) },
                        onTestAlert = { med -> viewModel.triggerReminderAlert(med) },
                        onDismissAlert = { viewModel.dismissAlert() },
                        onNavigateScan = { currentTab = NavDestination.SCANNER },
                        onNavigateVoice = { currentTab = NavDestination.VOICE },
                        onNavigateAdd = { currentTab = NavDestination.SCANNER },
                        onNavigateSymptoms = { currentTab = NavDestination.SYMPTOMS },
                        onNavigateList = { currentTab = NavDestination.MEDICINES }
                    )
                }

                NavDestination.MEDICINES -> {
                    MedicineListScreen(
                        medicines = medicines,
                        onAddMedicine = { currentTab = NavDestination.SCANNER },
                        onDeleteMedicine = { med -> viewModel.deleteMedicine(med) },
                        onUpdateMedicine = { med -> viewModel.updateMedicine(med) },
                        onTestVoiceAlert = { med -> viewModel.triggerReminderAlert(med) }
                    )
                }

                NavDestination.SCANNER -> {
                    AddMedicineCameraScreen(
                        scanResult = scanResult,
                        isScanning = isScanning,
                        capturedBitmap = capturedBitmap,
                        onScanImage = { bitmap -> viewModel.scanImage(bitmap) },
                        onScanPreset = { preset -> viewModel.scanSamplePreset(preset) },
                        onClearScan = { viewModel.clearScan() },
                        onSaveMedicine = { newMed ->
                            viewModel.addMedicine(newMed) {
                                currentTab = NavDestination.MEDICINES
                            }
                        },
                        onCancel = {
                            viewModel.clearScan()
                            currentTab = NavDestination.DASHBOARD
                        }
                    )
                }

                NavDestination.HISTORY -> {
                    DosageHistoryScreen(logs = allLogs)
                }

                NavDestination.VOICE -> {
                    VoiceAssistantScreen(
                        messages = assistantMessages,
                        onSendQuery = { q -> viewModel.sendVoiceQuery(q) },
                        onNavigateAction = { action ->
                            when (action) {
                                AssistantActionType.NAVIGATE_ADD -> currentTab = NavDestination.SCANNER
                                AssistantActionType.NAVIGATE_LIST -> currentTab = NavDestination.MEDICINES
                                AssistantActionType.NAVIGATE_HISTORY -> currentTab = NavDestination.HISTORY
                                AssistantActionType.NAVIGATE_SYMPTOMS -> currentTab = NavDestination.SYMPTOMS
                                AssistantActionType.MARK_TAKEN -> {
                                    val firstMed = medicines.firstOrNull()
                                    if (firstMed != null) viewModel.markDoseTaken(firstMed)
                                }
                            }
                        },
                        onSpeakText = { text -> viewModel.reminderAlertManager.speak(text) }
                    )
                }

                NavDestination.SYMPTOMS -> {
                    SymptomCheckerScreen(
                        onAddSuggestedMedicine = { med ->
                            viewModel.addMedicine(med) {
                                currentTab = NavDestination.MEDICINES
                            }
                        }
                    )
                }
            }
        }
    }
}
