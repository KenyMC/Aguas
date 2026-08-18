package pe.gob.cusco.geresa.calidad.agua.ui

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.gob.cusco.geresa.calidad.agua.data.local.RegisterEntry
import pe.gob.cusco.geresa.calidad.agua.data.local.MonitoringEntry
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RegisterScreen(viewModel: RegisterViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen
    val isAccepted by viewModel.isAccepted
    val currentModule by viewModel.currentModule

    if (!isAccepted) {
        PrivacyDialog(onAccept = { viewModel.acceptTerms() })
    }

    Scaffold(
        bottomBar = {
            if (currentScreen is Screen.List) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentModule == Module.DIAGNOSTICO,
                        onClick = { viewModel.setModule(Module.DIAGNOSTICO) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Assignment, null) },
                        label = { Text("Diagnóstico") }
                    )
                    NavigationBarItem(
                        selected = currentModule == Module.MONITOREO,
                        onClick = { viewModel.setModule(Module.MONITOREO) },
                        icon = { Icon(Icons.Default.WaterDrop, null) },
                        label = { Text("Monitoreo") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val screen = currentScreen) {
                is Screen.List -> {
                    if (currentModule == Module.DIAGNOSTICO) ListScreen(viewModel)
                    else MonitoringListScreen(viewModel)
                }
                is Screen.Form -> FormScreen(viewModel, screen.entry)
                is Screen.MonitoringForm -> MonitoringFormScreen(viewModel, screen.entry)
            }
        }
    }
}

// --- DIAGNÓSTICO ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(viewModel: RegisterViewModel) {
    val entries by viewModel.allEntries.collectAsState()
    val status by viewModel.syncStatus

    Scaffold(
        topBar = { TopAppBar(title = { Text("Diagnóstico IPRESS", color = Color.White) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.navigateTo(Screen.Form()) }) { Icon(Icons.Default.Add, "Nuevo") }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Estado: $status", color = if (status.contains("Error")) Color.Red else Color.Gray, fontSize = 12.sp)
            if (entries.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Sin registros de diagnóstico.", color = Color.Gray) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries) { entry -> EntryItem(entry) { viewModel.navigateTo(Screen.Form(entry)) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(viewModel: RegisterViewModel, existingEntry: RegisterEntry?) {
    val scope = rememberCoroutineScope()
    val status by viewModel.syncStatus
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var fechaRegistro by remember { mutableStateOf(existingEntry?.fechaRegistro?.take(10) ?: sdf.format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    // Campos
    var nombreIpress by remember { mutableStateOf(existingEntry?.nombreIpress ?: "") }
    var codigoRenipress by remember { mutableStateOf(existingEntry?.codigoRenipress ?: "") }
    var unidadEjecutora by remember { mutableStateOf(existingEntry?.unidadEjecutora ?: "Red Cusco Norte") }
    var provincia by remember { mutableStateOf(existingEntry?.provincia ?: "") }
    var distrito by remember { mutableStateOf(existingEntry?.distrito ?: "") }
    var centroPoblado by remember { mutableStateOf(existingEntry?.centroPoblado ?: "") }
    var ubigeo by remember { mutableStateOf(existingEntry?.ubigeo ?: "") }
    var latitud by remember { mutableStateOf(existingEntry?.latitud ?: "") }
    var longitud by remember { mutableStateOf(existingEntry?.longitud ?: "") }
    var altitud by remember { mutableStateOf(existingEntry?.altitud ?: "") }
    var precision by remember { mutableStateOf(existingEntry?.precision ?: "") }
    var aguaPropio by remember { mutableStateOf(existingEntry?.aguaPropio ?: "No") }
    var fuenteAgua by remember { mutableStateOf(existingEntry?.fuenteAgua ?: "Red Publica") }
    var bombasAgua by remember { mutableStateOf(existingEntry?.bombasAgua ?: "No") }
    var bombasOperativas by remember { mutableStateOf(existingEntry?.bombasOperativas ?: "No") }
    var reservorio by remember { mutableStateOf(existingEntry?.reservorio ?: "No") }
    var reservorioElevado by remember { mutableStateOf(existingEntry?.reservorioElevado ?: "No") }
    var reservorioOperativo by remember { mutableStateOf(existingEntry?.reservorioOperativo ?: "No") }
    var tratamientoAgua by remember { mutableStateOf(existingEntry?.tratamientoAgua ?: "No") }
    var observaciones by remember { mutableStateOf(existingEntry?.observaciones ?: "") }
    var responsable by remember { mutableStateOf(existingEntry?.responsable ?: "") }
    var dni by remember { mutableStateOf(existingEntry?.dni ?: "") }
    var firmaBase64 by remember { mutableStateOf(existingEntry?.firma ?: "") }
    val paths = remember { mutableStateListOf<Path>() }
    var signatureSize by remember { mutableStateOf(IntSize.Zero) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            scope.launch { viewModel.getCurrentLocation()?.let { l -> latitud = l.latitude.toString(); longitud = l.longitude.toString(); altitud = l.altitude.toString(); precision = l.accuracy.toString() } }
        }
    }

    BackHandler { viewModel.navigateTo(Screen.List) }
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { fechaRegistro = sdf.format(Date(it)) }; showDatePicker = false }) { Text("Aceptar") } }) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (existingEntry == null) "Nuevo Diagnóstico" else "Editar Diagnóstico", color = Color.White) }, navigationIcon = { IconButton(onClick = { viewModel.navigateTo(Screen.List) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) } }, actions = { if (existingEntry != null) IconButton(onClick = { viewModel.deleteEntry(existingEntry) }) { Icon(Icons.Default.Delete, "Eliminar", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)) }
    ) { p ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Estado: $status", color = if (status.contains("Error")) Color.Red else Color.Gray, fontSize = 12.sp)
                SectionTitle("0. Tiempo")
                OutlinedTextField(value = fechaRegistro, onValueChange = {}, readOnly = true, label = { Text("Fecha de Registro") }, modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline))
                Button(onClick = { showDatePicker = true }, Modifier.padding(top = 4.dp)) { Text("Cambiar Fecha") }

                SectionTitle("1. Identificación")
                FormTextField("Nombre de la IPRESS", nombreIpress) { nombreIpress = it }
                FormTextField("Código RENIPRESS", codigoRenipress) { codigoRenipress = it }
                DropdownSelector("Unidad Ejecutora", listOf("Red Cusco Norte", "Red Cusco Sur", "Red Cusco VRAEM", "Red CCE", "Red Chumbivilcas", "Red La Convencion", "Hospital", "Otro"), unidadEjecutora) { unidadEjecutora = it }
            }
            item {
                SectionTitle("2. Ubicación")
                FormTextField("Provincia", provincia) { provincia = it }
                FormTextField("Distrito", distrito) { distrito = it }
                FormTextField("Centro Poblado", centroPoblado) { centroPoblado = it }
                FormTextField("Ubigeo CCPP", ubigeo) { ubigeo = it }
            }
            item {
                SectionTitle("3. Georeferencia (GPS)")
                Button(onClick = { locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Icon(Icons.Default.LocationOn, null); Spacer(Modifier.width(8.dp)); Text("Capturar GPS") }
                Text("Lat: $latitud, Lon: $longitud", fontSize = 12.sp); Text("Alt: $altitud, Prec: $precision m", fontSize = 12.sp)
            }
            item {
                SectionTitle("4. Abastecimiento de Agua")
                YesNoSelector("¿Cuenta con sistema propio?", aguaPropio) { aguaPropio = it }
                DropdownSelector("Fuente de agua", listOf("Red Publica", "Manante", "Riachuelo", "Pozo", "Camion Cisterna", "Agua de lluvia"), fuenteAgua) { fuenteAgua = it }
                YesNoSelector("¿Tiene bombas de agua?", bombasAgua) { bombasAgua = it }
                if (bombasAgua == "Si") YesNoSelector("¿Bombas operativas?", bombasOperativas) { bombasOperativas = it }
                YesNoSelector("¿Tiene reservorio/cisterna?", reservorio) { reservorio = it }
                if (reservorio == "Si") { YesNoSelector("¿Reservorio elevado?", reservorioElevado) { reservorioElevado = it }; YesNoSelector("¿Reservorio operativo?", reservorioOperativo) { reservorioOperativo = it } }
                YesNoSelector("¿Tratamiento de agua?", tratamientoAgua) { tratamientoAgua = it }
            }
            item {
                SectionTitle("5. Finalización y Firma")
                FormTextField("Observaciones", observaciones) { observaciones = it }
                FormTextField("Nombre Responsable", responsable) { responsable = it }
                FormTextField("DNI", dni, KeyboardType.Number) { dni = it }
                Text("Firma del Responsable:", style = MaterialTheme.typography.bodyMedium)
                if (firmaBase64.isNotEmpty() && paths.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        decodeBase64ToBitmap(firmaBase64)?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                        IconButton(onClick = { firmaBase64 = "" }, Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    }
                } else {
                    SignaturePad(paths) { signatureSize = it }
                    Button(onClick = { paths.clear() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Limpiar Firma") }
                }
            }
            item {
                Button(onClick = {
                    if (nombreIpress.isNotBlank() && responsable.isNotBlank()) {
                        val f = if (paths.isNotEmpty()) captureSignatureFromPaths(paths, signatureSize) else firmaBase64
                        viewModel.saveEntry(RegisterEntry(id = existingEntry?.id ?: 0, uuid = existingEntry?.uuid ?: "", fechaRegistro = fechaRegistro, nombreIpress = nombreIpress, codigoRenipress = codigoRenipress, unidadEjecutora = unidadEjecutora, provincia = provincia, distrito = distrito, centroPoblado = centroPoblado, ubigeo = ubigeo, latitud = latitud, longitud = longitud, altitud = altitud, precision = precision, aguaPropio = aguaPropio, fuenteAgua = fuenteAgua, bombasAgua = bombasAgua, bombasOperativas = bombasOperativas, reservorio = reservorio, reservorioElevado = reservorioElevado, reservorioOperativo = reservorioOperativo, tratamientoAgua = tratamientoAgua, observaciones = observaciones, responsable = responsable, dni = dni, firma = f))
                    }
                }, Modifier.fillMaxWidth().padding(vertical = 16.dp), shape = RoundedCornerShape(8.dp)) { Text("GUARDAR DIAGNÓSTICO", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// --- MONITOREO ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringListScreen(viewModel: RegisterViewModel) {
    val entries by viewModel.allMonitoring.collectAsState()
    val status by viewModel.syncStatus

    Scaffold(
        topBar = { TopAppBar(title = { Text("Monitoreo Agua", color = Color.White) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondary)) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.navigateTo(Screen.MonitoringForm()) }) { Icon(Icons.Default.Add, "Nuevo") }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Estado: $status", color = if (status.contains("Error")) Color.Red else Color.Gray, fontSize = 12.sp)
            if (entries.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Sin registros de monitoreo.", color = Color.Gray) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries) { entry -> MonitoringEntryItem(entry) { viewModel.navigateTo(Screen.MonitoringForm(entry)) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringFormScreen(viewModel: RegisterViewModel, existingEntry: MonitoringEntry?) {
    val status by viewModel.syncStatus
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var fechaRegistro by remember { mutableStateOf(existingEntry?.fechaRegistro?.take(10) ?: sdf.format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    // Campos Monitoreo
    var nombreIpress by remember { mutableStateOf(existingEntry?.nombreIpress ?: "") }
    var codigoRenipress by remember { mutableStateOf(existingEntry?.codigoRenipress ?: "") }
    var unidadEjecutora by remember { mutableStateOf(existingEntry?.unidadEjecutora ?: "Red Cusco Norte") }
    var cloro by remember { mutableStateOf(existingEntry?.cloro ?: "") }
    var temperatura by remember { mutableStateOf(existingEntry?.temperatura ?: "") }
    var ph by remember { mutableStateOf(existingEntry?.ph ?: "") }
    var turbiedad by remember { mutableStateOf(existingEntry?.turbiedad ?: "") }
    var conductividad by remember { mutableStateOf(existingEntry?.conductividad ?: "") }

    BackHandler { viewModel.navigateTo(Screen.List) }
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { fechaRegistro = sdf.format(Date(it)) }; showDatePicker = false }) { Text("Aceptar") } }) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (existingEntry == null) "Nuevo Monitoreo" else "Editar Monitoreo", color = Color.White) }, navigationIcon = { IconButton(onClick = { viewModel.navigateTo(Screen.List) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) } }, actions = { if (existingEntry != null) IconButton(onClick = { viewModel.deleteMonitoring(existingEntry) }) { Icon(Icons.Default.Delete, "Eliminar", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondary)) }
    ) { p ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Estado: $status", color = if (status.contains("Error")) Color.Red else Color.Gray, fontSize = 12.sp)
                SectionTitle("1. Datos Generales")
                OutlinedTextField(value = fechaRegistro, onValueChange = {}, readOnly = true, label = { Text("Fecha") }, modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline))
                FormTextField("Nombre IPRESS", nombreIpress) { nombreIpress = it }
                FormTextField("Código RENIPRESS", codigoRenipress) { codigoRenipress = it }
                DropdownSelector("Unidad Ejecutora", listOf("Red Cusco Norte", "Red Cusco Sur", "Red Cusco VRAEM", "Red CCE", "Red Chumbivilcas", "Red La Convencion", "Hospital", "Otro"), unidadEjecutora) { unidadEjecutora = it }
                
                SectionTitle("2. Parámetros Técnicos")
                FormTextField("Cloro Residual (mg/L)", cloro, KeyboardType.Decimal) { cloro = it }
                FormTextField("Temperatura (°C)", temperatura, KeyboardType.Decimal) { temperatura = it }
                FormTextField("pH", ph, KeyboardType.Decimal) { ph = it }
                FormTextField("Turbiedad (UNT)", turbiedad, KeyboardType.Decimal) { turbiedad = it }
                FormTextField("Conductividad (µS/cm)", conductividad, KeyboardType.Decimal) { conductividad = it }
            }
            item {
                Button(onClick = {
                    if (nombreIpress.isNotBlank()) {
                        viewModel.saveMonitoring(MonitoringEntry(id = existingEntry?.id ?: 0, uuid = existingEntry?.uuid ?: "", fechaRegistro = fechaRegistro, nombreIpress = nombreIpress, codigoRenipress = codigoRenipress, unidadEjecutora = unidadEjecutora, cloro = cloro, temperatura = temperatura, ph = ph, turbiedad = turbiedad, conductividad = conductividad))
                    }
                }, Modifier.fillMaxWidth().padding(vertical = 16.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("GUARDAR MONITOREO", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// --- COMPONENTES COMUNES ---

@Composable
fun SectionTitle(title: String) { Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp)) }

@Composable
fun FormTextField(label: String, value: String, type: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = type))
}

@Composable
fun YesNoSelector(label: String, selected: String, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 14.sp)
        RadioButton(selected == "Si", { onSelect("Si") }); Text("Si", fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        RadioButton(selected == "No", { onSelect("No") }); Text("No", fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = selected, onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable
fun SignaturePad(paths: MutableList<Path>, onSizeChanged: (IntSize) -> Unit) {
    var drawTrigger by remember { mutableStateOf(0L) }
    Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).onSizeChanged { onSizeChanged(it) }.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(); val path = Path().apply { moveTo(down.position.x, down.position.y) }; paths.add(path); drawTrigger = System.nanoTime()
            do {
                val event = awaitPointerEvent()
                event.changes.forEach { change -> if (change.pressed) { paths.last().lineTo(change.position.x, change.position.y); change.consume(); drawTrigger = System.nanoTime() } }
            } while (event.changes.any { it.pressed })
        }
    }) {
        if (paths.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Firme aquí", color = Color.Gray) }
        Canvas(Modifier.fillMaxSize()) { val _u = drawTrigger; clipRect { paths.forEach { drawPath(it, Color.Black, style = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round)) } } }
    }
}

fun captureSignatureFromPaths(paths: List<Path>, size: IntSize): String {
    if (paths.isEmpty() || size.width <= 0 || size.height <= 0) return ""
    return try {
        val b = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888); val c = android.graphics.Canvas(b); c.drawColor(android.graphics.Color.WHITE)
        val p = android.graphics.Paint().apply { color = android.graphics.Color.BLACK; style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f; strokeCap = android.graphics.Paint.Cap.ROUND; strokeJoin = android.graphics.Paint.Join.ROUND; isAntiAlias = true }
        paths.forEach { c.drawPath(it.asAndroidPath(), p) }
        val os = ByteArrayOutputStream(); b.compress(Bitmap.CompressFormat.PNG, 100, os); Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) { "" }
}

fun decodeBase64ToBitmap(base64: String): ImageBitmap? {
    return try { val d = Base64.decode(base64, Base64.DEFAULT); BitmapFactory.decodeByteArray(d, 0, d.size).asImageBitmap() } catch (e: Exception) { null }
}

@Composable
fun PrivacyDialog(onAccept: () -> Unit) {
    AlertDialog(onDismissRequest = {}, title = { Text("GERESA Cusco") }, text = { Text("Esta app institucional recolecta GPS y Firma para el monitoreo de agua.") }, confirmButton = { Button(onClick = onAccept) { Text("Aceptar") } })
}

@Composable
fun EntryItem(entry: RegisterEntry, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = if (entry.isSynced) Color(0xFFF1F8E9) else Color(0xFFFFF8E1))) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { 
                Text(text = entry.nombreIpress, fontWeight = FontWeight.Bold)
                Text(text = entry.fechaRegistro, fontSize = 10.sp, color = Color.Gray) 
            }
            Text(text = if (entry.isSynced) "Sinc" else "Pend", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (entry.isSynced) Color(0xFF388E3C) else Color(0xFFF57C00))
        }
    }
}

@Composable
fun MonitoringEntryItem(entry: MonitoringEntry, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = if (entry.isSynced) Color(0xFFE3F2FD) else Color(0xFFFFF8E1))) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { 
                Text(text = entry.nombreIpress, fontWeight = FontWeight.Bold)
                Text(text = "Cloro: ${entry.cloro} | pH: ${entry.ph}", fontSize = 11.sp)
                Text(text = entry.fechaRegistro, fontSize = 10.sp, color = Color.Gray) 
            }
            Text(text = if (entry.isSynced) "Sinc" else "Pend", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (entry.isSynced) Color(0xFF1976D2) else Color(0xFFF57C00))
        }
    }
}
