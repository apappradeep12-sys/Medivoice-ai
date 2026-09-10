// MediVoice State
let currentUser = null;
let currentMedicines = [];
let pendingOcrMedicine = null;
let lastSimulatedOtp = "123456";

// Live Digital Clock
function updateClock() {
    const now = new Date();
    const timeElem = document.getElementById('digitalClock');
    const dateElem = document.getElementById('currentDate');
    
    if (timeElem) {
        timeElem.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true });
    }
    if (dateElem) {
        dateElem.textContent = now.toLocaleDateString([], { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
    }
}
setInterval(updateClock, 1000);
updateClock();

// Tab Switching
function switchTab(tabId) {
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    
    const panel = document.getElementById(`tab-${tabId}`);
    if (panel) panel.classList.add('active');
    
    if (event && event.target) {
        event.target.classList.add('active');
    }
}

// Initial Data Loading
async function loadMedicines() {
    try {
        const res = await fetch('/api/medicines');
        const data = await res.json();
        if (data.success) {
            currentMedicines = data.medicines;
            renderMedicines(data.medicines);
        }
    } catch (e) {
        console.error("Failed to load medicines", e);
    }
}

async function loadHistory() {
    try {
        const res = await fetch('/api/logs');
        const data = await res.json();
        if (data.success) {
            renderHistory(data.logs);
        }
    } catch (e) {
        console.error("Failed to load history logs", e);
    }
}

function renderMedicines(meds) {
    const container = document.getElementById('medicinesContainer');
    if (!container) return;
    
    container.innerHTML = meds.map(m => `
        <div class="med-card">
            <div class="med-card-header">
                <span class="med-pill-tag">${m.category}</span>
                <span class="med-time-badge">⏰ ${m.reminder_time}</span>
            </div>
            <h3>${m.name}</h3>
            <p><strong>Dosage:</strong> ${m.dosage} • ${m.frequency}</p>
            <p><strong>Advice:</strong> ${m.instructions}</p>
            <div class="med-card-actions">
                <button class="btn btn-success" onclick="recordDose('${m.name}', 'TAKEN')">✓ Taken</button>
                <button class="btn btn-warning" onclick="recordDose('${m.name}', 'SNOOZED')">⏰ Snooze</button>
                <button class="btn btn-outline" onclick="speakText('Reminder: Please take ${m.name}, ${m.dosage}. ${m.instructions}')">🔊 Speak</button>
            </div>
        </div>
    `).join('');
}

function renderHistory(logs) {
    const tbody = document.getElementById('historyTableBody');
    if (!tbody) return;
    
    tbody.innerHTML = logs.map(l => `
        <tr>
            <td>${l.date}</td>
            <td><strong>${l.medicine_name}</strong></td>
            <td><span class="med-pill-tag">${l.status}</span></td>
            <td>${l.action_time}</td>
        </tr>
    `).join('');
}

// Logging & Dose Actions
async function recordDose(medName, status) {
    try {
        const res = await fetch('/api/logs', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ medicine_name: medName, status: status })
        });
        const data = await res.json();
        if (data.success) {
            alert(`${medName} marked as ${status}!`);
            loadHistory();
        }
    } catch (e) {
        alert("Action failed to record.");
    }
}

// Client-Side Speech (100% Vercel Serverless Safe)
function speakText(text) {
    if ('speechSynthesis' in window) {
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.rate = 0.95;
        utterance.pitch = 1.0;
        window.speechSynthesis.speak(utterance);
    } else {
        alert("Speech synthesis is not supported on this browser.");
    }
}

function speakAlert() {
    const name = document.getElementById('alertMedName').textContent;
    const advice = document.getElementById('alertMedAdvice').textContent;
    speakText(`Attention: ${name} is due now! ${advice}`);
}

// OCR Scanner Simulation
async function simulateOcrScan(type) {
    try {
        const res = await fetch('/api/scan-ocr', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ filename: `${type}.jpg` })
        });
        const data = await res.json();
        if (data.success) {
            pendingOcrMedicine = data.extracted;
            document.getElementById('ocrResultText').textContent = JSON.stringify(data.extracted, null, 2);
            document.getElementById('ocrPreviewBox').style.display = 'block';
        }
    } catch (e) {
        alert("OCR parsing failed.");
    }
}

async function autoFillMedicineForm() {
    if (!pendingOcrMedicine) return;
    try {
        const res = await fetch('/api/medicines', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(pendingOcrMedicine)
        });
        const data = await res.json();
        if (data.success) {
            alert(`Added ${pendingOcrMedicine.name} to schedule!`);
            document.getElementById('ocrPreviewBox').style.display = 'none';
            loadMedicines();
            switchTab('dashboard');
        }
    } catch (e) {
        alert("Failed to auto-save scanned medicine.");
    }
}

// Voice Assistant
async function sendVoiceQuery(query) {
    const chatBox = document.getElementById('voiceChatBox');
    chatBox.innerHTML += `<div class="chat-message user"><strong>You:</strong> ${query}</div>`;
    chatBox.scrollTop = chatBox.scrollHeight;
    
    try {
        const res = await fetch('/api/voice-query', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query })
        });
        const data = await res.json();
        if (data.success) {
            chatBox.innerHTML += `<div class="chat-message assistant"><strong>MediVoice AI:</strong> ${data.response}</div>`;
            chatBox.scrollTop = chatBox.scrollHeight;
            speakText(data.response);
        }
    } catch (e) {
        chatBox.innerHTML += `<div class="chat-message assistant">Could not contact MediVoice server.</div>`;
    }
}

function submitVoiceInput() {
    const input = document.getElementById('voiceQueryInput');
    if (input.value.trim()) {
        sendVoiceQuery(input.value.trim());
        input.value = '';
    }
}

function startMicRecording() {
    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
        alert("Microphone recognition is not supported in this browser. Please type your query.");
        return;
    }
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const recognition = new SpeechRecognition();
    recognition.onresult = function(event) {
        const transcript = event.results[0][0].transcript;
        sendVoiceQuery(transcript);
    };
    recognition.start();
}

// Authentication Modal & Flow
function toggleAuthModal() {
    const modal = document.getElementById('authModal');
    modal.style.display = (modal.style.display === 'flex') ? 'none' : 'flex';
}

async function requestOtp() {
    const email = document.getElementById('authEmail').value;
    const password = document.getElementById('authPassword').value;
    const err = document.getElementById('authErrorMsg');
    err.textContent = '';
    
    try {
        const res = await fetch('/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();
        if (data.success) {
            lastSimulatedOtp = data.simulated_otp;
            document.getElementById('simulatedOtpCode').textContent = data.simulated_otp;
            document.getElementById('loginStep1').style.display = 'none';
            document.getElementById('loginStep2').style.display = 'block';
        } else {
            err.textContent = data.error || 'Login failed';
        }
    } catch (e) {
        err.textContent = 'Server error during login.';
    }
}

function fillOtp() {
    document.getElementById('authOtpInput').value = lastSimulatedOtp;
}

async function verifyOtp() {
    const email = document.getElementById('authEmail').value;
    const otp = document.getElementById('authOtpInput').value;
    const err = document.getElementById('authErrorMsg');
    
    try {
        const res = await fetch('/verify-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, otp })
        });
        const data = await res.json();
        if (data.success) {
            currentUser = data.user;
            document.getElementById('userStatusText').textContent = currentUser.name;
            document.getElementById('authActionBtn').textContent = 'Sign Out';
            document.getElementById('authActionBtn').onclick = () => location.reload();
            toggleAuthModal();
        } else {
            err.textContent = data.error || 'OTP verification failed';
        }
    } catch (e) {
        err.textContent = 'Verification error.';
    }
}

// Add Medicine Modal
function openAddMedModal() {
    document.getElementById('addMedModal').style.display = 'flex';
}
function closeAddMedModal() {
    document.getElementById('addMedModal').style.display = 'none';
}

async function saveNewMedicine() {
    const med = {
        name: document.getElementById('newMedName').value,
        category: document.getElementById('newMedCategory').value,
        dosage: document.getElementById('newMedDosage').value,
        frequency: document.getElementById('newMedFreq').value,
        reminder_time: document.getElementById('newMedTime').value,
        instructions: document.getElementById('newMedInstructions').value
    };
    
    if (!med.name) {
        alert("Please enter a medicine name.");
        return;
    }
    
    const res = await fetch('/api/medicines', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(med)
    });
    const data = await res.json();
    if (data.success) {
        closeAddMedModal();
        loadMedicines();
    }
}

// Boot
window.onload = function() {
    loadMedicines();
    loadHistory();
};
