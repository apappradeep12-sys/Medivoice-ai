package com.example.service

import com.example.data.model.DoseLogEntity
import com.example.data.model.MedicineEntity
import java.util.Locale

data class AssistantMessage(
    val id: String = System.currentTimeMillis().toString(),
    val sender: AssistantSender,
    val text: String,
    val timestamp: String,
    val actionType: AssistantActionType? = null,
    val actionPayload: String? = null
)

enum class AssistantSender {
    USER,
    ASSISTANT
}

enum class AssistantActionType {
    NAVIGATE_ADD,
    NAVIGATE_LIST,
    NAVIGATE_HISTORY,
    NAVIGATE_SYMPTOMS,
    MARK_TAKEN
}

object VoiceAssistantProcessor {

    fun processQuery(
        query: String,
        medicines: List<MedicineEntity>,
        todayLogs: List<DoseLogEntity>
    ): AssistantResponse {
        val lower = query.trim().lowercase(Locale.ROOT)

        // Check reminders / today medicines
        if (lower.contains("reminder") || lower.contains("today") || lower.contains("what medicine") || lower.contains("schedule")) {
            val count = medicines.size
            if (count == 0) {
                return AssistantResponse(
                    speech = "You do not have any active medicines scheduled for today. Would you like to add one?",
                    displayText = "You currently have no active medicines scheduled for today.",
                    actionType = AssistantActionType.NAVIGATE_ADD,
                    actionPayload = "Add Medicine"
                )
            }
            val names = medicines.joinToString(", ") { "${it.name} at ${it.reminderTime}" }
            return AssistantResponse(
                speech = "You have $count medicine reminders today: $names.",
                displayText = "Today's Schedule (${medicines.size} items):\n" +
                        medicines.joinToString("\n") { "• ${it.name} (${it.dosage}) - ${it.reminderTime}" },
                actionType = AssistantActionType.NAVIGATE_LIST,
                actionPayload = "View Schedule"
            )
        }

        // Dosage check / Did I take
        if (lower.contains("dosage") || lower.contains("did i take") || lower.contains("taken") || lower.contains("how many")) {
            val takenCount = todayLogs.count { it.status.equals("TAKEN", ignoreCase = true) }
            val total = medicines.size
            val speech = "Today you have taken $takenCount out of $total prescribed doses."
            return AssistantResponse(
                speech = speech,
                displayText = "Daily Dosage Tracking:\n• Doses Taken: $takenCount\n• Total Prescribed: $total\n• Pending: ${maxOf(0, total - takenCount)}",
                actionType = AssistantActionType.NAVIGATE_HISTORY,
                actionPayload = "View History"
            )
        }

        // History
        if (lower.contains("history") || lower.contains("log") || lower.contains("past")) {
            val recentLogs = todayLogs.take(3)
            val logText = if (recentLogs.isEmpty()) {
                "No dose actions recorded yet today."
            } else {
                recentLogs.joinToString("\n") { "• ${it.medicineName}: ${it.status} at ${it.actionTime}" }
            }
            return AssistantResponse(
                speech = "Here is your medicine history for today. You have ${todayLogs.size} logs recorded.",
                displayText = "Today's Medicine Logs:\n$logText",
                actionType = AssistantActionType.NAVIGATE_HISTORY,
                actionPayload = "Full History"
            )
        }

        // Add medicine
        if (lower.contains("add") || lower.contains("new medicine") || lower.contains("prescribe")) {
            return AssistantResponse(
                speech = "I can help you add a new medicine. You can also scan your medicine strip using the camera scanner.",
                displayText = "Opening Add Medicine. You can type manually or use the camera scanner to auto-fill details from your medicine strip.",
                actionType = AssistantActionType.NAVIGATE_ADD,
                actionPayload = "Open Scanner"
            )
        }

        // Symptom suggestions
        if (lower.contains("headache") || lower.contains("fever") || lower.contains("cough") ||
            lower.contains("symptom") || lower.contains("pain") || lower.contains("cold") || lower.contains("suggest")
        ) {
            val symptom = when {
                lower.contains("headache") -> "headache"
                lower.contains("fever") -> "fever"
                lower.contains("cough") -> "cough"
                lower.contains("cold") -> "cold & congestion"
                else -> "general symptoms"
            }
            return AssistantResponse(
                speech = "For $symptom, common OTC options include Paracetamol or Ibuprofen. Please note: This is informational only. Always consult a certified physician or pharmacist before taking medications.",
                displayText = "⚠️ Clinical Disclaimer: The following are safe common OTC references. Always consult a healthcare professional:\n• Paracetamol (500-650mg) for fever/pain\n• Ibuprofen (200-400mg with food)\n• Stay hydrated and rest",
                actionType = AssistantActionType.NAVIGATE_SYMPTOMS,
                actionPayload = "Explore Symptom Guide"
            )
        }

        // Medicine info queries
        if (lower.contains("paracetamol") || lower.contains("amoxicillin") || lower.contains("metformin") || lower.contains("info")) {
            val match = medicines.firstOrNull { lower.contains(it.name.lowercase(Locale.ROOT)) }
            if (match != null) {
                return AssistantResponse(
                    speech = "Information for ${match.name}: Category is ${match.category}, dosage is ${match.dosage}. Instructions: ${match.instructions}.",
                    displayText = "Medicine Info:\n• Name: ${match.name}\n• Category: ${match.category}\n• Dosage: ${match.dosage}\n• Timing: ${match.reminderTime}\n• Advice: ${match.instructions}",
                    actionType = AssistantActionType.NAVIGATE_LIST,
                    actionPayload = "Details"
                )
            }
        }

        // Default greeting / help
        return AssistantResponse(
            speech = "I am MediVoice AI, your medicine assistant. You can ask me to check your reminders, add a medicine, track daily dosage, or ask for symptom advice.",
            displayText = "Hello! I am MediVoice AI. Try saying:\n• 'What medicines do I take today?'\n• 'Did I take my morning dosage?'\n• 'Add a new medicine with camera'\n• 'Suggest medicine for headache'",
            actionType = null,
            actionPayload = null
        )
    }
}

data class AssistantResponse(
    val speech: String,
    val displayText: String,
    val actionType: AssistantActionType?,
    val actionPayload: String?
)
