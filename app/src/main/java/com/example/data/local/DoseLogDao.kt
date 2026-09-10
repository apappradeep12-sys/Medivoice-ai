package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DoseLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE dateStr = :dateStr ORDER BY timestamp DESC")
    fun getLogsForDate(dateStr: String): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE medicineId = :medicineId AND dateStr = :dateStr LIMIT 1")
    suspend fun getLogForMedicineOnDate(medicineId: Long, dateStr: String): DoseLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DoseLogEntity): Long

    @Query("DELETE FROM dose_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM dose_logs WHERE medicineId = :medicineId AND dateStr = :dateStr")
    suspend fun deleteLogForMedicineOnDate(medicineId: Long, dateStr: String)
}
