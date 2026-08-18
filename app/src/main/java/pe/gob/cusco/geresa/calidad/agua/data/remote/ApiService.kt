package pe.gob.cusco.geresa.calidad.agua.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

data class SyncRequest(
    val tipo: String, // "diagnostico" o "monitoreo"
    val uuid: String,
    val fechaRegistro: String,
    val nombreIpress: String,
    val codigoRenipress: String,
    val unidadEjecutora: String,
    
    // Campos Diagnóstico
    val provincia: String? = null,
    val distrito: String? = null,
    val centroPoblado: String? = null,
    val ubigeo: String? = null,
    val latitud: String? = null,
    val longitud: String? = null,
    val altitud: String? = null,
    val precision: String? = null,
    val aguaPropio: String? = null,
    val fuenteAgua: String? = null,
    val bombasAgua: String? = null,
    val bombasOperativas: String? = null,
    val reservorio: String? = null,
    val reservorioElevado: String? = null,
    val reservorioOperativo: String? = null,
    val tratamientoAgua: String? = null,
    val observaciones: String? = null,
    val responsable: String? = null,
    val dni: String? = null,
    val firma: String? = null,
    
    // Campos Monitoreo
    val cloro: String? = null,
    val temperatura: String? = null,
    val ph: String? = null,
    val turbiedad: String? = null,
    val conductividad: String? = null
)

interface ApiService {
    @POST
    suspend fun syncEntry(
        @Url url: String,
        @Body request: SyncRequest
    ): Response<ResponseBody>
}
