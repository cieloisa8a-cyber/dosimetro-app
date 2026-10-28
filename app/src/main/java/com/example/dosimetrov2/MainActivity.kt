package com.example.dosimetrov2

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val LightBg = Color(0xFFF4F6FA)
val SurfaceCard = Color(0xFFFFFFFF)
val BorderSoft = Color(0xFFE2E6EE)
val TextPrimary = Color(0xFF1A2233)
val TextSecondary = Color(0xFF6B7385)
val PeriwinklePrimary = Color(0xFF5B5FD6)
val PeriwinkleLight = Color(0xFFE4E5FB)
val AmberAlert = Color(0xFFE8A33D) 
val RedAlert = Color(0xFFF44336)

val BricolageGrotesque = FontFamily(
    Font(R.font.bricolage_grotesque, FontWeight.Normal),
    Font(R.font.bricolage_grotesque, FontWeight.Medium),
    Font(R.font.bricolage_grotesque, FontWeight.Bold),
    Font(R.font.bricolage_grotesque, FontWeight.ExtraBold)
)

val AppTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = BricolageGrotesque),
    displayMedium = Typography().displayMedium.copy(fontFamily = BricolageGrotesque),
    displaySmall = Typography().displaySmall.copy(fontFamily = BricolageGrotesque),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = BricolageGrotesque),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = BricolageGrotesque),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = BricolageGrotesque),
    titleLarge = Typography().titleLarge.copy(fontFamily = BricolageGrotesque),
    titleMedium = Typography().titleMedium.copy(fontFamily = BricolageGrotesque),
    titleSmall = Typography().titleSmall.copy(fontFamily = BricolageGrotesque),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = BricolageGrotesque),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = BricolageGrotesque),
    bodySmall = Typography().bodySmall.copy(fontFamily = BricolageGrotesque),
    labelLarge = Typography().labelLarge.copy(fontFamily = BricolageGrotesque),
    labelMedium = Typography().labelMedium.copy(fontFamily = BricolageGrotesque),
    labelSmall = Typography().labelSmall.copy(fontFamily = BricolageGrotesque)
)

class MainActivity : ComponentActivity() {
    private var dosimetryService by mutableStateOf<DosimetryService?>(null)
    private val viewModel: DosimeterViewModel by viewModels()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as DosimetryService.LocalBinder
            dosimetryService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            dosimetryService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val intent = Intent(this, DosimetryService::class.java)
        bindService(intent, connection, BIND_AUTO_CREATE)

        setContent {
            DosimetroTheme {
                DosimeterScreen(
                    service = dosimetryService,
                    viewModel = viewModel,
                    onStartClick = { isNiosh -> startMonitoring(isNiosh) },
                    onStopClick = { stopMonitoring() }
                )
            }
        }
    }

    private fun startMonitoring(isNiosh: Boolean) {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            val intent = Intent(this, DosimetryService::class.java).apply {
                putExtra("IS_NIOSH", isNiosh)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            Toast.makeText(this, "Concede los permisos para iniciar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopMonitoring() {
        dosimetryService?.stopMonitoring()
        val intent = Intent(this, DosimetryService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dosimetryService != null) unbindService(connection)
    }
}

@Composable
fun DosimetroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PeriwinklePrimary,
            background = LightBg,
            surface = SurfaceCard,
            onPrimary = Color.White,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        typography = AppTypography,
        content = content
    )
}

@Composable
fun DosimeterScreen(
    service: DosimetryService?,
    viewModel: DosimeterViewModel,
    onStartClick: (Boolean) -> Unit,
    onStopClick: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val prefs = context.getSharedPreferences("dosimetro_prefs", Context.MODE_PRIVATE)

    var isNioshSelected by rememberSaveable { mutableStateOf(true) }
    var showResults by rememberSaveable { mutableStateOf(false) }
    var showGuide by rememberSaveable { mutableStateOf(false) }
    var showCalibration by rememberSaveable { mutableStateOf(false) }
    
    var showStartDialog by rememberSaveable { mutableStateOf(false) }
    var showStopDialog by rememberSaveable { mutableStateOf(false) }
    
    val db by (service?.currentDb?.collectAsState() ?: remember { mutableFloatStateOf(0f) })
    val dose by (service?.currentDose?.collectAsState() ?: remember { mutableFloatStateOf(0f) })
    val isRunning by (service?.isMonitoring?.collectAsState() ?: remember { mutableStateOf(false) })
    
    val measurementName by viewModel.measurementName.collectAsState()
    val history by viewModel.history.collectAsState()
    var exportDirUriStr by remember { mutableStateOf(prefs.getString("export_dir_uri", null)) }

    LaunchedEffect(service, isRunning) {
        if (service != null && isRunning) {
            service.currentDb.collect {
                viewModel.addMeasurement(it)
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms[Manifest.permission.RECORD_AUDIO] == true) showStartDialog = true
    }
    
    val dirPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            exportDirUriStr = uri.toString()
            prefs.edit { putString("export_dir_uri", uri.toString()) }
        }
    }

    // Modal de inicio
    if (showStartDialog) {
        val defaultName = "Medicion_${SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())}"
        var tempName by remember { mutableStateOf(measurementName.ifEmpty { defaultName }) }
        var isError by remember { mutableStateOf(false) }
        val dirName = if (exportDirUriStr != null) {
            val dFile = DocumentFile.fromTreeUri(context, Uri.parse(exportDirUriStr!!))
            dFile?.name ?: "Descargas (Predeterminado)"
        } else "Descargas (Predeterminado)"

        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text(if (isRunning) "Medición reiniciada" else "Nueva medición", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (isRunning) {
                        Text("La medición actual se detendrá y guardará.", color = TextSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { 
                            tempName = it.replace(Regex("[/\\\\:*?\"<>|]"), "_")
                            isError = tempName.isBlank()
                        },
                        label = { Text("Nombre de la medición") },
                        isError = isError,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PeriwinklePrimary,
                            focusedLabelColor = PeriwinklePrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary
                        )
                    )
                    if (isError) Text("El nombre no puede estar vacío", color = RedAlert, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = "Carpeta", tint = TextSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(dirName, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { dirPickerLauncher.launch(null) }) {
                            Text("Cambiar", color = PeriwinklePrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    if (tempName.isNotBlank()) {
                        if (isRunning) {
                            exportCsv(context, service?.getSamples() ?: emptyList(), measurementName, exportDirUriStr)
                            service?.stopMonitoring()
                        }
                        viewModel.setMeasurementName(tempName)
                        viewModel.clearHistory()
                        showStartDialog = false
                        service?.clearData()
                        onStartClick(isNioshSelected)
                    }
                }, colors = ButtonDefaults.buttonColors(PeriwinklePrimary)) {
                    Text("Iniciar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) { Text("Cancelar", color = PeriwinklePrimary) }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Modal de detener
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("¿Detener el monitoreo?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("La medición se guardará como $measurementName.csv con una dosis de ${"%.2f".format(dose)}%.", color = TextSecondary) },
            confirmButton = {
                Button(onClick = { 
                    val fileName = exportCsv(context, service?.getSamples() ?: emptyList(), measurementName, exportDirUriStr)
                    showStopDialog = false
                    onStopClick()
                    if (fileName.isNotEmpty()) {
                        Toast.makeText(context, "Guardado como $fileName", Toast.LENGTH_LONG).show()
                    }
                }, colors = ButtonDefaults.buttonColors(PeriwinklePrimary)) {
                    Text("Detener", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("Seguir midiendo", color = PeriwinklePrimary) }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showGuide) GuideDialog { showGuide = false }
    if (showCalibration) CalibrationDialog(
        initialOffset = prefs.getFloat("calibration_offset", 0f),
        onDismiss = { showCalibration = false },
        onChangeDir = { dirPickerLauncher.launch(null) },
        onSave = { offset ->
            prefs.edit { putFloat("calibration_offset", offset) }
        }
    )
    if (showResults) ResultsDialog(
        samples = service?.getSamples() ?: emptyList(),
        onDismiss = { showResults = false },
        onExport = { 
            val fn = exportCsv(context, service?.getSamples() ?: emptyList(), measurementName.ifEmpty { "Export_${System.currentTimeMillis()}" }, exportDirUriStr)
            Toast.makeText(context, "Exportado: $fn", Toast.LENGTH_SHORT).show()
        }
    )

    // UI Content
    Box(Modifier.fillMaxSize().background(LightBg).systemBarsPadding().padding(16.dp)) {
        if (isLandscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Left Column
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Header(
                        measurementName = if (isRunning) measurementName else "",
                        onCalibrate = { showCalibration = true },
                        onGuide = { showGuide = true }
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        SoundLevelCircle(db, isRunning)
                    }
                    Spacer(Modifier.height(16.dp))
                    DoseCard(dose, isNioshSelected)
                }
                // Right Column
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    LevelHistoryGraph(Modifier.weight(1f).fillMaxWidth(), history, isRunning)
                    Spacer(Modifier.height(16.dp))
                    CriteriaToggleRow(isNioshSelected) { 
                        isNioshSelected = it
                        service?.updateCriterion(it) 
                    }
                    Spacer(Modifier.height(16.dp))
                    ActionButtons(
                        isRunning = isRunning,
                        onMenuClick = { showResults = true },
                        onMainClick = {
                            if (isRunning) showStopDialog = true else {
                                val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                launcher.launch(perms.toTypedArray())
                            }
                        },
                        onNewClick = {
                            val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            launcher.launch(perms.toTypedArray())
                        }
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Header(
                    measurementName = if (isRunning) measurementName else "",
                    onCalibrate = { showCalibration = true },
                    onGuide = { showGuide = true }
                )
                Spacer(Modifier.height(16.dp))
                Box(Modifier.height(200.dp), contentAlignment = Alignment.Center) {
                    SoundLevelCircle(db, isRunning)
                }
                Spacer(Modifier.height(24.dp))
                LevelHistoryGraph(Modifier.weight(1f).fillMaxWidth(), history, isRunning)
                Spacer(Modifier.height(24.dp))
                DoseCard(dose, isNioshSelected)
                Spacer(Modifier.height(16.dp))
                CriteriaToggleRow(isNioshSelected) { 
                    isNioshSelected = it
                    service?.updateCriterion(it) 
                }
                Spacer(Modifier.height(16.dp))
                ActionButtons(
                    isRunning = isRunning,
                    onMenuClick = { showResults = true },
                    onMainClick = {
                        if (isRunning) showStopDialog = true else {
                            val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            launcher.launch(perms.toTypedArray())
                        }
                    },
                    onNewClick = {
                        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        launcher.launch(perms.toTypedArray())
                    }
                )
            }
        }
    }
}

@Composable
fun Header(measurementName: String, onCalibrate: () -> Unit, onGuide: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(
            onClick = onCalibrate,
            modifier = Modifier.size(48.dp).background(SurfaceCard, RoundedCornerShape(12.dp)).border(1.dp, BorderSoft, RoundedCornerShape(12.dp))
        ) { Icon(Icons.Default.Settings, "Calibración", tint = TextSecondary) }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DOSÍMETRO", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            if (measurementName.isNotEmpty()) {
                Text(measurementName, color = PeriwinklePrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        
        IconButton(
            onClick = onGuide,
            modifier = Modifier.size(48.dp).background(SurfaceCard, RoundedCornerShape(12.dp)).border(1.dp, BorderSoft, RoundedCornerShape(12.dp))
        ) { Icon(Icons.AutoMirrored.Filled.HelpOutline, "Ayuda", tint = TextSecondary) }
    }
}

@Composable
fun SoundLevelCircle(db: Float, isRunning: Boolean) {
    val circleBg = if (db > 100) RedAlert else if (db > 85) AmberAlert else SurfaceCard
    val circleText = if (db > 85) Color.White else TextPrimary
    val circleSub = if (db > 85) Color.White.copy(alpha = 0.8f) else TextSecondary
    val borderColor = if (isRunning && db <= 85) PeriwinklePrimary else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .shadow(if (db <= 85) 4.dp else 12.dp, androidx.compose.foundation.shape.CircleShape)
            .background(circleBg, androidx.compose.foundation.shape.CircleShape)
            .border(4.dp, borderColor, androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NIVEL SONORO", color = circleSub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("%.1f".format(db), color = circleText, fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)
            Text("dB(A)", color = if(db > 85) circleText else PeriwinklePrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LevelHistoryGraph(modifier: Modifier, levels: List<Float>, isRunning: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val rightMargin = 40.dp.toPx()
        val bottomMargin = 20.dp.toPx()
        val graphW = w - rightMargin
        val graphH = h - bottomMargin

        // Draw grid
        val dbLevels = listOf(20, 40, 60, 80, 100)
        val gridColor = Color(0xFFE2E4F0)
        
        dbLevels.forEach { db ->
            val y = graphH - ((db - 20f) / 80f) * graphH
            drawLine(gridColor, Offset(0f, y), Offset(graphW, y), strokeWidth = 1f)
            drawText(
                textMeasurer = textMeasurer,
                text = "$db dB",
                topLeft = Offset(graphW + 4.dp.toPx(), y - 6.dp.toPx()),
                style = androidx.compose.ui.text.TextStyle(color = TextSecondary, fontSize = 10.sp, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Medium)
            )
        }

        val timeLabels = listOf(0, 10, 20, 30, 40)
        timeLabels.forEach { sec ->
            val x = graphW - (sec / 40f) * graphW
            drawLine(gridColor, Offset(x, 0f), Offset(x, graphH), strokeWidth = 1f)
            drawText(
                textMeasurer = textMeasurer,
                text = "-$sec s",
                topLeft = Offset(x - 12.dp.toPx(), graphH + 4.dp.toPx()),
                style = androidx.compose.ui.text.TextStyle(color = TextSecondary, fontSize = 10.sp, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Medium)
            )
        }

        // 85 dB Line
        val y85 = graphH - ((85f - 20f) / 80f) * graphH
        drawLine(
            color = AmberAlert,
            start = Offset(0f, y85),
            end = Offset(graphW, y85),
            strokeWidth = 2f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "85",
            topLeft = Offset(graphW + 4.dp.toPx(), y85 - 6.dp.toPx()),
            style = androidx.compose.ui.text.TextStyle(color = AmberAlert, fontSize = 10.sp, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold)
        )

        if (isRunning && levels.isNotEmpty()) {
            val maxPoints = 400
            val path = Path()
            val fillPath = Path()
            val step = graphW / (maxPoints - 1).toFloat()
            
            val coords = mutableListOf<Offset>()
            val startIdx = (maxPoints - levels.size).coerceAtLeast(0)
            levels.forEachIndexed { idx, db ->
                val x = (startIdx + idx) * step
                val clampedDb = db.coerceIn(20f, 100f)
                val y = graphH - ((clampedDb - 20f) / 80f) * graphH
                coords.add(Offset(x, y))
            }
            
            if (coords.size > 1) {
                path.moveTo(coords[0].x, coords[0].y)
                fillPath.moveTo(coords[0].x, graphH)
                fillPath.lineTo(coords[0].x, coords[0].y)
                
                for (i in 0 until coords.size - 1) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
                fillPath.lineTo(coords.last().x, graphH)
                fillPath.close()
                
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(PeriwinklePrimary.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = graphH
                    )
                )
                
                drawPath(
                    path,
                    color = PeriwinklePrimary,
                    style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun DoseCard(dose: Float, isNioshSelected: Boolean) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Dosis Acumulada", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(if (isNioshSelected) "NIOSH (8h)" else "OSHA (8h)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            
            val doseColor = if (dose >= 100f) RedAlert else if (dose >= 50f) AmberAlert else PeriwinklePrimary
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.2f".format(dose), color = doseColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("%", color = doseColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (dose / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = doseColor,
                trackColor = LightBg,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun CriteriaToggleRow(isNioshSelected: Boolean, onSelect: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).background(SurfaceCard, RoundedCornerShape(24.dp)).border(1.dp, BorderSoft, RoundedCornerShape(24.dp)).padding(4.dp)) {
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp))
                .background(if (isNioshSelected) PeriwinklePrimary else Color.Transparent)
                .clickable { onSelect(true) }, 
            contentAlignment = Alignment.Center
        ) { Text("NIOSH", color = if (isNioshSelected) Color.White else TextSecondary, fontWeight = FontWeight.Bold) }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp))
                .background(if (!isNioshSelected) PeriwinklePrimary else Color.Transparent)
                .clickable { onSelect(false) }, 
            contentAlignment = Alignment.Center
        ) { Text("OSHA", color = if (!isNioshSelected) Color.White else TextSecondary, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun ActionButtons(isRunning: Boolean, onMenuClick: () -> Unit, onMainClick: () -> Unit, onNewClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(56.dp).background(SurfaceCard, RoundedCornerShape(20.dp)).border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
        ) { Icon(Icons.Default.Menu, "Resultados", tint = TextSecondary) }

        Button(
            onClick = onMainClick,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(if (isRunning) PeriwinkleLight else PeriwinklePrimary),
            shape = RoundedCornerShape(20.dp),
            border = if (isRunning) BorderStroke(1.dp, BorderSoft) else null
        ) {
            Text(
                if (isRunning) "DETENER" else "COMENZAR", 
                color = if (isRunning) PeriwinklePrimary else Color.White, 
                fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
            )
        }
        
        IconButton(
            onClick = onNewClick,
            modifier = Modifier.size(56.dp).background(SurfaceCard, RoundedCornerShape(20.dp)).border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
        ) { Icon(Icons.Default.Add, "Nuevo", tint = TextSecondary) }
    }
}

@Composable
fun CalibrationDialog(initialOffset: Float, onDismiss: () -> Unit, onChangeDir: () -> Unit, onSave: (Float) -> Unit) {
    var textValue by remember { mutableStateOf(initialOffset.toString()) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(SurfaceCard)) {
            Column(Modifier.padding(24.dp)) {
                Text("Calibración", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue, 
                    onValueChange = { textValue = it }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PeriwinklePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onChangeDir, colors = ButtonDefaults.buttonColors(PeriwinkleLight), modifier = Modifier.fillMaxWidth()) {
                    Text("Elegir carpeta por defecto", color = PeriwinklePrimary)
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onDismiss) { Text("CANCELAR", color = TextSecondary, fontWeight = FontWeight.Bold) }
                    Button(onClick = { onSave(textValue.toFloatOrNull() ?: 0f); onDismiss() }, colors = ButtonDefaults.buttonColors(PeriwinklePrimary)) { 
                        Text("GUARDAR", color = Color.White, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}

@Composable
fun GuideDialog(onDismiss: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(SurfaceCard)) {
            Column(Modifier.padding(24.dp).height(480.dp)) {
                Text("Guía Técnica (${page + 1}/4)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PeriwinklePrimary)
                Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp).verticalScroll(rememberScrollState())) {
                    when (page) {
                        0 -> Column {
                            Text("Introducción", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("\nEsta aplicación es un dosímetro de ruido laboral que cumple con estándares internacionales de higiene ocupacional.", color = TextSecondary)
                        }
                        1 -> Column {
                            Text("Fundamento Físico", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("\nEl nivel SPL se calcula como: L = 20 * log10(RMS / Ref) + offset", color = TextSecondary)
                        }
                        2 -> Column {
                            Text("Criterios de Dosimetría", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("\n• NIOSH: Límite 85dB, Intercambio 3dB.\n• OSHA: Límite 90dB, Intercambio 5dB.", color = TextSecondary)
                        }
                        3 -> Column {
                            Text("Calibración y Reportes", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("\nEl archivo CSV guardado en Descargas incluye timestamp, SPL_dBA, y dosis_parcial_pct.", color = TextSecondary)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    TextButton({ if (page > 0) page-- else onDismiss() }) { 
                        Text(if (page == 0) "CERRAR" else "ATRÁS", color = TextSecondary, fontWeight = FontWeight.Bold) 
                    }
                    Button(onClick = { if (page < 3) page++ else onDismiss() }, colors = ButtonDefaults.buttonColors(PeriwinklePrimary)) { 
                        Text(if (page == 3) "FINALIZAR" else "SIGUIENTE", color = Color.White, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsDialog(samples: List<MeasurementSample>, onDismiss: () -> Unit, onExport: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(8.dp), RoundedCornerShape(24.dp), CardDefaults.cardColors(SurfaceCard)) {
            Column(Modifier.padding(20.dp)) {
                Text("Resultados de Sesión", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                
                Row(Modifier.fillMaxWidth().background(LightBg, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text("Hora", Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("dB(A)", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Dosis %", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                LazyColumn(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    items(samples.reversed()) { s ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(sdf.format(Date(s.timestamp)), Modifier.weight(1.2f), fontSize = 13.sp, color = TextSecondary)
                            Text("%.1f".format(s.spl), Modifier.weight(1f), fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("%.4f".format(s.partialDose), Modifier.weight(1f), fontSize = 13.sp, color = PeriwinklePrimary)
                        }
                        HorizontalDivider(color = BorderSoft)
                    }
                }
                
                Button(onExport, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(PeriwinklePrimary)) {
                    Text("EXPORTAR REPORTE CSV", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onDismiss, Modifier.align(Alignment.CenterHorizontally)) {
                    Text("VOLVER", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun exportCsv(context: Context, samples: List<MeasurementSample>, baseName: String, dirUriStr: String?): String {
    if (samples.isEmpty()) return ""
    try {
        val outStream = if (dirUriStr.isNullOrEmpty()) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var file = File(downloadsDir, "$baseName.csv")
            var counter = 1
            while (file.exists()) {
                file = File(downloadsDir, "${baseName}_$counter.csv")
                counter++
            }
            file.outputStream()
        } else {
            val dirUri = Uri.parse(dirUriStr)
            val docFile = DocumentFile.fromTreeUri(context, dirUri) ?: return ""
            var newFile = docFile.findFile("$baseName.csv")
            if (newFile != null) {
                var counter = 1
                while (docFile.findFile("${baseName}_$counter.csv") != null) {
                    counter++
                }
                newFile = docFile.createFile("text/csv", "${baseName}_$counter")
            } else {
                newFile = docFile.createFile("text/csv", baseName)
            }
            if (newFile == null) return ""
            context.contentResolver.openOutputStream(newFile.uri)
        }
        
        outStream?.bufferedWriter()?.use { out ->
            out.write("Hora,SPL_dBA,Dosis_Pct\n")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            samples.forEach { out.write("${sdf.format(Date(it.timestamp))},${it.spl},${it.partialDose}\n") }
        }
        return baseName
    } catch (e: Exception) {
        return ""
    }
}
