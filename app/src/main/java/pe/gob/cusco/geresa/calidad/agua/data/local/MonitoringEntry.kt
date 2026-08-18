package pe.gob.cusco.geresa.calidad.agua.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitoring_entries")
data class MonitoringEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val uuid: String = "",
    val isSynced: Boolean = false,
    val fechaRegistro: String = "",
    
    // Identificación
    val nombreIpress: String = "",
    val codigoRenipress: String = "",
    val unidadEjecutora: String = "",
    
    // Datos Técnicos de Monitoreo
    val cloro: String = "",
    val temperatura: String = "",
    val ph: String = "",
    val turbiedad: String = "",
    val conductividad: String = ""
)
