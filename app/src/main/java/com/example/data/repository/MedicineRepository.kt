package com.example.data.repository

import com.example.data.local.DoseLogDao
import com.example.data.local.MedicineDao
import com.example.data.local.UserProfileDao
import com.example.data.model.DoseLogEntity
import com.example.data.model.MedicineEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicineRepository(
    private val medicineDao: MedicineDao,
    private val doseLogDao: DoseLogDao,
    private val userProfileDao: UserProfileDao
) {
    val allMedicines: Flow<List<MedicineEntity>> = medicineDao.getAllMedicines()
    val activeMedicines: Flow<List<MedicineEntity>> = medicineDao.getActiveMedicines()
    val allLogs: Flow<List<DoseLogEntity>> = doseLogDao.getAllLogs()
    val currentUserFlow: Flow<UserProfileEntity?> = userProfileDao.getCurrentUserFlow()

    fun getLogsForDate(dateStr: String): Flow<List<DoseLogEntity>> = doseLogDao.getLogsForDate(dateStr)

    suspend fun insertMedicine(medicine: MedicineEntity): Long {
        return medicineDao.insertMedicine(medicine)
    }

    suspend fun updateMedicine(medicine: MedicineEntity) {
        medicineDao.updateMedicine(medicine)
    }

    suspend fun deleteMedicine(medicine: MedicineEntity) {
        medicineDao.deleteMedicine(medicine)
    }

    suspend fun deleteMedicineById(id: Long) {
        medicineDao.deleteMedicineById(id)
    }

    suspend fun logDoseAction(medicine: MedicineEntity, status: String, scheduledTime: String = medicine.reminderTime) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        
        // Remove prior log for this medicine today to allow re-marking if user changed mind
        doseLogDao.deleteLogForMedicineOnDate(medicine.id, today)
        
        val log = DoseLogEntity(
            medicineId = medicine.id,
            medicineName = medicine.name,
            dosage = medicine.dosage,
            scheduledTime = scheduledTime,
            actionTime = nowTime,
            dateStr = today,
            status = status
        )
        doseLogDao.insertLog(log)
    }

    suspend fun getTodayLogForMedicine(medicineId: Long): DoseLogEntity? {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return doseLogDao.getLogForMedicineOnDate(medicineId, today)
    }

    suspend fun saveUser(email: String, fullName: String) {
        userProfileDao.saveUser(
            UserProfileEntity(
                email = email,
                fullName = fullName,
                isLoggedIn = true
            )
        )
    }

    suspend fun logout() {
        userProfileDao.clearUser()
    }
}
