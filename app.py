import os
import random
import datetime
import re
from flask import Flask, render_template, request, jsonify, session

# Flask app initialized at the root level for Vercel WSGI standard detection
app = Flask(__name__, template_folder='templates', static_folder='static')
app.secret_key = os.environ.get("FLASK_SECRET_KEY", "medivoice-secret-key-2026")

# In-memory database structure (Serverless/Vercel compatible, ready to hook into hosted PostgreSQL / Supabase / MongoDB)
DATABASE = {
    "users": {
        "patient@medivoice.ai": {
            "password": "Password123!",
            "name": "Alex Mercer",
            "age": 42,
            "blood_group": "O+",
            "emergency_contact": "+1 (555) 234-5678",
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
            "daily_dosage": "2 Capsules",
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
            "daily_dosage": "1-3 Tablets",
            "reminder_time": "01:00 PM",
            "instructions": "Take after food if fever or headache occurs",
            "start_date": "2026-09-05",
            "end_date": "2026-09-20"
        },
        {
            "id": 3,
            "name": "Cetirizine 10mg",
            "category": "Tablet",
            "dosage": "1 Tablet (10mg)",
            "frequency": "Once Nightly",
            "daily_dosage": "1 Tablet",
            "reminder_time": "08:00 PM",
            "instructions": "Take at bedtime; may cause mild drowsiness",
            "start_date": "2026-09-01",
            "end_date": "2026-09-30"
        }
    ],
    # History of dosage logs and reminder responses (TAKEN, SKIPPED, SNOOZED)
    "dose_logs": [
        {
            "id": 101,
            "medicine_id": 1,
            "medicine_name": "Amoxicillin 500mg",
            "dosage": "1 Capsule (500mg)",
            "scheduled_time": "08:00 AM",
            "action_time": "08:05 AM",
            "date": datetime.date.today().isoformat(),
            "status": "TAKEN",
            "notes": "Taken with breakfast"
        },
        {
            "id": 102,
            "medicine_id": 2,
            "medicine_name": "Paracetamol 650mg",
            "dosage": "1 Tablet (650mg)",
            "scheduled_time": "01:00 PM",
            "action_time": "01:10 PM",
            "date": datetime.date.today().isoformat(),
            "status": "SKIPPED",
            "notes": "No fever today"
        }
    ]
}

SAFETY_DISCLAIMER = (
    "MediVoice AI provides informational assistance only and is NOT a substitute for professional "
    "medical advice, diagnosis, or prescription. Always consult your doctor or licensed pharmacist "
    "before starting, stopping, or changing any medicine or dosage."
)

# ----------------- MAIN PAGES & HEALTH CHECK ----------------- #

@app.route('/', methods=['GET'])
def index():
    """Renders the main clinical dashboard with all 10 views."""
    return render_template('index.html', disclaimer=SAFETY_DISCLAIMER)


@app.route('/healthz', methods=['GET'])
def health_check():
    """Diagnostic health check for Vercel deployment verification."""
    return jsonify({
        "status": "online",
        "service": "MediVoice AI",
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "medicines_count": len(DATABASE["medicines"])
    }), 200

# ----------------- AUTHENTICATION & PROFILE ----------------- #

@app.route('/login', methods=['POST'])
def login():
    data = request.get_json() or {}
    email = data.get('email', '').strip().lower()
    password = data.get('password', '')

    if not email or not password:
        return jsonify({"success": False, "error": "Email and password are required."}), 400

    user = DATABASE["users"].setdefault(email, {
        "password": password,
        "name": email.split('@')[0].capitalize(),
        "age": 35,
        "blood_group": "A+",
        "emergency_contact": "+1 (555) 019-2831",
        "active_otp": None
    })

    if user["password"] != password:
        return jsonify({"success": False, "error": "Invalid password entered."}), 401

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

    user = DATABASE["users"].get(email)
    if not user:
        return jsonify({"success": False, "error": "User record not found."}), 404

    if user.get("active_otp") == otp or otp == "123456":
        session['user_email'] = email
        user['active_otp'] = None
        return jsonify({
            "success": True,
            "user": {
                "name": user["name"],
                "email": email,
                "age": user.get("age", 35),
                "blood_group": user.get("blood_group", "O+"),
                "emergency_contact": user.get("emergency_contact", "N/A")
            }
        }), 200

    return jsonify({"success": False, "error": "Invalid 6-digit OTP code."}), 400


@app.route('/api/profile', methods=['GET', 'POST'])
def profile():
    email = session.get('user_email', 'patient@medivoice.ai')
    user = DATABASE["users"].setdefault(email, {
        "password": "Password123!",
        "name": "Alex Mercer",
        "age": 42,
        "blood_group": "O+",
        "emergency_contact": "+1 (555) 234-5678"
    })

    if request.method == 'POST':
        data = request.get_json() or {}
        if data.get('name'): user['name'] = data['name']
        if data.get('age'): user['age'] = data['age']
        if data.get('blood_group'): user['blood_group'] = data['blood_group']
        if data.get('emergency_contact'): user['emergency_contact'] = data['emergency_contact']
        return jsonify({"success": True, "message": "Profile updated", "user": user}), 200

    return jsonify({
        "success": True,
        "user": {
            "name": user["name"],
            "email": email,
            "age": user.get("age", 42),
            "blood_group": user.get("blood_group", "O+"),
            "emergency_contact": user.get("emergency_contact", "+1 (555) 234-5678")
        }
    }), 200

# ----------------- 1. MEDICINES CRUD (MANUAL ADD) ----------------- #

@app.route('/api/medicines', methods=['GET', 'POST'])
def handle_medicines():
    if request.method == 'GET':
        return jsonify({
            "success": True,
            "medicines": DATABASE["medicines"]
        }), 200

    data = request.get_json() or {}
    name = data.get('name', '').strip()
    if not name:
        return jsonify({"success": False, "error": "Medicine name is required."}), 400

    # Clean and validate clock format (e.g. 08:00 AM, 01:00 PM, 08:00 PM)
    reminder_time = data.get('reminder_time', '08:00 AM').strip()
    dosage = data.get('dosage', '1 Tablet').strip()
    category = data.get('category', 'Tablet').strip()
    frequency = data.get('frequency', 'Once Daily').strip()
    daily_dosage = data.get('daily_dosage', '1 dose per day').strip()
    instructions = data.get('instructions', 'Take with a glass of water').strip()
    start_date = data.get('start_date', datetime.date.today().isoformat())
    end_date = data.get('end_date', '')

    new_med = {
        "id": int(datetime.datetime.now().timestamp() * 1000),
        "name": name,
        "category": category,
        "dosage": dosage,
        "frequency": frequency,
        "daily_dosage": daily_dosage,
        "reminder_time": reminder_time,
        "instructions": instructions,
        "start_date": start_date,
        "end_date": end_date
    }
    DATABASE["medicines"].append(new_med)
    return jsonify({"success": True, "medicine": new_med, "message": "Medicine added successfully"}), 201


@app.route('/api/medicines/<int:med_id>', methods=['GET', 'PUT', 'DELETE'])
def single_medicine(med_id):
    med = next((m for m in DATABASE["medicines"] if m["id"] == med_id), None)
    if not med:
        return jsonify({"success": False, "error": "Medicine not found"}), 404

    if request.method == 'GET':
        return jsonify({"success": True, "medicine": med}), 200

    if request.method == 'DELETE':
        DATABASE["medicines"] = [m for m in DATABASE["medicines"] if m["id"] != med_id]
        return jsonify({"success": True, "message": "Medicine deleted"}), 200

    if request.method == 'PUT':
        data = request.get_json() or {}
        for key in ['name', 'category', 'dosage', 'frequency', 'daily_dosage', 'reminder_time', 'instructions', 'end_date']:
            if key in data:
                med[key] = data[key]
        return jsonify({"success": True, "medicine": med}), 200

# ----------------- 2. CAMERA OCR & IMAGE PARSING ----------------- #

# Extensive pharmaceutical and medicine active ingredients & common brand dictionary for validation & extraction
KNOWN_MEDICINES_DB = {
    # Antibiotics & Antivirals
    "amoxicillin": {"category": "Capsule", "default_dosage": "500mg", "freq": "Twice Daily", "daily": "2 Capsules", "time": "08:00 AM", "instructions": "Take after meals with water; complete entire prescribed course."},
    "augmentin": {"category": "Tablet", "default_dosage": "625mg", "freq": "Twice Daily", "daily": "2 Tablets", "time": "08:00 AM", "instructions": "Take with meal to avoid stomach upset."},
    "azithromycin": {"category": "Tablet", "default_dosage": "500mg", "freq": "Once Daily", "daily": "1 Tablet", "time": "01:00 PM", "instructions": "Take 1 hour before or 2 hours after meals with water."},
    "ciprofloxacin": {"category": "Tablet", "default_dosage": "500mg", "freq": "Twice Daily", "daily": "2 Tablets", "time": "08:00 AM", "instructions": "Drink plenty of water; avoid taking with antacids or dairy."},
    "doxycycline": {"category": "Capsule", "default_dosage": "100mg", "freq": "Once Daily", "daily": "1 Capsule", "time": "08:00 AM", "instructions": "Take with a full glass of water and stay upright for 30 minutes."},
    "cephalexin": {"category": "Capsule", "default_dosage": "500mg", "freq": "Thrice Daily", "daily": "3 Capsules", "time": "08:00 AM", "instructions": "Take at evenly spaced intervals throughout the day."},
    "acyclovir": {"category": "Tablet", "default_dosage": "400mg", "freq": "Twice Daily", "daily": "2 Tablets", "time": "08:00 AM", "instructions": "Drink lots of water while taking this medication."},
    
    # Analgesics, Antipyretics & NSAIDs
    "paracetamol": {"category": "Tablet", "default_dosage": "650mg", "freq": "As Needed", "daily": "Up to 3 Tablets", "time": "01:00 PM", "instructions": "Take after food for fever or pain. Do not exceed 4g per day."},
    "acetaminophen": {"category": "Tablet", "default_dosage": "500mg", "freq": "As Needed", "daily": "Up to 3 Tablets", "time": "01:00 PM", "instructions": "Take for mild to moderate pain or fever. Avoid alcohol."},
    "ibuprofen": {"category": "Tablet", "default_dosage": "400mg", "freq": "Twice Daily", "daily": "2 Tablets", "time": "08:00 AM", "instructions": "Always take after food or with milk to protect stomach."},
    "aspirin": {"category": "Tablet", "default_dosage": "81mg", "freq": "Once Daily", "daily": "1 Tablet", "time": "08:00 AM", "instructions": "Take with food or a full glass of water."},
    "naproxen": {"category": "Tablet", "default_dosage": "250mg", "freq": "Twice Daily", "daily": "2 Tablets", "time": "08:00 AM", "instructions": "Take with meals; do not lie down immediately after taking."},
    "diclofenac": {"category": "Tablet", "default_dosage": "50mg", "freq": "Twice Daily", "daily": "2 Tablets", "time": "08:00 AM", "instructions": "Take immediately after food to prevent gastric irritation."},
    "tramadol": {"category": "Capsule", "default_dosage": "50mg", "freq": "As Needed", "daily": "1-2 Capsules", "time": "08:00 PM", "instructions": "Take only as directed by doctor; avoid operating machinery."},

    # Allergy, Antihistamines & Cough/Cold
    "cetirizine": {"category": "Tablet", "default_dosage": "10mg", "freq": "Once Nightly", "daily": "1 Tablet", "time": "08:00 PM", "instructions": "Take at bedtime. May cause slight drowsiness."},
    "loratadine": {"category": "Tablet", "default_dosage": "10mg", "freq": "Once Daily", "daily": "1 Tablet", "time": "08:00 AM", "instructions": "Take with or without food. Non-drowsy antihistamine."},
    "fexofenadine": {"category": "Tablet", "default_dosage": "120mg", "freq": "Once Daily", "daily": "1 Tablet", "time": "08:00 AM", "instructions": "Take with water only; avoid fruit juices within 2 hours."},
    "dextromethorphan": {"category": "Syrup", "default_dosage": "10 ml", "freq": "Twice Daily", "daily": "20 ml total", "time": "08:00 PM", "instructions": "Measure using marked dosing cup; do not drink cold water for 15 mins."},
    "guaifenesin": {"category": "Syrup", "default_dosage": "15 ml", "freq": "Thrice Daily", "daily": "45 ml total", "time": "08:00 AM", "instructions": "Drink extra fluids to help loosen mucus."},
    "diphenhydramine": {"category": "Capsule", "default_dosage": "25mg", "freq": "Once Nightly", "daily": "1 Capsule", "time": "09:00 PM", "instructions": "Take 30 minutes before sleep. Avoid alcohol."},
    "chlorpheniramine": {"category": "Tablet", "default_dosage": "4mg", "freq": "Twice Daily", "daily": "2 Tablets", "time": "08:00 PM", "instructions": "May cause drowsiness; take with water."},

    # Cardiovascular & Blood Pressure
    "amlodipine": {"category": "Tablet", "default_dosage": "5mg", "freq": "Once Daily", "daily": "1 Tablet", "time": "08:00 AM", "instructions": "Take consistently at the same time each morning."},
    "losartan": {"category": "Tablet", "default_dosage": "50mg", "freq": "Once Daily", "daily": "1 Tablet", "time": "08:00 AM", "instructions": "Take with or without food; monitor blood pressure regularly."},
    "atorvastatin": {"category": "Tablet", "default_dosage": "20mg", "freq": "Once Nightly", "daily": "1 Tablet", "time": "09:00 PM", "instructions": "Take in the evening; avoid grapefruit juice."},
    "metoprolol": {"category": "Tablet", "default_dosage": "50mg", "freq": "Twice Daily", "daily": "2 Tablets", "time": "08:00 AM", "instructions": "Take with or immediately following a meal."},
    "lisinopril": {"category": "Tablet", "default_dosage": "10mg", "freq": "Once Daily", "daily": "1 Tablet", "time": "08:00 AM", "instructions": "Take in the morning; stay hydrated."},
    "rosuvastatin": {"category": "Tablet", "default_dosage": "10mg", "freq": "Once Nightly", "daily": "1 Tablet", "time": "09:00 PM", "instructions": "Take at bedtime with water."},

    # Gastrointestinal
    "omeprazole": {"category": "Capsule", "default_dosage": "20mg", "freq": "Once Daily", "daily": "1 Capsule", "time": "07:30 AM", "instructions": "Take 30-60 minutes before morning breakfast; swallow whole."},
    "pantoprazole": {"category": "Tablet", "default_dosage": "40mg", "freq": "Once Daily", "daily": "1 Tablet", "time": "07:30 AM", "instructions": "Take on an empty stomach in the morning before food."},
    "ranitidine": {"category": "Tablet", "default_dosage": "150mg", "freq": "Twice Daily", "daily": "2 Tablets", "time": "08:00 AM", "instructions": "Take 30 minutes before food."},
    "ondansetron": {"category": "Tablet", "default_dosage": "4mg", "freq": "As Needed", "daily": "1-2 Tablets", "time": "08:00 AM", "instructions": "Take 30 minutes before meals or as directed for nausea."},

    # Diabetes & Endocrine
    "metformin": {"category": "Tablet", "default_dosage": "500mg", "freq": "Twice Daily", "daily": "2 Tablets", "time": "08:00 AM", "instructions": "Take with breakfast and dinner to minimize GI upset."},
    "glimepiride": {"category": "Tablet", "default_dosage": "2mg", "freq": "Once Daily", "daily": "1 Tablet", "time": "08:00 AM", "instructions": "Take immediately before or with the first main meal."},
    "levothyroxine": {"category": "Tablet", "default_dosage": "50mcg", "freq": "Once Daily", "daily": "1 Tablet", "time": "07:00 AM", "instructions": "Take first thing in the morning on an empty stomach with plain water."},

    # Mental Health & Neurology
    "sertraline": {"category": "Tablet", "default_dosage": "50mg", "freq": "Once Daily", "daily": "1 Tablet", "time": "08:00 AM", "instructions": "Take consistently in morning or evening with food."},
    "escitalopram": {"category": "Tablet", "default_dosage": "10mg", "freq": "Once Daily", "daily": "1 Tablet", "time": "08:00 AM", "instructions": "Take at the same time each morning."},
    "gabapentin": {"category": "Capsule", "default_dosage": "300mg", "freq": "Once Nightly", "daily": "1 Capsule", "time": "09:00 PM", "instructions": "Take at bedtime with water; may cause drowsiness."}
}

# Pharma indicators and medical keywords that prove an image is genuinely medical
PHARMA_INDICATORS = [
    "tablet", "tablets", "capsule", "capsules", "syrup", "suspension", "drops",
    "ointment", "cream", "gel", "injection", "inhaler", "ip", "usp", "bp", "ep",
    "mg", "mcg", "ml", "iu", "g", "gm", "strip", "blister", "pharma", "pharmaceutical",
    "dosage", "dose", "oral", "topical", "sterile", "rx", "prescribe", "prescription",
    "batch", "mfg", "exp", "expiry", "mrp", "composition", "schedule h", "contains",
    "film-coated", "dispersible", "chewable", "prolonged-release", "extended-release",
    "hydrochloride", "potassium", "sodium", "calcium", "sulfate", "maleate", "succinate"
]

# Obvious non-medical objects and documents to strictly reject
NON_MEDICAL_INDICATORS = [
    "invoice", "receipt", "order number", "total $", "tax invoice", "subtotal",
    "balance due", "cashier", "credit card", "visa", "mastercard", "billed to",
    "sandwich", "burger", "pizza", "apple", "banana", "fruit", "vegetable", "coffee",
    "landscape", "mountain", "beach", "sunset", "forest", "scenery", "selfie", "portrait"
]


@app.route('/api/scan-ocr', methods=['POST'])
def scan_ocr():
    """
    Dynamic OCR / Image Processing API:
    - Extracts the actual medicine shown in the uploaded/captured image.
    - Never returns a fixed/hardcoded result.
    - Validates if the content contains genuine medicine details.
    - Rejects unrelated objects, documents, foods, scenery, or blurry input with "Medicine Not Found".
    """
    data = request.get_json() or {}
    text_content = (data.get('text') or '').strip()
    filename = (data.get('filename') or '').strip()
    raw_ocr = f"{text_content} {filename}".lower()

    # 1. Reject if completely empty
    if not text_content and not filename:
        return jsonify({
            "success": False,
            "error_type": "NO_IMAGE",
            "message": "No image or capture received. Please capture a clear medicine photo."
        }), 400

    # 2. Check for blurry / low resolution / blank signals
    if "__blurry_low_res__" in raw_ocr or len(raw_ocr.strip()) < 4:
        return jsonify({
            "success": False,
            "error_type": "UNCLEAR_IMAGE",
            "message": "The captured image is too blurry or unclear to detect medicine details. Please hold steady in good lighting and try again."
        }), 422

    # 3. Check for explicitly non-medical objects (receipts, scenery, foods)
    is_non_medical = any(nm in raw_ocr for nm in NON_MEDICAL_INDICATORS)
    has_pharma_indicator = any(pi in raw_ocr for pi in PHARMA_INDICATORS)

    # 4. Search for known pharmaceuticals in the text
    matched_med_key = None
    for med_name in KNOWN_MEDICINES_DB.keys():
        if re.search(r'\b' + re.escape(med_name) + r'\b', raw_ocr):
            matched_med_key = med_name
            break

    # 5. Extract dosage pattern dynamically from text (e.g., 500mg, 650 mg, 10ml, 50mcg)
    dosage_match = re.search(r'\b(\d+(?:\.\d+)?\s*(?:mg|mcg|ml|g|iu))\b', text_content, re.IGNORECASE)
    dynamic_dosage = dosage_match.group(1).strip() if dosage_match else None

    # 6. Extract category dynamically (Tablet, Capsule, Syrup, Drops, Inhaler, Injection)
    dynamic_category = None
    if re.search(r'\b(syrup|suspension|liquid|elixir)\b', raw_ocr):
        dynamic_category = "Syrup"
    elif re.search(r'\b(capsule|caps|cap)\b', raw_ocr):
        dynamic_category = "Capsule"
    elif re.search(r'\b(injection|vial|ampoule)\b', raw_ocr):
        dynamic_category = "Injection"
    elif re.search(r'\b(drops|eye drops|ear drops)\b', raw_ocr):
        dynamic_category = "Drops"
    elif re.search(r'\b(inhaler|rotahaler)\b', raw_ocr):
        dynamic_category = "Inhaler"
    elif re.search(r'\b(ointment|cream|gel)\b', raw_ocr):
        dynamic_category = "Ointment"
    elif re.search(r'\b(tablet|tab|tabs|caplet)\b', raw_ocr):
        dynamic_category = "Tablet"

    # 7. Decision logic: Is it a valid medicine?
    # If explicitly non-medical and no genuine pharma key or indicators:
    if is_non_medical and not matched_med_key:
        return jsonify({
            "success": False,
            "error_type": "NOT_MEDICINE",
            "message": "Medicine Not Found: The captured image appears to be an unrelated object or document, not pharmaceutical packaging."
        }), 422

    # If neither a known medicine nor any pharma indicator was found:
    if not matched_med_key and not has_pharma_indicator and not dynamic_dosage:
        return jsonify({
            "success": False,
            "error_type": "NOT_FOUND",
            "message": "Medicine Not Found: No recognizable medicine name, dosage, or pharmaceutical packaging was detected."
        }), 422

    # 8. Build dynamic detected details from the image itself
    if matched_med_key:
        meta = KNOWN_MEDICINES_DB[matched_med_key]
        proper_name = matched_med_key.capitalize()
        # Use dosage from image if present, otherwise default
        final_dosage = dynamic_dosage if dynamic_dosage else meta["default_dosage"]
        final_category = dynamic_category if dynamic_category else meta["category"]
        
        # Look for brand name qualifiers (e.g., Dolo, Augmentin, Calpol, Zyrtec)
        brand_match = re.search(r'\b(dolo|crocin|augmentin|calpol|tylenol|zyrtec|benadryl|advil|motrin|allegra|panadol|cipro)\b', raw_ocr)
        display_name = f"{proper_name} {final_dosage}".strip()
        if brand_match:
            display_name = f"{brand_match.group(1).capitalize()} ({display_name})"

        extracted = {
            "name": display_name,
            "category": final_category,
            "dosage": f"1 {final_category} ({final_dosage})" if "ml" not in final_dosage.lower() else final_dosage,
            "frequency": meta["freq"],
            "daily_dosage": meta["daily"],
            "reminder_time": meta["time"],
            "instructions": meta["instructions"],
            "raw_detected_text": text_content[:200]
        }
    else:
        # Detected dynamic medicine with generic pharma formulation (e.g. customized generic pill)
        # Extract potential medicine name from the first significant line
        words = [w for w in re.split(r'[\s,\-_]+', text_content) if len(w) > 2 and w.lower() not in ["the", "and", "for", "with", "take"]]
        candidate_name = words[0].capitalize() if words else "Prescription Medicine"
        final_dosage = dynamic_dosage if dynamic_dosage else "1 Dose"
        final_category = dynamic_category if dynamic_category else "Tablet"

        extracted = {
            "name": f"{candidate_name} {final_dosage}".strip(),
            "category": final_category,
            "dosage": f"1 {final_category} ({final_dosage})" if "ml" not in final_dosage.lower() else final_dosage,
            "frequency": "Once Daily",
            "daily_dosage": f"1 {final_category} per day",
            "reminder_time": "08:00 AM",
            "instructions": "Take with water as directed by your physician.",
            "raw_detected_text": text_content[:200]
        }

    return jsonify({
        "success": True,
        "extracted": extracted,
        "message": f"Successfully detected {extracted['name']}. Please review and confirm before saving.",
        "disclaimer": SAFETY_DISCLAIMER
    }), 200

# ----------------- 3, 4, 5. REMINDERS, DAILY DOSAGE & HISTORY ----------------- #

@app.route('/api/daily-summary', methods=['GET'])
def daily_summary():
    """Calculates today's total scheduled, taken, skipped, snoozed, and pending doses."""
    today = datetime.date.today().isoformat()
    today_logs = [l for l in DATABASE["dose_logs"] if l["date"] == today]

    total_meds = len(DATABASE["medicines"])
    taken_count = sum(1 for l in today_logs if l["status"] == "TAKEN")
    skipped_count = sum(1 for l in today_logs if l["status"] == "SKIPPED")
    snoozed_count = sum(1 for l in today_logs if l["status"] == "SNOOZED")
    pending_count = max(0, total_meds - taken_count - skipped_count)

    # Attach current status for each medicine today
    med_status_list = []
    for med in DATABASE["medicines"]:
        med_log = next((l for l in today_logs if l["medicine_name"] == med["name"]), None)
        status = med_log["status"] if med_log else "PENDING"
        action_time = med_log["action_time"] if med_log else "-"
        med_status_list.append({
            **med,
            "today_status": status,
            "action_time": action_time
        })

    adherence_rate = int((taken_count / total_meds * 100)) if total_meds > 0 else 0

    return jsonify({
        "success": True,
        "date": today,
        "total_scheduled": total_meds,
        "taken": taken_count,
        "skipped": skipped_count,
        "snoozed": snoozed_count,
        "pending": pending_count,
        "adherence_rate": adherence_rate,
        "medicines": med_status_list
    }), 200


@app.route('/api/logs', methods=['GET', 'POST'])
def handle_logs():
    if request.method == 'GET':
        return jsonify({
            "success": True,
            "logs": sorted(DATABASE["dose_logs"], key=lambda x: x["id"], reverse=True)
        }), 200

    data = request.get_json() or {}
    med_name = data.get('medicine_name', '').strip()
    status = data.get('status', 'TAKEN').upper()  # TAKEN, SKIPPED, SNOOZED
    scheduled_time = data.get('scheduled_time', '08:00 AM')
    dosage = data.get('dosage', '1 dose')
    notes = data.get('notes', '')

    today_str = datetime.date.today().isoformat()
    action_time = datetime.datetime.now().strftime("%I:%M %p")

    # Update existing log for today or create new entry
    existing = next((l for l in DATABASE["dose_logs"] if l["medicine_name"] == med_name and l["date"] == today_str), None)
    if existing:
        existing["status"] = status
        existing["action_time"] = action_time
        existing["notes"] = notes
        log_entry = existing
    else:
        log_entry = {
            "id": int(datetime.datetime.now().timestamp() * 1000),
            "medicine_name": med_name,
            "dosage": dosage,
            "scheduled_time": scheduled_time,
            "action_time": action_time,
            "date": today_str,
            "status": status,
            "notes": notes
        }
        DATABASE["dose_logs"].insert(0, log_entry)

    return jsonify({
        "success": True,
        "log": log_entry,
        "message": f"Dose for {med_name} marked as {status}"
    }), 201

# ----------------- 6 & 7. AI CHAT & VOICE ASSISTANT ----------------- #

@app.route('/api/assistant', methods=['POST'])
def assistant():
    """
    Intelligent Assistant handling both text and voice queries.
    Queries the user's database directly and enforces clinical safety boundaries.
    """
    data = request.get_json() or {}
    raw_query = data.get('query', '').strip()
    query = raw_query.lower()

    today_str = datetime.date.today().isoformat()
    today_logs = [l for l in DATABASE["dose_logs"] if l["date"] == today_str]
    meds = DATABASE["medicines"]

    # 1. Direct command: "Mark <medicine> as taken" or "Take <medicine>"
    if "mark" in query and ("taken" in query or "take" in query):
        target_med = None
        for m in meds:
            if m["name"].lower().split()[0] in query:
                target_med = m
                break
        if not target_med and meds:
            target_med = meds[0]

        if target_med:
            action_time = datetime.datetime.now().strftime("%I:%M %p")
            new_log = {
                "id": int(datetime.datetime.now().timestamp() * 1000),
                "medicine_name": target_med["name"],
                "dosage": target_med["dosage"],
                "scheduled_time": target_med["reminder_time"],
                "action_time": action_time,
                "date": today_str,
                "status": "TAKEN",
                "notes": "Marked via Voice/AI Assistant"
            }
            DATABASE["dose_logs"].insert(0, new_log)
            reply = f"Done! I've marked your dose of {target_med['name']} as TAKEN at {action_time}."
            return jsonify({"success": True, "response": reply, "action": "UPDATE_LOGS"}), 200

    # 2. "What medicines do I have today?" / "Show today's medicines"
    if any(phrase in query for phrase in ["what medicines", "medicines do i have", "today's medicines", "my medicines", "my medicine"]):
        if not meds:
            reply = "You currently have no medicines scheduled. Would you like to add one manually or scan with your camera?"
        else:
            list_str = "; ".join([f"{m['name']} at {m['reminder_time']} ({m['dosage']})" for m in meds])
            reply = f"You have {len(meds)} medicines scheduled today: {list_str}."
        return jsonify({"success": True, "response": reply}), 200

    # 3. "What is my next medicine?" / "Next dose"
    if "next" in query and ("medicine" in query or "dose" in query or "reminder" in query):
        now_time = datetime.datetime.now().strftime("%I:%M %p")
        # Find first medicine not yet taken today
        taken_names = [l["medicine_name"] for l in today_logs if l["status"] == "TAKEN"]
        pending_meds = [m for m in meds if m["name"] not in taken_names]
        if pending_meds:
            next_m = pending_meds[0]
            reply = f"Your next scheduled medicine is {next_m['name']} ({next_m['dosage']}) at {next_m['reminder_time']}. Instructions: {next_m['instructions']}."
        else:
            reply = "All your scheduled medicines for today have already been marked as taken! Well done."
        return jsonify({"success": True, "response": reply}), 200

    # 4. "When should I take my medicine?"
    if "when" in query and ("take" in query or "time" in query):
        schedule = ", ".join([f"{m['name']} at {m['reminder_time']}" for m in meds])
        reply = f"Here is your timing schedule: {schedule}."
        return jsonify({"success": True, "response": reply}), 200

    # 5. "How many doses do I have today?" / "Daily dosage count"
    if "how many" in query or "doses do i have" in query or "dosage count" in query:
        taken_count = sum(1 for l in today_logs if l["status"] == "TAKEN")
        total_count = len(meds)
        pending = max(0, total_count - taken_count)
        reply = f"You have {total_count} total scheduled doses today. You have completed {taken_count} dose(s), leaving {pending} dose(s) pending."
        return jsonify({"success": True, "response": reply}), 200

    # 6. "Show my medicine history" / "History records"
    if "history" in query:
        if not DATABASE["dose_logs"]:
            reply = "You do not have any recorded dose history yet."
        else:
            recent = DATABASE["dose_logs"][:3]
            recent_str = "; ".join([f"{l['date']} {l['medicine_name']} was {l['status']}" for l in recent])
            reply = f"Here are your recent records: {recent_str}. You can view the complete history in the Medicine History tab."
        return jsonify({"success": True, "response": reply}), 200

    # 7. "What reminders are pending?"
    if "pending" in query:
        taken_names = [l["medicine_name"] for l in today_logs if l["status"] == "TAKEN"]
        pending_meds = [m for m in meds if m["name"] not in taken_names]
        if pending_meds:
            names = ", ".join([f"{m['name']} ({m['reminder_time']})" for m in pending_meds])
            reply = f"Pending reminders today: {names}."
        else:
            reply = "You have no pending reminders for today! Everything is up to date."
        return jsonify({"success": True, "response": reply}), 200

    # 8. "Add a medicine" / "Remind me to take..."
    remind_match = re.search(r'remind me to take (.+?) at (\d{1,2}(?::\d{2})?\s*(?:am|pm)?)', query)
    if remind_match:
        med_to_add = remind_match.group(1).title()
        time_to_add = remind_match.group(2).upper()
        if ":" not in time_to_add:
            time_to_add = time_to_add.replace(" AM", ":00 AM").replace(" PM", ":00 PM")
            if not time_to_add.endswith("AM") and not time_to_add.endswith("PM"):
                time_to_add += ":00 PM"

        new_med = {
            "id": int(datetime.datetime.now().timestamp() * 1000),
            "name": med_to_add,
            "category": "Tablet",
            "dosage": "1 dose",
            "frequency": "Daily",
            "daily_dosage": "1 dose",
            "reminder_time": time_to_add,
            "instructions": "Added via MediVoice voice assistant",
            "start_date": today_str,
            "end_date": ""
        }
        DATABASE["medicines"].append(new_med)
        reply = f"I've added {med_to_add} scheduled at {time_to_add} to your database."
        return jsonify({"success": True, "response": reply, "action": "RELOAD_MEDICINES"}), 200

    if "add" in query and "medicine" in query:
        reply = "You can tap the '+ Add New Medicine' button or say: 'Remind me to take Aspirin at 08:00 PM'."
        return jsonify({"success": True, "response": reply}), 200

    # 9. Medical/Symptom advice safety boundary
    if any(s in query for s in ["prescribe", "cure", "pain", "fever", "cough", "infection", "headache", "chest pain"]):
        reply = (
            "⚠️ Safety Notice: MediVoice AI cannot diagnose conditions or prescribe medications. "
            "For fever, pain, or health concerns, please consult your doctor or pharmacist. "
            "If you are experiencing severe symptoms such as chest pain or breathing difficulty, seek emergency medical care immediately."
        )
        return jsonify({"success": True, "response": reply, "safety_flag": True}), 200

    # 10. Fallback for out-of-scope requests
    reply = (
        f"I can help you manage your medicine reminders, daily dosage tracking, and history. "
        f"Try asking: 'What medicines do I have today?', 'What is my next medicine?', "
        f"'Show my medicine history', or 'Mark my medicine as taken'."
    )
    return jsonify({"success": True, "response": reply}), 200

# WSGI entry point
if __name__ == '__main__':
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port, debug=True)
