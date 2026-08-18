package pe.gob.cusco.geresa.calidad.agua.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pe.gob.cusco.geresa.calidad.agua.data.local.AppDatabase
import pe.gob.cusco.geresa.calidad.agua.data.remote.ApiService
import pe.gob.cusco.geresa.calidad.agua.data.remote.SyncRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbyNsRr6X3lMVZKFQ7i371hkmvnkLDY1C7T1Yt236JNyz2FHKjQMMrD2bvzqeXNPF4xg/exec"

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.registerDao()
        val client = okhttp3.OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS).build()
        val api = Retrofit.Builder().baseUrl("https://script.google.com/").client(client).addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)

        var allSynced = true

        // 1. Sync Diagnóstico
        dao.getUnsyncedEntries().forEach { entry ->
            try {
                val response = api.syncEntry(SCRIPT_URL, SyncRequest(
                    tipo = "diagnostico", uuid = entry.uuid, fechaRegistro = entry.fechaRegistro,
                    nombreIpress = entry.nombreIpress, codigoRenipress = entry.codigoRenipress, unidadEjecutora = entry.unidadEjecutora,
                    provincia = entry.provincia, distrito = entry.distrito, centroPoblado = entry.centroPoblado, ubigeo = entry.ubigeo,
                    latitud = entry.latitud, longitud = entry.longitud, altitud = entry.altitud, precision = entry.precision,
                    aguaPropio = entry.aguaPropio, fuenteAgua = entry.fuenteAgua, bombasAgua = entry.bombasAgua, bombasOperativas = entry.bombasOperativas,
                    reservorio = entry.reservorio, reservorioElevado = entry.reservorioElevado, reservorioOperativo = entry.reservorioOperativo,
                    tratamientoAgua = entry.tratamientoAgua, observaciones = entry.observaciones, responsable = entry.responsable, dni = entry.dni, firma = entry.firma
                ))
                if (response.isSuccessful) dao.markAsSynced(entry.id) else allSynced = false
            } catch (e: Exception) { allSynced = false }
        }

        // 2. Sync Monitoreo
        dao.getUnsyncedMonitoring().forEach { entry ->
            try {
                val response = api.syncEntry(SCRIPT_URL, SyncRequest(
                    tipo = "monitoreo", uuid = entry.uuid, fechaRegistro = entry.fechaRegistro,
                    nombreIpress = entry.nombreIpress, codigoRenipress = entry.codigoRenipress, unidadEjecutora = entry.unidadEjecutora,
                    cloro = entry.cloro, temperatura = entry.temperatura, ph = entry.ph, turbiedad = entry.turbiedad, conductividad = entry.conductividad
                ))
                if (response.isSuccessful) dao.markMonitoringAsSynced(entry.id) else allSynced = false
            } catch (e: Exception) { allSynced = false }
        }

        return if (allSynced) Result.success() else Result.retry()
    }
}
