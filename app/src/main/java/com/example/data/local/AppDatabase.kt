package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DoseLogEntity
import com.example.data.model.MedicineEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [MedicineEntity::class, DoseLogEntity::class, UserProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medivoice_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        prepopulateData(database)
                    }
                }
            }
        }

        private suspend fun prepopulateData(database: AppDatabase) {
            val medicineDao = database.medicineDao()
            val doseLogDao = database.doseLogDao()
            val userProfileDao = database.userProfileDao()

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val m1 = MedicineEntity(
                name = "Amoxicillin 500mg",
                category = "Capsule",
                dosage = "1 capsule (500mg)",
                frequency = "Twice Daily",
                reminderTime = "08:00 AM",
                startDate = today,
                endDate = "2026-09-20",
                instructions = "Take after breakfast with water"
            )
            val m2 = MedicineEntity(
                name = "Paracetamol 650mg",
                category = "Tablet",
                dosage = "1 tablet",
                frequency = "As Needed",
                reminderTime = "01:00 PM",
                startDate = today,
                endDate = "2026-09-15",
                instructions = "Take after lunch for headache/fever"
            )
            val m3 = MedicineEntity(
                name = "Metformin 500mg",
                category = "Tablet",
                dosage = "1 tablet",
                frequency = "Once Daily",
                reminderTime = "08:00 PM",
                startDate = today,
                endDate = "2026-12-31",
                instructions = "Take with dinner"
            )
            val m4 = MedicineEntity(
                name = "Vitamin D3 60,000 IU",
                category = "Capsule",
                dosage = "1 capsule",
                frequency = "Once Weekly",
                reminderTime = "09:30 AM",
                startDate = today,
                endDate = "2026-10-30",
                instructions = "Take with milk on Sundays"
            )

            val id1 = medicineDao.insertMedicine(m1)
            val id2 = medicineDao.insertMedicine(m2)
            medicineDao.insertMedicine(m3)
            medicineDao.insertMedicine(m4)

            // Add one historical taken log for today
            doseLogDao.insertLog(
                DoseLogEntity(
                    medicineId = id1,
                    medicineName = "Amoxicillin 500mg",
                    dosage = "1 capsule (500mg)",
                    scheduledTime = "08:00 AM",
                    actionTime = "08:05 AM",
                    dateStr = today,
                    status = "TAKEN"
                )
            )

            // Default logged-in user profile
            userProfileDao.saveUser(
                UserProfileEntity(
                    email = "patient@medivoice.ai",
                    fullName = "Alex Mercer",
                    isLoggedIn = true,
                    emergencyContact = "+1 555-0199"
                )
            )
        }
    }
}
