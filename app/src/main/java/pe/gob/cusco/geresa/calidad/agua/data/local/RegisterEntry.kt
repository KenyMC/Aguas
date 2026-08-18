package pe.gob.cusco.geresa.calidad.agua.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "register_entries")
data class RegisterEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val uuid: String = "",
    val isSynced: Boolean = false,
    val fechaRegistro: String = "",
    
    // Identificación
    val nombreIpress: String = "",
    val codigoRenipress: String = "",
    val unidadEjecutora: String = "",
    
    // Ubicación
    val provincia: String = "",
    val distrito: String = "",
    val centroPoblado: String = "",
    val ubigeo: String = "",
    
    // Georeferencia
    val latitud: String = "",
    val longitud: String = "",
    val altitud: String = "",
    val precision: String = "",
    
    // Sistema de Agua
    val aguaPropio: String = "", // Si/No
    val fuenteAgua: String = "",
    val bombasAgua: String = "", // Si/No
    val bombasOperativas: String = "", // Si/No
    val reservorio: String = "", // Si/No
    val reservorioElevado: String = "", // Si/No
    val reservorioOperativo: String = "", // Si/No
    val tratamientoAgua: String = "", // Si/No
    
    // Finalización
    val observaciones: String = "",
    val responsable: String = "",
    val dni: String = "",
    val firma: String = "" // Base64
)
