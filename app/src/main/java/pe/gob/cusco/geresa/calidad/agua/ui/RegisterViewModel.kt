package pe.gob.cusco.geresa.calidad.agua.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import pe.gob.cusco.geresa.calidad.agua.data.local.AppDatabase
import pe.gob.cusco.geresa.calidad.agua.data.local.RegisterEntry
import pe.gob.cusco.geresa.calidad.agua.data.local.MonitoringEntry
import pe.gob.cusco.geresa.calidad.agua.data.remote.ApiService
import pe.gob.cusco.geresa.calidad.agua.data.remote.SyncRequest
import pe.gob.cusco.geresa.calidad.agua.worker.SyncWorker
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen {
    object List : Screen()
    data class Form(val entry: RegisterEntry? = null) : Screen()
    data class MonitoringForm(val entry: MonitoringEntry? = null) : Screen()
}

enum class Module { DIAGNOSTICO, MONITOREO }

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.registerDao()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val prefs = application.getSharedPreferences("aguas_prefs", Context.MODE_PRIVATE)

    private val _syncStatus = mutableStateOf("Listo")
    val syncStatus: State<String> = _syncStatus

    private val _isAccepted = mutableStateOf(prefs.getBoolean("accepted_terms", false))
    val isAccepted: State<Boolean> = _isAccepted

    private val _currentScreen = mutableStateOf<Screen>(Screen.List)
    val currentScreen: State<Screen> = _currentScreen

    private val _currentModule = mutableStateOf(Module.DIAGNOSTICO)
    val currentModule: State<Module> = _currentModule

    // URL final del Script Inteligente que maneja Diagnóstico y Monitoreo
    private val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbyNsRr6X3lMVZKFQ7i371hkmvnkLDY1C7T1Yt236JNyz2FHKjQMMrD2bvzqeXNPF4xg/exec".trim()

    val allEntries: StateFlow<List<RegisterEntry>> = dao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMonitoring: StateFlow<List<MonitoringEntry>> = dao.getAllMonitoring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setModule(module: Module) {
        _currentModule.value = module
        _currentScreen.value = Screen.List
    }

    fun navigateTo(screen: Screen) { _currentScreen.value = screen }

    fun acceptTerms() {
        _isAccepted.value = true
        prefs.edit().putBoolean("accepted_terms", true).apply()
    }

    fun saveEntry(entry: RegisterEntry) {
        viewModelScope.launch {
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val datePart = if (entry.fechaRegistro.length >= 10) entry.fechaRegistro.substring(0, 10) else entry.fechaRegistro
            val finalEntry = entry.copy(
                fechaRegistro = "$datePart $timeStr",
                uuid = if (entry.uuid.isBlank()) UUID.randomUUID().toString() else entry.uuid,
                isSynced = false
            )
            if (finalEntry.id == 0) dao.insert(finalEntry) else dao.update(finalEntry)
            _syncStatus.value = "Guardado local."
            _currentScreen.value = Screen.List
            attemptImmediateSync()
            scheduleSync()
        }
    }

    fun saveMonitoring(entry: MonitoringEntry) {
        viewModelScope.launch {
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val datePart = if (entry.fechaRegistro.length >= 10) entry.fechaRegistro.substring(0, 10) else entry.fechaRegistro
            val finalEntry = entry.copy(
                fechaRegistro = "$datePart $timeStr",
                uuid = if (entry.uuid.isBlank()) UUID.randomUUID().toString() else entry.uuid,
                isSynced = false
            )
            if (finalEntry.id == 0) dao.insertMonitoring(finalEntry) else dao.updateMonitoring(finalEntry)
            _syncStatus.value = "Guardado local."
            _currentScreen.value = Screen.List
            attemptImmediateSync()
            scheduleSync()
        }
    }

    fun deleteEntry(entry: RegisterEntry) { viewModelScope.launch { dao.delete(entry); _currentScreen.value = Screen.List } }
    fun deleteMonitoring(entry: MonitoringEntry) { viewModelScope.launch { dao.deleteMonitoring(entry); _currentScreen.value = Screen.List } }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): android.location.Location? {
        return try { fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await() } catch (e: Exception) { null }
    }

    private suspend fun attemptImmediateSync() {
        withContext(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder()
                .followRedirects(true).followSslRedirects(true)
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val retrofit = Retrofit.Builder().baseUrl("https://script.google.com/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
            val apiService = retrofit.create(ApiService::class.java)

            // Sync Diagnóstico
            dao.getUnsyncedEntries().forEach { entry ->
                try {
                    val response = apiService.syncEntry(SCRIPT_URL, SyncRequest(
                        tipo = "diagnostico", uuid = entry.uuid, fechaRegistro = entry.fechaRegistro,
                        nombreIpress = entry.nombreIpress, codigoRenipress = entry.codigoRenipress, unidadEjecutora = entry.unidadEjecutora,
                        provincia = entry.provincia, distrito = entry.distrito, centroPoblado = entry.centroPoblado, ubigeo = entry.ubigeo,
                        latitud = entry.latitud, longitud = entry.longitud, altitud = entry.altitud, precision = entry.precision,
                        aguaPropio = entry.aguaPropio, fuenteAgua = entry.fuenteAgua, bombasAgua = entry.bombasAgua, bombasOperativas = entry.bombasOperativas,
                        reservorio = entry.reservorio, reservorioElevado = entry.reservorioElevado, reservorioOperativo = entry.reservorioOperativo,
                        tratamientoAgua = entry.tratamientoAgua, observaciones = entry.observaciones, responsable = entry.responsable, dni = entry.dni, firma = entry.firma
                    ))
                    if (response.isSuccessful) dao.markAsSynced(entry.id)
                } catch (e: Exception) { }
            }

            // Sync Monitoreo
            dao.getUnsyncedMonitoring().forEach { entry ->
                try {
                    val response = apiService.syncEntry(SCRIPT_URL, SyncRequest(
                        tipo = "monitoreo", uuid = entry.uuid, fechaRegistro = entry.fechaRegistro,
                        nombreIpress = entry.nombreIpress, codigoRenipress = entry.codigoRenipress, unidadEjecutora = entry.unidadEjecutora,
                        cloro = entry.cloro, temperatura = entry.temperatura, ph = entry.ph, turbiedad = entry.turbiedad, conductividad = entry.conductividad
                    ))
                    if (response.isSuccessful) dao.markMonitoringAsSynced(entry.id)
                } catch (e: Exception) { }
            }
        }
    }

    private fun scheduleSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork("sync_work", ExistingWorkPolicy.REPLACE, syncRequest)
    }
}
