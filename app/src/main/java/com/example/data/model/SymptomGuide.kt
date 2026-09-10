package com.example.data.model

data class SymptomSuggestion(
    val id: String,
    val symptomName: String,
    val iconName: String,
    val description: String,
    val safeOtcMedicines: List<OtcMedicineOption>,
    val homeCareTips: List<String>,
    val whenToSeeDoctor: List<String>
)

data class OtcMedicineOption(
    val genericName: String,
    val brandExamples: String,
    val typicalDosage: String,
    val category: String,
    val recommendedFrequency: String,
    val defaultReminderTime: String,
    val precautions: String
)

object SymptomData {
    val disclaimerText = "⚠️ CRITICAL MEDICAL WARNING: The information provided here is strictly for general educational guidance and symptom assessment. It does NOT constitute medical diagnosis or formal prescription. Always consult a certified physician, licensed doctor, or registered pharmacist before starting any new medication, especially if you have underlying conditions, allergies, or are pregnant."

    val suggestions = listOf(
        SymptomSuggestion(
            id = "headache",
            symptomName = "Headache & Migraine",
            iconName = "psychology",
            description = "Tension headache, mild migraine, or stress-related cranial pressure.",
            safeOtcMedicines = listOf(
                OtcMedicineOption(
                    genericName = "Paracetamol (Acetaminophen)",
                    brandExamples = "Tylenol, Dolo 650, Panadol",
                    typicalDosage = "500 mg - 650 mg",
                    category = "Tablet",
                    recommendedFrequency = "Every 6-8 hours as needed",
                    defaultReminderTime = "01:00 PM",
                    precautions = "Do not exceed 3,000 mg in 24 hours. Avoid alcohol to protect liver."
                ),
                OtcMedicineOption(
                    genericName = "Ibuprofen",
                    brandExamples = "Advil, Motrin, Brufen",
                    typicalDosage = "400 mg",
                    category = "Tablet",
                    recommendedFrequency = "Twice Daily with food",
                    defaultReminderTime = "02:00 PM",
                    precautions = "Always take with food or milk to prevent stomach irritation."
                )
            ),
            homeCareTips = listOf(
                "Drink a tall glass of cool water to rule out dehydration",
                "Rest in a quiet, dark room for 20-30 minutes",
                "Apply a cold or warm compress to forehead or neck"
            ),
            whenToSeeDoctor = listOf(
                "Sudden, thunderclap onset ('worst headache of life')",
                "Accompanied by stiff neck, fever, or confusion",
                "Vision changes, slurred speech, or weakness"
            )
        ),
        SymptomSuggestion(
            id = "fever",
            symptomName = "Fever & Chills",
            iconName = "thermostat",
            description = "Elevated body temperature, malaise, mild body aches.",
            safeOtcMedicines = listOf(
                OtcMedicineOption(
                    genericName = "Paracetamol",
                    brandExamples = "Calpol, Dolo, Crocin",
                    typicalDosage = "650 mg",
                    category = "Tablet",
                    recommendedFrequency = "Every 6 hours as needed",
                    defaultReminderTime = "12:00 PM",
                    precautions = "Stay well hydrated. Monitor temperature with a thermometer."
                ),
                OtcMedicineOption(
                    genericName = "Oral Rehydration Salts (ORS)",
                    brandExamples = "Electral, Hydralyte",
                    typicalDosage = "200 ml solution",
                    category = "Other",
                    recommendedFrequency = "As Needed",
                    defaultReminderTime = "03:00 PM",
                    precautions = "Mix in clean drinking water; drink periodically."
                )
            ),
            homeCareTips = listOf(
                "Wear light, breathable clothing",
                "Rest adequately and drink plenty of fluids",
                "Lukewarm sponge baths (avoid ice-cold water)"
            ),
            whenToSeeDoctor = listOf(
                "Fever exceeds 103°F (39.4°C) or persists for > 3 days",
                "Accompanied by rash, difficulty breathing, or severe lethargy",
                "Fever in infants under 3 months of age"
            )
        ),
        SymptomSuggestion(
            id = "cough",
            symptomName = "Cough & Cold",
            iconName = "masks",
            description = "Dry irritating cough, chest congestion, runny nose.",
            safeOtcMedicines = listOf(
                OtcMedicineOption(
                    genericName = "Dextromethorphan Syrup",
                    brandExamples = "Robitussin, Benylin, Ascoril-D",
                    typicalDosage = "10 ml (2 teaspoons)",
                    category = "Syrup",
                    recommendedFrequency = "Thrice Daily",
                    defaultReminderTime = "08:00 AM",
                    precautions = "Best for dry, non-productive cough. May cause slight drowsiness."
                ),
                OtcMedicineOption(
                    genericName = "Cetirizine 10mg",
                    brandExamples = "Zyrtec, Cetzine, Alerid",
                    typicalDosage = "10 mg (1 tablet)",
                    category = "Tablet",
                    recommendedFrequency = "Once Daily at bedtime",
                    defaultReminderTime = "09:30 PM",
                    precautions = "Relieves sneezing and runny nose. Take before sleep."
                )
            ),
            homeCareTips = listOf(
                "Steam inhalation with eucalyptus or warm mint water",
                "Warm honey and lemon water (avoid honey in infants < 1 yr)",
                "Elevate head with an extra pillow while sleeping"
            ),
            whenToSeeDoctor = listOf(
                "Coughing up blood or rust-colored phlegm",
                "Shortness of breath or audible wheezing",
                "Cough lasting longer than 2-3 weeks"
            )
        ),
        SymptomSuggestion(
            id = "acid_reflux",
            symptomName = "Acidity & Heartburn",
            iconName = "local_fire_department",
            description = "Burning chest sensation, sour burping, acid regurgitation.",
            safeOtcMedicines = listOf(
                OtcMedicineOption(
                    genericName = "Omeprazole / Pantoprazole",
                    brandExamples = "Prilosec, Pan 40, Omez",
                    typicalDosage = "20 mg - 40 mg",
                    category = "Capsule",
                    recommendedFrequency = "Once Daily before breakfast",
                    defaultReminderTime = "07:30 AM",
                    precautions = "Must be taken on an empty stomach, 30 minutes before first meal."
                ),
                OtcMedicineOption(
                    genericName = "Antacid Suspension",
                    brandExamples = "Gelusil, Digene, Mylanta",
                    typicalDosage = "10 ml after meals",
                    category = "Syrup",
                    recommendedFrequency = "As Needed after meals",
                    defaultReminderTime = "02:00 PM",
                    precautions = "Provides quick buffering. Do not take with other meds simultaneously."
                )
            ),
            homeCareTips = listOf(
                "Avoid lying down for at least 2 hours after meals",
                "Reduce spicy, oily, caffeinated, and carbonated beverages",
                "Eat smaller, more frequent meals"
            ),
            whenToSeeDoctor = listOf(
                "Difficulty swallowing or food getting stuck in throat",
                "Black or tarry stools, or vomiting blood",
                "Chest pain that radiates to arm, shoulder, or jaw"
            )
        ),
        SymptomSuggestion(
            id = "allergies",
            symptomName = "Allergies & Hives",
            iconName = "grain",
            description = "Itchy eyes, sneezing, skin hives, allergic rhinitis.",
            safeOtcMedicines = listOf(
                OtcMedicineOption(
                    genericName = "Fexofenadine 120mg",
                    brandExamples = "Allegra, Telfast",
                    typicalDosage = "120 mg",
                    category = "Tablet",
                    recommendedFrequency = "Once Daily",
                    defaultReminderTime = "09:00 AM",
                    precautions = "Non-drowsy 24-hour antihistamine. Take with water, avoid fruit juices."
                ),
                OtcMedicineOption(
                    genericName = "Calamine Lotion",
                    brandExamples = "Caladryl, Lacto Calamine",
                    typicalDosage = "Topical application",
                    category = "Ointment",
                    recommendedFrequency = "Twice Daily as needed",
                    defaultReminderTime = "10:00 AM",
                    precautions = "For external skin use only. Soothes itching and redness."
                )
            ),
            homeCareTips = listOf(
                "Wash face and hands immediately after outdoor exposure",
                "Use a saline nasal spray to clear allergens",
                "Keep windows closed during high pollen counts"
            ),
            whenToSeeDoctor = listOf(
                "Swelling of lips, tongue, face, or throat (Emergency: Call 911/EMS)",
                "Wheezing or difficulty breathing (Anaphylaxis)",
                "Widespread blistering rash"
            )
        )
    )
}
