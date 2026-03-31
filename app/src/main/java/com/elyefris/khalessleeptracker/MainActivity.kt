package com.elyefris.khalessleeptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.elyefris.khalessleeptracker.data.model.*
import com.elyefris.khalessleeptracker.data.repository.FirebaseSleepRepository
import com.elyefris.khalessleeptracker.ui.components.LiquidCard
import com.elyefris.khalessleeptracker.ui.theme.*
import com.elyefris.khalessleeptracker.utils.calculateRealSleepTime
import com.elyefris.khalessleeptracker.utils.formatSleepDuration
import com.elyefris.khalessleeptracker.viewmodel.SleepViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val repository = FirebaseSleepRepository()
    private val viewModel: SleepViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return SleepViewModel(repository) as T
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KhalesSleepTrackerTheme { Surface(modifier = Modifier.fillMaxSize()) { MainScreen(viewModel) } } }
    }
}

// NUEVO COMPONENTE: Botón Estilizado Premium
@Composable
fun BabyStyledButton(
    text: String,
    subText: String? = null,
    icon: String? = null,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.shadow(6.dp, RoundedCornerShape(20.dp), spotColor = color.copy(alpha = 0.5f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (icon != null) Text(icon, fontSize = 24.sp)
            Text(text, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (subText != null) Text(subText, color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
fun MainScreen(viewModel: SleepViewModel) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val backgroundBrush = if (isDark) Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))
    else Brush.linearGradient(listOf(PastelBlue, PastelPurple, PastelPink))
    val primaryTextColor = if (isDark) Color(0xFFF7FAFC) else Color(0xFF2D3748)
    val secondaryTextColor = if (isDark) Color(0xFFA0AEC0) else Color(0xFF718096)
    val cardBackgroundColor = if (isDark) Color(0xFF1A202C).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)

    var selectedSession by remember { mutableStateOf<SleepSession?>(null) }
    var sessionToEdit by remember { mutableStateOf<SleepSession?>(null) } // NUEVO: Estado para editar
    var showAchievements by remember { mutableStateOf(false) }
    var showDiaperDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<SleepSession?>(null) }
    var showManualEntry by remember { mutableStateOf(false) }
    var showDiaperHistory by remember { mutableStateOf(false) }

    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(state.session?.status) {
        if (state.session?.status == SleepStatus.DURMIENDO || state.session?.status == SleepStatus.DESPIERTO) {
            while (true) { currentTimeMillis = System.currentTimeMillis(); delay(60000L) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 48.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // HEADER
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallFloatingActionButton(onClick = { showDiaperDialog = true }, containerColor = if (isDark) Color(0xFF2D3748) else Color.White.copy(alpha=0.9f)) { Text("🫧", fontSize = 20.sp) }
                    SmallFloatingActionButton(onClick = { showDiaperHistory = true }, containerColor = if (isDark) Color(0xFF2D3748) else Color.White.copy(alpha=0.9f)) { Text("📋", fontSize = 20.sp) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallFloatingActionButton(onClick = { showManualEntry = true }, containerColor = if (isDark) Color(0xFF2D3748) else Color.White.copy(alpha=0.9f)) { Text("📝", fontSize = 20.sp) }
                    SmallFloatingActionButton(onClick = { showAchievements = true }, containerColor = if (isDark) Color(0xFF2D3748) else Color.White.copy(alpha=0.9f)) { Icon(Icons.Default.Star, "Logros", tint = Color(0xFFECC94B)) }
                }
            }

            // TARJETA PRINCIPAL
            LiquidCard(modifier = Modifier.size(width = 340.dp, height = 240.dp)) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when (state.session?.status) { SleepStatus.DURMIENDO -> if (state.session?.type == SleepType.SIESTA) "Siesta Activa 🌤️" else "Dulces Sueños 🌜"; SleepStatus.DESPIERTO -> "En Pausa ✨"; else -> "Listo para dormir" },
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = primaryTextColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    state.session?.let { session ->
                        if (session.status != SleepStatus.FINALIZADO) {
                            val diff = currentTimeMillis - session.startTime.time
                            Text("${TimeUnit.MILLISECONDS.toHours(diff)}h ${TimeUnit.MILLISECONDS.toMinutes(diff) % 60}m", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = if (isDark) Color.White else Color(0xFF6B46C1))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Desde: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(session.startTime)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = secondaryTextColor)
                        } else { Text("🧸", fontSize = 72.sp); Spacer(modifier = Modifier.height(12.dp)) }
                    } ?: run { Text("🧸", fontSize = 72.sp); Spacer(modifier = Modifier.height(12.dp)) }
                }
            }

            // HISTORIAL
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp)) {
                Text("Historial de Sueño", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = secondaryTextColor, modifier = Modifier.padding(vertical = 12.dp))
                if (selectedDate == null) DateListView(state.history, primaryTextColor, secondaryTextColor) { selectedDate = it }
                else SessionsOfDayView(state.history, selectedDate!!, primaryTextColor, secondaryTextColor, { selectedDate = null }) { selectedSession = it }
            }

            // BOTONES PRINCIPALES (USANDO EL NUEVO DISEÑO)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(80.dp)) {
                if (state.session == null || state.session?.status == SleepStatus.FINALIZADO) {
                    BabyStyledButton("Siesta", icon="🌤️", color = if(isDark) Color(0xFF4A5568) else PastelCream, textColor = if(isDark) Color.White else Color(0xFF2D3748), modifier = Modifier.weight(1f).fillMaxHeight()) { viewModel.startNap() }
                    BabyStyledButton("Noche", icon="🌜", color = Color(0xFF805AD5), textColor = Color.White, modifier = Modifier.weight(1f).fillMaxHeight()) { viewModel.startNight() }
                } else {
                    if (state.session?.status == SleepStatus.DURMIENDO) {
                        BabyStyledButton("¡Despertó! ✨", color = PastelPink, textColor = Color(0xFF2D3748), modifier = Modifier.fillMaxWidth().fillMaxHeight()) { viewModel.wakeUp() }
                    } else if (state.session?.status == SleepStatus.DESPIERTO) {
                        BabyStyledButton("Reanudar", "Volvió a dormir", color = PastelBlue, textColor = Color(0xFF2D3748), modifier = Modifier.weight(1f).fillMaxHeight()) { viewModel.backToSleep() }
                        BabyStyledButton("Finalizar", "Guardar", color = Color(0xFFE53E3E), textColor = Color.White, modifier = Modifier.weight(1f).fillMaxHeight()) { viewModel.finishSleep() }
                    }
                }
            }
        }

        // POPUPS
        if (selectedSession != null) {
            SessionDetailDialog(selectedSession!!, cardBackgroundColor, primaryTextColor,
                onDismiss = { selectedSession = null },
                onDelete = { sessionToDelete = it; showDeleteConfirmation = true },
                onEdit = { sessionToEdit = it; selectedSession = null } // Abre el dialog de edición
            )
        }

        if (sessionToEdit != null) {
            EditSessionDialog(
                session = sessionToEdit!!, cardBg = cardBackgroundColor, textPrimary = primaryTextColor,
                onDismiss = { sessionToEdit = null },
                onSave = { updatedSession -> viewModel.updateSession(updatedSession); sessionToEdit = null }
            )
        }

        if (showAchievements) AchievementsDialog(state.history, isDark, cardBackgroundColor, primaryTextColor) { showAchievements = false }
        if (showDiaperDialog) DiaperDialog(cardBackgroundColor, primaryTextColor, { showDiaperDialog = false }) { type -> viewModel.addDiaperChange(type); showDiaperDialog = false }
        if (showDeleteConfirmation && sessionToDelete != null) {
            DeleteConfirmationDialog(sessionToDelete!!, cardBackgroundColor, primaryTextColor,
                onConfirm = { viewModel.deleteSession(sessionToDelete!!.id); showDeleteConfirmation = false; sessionToDelete = null },
                onDismiss = { showDeleteConfirmation = false; sessionToDelete = null })
        }
        if (showManualEntry) ManualEntryDialog(cardBackgroundColor, primaryTextColor, { showManualEntry = false }) { s -> viewModel.addManualSession(s); showManualEntry = false }
        if (showDiaperHistory) DiaperHistoryDialog(state.diaperChanges, cardBackgroundColor, primaryTextColor, secondaryTextColor, { showDiaperHistory = false }) { id -> viewModel.deleteDiaperChange(id) }
    }
}

// ==========================================
// VISTAS DE HISTORIAL (Sin cambios)
// ==========================================
@Composable
fun DateListView(history: List<SleepSession>, primaryTextColor: Color, secondaryTextColor: Color, onDateClick: (String) -> Unit) {
    val groupedByDate = history.filter { it.status == SleepStatus.FINALIZADO }.groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.startTime) }.toSortedMap(compareByDescending { it })
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
        items(groupedByDate.keys.toList()) { dateKey ->
            val sessions = groupedByDate[dateKey] ?: emptyList()
            var totalMillis = 0L
            sessions.forEach { val (h, m) = calculateRealSleepTime(it); totalMillis += (h * 60L + m) * 60000L }
            LiquidCard(modifier = Modifier.fillMaxWidth().clickable { onDateClick(dateKey) }) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(SimpleDateFormat("EEEE dd MMMM yyyy", Locale("es", "ES")).format(sessions.first().startTime).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = primaryTextColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☁️ Total: ${totalMillis / 3600000L}h ${(totalMillis % 3600000L) / 60000L}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF805AD5))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("📊 ${sessions.size} registros", style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                        }
                    }
                    Text("➜", fontSize = 24.sp, color = primaryTextColor)
                }
            }
        }
    }
}

@Composable
fun SessionsOfDayView(history: List<SleepSession>, selectedDate: String, primaryTextColor: Color, secondaryTextColor: Color, onBack: () -> Unit, onSessionClick: (SleepSession) -> Unit) {
    val sessions = history.filter { it.status == SleepStatus.FINALIZADO && SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.startTime) == selectedDate }.sortedByDescending { it.startTime }
    Column {
        LiquidCard(modifier = Modifier.fillMaxWidth().clickable { onBack() }) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Text("⬅", fontSize = 24.sp); Spacer(modifier = Modifier.width(12.dp)); Text("Volver a fechas", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = primaryTextColor) }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sessions) { session ->
                LiquidCard(modifier = Modifier.fillMaxWidth().clickable { onSessionClick(session) }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (session.type == SleepType.SIESTA) "🌤️" else "🌜", fontSize = 28.sp); Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val endFmt = session.endTime?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it) } ?: "..."
                            Text("${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(session.startTime)} - $endFmt", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                            val (hours, minutes) = calculateRealSleepTime(session)
                            Text(formatSleepDuration(hours, minutes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF805AD5))
                        }
                        Text("➜", fontSize = 20.sp, color = primaryTextColor)
                    }
                }
            }
        }
    }
}

// ==========================================
// EDICIÓN DE SESIÓN (NUEVO) Y ENTRADA MANUAL
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSessionDialog(session: SleepSession, cardBg: Color, textPrimary: Color, onDismiss: () -> Unit, onSave: (SleepSession) -> Unit) {
    var selectedType by remember { mutableStateOf(session.type) }
    var startDate by remember { mutableStateOf(session.startTime) }
    var endDate by remember { mutableStateOf(session.endTime ?: Date()) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("EEE dd MMM yyyy", Locale("es", "ES"))
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cardBg).border(3.dp, Brush.linearGradient(listOf(PastelBlue, PastelPurple)), RoundedCornerShape(28.dp))) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✏️ Editar Sesión", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { selectedType = SleepType.SIESTA }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == SleepType.SIESTA) PastelCream else Color.Gray.copy(alpha = 0.3f)), modifier = Modifier.weight(1f)) { Text("🌤️ Siesta", color = if(selectedType == SleepType.SIESTA) Color(0xFF2D3748) else Color.Gray) }
                    Button(onClick = { selectedType = SleepType.NOCHE }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == SleepType.NOCHE) Color(0xFF805AD5) else Color.Gray.copy(alpha = 0.3f)), modifier = Modifier.weight(1f)) { Text("🌜 Noche", color = Color.White) }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Inicio", style = MaterialTheme.typography.titleSmall, color = textPrimary, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f)) { Text(dateFormat.format(startDate), fontSize = 12.sp, color = textPrimary) }
                    OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f)) { Text(timeFormat.format(startDate), fontSize = 12.sp, color = textPrimary) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Fin", style = MaterialTheme.typography.titleSmall, color = textPrimary, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showEndDatePicker = true }, modifier = Modifier.weight(1f)) { Text(dateFormat.format(endDate), fontSize = 12.sp, color = textPrimary) }
                    OutlinedButton(onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f)) { Text(timeFormat.format(endDate), fontSize = 12.sp, color = textPrimary) }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar", color = textPrimary) }
                    Button(onClick = { if (endDate.time > startDate.time) onSave(session.copy(startTime = startDate, endTime = endDate, type = selectedType)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF805AD5)), modifier = Modifier.weight(1f)) { Text("Guardar", color = Color.White) }
                }
            }
        }
    }
    // Pickers simplificados
    if (showStartDatePicker) { DatePickerDialog(onDismissRequest = { showStartDatePicker = false }, confirmButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("OK") } }) { /* Picker UI */ } }
    if (showStartTimePicker) { val tps = rememberTimePickerState(initialHour = Calendar.getInstance().apply{time=startDate}.get(Calendar.HOUR_OF_DAY), initialMinute = Calendar.getInstance().apply{time=startDate}.get(Calendar.MINUTE)); AlertDialog(onDismissRequest = { showStartTimePicker = false }, confirmButton = { TextButton(onClick = { startDate = Calendar.getInstance().apply { time = startDate; set(Calendar.HOUR_OF_DAY, tps.hour); set(Calendar.MINUTE, tps.minute) }.time; showStartTimePicker = false }) { Text("OK") } }, text = { TimePicker(tps) }) }
    if (showEndDatePicker) { DatePickerDialog(onDismissRequest = { showEndDatePicker = false }, confirmButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("OK") } }) { /* Picker UI */ } }
    if (showEndTimePicker) { val tps = rememberTimePickerState(initialHour = Calendar.getInstance().apply{time=endDate}.get(Calendar.HOUR_OF_DAY), initialMinute = Calendar.getInstance().apply{time=endDate}.get(Calendar.MINUTE)); AlertDialog(onDismissRequest = { showEndTimePicker = false }, confirmButton = { TextButton(onClick = { endDate = Calendar.getInstance().apply { time = endDate; set(Calendar.HOUR_OF_DAY, tps.hour); set(Calendar.MINUTE, tps.minute) }.time; showEndTimePicker = false }) { Text("OK") } }, text = { TimePicker(tps) }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryDialog(cardBg: Color, textPrimary: Color, onDismiss: () -> Unit, onSave: (SleepSession) -> Unit) {
    var selectedType by remember { mutableStateOf(SleepType.NOCHE) }
    var startDate by remember { mutableStateOf(Date(System.currentTimeMillis() - 8 * 3600000)) }
    var endDate by remember { mutableStateOf(Date()) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("EEE dd MMM", Locale("es", "ES"))
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cardBg).border(3.dp, Brush.linearGradient(listOf(PastelBlue, PastelPurple)), RoundedCornerShape(28.dp))) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📝 Registro Manual", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { selectedType = SleepType.SIESTA }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == SleepType.SIESTA) PastelCream else Color.Gray.copy(alpha = 0.3f)), modifier = Modifier.weight(1f)) { Text("🌤️ Siesta", color = if(selectedType == SleepType.SIESTA) Color(0xFF2D3748) else Color.Gray) }
                    Button(onClick = { selectedType = SleepType.NOCHE }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == SleepType.NOCHE) Color(0xFF805AD5) else Color.Gray.copy(alpha = 0.3f)), modifier = Modifier.weight(1f)) { Text("🌜 Noche", color = Color.White) }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Inicio", style = MaterialTheme.typography.titleSmall, color = textPrimary, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.fillMaxWidth()) { Text("${dateFormat.format(startDate)} - ${timeFormat.format(startDate)}", fontSize = 14.sp, color = textPrimary) }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Fin", style = MaterialTheme.typography.titleSmall, color = textPrimary, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { showEndTimePicker = true }, modifier = Modifier.fillMaxWidth()) { Text("${dateFormat.format(endDate)} - ${timeFormat.format(endDate)}", fontSize = 14.sp, color = textPrimary) }
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar", color = textPrimary) }
                    Button(onClick = { if (endDate.time > startDate.time) onSave(SleepSession(startTime = startDate, endTime = endDate, type = selectedType, status = SleepStatus.FINALIZADO)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF805AD5)), modifier = Modifier.weight(1f)) { Text("Guardar", color = Color.White) }
                }
            }
        }
    }
    if (showStartTimePicker) { val tps = rememberTimePickerState(initialHour = Calendar.getInstance().apply{time=startDate}.get(Calendar.HOUR_OF_DAY), initialMinute = Calendar.getInstance().apply{time=startDate}.get(Calendar.MINUTE)); AlertDialog(onDismissRequest = { showStartTimePicker = false }, confirmButton = { TextButton(onClick = { startDate = Calendar.getInstance().apply { time = startDate; set(Calendar.HOUR_OF_DAY, tps.hour); set(Calendar.MINUTE, tps.minute) }.time; showStartTimePicker = false }) { Text("OK") } }, text = { TimePicker(tps) }) }
    if (showEndTimePicker) { val tps = rememberTimePickerState(initialHour = Calendar.getInstance().apply{time=endDate}.get(Calendar.HOUR_OF_DAY), initialMinute = Calendar.getInstance().apply{time=endDate}.get(Calendar.MINUTE)); AlertDialog(onDismissRequest = { showEndTimePicker = false }, confirmButton = { TextButton(onClick = { endDate = Calendar.getInstance().apply { time = endDate; set(Calendar.HOUR_OF_DAY, tps.hour); set(Calendar.MINUTE, tps.minute) }.time; showEndTimePicker = false }) { Text("OK") } }, text = { TimePicker(tps) }) }
}

@Composable
fun SessionDetailDialog(session: SleepSession, cardBg: Color, textPrimary: Color, onDismiss: () -> Unit, onDelete: (SleepSession) -> Unit, onEdit: (SleepSession) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cardBg).border(3.dp, Brush.linearGradient(listOf(PastelPink, PastelPurple)), RoundedCornerShape(28.dp))) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                // ICONOS ARRIBA: EDITAR Y ELIMINAR
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onEdit(session) }) { Text("✏️", fontSize = 20.sp) }
                    IconButton(onClick = { onDelete(session) }) { Text("🗑️", fontSize = 20.sp) }
                }
                val (hours, minutes) = calculateRealSleepTime(session)
                Text(formatSleepDuration(hours, minutes), fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF805AD5))
                Text("Tiempo Total de Sueño", style = MaterialTheme.typography.bodyMedium, color = textPrimary.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(24.dp))
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                TimelineEvent(timeFormat.format(session.startTime), "Se durmió", if (session.type == SleepType.SIESTA) "🌤️" else "🌜", PastelBlue, true, session.interruptions.isEmpty() && session.endTime == null, textPrimary)
                session.interruptions.forEach { interruption ->
                    TimelineEvent(timeFormat.format(interruption.wokeUpAt), "Despertó", "✨", PastelPink, false, false, textPrimary)
                    interruption.backToSleepAt?.let { backTime -> TimelineEvent(timeFormat.format(backTime), "Volvió a dormir", "💤", PastelBlue, false, false, textPrimary) }
                }
                session.endTime?.let { end -> TimelineEvent(timeFormat.format(end), "Despertó Definitivo", "🏁", PastelPurple, false, true, textPrimary) }
                Spacer(modifier = Modifier.height(32.dp))
                BabyStyledButton("Cerrar", color = Color(0xFF805AD5), textColor = Color.White, modifier = Modifier.fillMaxWidth().height(50.dp)) { onDismiss() }
            }
        }
    }
}

// ==========================================
// RESTO DE DIALOGOS: Achievements, Diapers, etc (se mantienen como estaban)
// ==========================================
@Composable
fun AchievementsDialog(history: List<SleepSession>, isDark: Boolean, cardBg: Color, textPrimary: Color, onDismiss: () -> Unit) {
    val milestones = remember(history) { checkMilestones(history) }
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cardBg).border(3.dp, Brush.linearGradient(listOf(Color(0xFFECC94B), Color(0xFFDD6B20))), RoundedCornerShape(28.dp))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Logros Semanales 🏆", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(350.dp)) {
                    items(milestones) { milestone ->
                        val itemBg = if (milestone.isUnlocked) if(isDark) Color(0xFF2B6CB0).copy(alpha=0.3f) else PastelBlue.copy(alpha=0.4f) else if(isDark) Color.Black.copy(alpha=0.3f) else Color.LightGray.copy(alpha=0.2f)
                        Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(itemBg).border(1.dp, if (milestone.isUnlocked) PastelBlue else Color.Transparent, RoundedCornerShape(16.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(milestone.emoji, fontSize = 32.sp, color = if (milestone.isUnlocked) Color.Unspecified else Color.Gray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(milestone.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (milestone.isUnlocked) textPrimary else Color.Gray, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(milestone.description, style = MaterialTheme.typography.bodySmall, color = if (milestone.isUnlocked) textPrimary.copy(alpha=0.8f) else Color.Gray, textAlign = TextAlign.Center, lineHeight = 12.sp, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                BabyStyledButton("Cerrar", color = Color(0xFF805AD5), textColor = Color.White, modifier = Modifier.fillMaxWidth().height(50.dp)) { onDismiss() }
            }
        }
    }
}

@Composable
fun DiaperDialog(cardBg: Color, textPrimary: Color, onDismiss: () -> Unit, onAddDiaper: (DiaperType) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cardBg).border(3.dp, Brush.linearGradient(listOf(PastelBlue, PastelPink)), RoundedCornerShape(28.dp))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("¿Qué pasó? 🫧", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BabyStyledButton("💧 Orina", color = PastelBlue, textColor = Color(0xFF2D3748), modifier = Modifier.fillMaxWidth().height(60.dp)) { onAddDiaper(DiaperType.ORINA) }
                    BabyStyledButton("🤎 Sólido", color = Color(0xFFEDDFD6), textColor = Color(0xFF5C4033), modifier = Modifier.fillMaxWidth().height(60.dp)) { onAddDiaper(DiaperType.POPO) }
                    BabyStyledButton("💧🤎 Ambos", color = Color(0xFFE9D8FD), textColor = Color(0xFF44337A), modifier = Modifier.fillMaxWidth().height(60.dp)) { onAddDiaper(DiaperType.AMBOS) }
                }
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onDismiss) { Text("Cancelar", color = textPrimary) }
            }
        }
    }
}

@Composable
fun DiaperHistoryDialog(diaperChanges: List<DiaperChange>, cardBg: Color, textPrimary: Color, secondaryTextColor: Color, onDismiss: () -> Unit, onDelete: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).clip(RoundedCornerShape(28.dp)).background(cardBg).border(3.dp, Brush.linearGradient(listOf(PastelBlue, PastelPink)), RoundedCornerShape(28.dp))) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Historial de Pañales", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(diaperChanges.sortedByDescending { it.timestamp }) { change ->
                        val itemColor = when(change.type) { DiaperType.ORINA -> PastelBlue.copy(alpha=0.3f); DiaperType.POPO -> Color(0xFFEDDFD6).copy(alpha=0.5f); DiaperType.AMBOS -> Color(0xFFE9D8FD).copy(alpha=0.5f) }
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(itemColor).padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(when(change.type){ DiaperType.ORINA -> "💧"; DiaperType.POPO -> "🤎"; DiaperType.AMBOS -> "💧🤎" }, fontSize = 28.sp); Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(when(change.type){ DiaperType.ORINA -> "Orina"; DiaperType.POPO -> "Sólido"; DiaperType.AMBOS -> "Orina y Sólido" }, fontWeight = FontWeight.Bold, color = textPrimary)
                                    Text(SimpleDateFormat("EEE dd MMM, hh:mm a", Locale("es", "ES")).format(change.timestamp).replaceFirstChar{it.uppercase()}, fontSize = 12.sp, color = secondaryTextColor)
                                }
                                IconButton(onClick = { onDelete(change.id) }) { Text("🗑️", fontSize = 18.sp) }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                BabyStyledButton("Cerrar", color = Color(0xFF805AD5), textColor = Color.White, modifier = Modifier.fillMaxWidth().height(50.dp)) { onDismiss() }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(session: SleepSession, cardBg: Color, textPrimary: Color, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚠️ Confirmar") },
        text = { Text("¿Eliminar este registro?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Eliminar", color = Color(0xFFE53E3E)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = textPrimary) } },
        containerColor = cardBg
    )
}

@Composable
fun TimelineEvent(time: String, title: String, icon: String, color: Color, isFirst: Boolean, isLast: Boolean, textColor: Color) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.width(48.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            if (!isFirst) Box(modifier = Modifier.width(2.dp).height(18.dp).background(Color.LightGray.copy(alpha = 0.5f)).align(Alignment.TopCenter))
            if (!isLast) Box(modifier = Modifier.padding(top = 18.dp).width(2.dp).fillMaxHeight().background(Color.LightGray.copy(alpha = 0.5f)).align(Alignment.TopCenter))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)).border(2.dp, color, CircleShape).align(Alignment.TopCenter)) { Text(text = icon, fontSize = 16.sp, textAlign = TextAlign.Center) }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)) {
            Text(text = time, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.ExtraBold)
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = textColor)
        }
    }
}