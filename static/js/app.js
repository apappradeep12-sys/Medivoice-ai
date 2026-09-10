/**
 * MediVoice AI - Medicine Reminder and Voice Assistant
 * Complete Frontend Controller & Client-Side Engine
 */

// Application State
let appState = {
    currentUser: {
        name: "Alex Mercer",
        email: "patient@medivoice.ai",
        age: 42,
        blood_group: "O+",
        emergency_contact: "+1 (555) 234-5678"
    },
    medicines: [],
    logs: [],
    dailySummary: null,
    pendingOcrMedicine: null,
    activeDueReminder: null,
    activeView: 'dashboard',
    notificationsEnabled: false,
    audioMuted: false,
    isRecordingVoice: false,
    recognition: null,
    audioContext: null
};

// ----------------- 1. VIEW NAVIGATION ----------------- //

function switchView(viewName) {
    appState.activeView = viewName;

    // Toggle active view panel
    document.querySelectorAll('.app-view').forEach(view => {
        view.classList.remove('active');
    });
    const target = document.getElementById(`view-${viewName}`);
    if (target) target.classList.add('active');

    // Toggle active navigation link
    document.querySelectorAll('#mainNavigationTabs .nav-link').forEach(link => {
        link.classList.remove('active');
    });
    const activeBtn = document.querySelector(`[onclick="switchView('${viewName}')"]`);
    if (activeBtn) activeBtn.classList.add('active');

    // Scroll to top of content on mobile
    window.scrollTo({ top: 0, behavior: 'smooth' });

    // Refresh context data when entering views
    if (viewName === 'daily-dosage') loadDailySummary();
    if (viewName === 'medicine-list') renderMedicineListTable();
    if (viewName === 'history') loadHistoryLogs();
    if (viewName === 'reminders') renderRemindersTimeline();
}

// ----------------- 2. REAL-TIME DIGITAL CLOCK & REMINDER ENGINE ----------------- //

function updateLiveClock() {
    const now = new Date();

    // 12-hour clock format (e.g. 08:00:15 AM)
    const timeStr = now.toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true
    });

    const dateStr = now.toLocaleDateString('en-US', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });

    const clockElem = document.getElementById('digitalClockDisplay');
    const dateElem = document.getElementById('currentDateDisplay');

    if (clockElem) clockElem.textContent = timeStr;
    if (dateElem) dateElem.textContent = dateStr;

    // Check for reminder matches every second
    checkScheduledReminderMatch(now);
}

// Check if current system time matches any scheduled medicine (e.g. "08:00 AM")
let lastAlertMinute = "";
function checkScheduledReminderMatch(nowDate) {
    const currentClockTime = nowDate.toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: true
    }); // e.g. "08:00 AM"

    if (currentClockTime === lastAlertMinute) return; // Prevent duplicate alerts in same minute

    const matchedMed = appState.medicines.find(m => m.reminder_time.trim().toUpperCase() === currentClockTime.toUpperCase());
    if (matchedMed) {
        lastAlertMinute = currentClockTime;
        triggerReminderAlert(matchedMed);
    }
}

// Trigger alert box, sound, speech, and browser notification
function triggerReminderAlert(med) {
    appState.activeDueReminder = med;

    // Update active prompt card
    const nameElem = document.getElementById('promptMedName');
    const detailsElem = document.getElementById('promptMedDetails');
    const instElem = document.getElementById('promptMedInstructions');

    if (nameElem) nameElem.textContent = med.name;
    if (detailsElem) detailsElem.textContent = `${med.dosage} • Scheduled: ${med.reminder_time}`;
    if (instElem) instElem.textContent = med.instructions || "Take as directed with water";

    // Play chime sound if enabled
    const soundPref = document.getElementById('prefSoundToggle');
    if (!soundPref || soundPref.checked) {
        playNotificationChime();
    }

    // Spoken voice announcement
    const speechPref = document.getElementById('prefSpeechToggle');
    if (!speechPref || speechPref.checked) {
        speakUtterance(`Reminder: It is time to take your dose of ${med.name}. ${med.instructions}`);
    }

    // System browser notification
    showBrowserNotification(
        `Medicine Reminder: ${med.name}`,
        `Time to take ${med.dosage}. Scheduled for ${med.reminder_time}.`
    );
}

// ----------------- 3. BROWSER NOTIFICATIONS & SOUND CHIME ----------------- //

function requestNotificationPermission() {
    if (!("Notification" in window)) {
        alert("This browser does not support desktop notifications.");
        return;
    }

    Notification.requestPermission().then(permission => {
        const badge = document.getElementById('notifStatusBadge');
        if (permission === "granted") {
            appState.notificationsEnabled = true;
            if (badge) {
                badge.className = "badge bg-success ms-1";
                badge.textContent = "Active";
            }
            showBrowserNotification("MediVoice AI", "Notifications are now active for your medicine reminders!");
            playNotificationChime();
        } else {
            appState.notificationsEnabled = false;
            if (badge) {
                badge.className = "badge bg-secondary ms-1";
                badge.textContent = "Muted";
            }
        }
    });
}

function showBrowserNotification(title, body) {
    if ("Notification" in window && Notification.permission === "granted") {
        try {
            new Notification(title, {
                body: body,
                icon: "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>💊</text></svg>"
            });
        } catch (e) {
            console.log("Notification trigger error:", e);
        }
    }
}

// Web Audio API pure synthesized melodic chime (zero external audio file dependencies)
function playNotificationChime() {
    try {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (!AudioContext) return;

        if (!appState.audioContext) {
            appState.audioContext = new AudioContext();
        }
        const ctx = appState.audioContext;
        if (ctx.state === 'suspended') {
            ctx.resume();
        }

        const now = ctx.currentTime;
        const notes = [523.25, 659.25, 783.99, 1046.50]; // C5, E5, G5, C6 chord

        notes.forEach((freq, idx) => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();

            osc.type = 'sine';
            osc.frequency.setValueAtTime(freq, now + (idx * 0.1));

            gain.gain.setValueAtTime(0, now + (idx * 0.1));
            gain.gain.linearRampToValueAtTime(0.2, now + (idx * 0.1) + 0.05);
            gain.gain.exponentialRampToValueAtTime(0.001, now + (idx * 0.1) + 0.5);

            osc.connect(gain);
            gain.connect(ctx.destination);

            osc.start(now + (idx * 0.1));
            osc.stop(now + (idx * 0.1) + 0.5);
        });
    } catch (e) {
        console.log("Audio chime error:", e);
    }
}

function testNotificationBeep() {
    playNotificationChime();
    speakUtterance("Notification sound and voice alert verified.");
}

function simulateActiveReminderTrigger() {
    if (appState.medicines.length > 0) {
        triggerReminderAlert(appState.medicines[0]);
    } else {
        alert("Please add a medicine first to simulate a reminder.");
    }
}

function speakCurrentReminder() {
    const medName = document.getElementById('promptMedName').textContent;
    const medDetails = document.getElementById('promptMedDetails').textContent;
    const inst = document.getElementById('promptMedInstructions').textContent;
    speakUtterance(`Reminder: Please take ${medName}. ${medDetails}. ${inst}`);
}

// ----------------- 4. TEXT-TO-SPEECH (WEB SPEECH API) ----------------- //

function speakUtterance(text) {
    if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel(); // Stop ongoing speech
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.rate = 0.95;
        utterance.pitch = 1.0;
        window.speechSynthesis.speak(utterance);
    }
}

// ----------------- 5. TAKE, SKIP, SNOOZE ACTION HANDLER ----------------- //

async function handleDoseAction(status) {
    const currentMedName = document.getElementById('promptMedName').textContent;
    const currentMed = appState.medicines.find(m => m.name === currentMedName) || appState.medicines[0];

    if (!currentMed) {
        alert("No active medicine reminder selected.");
        return;
    }

    try {
        const payload = {
            medicine_name: currentMed.name,
            status: status, // TAKEN, SKIPPED, SNOOZED
            scheduled_time: currentMed.reminder_time,
            dosage: currentMed.dosage,
            notes: status === 'SNOOZED' ? 'Snoozed for 10 minutes' : `Marked as ${status}`
        };

        const res = await fetch('/api/logs', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (data.success) {
            // Provide voice confirmation
            if (status === 'TAKEN') {
                speakUtterance(`Confirmed: ${currentMed.name} recorded as taken.`);
            } else if (status === 'SKIPPED') {
                speakUtterance(`Noted: ${currentMed.name} recorded as skipped.`);
            } else if (status === 'SNOOZED') {
                speakUtterance(`Reminder snoozed for 10 minutes.`);
            }

            // Reload summaries and tables
            await loadDailySummary();
            await loadHistoryLogs();
            alert(`${currentMed.name} successfully updated to: ${status}`);
        }
    } catch (e) {
        console.error("Failed to record dose action:", e);
        alert("Failed to record dose action.");
    }
}

// ----------------- 6. LOAD MEDICINES & DASHBOARD TIMELINE ----------------- //

async function loadMedicines() {
    try {
        const res = await fetch('/api/medicines');
        const data = await res.json();
        if (data.success) {
            appState.medicines = data.medicines;
            renderDashboardCards();
            renderMedicineListTable();
            renderRemindersTimeline();
            if (data.medicines.length > 0 && !appState.activeDueReminder) {
                // Set first medicine as active prompt default
                triggerReminderAlert(data.medicines[0]);
            }
        }
    } catch (e) {
        console.error("Error loading medicines:", e);
    }
}

function renderDashboardCards() {
    const container = document.getElementById('dashMedicineCardsRow');
    if (!container) return;

    if (appState.medicines.length === 0) {
        container.innerHTML = `
            <div class="col-12 text-center py-4">
                <p class="text-muted">No medicines added yet. Tap "+ Add Manually" or "Add with Camera" to begin.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = appState.medicines.map(m => `
        <div class="col-md-6 col-xl-4">
            <div class="clinical-card h-100 d-flex flex-column justify-content-between">
                <div>
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <span class="badge bg-teal-subtle text-teal fw-bold">${m.category}</span>
                        <span class="time-pill-badge">⏰ ${m.reminder_time}</span>
                    </div>
                    <h5 class="fw-bold mb-1">${m.name}</h5>
                    <p class="small text-muted mb-2">
                        <strong>Dosage:</strong> ${m.dosage} • ${m.frequency}
                    </p>
                    <p class="small text-secondary mb-3">
                        <i class="bi bi-info-circle me-1"></i> ${m.instructions}
                    </p>
                </div>
                <div class="d-flex gap-2 pt-2 border-top">
                    <button class="btn btn-sm btn-success flex-fill fw-semibold" onclick="logQuickDose('${m.name}', 'TAKEN', '${m.reminder_time}', '${m.dosage}')">
                        <i class="bi bi-check-lg"></i> Take
                    </button>
                    <button class="btn btn-sm btn-outline-secondary flex-fill" onclick="logQuickDose('${m.name}', 'SKIPPED', '${m.reminder_time}', '${m.dosage}')">
                        Skip
                    </button>
                    <button class="btn btn-sm btn-light border" onclick="speakUtterance('Reminder for ${m.name}: take ${m.dosage} at ${m.reminder_time}. ${m.instructions}')" title="Listen">
                        <i class="bi bi-volume-up-fill text-teal"></i>
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

async function logQuickDose(medName, status, schedTime, dosage) {
    try {
        const res = await fetch('/api/logs', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                medicine_name: medName,
                status: status,
                scheduled_time: schedTime,
                dosage: dosage,
                notes: `Quick action: ${status}`
            })
        });
        const data = await res.json();
        if (data.success) {
            speakUtterance(`${medName} marked as ${status}.`);
            await loadDailySummary();
            await loadHistoryLogs();
            alert(`${medName} recorded as ${status}!`);
        }
    } catch (e) {
        alert("Failed to update status.");
    }
}

// ----------------- 7. MEDICINE LIST TABLE VIEW ----------------- //

function renderMedicineListTable() {
    const tbody = document.getElementById('medicineListTableBody');
    if (!tbody) return;

    if (appState.medicines.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted">No medicines found in database.</td></tr>`;
        return;
    }

    tbody.innerHTML = appState.medicines.map(m => `
        <tr>
            <td class="fw-bold">${m.name}</td>
            <td><span class="badge bg-light text-dark border">${m.category}</span></td>
            <td>${m.dosage}</td>
            <td>${m.daily_dosage || m.dosage}</td>
            <td>${m.frequency}</td>
            <td class="font-monospace fw-semibold text-teal">${m.reminder_time}</td>
            <td class="small text-muted">${m.instructions}</td>
            <td class="text-end">
                <button class="btn btn-sm btn-outline-danger" onclick="deleteMedicine(${m.id})" title="Delete Medicine">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

async function deleteMedicine(medId) {
    if (!confirm("Are you sure you want to delete this medicine from your schedule?")) return;
    try {
        const res = await fetch(`/api/medicines/${medId}`, { method: 'DELETE' });
        const data = await res.json();
        if (data.success) {
            await loadMedicines();
            await loadDailySummary();
        }
    } catch (e) {
        alert("Failed to delete medicine.");
    }
}

// ----------------- 8. MANUAL ADD MEDICINE FORM ----------------- //

function setTimePreset(timeValue) {
    document.getElementById('formMedReminderTime').value = timeValue;
}

function suggestReminderTimes(frequency) {
    const timeInput = document.getElementById('formMedReminderTime');
    if (frequency === 'Once Daily') timeInput.value = '08:00 AM';
    else if (frequency === 'Twice Daily') timeInput.value = '08:00 AM, 08:00 PM';
    else if (frequency === 'Thrice Daily') timeInput.value = '08:00 AM, 01:00 PM, 08:00 PM';
    else if (frequency === 'Once Nightly') timeInput.value = '09:00 PM';
    else if (frequency === 'As Needed') timeInput.value = 'As Needed';
}

async function handleManualMedicineSubmit(event) {
    event.preventDefault();

    const name = document.getElementById('formMedName').value.trim();
    const category = document.getElementById('formMedCategory').value;
    const dosage = document.getElementById('formMedDosage').value.trim();
    const daily_dosage = document.getElementById('formMedDailyDosage').value.trim() || dosage;
    const frequency = document.getElementById('formMedFrequency').value;
    const reminder_time = document.getElementById('formMedReminderTime').value.trim();
    const instructions = document.getElementById('formMedInstructions').value.trim();
    const start_date = document.getElementById('formMedStartDate').value;
    const end_date = document.getElementById('formMedEndDate').value;

    if (!name || !dosage || !reminder_time) {
        alert("Please fill in Medicine Name, Dosage, and Reminder Time.");
        return;
    }

    try {
        const res = await fetch('/api/medicines', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name, category, dosage, daily_dosage, frequency, reminder_time, instructions, start_date, end_date
            })
        });
        const data = await res.json();
        if (data.success) {
            alert(`Medicine "${name}" saved successfully!`);
            document.getElementById('manualAddMedicineForm').reset();
            await loadMedicines();
            await loadDailySummary();
            switchView('medicine-list');
        } else {
            alert(data.error || "Could not save medicine.");
        }
    } catch (e) {
        alert("Error connecting to server.");
    }
}

// ----------------- 9. CAMERA OCR SCANNER ----------------- //

let mediaStream = null;

async function startCameraStream() {
    const video = document.getElementById('cameraVideo');
    const placeholder = document.getElementById('cameraPlaceholder');
    const captureBtn = document.getElementById('capturePhotoBtn');
    const stopBtn = document.getElementById('stopCameraBtn');
    const overlay = document.getElementById('scannerOverlay');

    try {
        mediaStream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'environment' }
        });
        video.srcObject = mediaStream;
        video.style.display = 'block';
        placeholder.style.display = 'none';
        captureBtn.style.display = 'inline-block';
        stopBtn.style.display = 'inline-block';
        overlay.style.display = 'block';
    } catch (err) {
        console.error("Camera access error:", err);
        alert("Could not access device camera. Please grant camera permission or use the 'Upload Medicine Photo' or sample buttons.");
    }
}

function stopCameraStream() {
    if (mediaStream) {
        mediaStream.getTracks().forEach(track => track.stop());
        mediaStream = null;
    }
    const video = document.getElementById('cameraVideo');
    const placeholder = document.getElementById('cameraPlaceholder');
    const captureBtn = document.getElementById('capturePhotoBtn');
    const stopBtn = document.getElementById('stopCameraBtn');
    const overlay = document.getElementById('scannerOverlay');

    if (video) video.style.display = 'none';
    if (placeholder) placeholder.style.display = 'flex';
    if (captureBtn) captureBtn.style.display = 'none';
    if (stopBtn) stopBtn.style.display = 'none';
    if (overlay) overlay.style.display = 'none';
}

function captureCameraPhoto() {
    const video = document.getElementById('cameraVideo');
    const canvas = document.getElementById('cameraCanvas');
    if (!video || !canvas) return;

    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    // Stop camera after snapshot
    stopCameraStream();

    // Trigger OCR parsing
    processOcrRecognition("Captured camera photo - Amoxicillin 500mg prescription packaging");
}

function handleImageFileUpload(event) {
    const file = event.target.files[0];
    if (!file) return;

    processOcrRecognition(file.name);
}

function simulateOcrSample(sampleName) {
    processOcrRecognition(sampleName);
}

async function processOcrRecognition(textIdentifier) {
    const spinner = document.getElementById('ocrLoadingSpinner');
    const container = document.getElementById('ocrConfirmationFormContainer');

    if (spinner) spinner.style.display = 'block';
    if (container) container.style.display = 'none';

    try {
        const res = await fetch('/api/scan-ocr', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ filename: textIdentifier, text: textIdentifier })
        });
        const data = await res.json();

        if (spinner) spinner.style.display = 'none';
        if (container) container.style.display = 'block';

        if (data.success && data.extracted) {
            const ext = data.extracted;
            appState.pendingOcrMedicine = ext;

            document.getElementById('ocrDetectedName').value = ext.name || '';
            document.getElementById('ocrDetectedCategory').value = ext.category || 'Tablet';
            document.getElementById('ocrDetectedDosage').value = ext.dosage || '';
            document.getElementById('ocrDetectedFrequency').value = ext.frequency || '';
            document.getElementById('ocrDetectedDailyDosage').value = ext.daily_dosage || ext.dosage;
            document.getElementById('ocrDetectedTime').value = ext.reminder_time || '08:00 AM';
            document.getElementById('ocrDetectedInstructions').value = ext.instructions || '';

            speakUtterance(`Detected ${ext.name}. Please review the details and confirm.`);
        }
    } catch (e) {
        if (spinner) spinner.style.display = 'none';
        if (container) container.style.display = 'block';
        alert("OCR processing failed. Please try manual entry.");
    }
}

async function confirmAndSaveOcrMedicine() {
    const name = document.getElementById('ocrDetectedName').value.trim();
    if (!name) {
        alert("Please enter a medicine name.");
        return;
    }

    const payload = {
        name: name,
        category: document.getElementById('ocrDetectedCategory').value.trim() || 'Tablet',
        dosage: document.getElementById('ocrDetectedDosage').value.trim() || '1 dose',
        frequency: document.getElementById('ocrDetectedFrequency').value.trim() || 'Once Daily',
        daily_dosage: document.getElementById('ocrDetectedDailyDosage').value.trim() || '1 dose per day',
        reminder_time: document.getElementById('ocrDetectedTime').value.trim() || '08:00 AM',
        instructions: document.getElementById('ocrDetectedInstructions').value.trim() || 'Take after meals'
    };

    try {
        const res = await fetch('/api/medicines', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (data.success) {
            alert(`Medicine "${name}" confirmed and saved to database!`);
            await loadMedicines();
            await loadDailySummary();
            switchView('medicine-list');
        }
    } catch (e) {
        alert("Failed to save detected medicine.");
    }
}

// ----------------- 10. DAILY DOSAGE TRACKING ----------------- //

async function loadDailySummary() {
    try {
        const res = await fetch('/api/daily-summary');
        const data = await res.json();
        if (data.success) {
            appState.dailySummary = data;

            // Update Header Stats
            const bar = document.getElementById('headerAdherenceBar');
            const percent = document.getElementById('headerAdherencePercent');
            const summary = document.getElementById('headerTakenSummary');

            if (bar) bar.style.width = `${data.adherence_rate}%`;
            if (percent) percent.textContent = `${data.adherence_rate}%`;
            if (summary) summary.textContent = `${data.taken} of ${data.total_scheduled} Doses Taken`;

            // Dashboard Quick Stats
            const dTotal = document.getElementById('dashTotalMeds');
            const dTaken = document.getElementById('dashTakenDoses');
            const dPending = document.getElementById('dashPendingDoses');
            const dSkipped = document.getElementById('dashSkippedDoses');

            if (dTotal) dTotal.textContent = data.total_scheduled;
            if (dTaken) dTaken.textContent = data.taken;
            if (dPending) dPending.textContent = data.pending;
            if (dSkipped) dSkipped.textContent = data.skipped;

            // Daily Dosage Page Tiles
            const dtTotal = document.getElementById('dailyTotalCount');
            const dtTaken = document.getElementById('dailyTakenCount');
            const dtPending = document.getElementById('dailyPendingCount');
            const dtSkipped = document.getElementById('dailySkippedCount');

            if (dtTotal) dtTotal.textContent = data.total_scheduled;
            if (dtTaken) dtTaken.textContent = data.taken;
            if (dtPending) dtPending.textContent = data.pending;
            if (dtSkipped) dtSkipped.textContent = data.skipped;

            renderDailyDosageTable(data.medicines);
        }
    } catch (e) {
        console.error("Failed to load daily summary:", e);
    }
}

function renderDailyDosageTable(medicines) {
    const tbody = document.getElementById('dailyDosageTableBody');
    if (!tbody) return;

    if (!medicines || medicines.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No doses scheduled for today.</td></tr>`;
        return;
    }

    tbody.innerHTML = medicines.map(m => {
        let badgeClass = "bg-warning text-dark";
        if (m.today_status === "TAKEN") badgeClass = "bg-success text-white";
        if (m.today_status === "SKIPPED") badgeClass = "bg-danger text-white";
        if (m.today_status === "SNOOZED") badgeClass = "bg-info text-dark";

        return `
            <tr>
                <td class="fw-bold">${m.name}</td>
                <td class="font-monospace fw-semibold text-teal">${m.reminder_time}</td>
                <td>${m.dosage}</td>
                <td>${m.daily_dosage || m.dosage}</td>
                <td><span class="badge ${badgeClass} px-3 py-2">${m.today_status}</span></td>
                <td>${m.action_time || '-'}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-success me-1" onclick="logQuickDose('${m.name}', 'TAKEN', '${m.reminder_time}', '${m.dosage}')">
                        ✓ Taken
                    </button>
                    <button class="btn btn-sm btn-outline-secondary" onclick="logQuickDose('${m.name}', 'SKIPPED', '${m.reminder_time}', '${m.dosage}')">
                        Skip
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

// ----------------- 11. REMINDERS TIMELINE VIEW ----------------- //

function renderRemindersTimeline() {
    const container = document.getElementById('remindersTimelineContainer');
    if (!container) return;

    if (appState.medicines.length === 0) {
        container.innerHTML = `<p class="text-muted text-center py-4">No active reminders configured.</p>`;
        return;
    }

    container.innerHTML = appState.medicines.map(m => `
        <div class="reminder-timeline-item align-items-center">
            <div class="reminder-icon-box">
                <i class="bi bi-alarm"></i>
            </div>
            <div class="flex-grow-1">
                <div class="d-flex justify-content-between align-items-center mb-1">
                    <h6 class="fw-bold mb-0">${m.name}</h6>
                    <span class="time-pill-badge">⏰ ${m.reminder_time}</span>
                </div>
                <p class="small text-muted mb-0">${m.dosage} • ${m.frequency} • ${m.instructions}</p>
            </div>
            <div>
                <button class="btn btn-sm btn-outline-teal" onclick="triggerReminderAlert(${JSON.stringify(m).replace(/"/g, '&quot;')})">
                    Test Alert
                </button>
            </div>
        </div>
    `).join('');
}

// ----------------- 12. MEDICINE HISTORY & AUDIT LOG ----------------- //

async function loadHistoryLogs() {
    try {
        const res = await fetch('/api/logs');
        const data = await res.json();
        if (data.success) {
            appState.logs = data.logs;
            renderHistoryTable(data.logs);
        }
    } catch (e) {
        console.error("Error loading logs:", e);
    }
}

function renderHistoryTable(logs) {
    const tbody = document.getElementById('medicineHistoryTableBody');
    if (!tbody) return;

    if (!logs || logs.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No dose history records found.</td></tr>`;
        return;
    }

    tbody.innerHTML = logs.map(l => {
        let badgeClass = "bg-success";
        if (l.status === "SKIPPED") badgeClass = "bg-danger";
        if (l.status === "SNOOZED") badgeClass = "bg-warning text-dark";

        return `
            <tr>
                <td>${l.date}</td>
                <td class="fw-bold">${l.medicine_name}</td>
                <td>${l.dosage || '-'}</td>
                <td class="font-monospace">${l.scheduled_time || '-'}</td>
                <td class="font-monospace">${l.action_time || '-'}</td>
                <td><span class="badge ${badgeClass} px-3 py-2">${l.status}</span></td>
                <td class="small text-muted">${l.notes || '-'}</td>
            </tr>
        `;
    }).join('');
}

function filterHistoryTable(statusFilter) {
    if (statusFilter === 'ALL') {
        renderHistoryTable(appState.logs);
    } else {
        const filtered = appState.logs.filter(l => l.status === statusFilter);
        renderHistoryTable(filtered);
    }
}

function exportHistoryLogs() {
    const csvContent = "data:text/csv;charset=utf-8," 
        + "Date,Medicine,Dosage,Scheduled Time,Action Time,Status,Notes\n"
        + appState.logs.map(l => `"${l.date}","${l.medicine_name}","${l.dosage || ''}","${l.scheduled_time || ''}","${l.action_time || ''}","${l.status}","${l.notes || ''}"`).join("\n");

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `medivoice_history_${new Date().toISOString().slice(0,10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// ----------------- 13. AI CHAT ASSISTANT ----------------- //

async function handleChatSubmit(event) {
    if (event) event.preventDefault();
    const input = document.getElementById('chatInputText');
    const query = input.value.trim();
    if (!query) return;

    sendChatPrompt(query);
    input.value = '';
}

async function sendChatPrompt(queryText) {
    const thread = document.getElementById('chatThreadContainer');

    // Append User Message
    thread.innerHTML += `
        <div class="chat-bubble user-bubble">
            <div class="bubble-content">${escapeHtml(queryText)}</div>
        </div>
    `;
    thread.scrollTop = thread.scrollHeight;

    // Show bot typing placeholder
    const typingId = `typing-${Date.now()}`;
    thread.innerHTML += `
        <div class="chat-bubble bot-bubble" id="${typingId}">
            <div class="bubble-sender"><i class="bi bi-shield-check text-teal me-1"></i> MediVoice AI</div>
            <div class="bubble-content"><div class="spinner-grow spinner-grow-sm text-teal" role="status"></div> Consulting database...</div>
        </div>
    `;
    thread.scrollTop = thread.scrollHeight;

    try {
        const res = await fetch('/api/assistant', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query: queryText })
        });
        const data = await res.json();
        const typingElem = document.getElementById(typingId);

        if (data.success) {
            if (typingElem) {
                typingElem.querySelector('.bubble-content').innerHTML = `
                    ${escapeHtml(data.response)}
                    ${data.safety_flag ? '<div class="mt-2 alert alert-warning p-2 small mb-0"><i class="bi bi-shield-exclamation me-1"></i> For non-routine symptoms, consult a doctor.</div>' : ''}
                `;
            }
            speakUtterance(data.response);

            // Execute client-side side-effects if returned by assistant
            if (data.action === 'UPDATE_LOGS') {
                await loadDailySummary();
                await loadHistoryLogs();
            } else if (data.action === 'RELOAD_MEDICINES') {
                await loadMedicines();
                await loadDailySummary();
            }
        }
    } catch (e) {
        const typingElem = document.getElementById(typingId);
        if (typingElem) {
            typingElem.querySelector('.bubble-content').textContent = "Could not contact MediVoice assistant.";
        }
    }
    thread.scrollTop = thread.scrollHeight;
}

function clearChatHistory() {
    const thread = document.getElementById('chatThreadContainer');
    thread.innerHTML = `
        <div class="chat-bubble bot-bubble">
            <div class="bubble-sender"><i class="bi bi-shield-check text-teal me-1"></i> MediVoice AI</div>
            <div class="bubble-content">Chat cleared. Ask any question about your medicines or reminders!</div>
        </div>
    `;
}

// ----------------- 14. VOICE ASSISTANT WITH SPEECH RECOGNITION ----------------- //

let lastVoiceResponse = "";

function toggleVoiceRecording() {
    if (appState.isRecordingVoice) {
        stopVoiceRecording();
    } else {
        startVoiceRecording();
    }
}

function startVoiceRecording() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
        alert("Speech recognition is not supported in this browser. Please type commands in the Chat Assistant.");
        return;
    }

    try {
        appState.recognition = new SpeechRecognition();
        appState.recognition.continuous = false;
        appState.recognition.interimResults = false;
        appState.recognition.lang = 'en-US';

        appState.recognition.onstart = function() {
            appState.isRecordingVoice = true;
            document.getElementById('voiceMicOrbBtn').classList.add('recording');
            document.getElementById('voiceMicIcon').className = 'bi bi-soundwave';
            document.getElementById('voiceStatusHeading').textContent = "Listening... Speak now";
            document.getElementById('voiceTranscriptText').textContent = "Listening to your medicine command...";
        };

        appState.recognition.onresult = function(event) {
            const transcript = event.results[0][0].transcript;
            document.getElementById('voiceTranscriptText').textContent = `Heard: "${transcript}"`;
            processVoiceCommand(transcript);
        };

        appState.recognition.onerror = function(event) {
            console.log("Speech recognition error:", event.error);
            stopVoiceRecording();
            document.getElementById('voiceStatusHeading').textContent = "Mic Error. Tap to Retry";
        };

        appState.recognition.onend = function() {
            stopVoiceRecording();
        };

        appState.recognition.start();
    } catch (e) {
        console.error("Speech start error:", e);
        stopVoiceRecording();
    }
}

function stopVoiceRecording() {
    appState.isRecordingVoice = false;
    const btn = document.getElementById('voiceMicOrbBtn');
    const icon = document.getElementById('voiceMicIcon');
    const heading = document.getElementById('voiceStatusHeading');

    if (btn) btn.classList.remove('recording');
    if (icon) icon.className = 'bi bi-mic-fill';
    if (heading) heading.textContent = "Tap Microphone to Speak";

    if (appState.recognition) {
        try { appState.recognition.stop(); } catch(e) {}
    }
}

async function processVoiceCommand(commandText) {
    document.getElementById('voiceStatusHeading').textContent = "Processing Command...";
    try {
        const res = await fetch('/api/assistant', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query: commandText })
        });
        const data = await res.json();
        if (data.success) {
            lastVoiceResponse = data.response;
            const respCard = document.getElementById('voiceResponseCard');
            const respText = document.getElementById('voiceResponseText');

            if (respCard) respCard.style.display = 'block';
            if (respText) respText.textContent = data.response;

            document.getElementById('voiceStatusHeading').textContent = "Answered";
            speakUtterance(data.response);

            if (data.action === 'UPDATE_LOGS') {
                await loadDailySummary();
                await loadHistoryLogs();
            } else if (data.action === 'RELOAD_MEDICINES') {
                await loadMedicines();
                await loadDailySummary();
            }
        }
    } catch (e) {
        document.getElementById('voiceStatusHeading').textContent = "Error processing query";
    }
}

function replayVoiceReply() {
    if (lastVoiceResponse) {
        speakUtterance(lastVoiceResponse);
    }
}

// ----------------- 15. PROFILE & AUTHENTICATION ----------------- //

function openLoginModal() {
    const modalElem = document.getElementById('authModal');
    if (modalElem) {
        const modal = new bootstrap.Modal(modalElem);
        modal.show();
    }
}

async function submitAuthRequestOtp() {
    const email = document.getElementById('authEmail').value.trim();
    const password = document.getElementById('authPassword').value;
    const alertBox = document.getElementById('authAlertBox');
    alertBox.style.display = 'none';

    try {
        const res = await fetch('/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();
        if (data.success) {
            document.getElementById('simulatedCodeText').textContent = data.simulated_otp;
            document.getElementById('loginStep1').style.display = 'none';
            document.getElementById('loginStep2').style.display = 'block';
        } else {
            alertBox.textContent = data.error || "Login failed";
            alertBox.style.display = 'block';
        }
    } catch (e) {
        alertBox.textContent = "Server error";
        alertBox.style.display = 'block';
    }
}

function fillQuickOtp() {
    const code = document.getElementById('simulatedCodeText').textContent;
    document.getElementById('authOtpInput').value = code;
}

async function submitAuthVerifyOtp() {
    const email = document.getElementById('authEmail').value.trim();
    const otp = document.getElementById('authOtpInput').value.trim();
    const alertBox = document.getElementById('authAlertBox');

    try {
        const res = await fetch('/verify-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, otp })
        });
        const data = await res.json();
        if (data.success) {
            appState.currentUser = data.user;
            document.getElementById('currentUserDisplay').textContent = data.user.name;
            document.getElementById('profileNameDisplay').textContent = data.user.name;
            document.getElementById('profileFormName').value = data.user.name;
            document.getElementById('profileFormAge').value = data.user.age;
            document.getElementById('profileFormBlood').value = data.user.blood_group;
            document.getElementById('profileFormEmergency').value = data.user.emergency_contact;

            const modalInstance = bootstrap.Modal.getInstance(document.getElementById('authModal'));
            if (modalInstance) modalInstance.hide();
            alert("Signed in successfully!");
        } else {
            alertBox.textContent = data.error || "Invalid OTP";
            alertBox.style.display = 'block';
        }
    } catch (e) {
        alertBox.textContent = "Verification failed";
        alertBox.style.display = 'block';
    }
}

async function handleProfileUpdate(event) {
    event.preventDefault();
    const name = document.getElementById('profileFormName').value.trim();
    const age = document.getElementById('profileFormAge').value.trim();
    const blood_group = document.getElementById('profileFormBlood').value.trim();
    const emergency_contact = document.getElementById('profileFormEmergency').value.trim();

    try {
        const res = await fetch('/api/profile', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, age, blood_group, emergency_contact })
        });
        const data = await res.json();
        if (data.success) {
            appState.currentUser.name = name;
            document.getElementById('currentUserDisplay').textContent = name;
            document.getElementById('profileNameDisplay').textContent = name;
            alert("Profile successfully updated!");
        }
    } catch (e) {
        alert("Failed to update profile.");
    }
}

// Helper: Escape HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ----------------- BOOTSTRAP INIT ----------------- //

window.addEventListener('DOMContentLoaded', async () => {
    // Set default dates in form inputs
    const todayIso = new Date().toISOString().slice(0, 10);
    const startInput = document.getElementById('formMedStartDate');
    if (startInput) startInput.value = todayIso;

    // Start Live Clock
    setInterval(updateLiveClock, 1000);
    updateLiveClock();

    // Load initial data
    await loadMedicines();
    await loadDailySummary();
    await loadHistoryLogs();
});
