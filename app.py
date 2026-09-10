import os
import random
import datetime
from flask import Flask, render_template, request, jsonify, session

# Flask app initialized at the root level for Vercel WSGI standard detection
app = Flask(__name__, template_folder='templates', static_folder='static')
app.secret_key = os.environ.get("FLASK_SECRET_KEY", "medivoice-secret-key-2026")

# In-memory store (Vercel serverless compatible, ready for hosted PostgreSQL / Supabase / MongoDB Atlas)
DEMO_DATABASE = {
    "users": {
        "patient@medivoice.ai": {
            "password": "Password123!",
            "name": "Alex Mercer",
            "active_otp": None
        }
    },
    "medicines": [
        {
            "id": 1,
            "name": "Amoxicillin 500mg",
            "category": "Capsule",
            "dosage": "1 Capsule (500mg)",
            "frequency": "Twice Daily",
            "reminder_time": "08:00 AM",
            "instructions": "Take after meals with a full glass of water",
            "start_date": "2026-09-01",
            "end_date": "2026-09-15"
        },
        {
            "id": 2,
            "name": "Paracetamol 650mg",
            "category": "Tablet",
            "dosage": "1 Tablet (650mg)",
            "frequency": "As Needed",
            "reminder_time": "02:00 PM",
            "instructions": "Take after food if fever or headache occurs",
            "start_date": "2026-09-05",
            "end_date": "2026-09-20"
        },
        {
            "id": 3,
            "name": "Cetirizine 10mg",
            "category": "Tablet",
            "dosage": "1 Tablet",
            "frequency": "Once Nightly",
            "reminder_time": "09:00 PM",
            "instructions": "Take at bedtime; may cause drowsiness",
            "start_date": "2026-09-01",
            "end_date": "2026-09-30"
        }
    ],
    "dose_logs": [
        {
            "id": 101,
            "medicine_name": "Amoxicillin 500mg",
            "status": "TAKEN",
            "action_time": "08:05 AM",
            "date": "2026-09-09"
        }
    ]
}

# ----------------- MAIN PAGES & HEALTH CHECK ----------------- #

@app.route('/', methods=['GET'])
def index():
    """Renders the main single-page clinical portal."""
    return render_template('index.html')


@app.route('/healthz', methods=['GET'])
def health_check():
    """Verification route for testing Vercel deployment status."""
    return jsonify({
        "status": "online",
        "service": "MediVoice AI",
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat()
    }), 200

# ----------------- AUTHENTICATION & OTP ----------------- #

@app.route('/login', methods=['POST'])
def login():
    data = request.get_json() or {}
    email = data.get('email', '').strip().lower()
    password = data.get('password', '')

    if not email or not password:
        return jsonify({"success": False, "error": "Email and password are required."}), 400

    # Retrieve or initialize user
    user = DEMO_DATABASE["users"].setdefault(email, {
        "password": password,
        "name": email.split('@')[0].capitalize(),
        "active_otp": None
    })

    if user["password"] != password:
        return jsonify({"success": False, "error": "Invalid credentials provided."}), 401

    # Generate 6-digit OTP code
    otp_code = f"{random.randint(100000, 999999)}"
    user["active_otp"] = otp_code

    return jsonify({
        "success": True,
        "message": f"OTP sent to {email}",
        "simulated_otp": otp_code,
        "email": email
    }), 200


@app.route('/verify-otp', methods=['POST'])
def verify_otp():
    data = request.get_json() or {}
    email = data.get('email', '').strip().lower()
    otp = data.get('otp', '').strip()

    user = DEMO_DATABASE["users"].get(email)
    if not user:
        return jsonify({"success": False, "error": "User record not found."}), 404

    # Allow generated OTP or master demo fallback '123456'
    if user.get("active_otp") == otp or otp == "123456":
        session['user_email'] = email
        user['active_otp'] = None
        return jsonify({
            "success": True,
            "user": {
                "name": user["name"],
                "email": email
            }
        }), 200

    return jsonify({"success": False, "error": "Invalid or expired 6-digit OTP."}), 400

# ----------------- MEDICINE MANAGEMENT ----------------- #

@app.route('/api/medicines', methods=['GET', 'POST'])
def handle_medicines():
    if request.method == 'GET':
        return jsonify({"success": True, "medicines": DEMO_DATABASE["medicines"]}), 200

    data = request.get_json() or {}
    name = data.get('name', '').strip()
    if not name:
        return jsonify({"success": False, "error": "Medicine name is required."}), 400

    new_med = {
        "id": int(datetime.datetime.now().timestamp()),
        "name": name,
        "category": data.get('category', 'Tablet'),
        "dosage": data.get('dosage', '1 dose'),
        "frequency": data.get('frequency', 'Once Daily'),
        "reminder_time": data.get('reminder_time', '08:00 AM'),
        "instructions": data.get('instructions', 'Take after meals'),
        "start_date": data.get('start_date', datetime.date.today().isoformat()),
        "end_date": data.get('end_date', '')
    }
    DEMO_DATABASE["medicines"].append(new_med)
    return jsonify({"success": True, "medicine": new_med}), 201


@app.route('/api/medicines/<int:med_id>', methods=['DELETE'])
def delete_medicine(med_id):
    DEMO_DATABASE["medicines"] = [m for m in DEMO_DATABASE["medicines"] if m["id"] != med_id]
    return jsonify({"success": True, "message": "Medicine deleted"}), 200

# ----------------- DOSAGE AUDIT LOGS ----------------- #

@app.route('/api/logs', methods=['GET', 'POST'])
def handle_logs():
    if request.method == 'GET':
        return jsonify({"success": True, "logs": DEMO_DATABASE["dose_logs"]}), 200

    data = request.get_json() or {}
    med_name = data.get('medicine_name', 'Prescription')
    status = data.get('status', 'TAKEN')

    new_log = {
        "id": int(datetime.datetime.now().timestamp()),
        "medicine_name": med_name,
        "status": status,
        "action_time": datetime.datetime.now().strftime("%I:%M %p"),
        "date": datetime.date.today().isoformat()
    }
    DEMO_DATABASE["dose_logs"].insert(0, new_log)
    return jsonify({"success": True, "log": new_log}), 201

# ----------------- OCR SCANNER SIMULATION ----------------- #

@app.route('/api/scan-ocr', methods=['POST'])
def scan_ocr():
    """
    Simulates OCR extraction on uploaded strip/bottle image.
    Pure serverless Python logic without heavy uncompiled C-binaries.
    """
    data = request.get_json() or {}
    filename = data.get('filename', 'strip.jpg').lower()

    if "amox" in filename:
        result = {
            "name": "Amoxicillin 500mg",
            "category": "Capsule",
            "dosage": "500mg",
            "frequency": "Twice Daily",
            "reminder_time": "08:00 AM",
            "instructions": "Take after meals; complete full course."
        }
    elif "para" in filename or "crocin" in filename or "dolo" in filename:
        result = {
            "name": "Paracetamol 650mg",
            "category": "Tablet",
            "dosage": "650mg",
            "frequency": "Thrice Daily",
            "reminder_time": "02:00 PM",
            "instructions": "Take after food if fever or pain occurs."
        }
    elif "cough" in filename or "syrup" in filename:
        result = {
            "name": "Dextromethorphan Syrup",
            "category": "Syrup",
            "dosage": "10ml",
            "frequency": "Twice Daily",
            "reminder_time": "09:00 PM",
            "instructions": "Use measuring cup; avoid drinking cold fluids immediately."
        }
    else:
        result = {
            "name": "Azithromycin 250mg",
            "category": "Tablet",
            "dosage": "250mg",
            "frequency": "Once Daily",
            "reminder_time": "08:00 PM",
            "instructions": "Take 1 hour before or 2 hours after food."
        }

    return jsonify({"success": True, "extracted": result}), 200

# ----------------- CLINICAL VOICE ASSISTANT ----------------- #

@app.route('/api/voice-query', methods=['POST'])
def voice_query():
    data = request.get_json() or {}
    query = data.get('query', '').lower()

    if "what" in query or "today" in query or "medicine" in query:
        med_names = ", ".join([m['name'] for m in DEMO_DATABASE["medicines"]])
        reply = f"Your schedule today includes: {med_names}. Remember to stay hydrated and take them on time!"
    elif "taken" in query or "log" in query:
        taken_count = sum(1 for l in DEMO_DATABASE["dose_logs"] if l['status'] == 'TAKEN')
        reply = f"You have logged {taken_count} doses taken today. Keep up the good work!"
    elif "headache" in query or "fever" in query:
        reply = "For mild headache or fever, paracetamol 650mg is commonly used after food. Please consult your physician if symptoms persist."
    else:
        reply = f"Understood: '{query}'. You can say 'What are my medicines today?' or tap the microphone anytime."

    return jsonify({"success": True, "response": reply}), 200

# Local WSGI development server
if __name__ == '__main__':
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port, debug=True)
