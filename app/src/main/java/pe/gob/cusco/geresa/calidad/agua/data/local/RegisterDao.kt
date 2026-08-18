package pe.gob.cusco.geresa.calidad.agua.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RegisterDao {
    // Diagnóstico
    @Insert
    suspend fun insert(entry: RegisterEntry): Long
    @Update
    suspend fun update(entry: RegisterEntry): Int
    @Delete
    suspend fun delete(entry: RegisterEntry): Int
    @Query("SELECT * FROM register_entries WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<RegisterEntry>
    @Query("UPDATE register_entries SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int): Int
    @Query("SELECT * FROM register_entries ORDER BY id DESC")
    fun getAllEntries(): Flow<List<RegisterEntry>>

    // Monitoreo
    @Insert
    suspend fun insertMonitoring(entry: MonitoringEntry): Long
    @Update
    suspend fun updateMonitoring(entry: MonitoringEntry): Int
    @Delete
    suspend fun deleteMonitoring(entry: MonitoringEntry): Int
    @Query("SELECT * FROM monitoring_entries WHERE isSynced = 0")
    suspend fun getUnsyncedMonitoring(): List<MonitoringEntry>
    @Query("UPDATE monitoring_entries SET isSynced = 1 WHERE id = :id")
    suspend fun markMonitoringAsSynced(id: Int): Int
    @Query("SELECT * FROM monitoring_entries ORDER BY id DESC")
    fun getAllMonitoring(): Flow<List<MonitoringEntry>>
}

