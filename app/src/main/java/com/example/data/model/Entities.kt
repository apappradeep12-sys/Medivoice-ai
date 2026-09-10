package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MedicineCategory(val displayName: String, val iconName: String) {
    TABLET("Tablet", "pill"),
    CAPSULE("Capsule", "capsule"),
    SYRUP("Syrup", "liquid"),
    INJECTION("Injection", "needle"),
    DROPS("Drops", "water_drop"),
    INHALER("Inhaler", "air"),
    OINTMENT("Ointment", "cream"),
    OTHER("Other", "medication");

    companion object {
        fun fromString(value: String): MedicineCategory {
            return entries.firstOrNull { it.displayName.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) } ?: TABLET
        }
    }
}

enum class DoseStatus(val label: String) {
    PENDING("Pending"),
    TAKEN("Taken"),
    SKIPPED("Skipped"),
    SNOOZED("Snoozed")
}

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "Tablet",
    val dosage: String = "1 tablet",
    val frequency: String = "Once Daily",
    val reminderTime: String = "08:00 AM", // Normal AM/PM format
    val startDate: String = "",
    val endDate: String = "",
    val instructions: String = "Take after meal",
    val imageUri: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicineId: Long,
    val medicineName: String,
    val dosage: String,
    val scheduledTime: String,
    val actionTime: String,
    val dateStr: String,
    val status: String, // TAKEN, SKIPPED, SNOOZED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val email: String,
    val fullName: String,
    val isLoggedIn: Boolean = true,
    val emergencyContact: String = ""
)
