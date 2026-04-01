package com.elyefris.khalessleeptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import kotlinx.coroutines.launch

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

// --- COMPONENTES VISUALES ---

@Composable
fun BabyStyledButton(
    text: String, subText: String? = null, icon: String? = null,
    color: Color, textColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.shadow(4.dp, RoundedCornerShape(18.dp), spotColor = color.copy(alpha = 0.4f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (icon != null) Text(icon, fontSize = 22.sp)
            Text(text, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (subText != null) Text(subText, color = textColor.copy(alpha = 0.8f), fontSize = 11.sp)
        }
    }
}

@Composable
fun MenuIcon(icon: String, label: String, bgColor: Color, textColor: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }.padding(4.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(bgColor).shadow(2.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 22.sp) }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SleepViewModel) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val backgroundBrush = if (isDark) Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))
    else Brush.linearGradient(listOf(PastelBlue, PastelPurple, PastelPink))

    val primaryTextColor = if (isDark) Color(0xFFF7FAFC) else Color(0xFF2D3748)
    val secondaryTextColor = if (isDark) Color(0xFFA0AEC0) else Color(0xFF718096)
    val cardBackgroundColor = if (isDark) Color(0xFF1A202C).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)

    var selectedSession by remember { mutableStateOf<SleepSession?>(null) }
    var sessionToEdit by remember { mutableStateOf<SleepSession?>(null) }
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
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        containerColor = if (isDark) Color(0xFF805AD5) else Color(0xFF9F7AEA).copy(alpha = 0.95f),
                        contentColor = Color.White, shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)
                    ) { Text(data.visuals.message, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                }
            },
            bottomBar = {
                Surface(
                    color = cardBackgroundColor,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth().height(65.dp).navigationBarsPadding()
                    ) {
                        if (state.session == null || state.session?.status == SleepStatus.FINALIZADO) {
                            BabyStyledButton("Siesta", icon="🌤️", color = if(isDark) Color(0xFF4A5568) else PastelCream, textColor = if(isDark) Color.White else Color(0xFF2D3748), modifier = Modifier.weight(1f).fillMaxHeight()) { viewModel.startNap() }
                            BabyStyledButton("Noche", icon="🌜", color = Color(0xFF805AD5), textColor = Color.White, modifier = Modifier.weight(1f).fillMaxHeight()) { viewModel.startNight() }
                        } else {
                            if (state.session?.status == SleepStatus.DURMIENDO) {
                                BabyStyledButton("¡Despertó! ✨", color = PastelPink, textColor = Color(0xFF2D3748), modifier = Modifier.fillMaxWidth().fillMaxHeight()) { viewModel.wakeUp() }
                            } else if (state.session?.status == SleepStatus.DESPIERTO) {
                                BabyStyledButton("Reanudar", "Volvió a dormir", color = PastelBlue, textColor = Color(0xFF2D3748), modifier = Modifier.weight(1f).fillMaxHeight()) { viewModel.backToSleep() }
                                BabyStyledButton("Finalizar", "Guardar", color = Color(0xFFE53E3E), textColor = Color.White, modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    viewModel.finishSleep(); scope.launch { snackbarHostState.showSnackbar("✅ Sesión guardada") }
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            // --- ESTRUCTURA PRINCIPAL DIVIDIDA ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding() + 24.dp,
                        bottom = paddingValues.calculateBottomPadding(), // Solo margen para el BottomBar
                        start = 20.dp,
                        end = 20.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- PARTE 1: FIJA (NO SCROLLABLE) ---

                // MENÚ SUPERIOR FIJO
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    MenuIcon("🫧", "Pañal", cardBackgroundColor, primaryTextColor) { showDiaperDialog = true }
                    MenuIcon("📋", "Pañales", cardBackgroundColor, primaryTextColor) { showDiaperHistory = true }
                    MenuIcon("📝", "Manual", cardBackgroundColor, primaryTextColor) { showManualEntry = true }
                    MenuIcon("🏆", "Logros", cardBackgroundColor, primaryTextColor) { showAchievements = true }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TEMPORIZADOR FIJO
                LiquidCard(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (state.session?.status) { SleepStatus.DURMIENDO -> if (state.session?.type == SleepType.SIESTA) "Siesta Activa 🌤️" else "Dulces Sueños 🌜"; SleepStatus.DESPIERTO -> "En Pausa ✨"; else -> "Listo para dormir" },
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = primaryTextColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        state.session?.let { session ->
                            if (session.status != SleepStatus.FINALIZADO) {
                                val diff = currentTimeMillis - session.startTime.time
                                Text("${TimeUnit.MILLISECONDS.toHours(diff)}h ${TimeUnit.MILLISECONDS.toMinutes(diff) % 60}m", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = if (isDark) Color.White else Color(0xFF6B46C1))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Desde: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(session.startTime)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = secondaryTextColor)
                            } else { Text("🧸", fontSize = 56.sp); Spacer(modifier = Modifier.height(8.dp)) }
                        } ?: run { Text("🧸", fontSize = 56.sp); Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- PARTE 2: ZONA SCROLLABLE (Gráfica e Historial) ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Esto hace que ocupe todo el espacio sobrante debajo del temporizador
                        .animateContentSize(),
                    contentPadding = PaddingValues(bottom = 16.dp), // Espacio final antes del botón de siesta
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // GRÁFICA SEMANAL COMPACTA
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Tendencia Semanal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = secondaryTextColor, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                            LiquidCard(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                                WeeklySleepGraph(history = state.history, primaryTextColor = primaryTextColor, secondaryTextColor = secondaryTextColor, barColor = Color(0xFF805AD5), isDark = isDark)
                            }
                        }
                    }

                    // TÍTULO DEL HISTORIAL
                    item {
                        Text("Historial de Sueño", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = secondaryTextColor, modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp, start = 4.dp))
                    }

                    // LISTA DEL HISTORIAL
                    if (selectedDate == null) {
                        val groupedByDate = state.history.filter { it.status == SleepStatus.FINALIZADO }.groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.startTime) }.toSortedMap(compareByDescending { it })
                        if (groupedByDate.isEmpty()) {
                            item { Text("Aún no hay registros.", color = secondaryTextColor) }
                        } else {
                            items(groupedByDate.keys.toList()) { dateKey ->
                                val sessions = groupedByDate[dateKey] ?: emptyList()
                                var totalMillis = 0L
                                sessions.forEach { val (h, m) = calculateRealSleepTime(it); totalMillis += (h * 60L + m) * 60000L }
                                LiquidCard(modifier = Modifier.fillMaxWidth().clickable { selectedDate = dateKey }) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(SimpleDateFormat("EEEE dd MMMM", Locale("es", "ES")).format(sessions.first().startTime).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("☁️ ${totalMillis / 3600000L}h ${(totalMillis % 3600000L) / 60000L}m", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFF805AD5))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text("📊 ${sessions.size} regs", style = MaterialTheme.typography.labelMedium, color = secondaryTextColor)
                                            }
                                        }
                                        Text("➜", fontSize = 18.sp, color = secondaryTextColor)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            LiquidCard(modifier = Modifier.fillMaxWidth().clickable { selectedDate = null }) {
                                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Text("⬅", fontSize = 18.sp); Spacer(modifier = Modifier.width(8.dp)); Text("Volver", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryTextColor) }
                            }
                        }
                        val sessions = state.history.filter { it.status == SleepStatus.FINALIZADO && SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.startTime) == selectedDate }.sortedByDescending { it.startTime }
                        items(sessions) { session ->
                            LiquidCard(modifier = Modifier.fillMaxWidth().clickable { selectedSession = session }) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (session.type == SleepType.SIESTA) "🌤️" else "🌜", fontSize = 22.sp); Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        val endFmt = session.endTime?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it) } ?: "..."
                                        Text("${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(session.startTime)} - $endFmt", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                                        val (hours, minutes) = calculateRealSleepTime(session)
                                        Text(formatSleepDuration(hours, minutes), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF805AD5))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DIÁLOGOS Y MODALES ---
        if (selectedSession != null) {
            SessionDetailDialog(selectedSession!!, cardBackgroundColor, primaryTextColor,
                onDismiss = { selectedSession = null }, onDelete = { sessionToDelete = it; showDeleteConfirmation = true }, onEdit = { sessionToEdit = it; selectedSession = null })
        }

        if (sessionToEdit != null) {
            EditSessionDialog(sessionToEdit!!, cardBackgroundColor, primaryTextColor, onDismiss = { sessionToEdit = null }, onSave = { updatedSession ->
                viewModel.updateSession(updatedSession); sessionToEdit = null; scope.launch { snackbarHostState.showSnackbar("✏️ Actualizado") }
            })
        }

        if (showAchievements) AchievementsDialog(state.history, isDark, cardBackgroundColor, primaryTextColor) { showAchievements = false }

        if (showDiaperDialog) DiaperDialog(cardBackgroundColor, primaryTextColor, onDismiss = { showDiaperDialog = false }) { type ->
            viewModel.addDiaperChange(type, "", Date()); showDiaperDialog = false; scope.launch { snackbarHostState.showSnackbar("✅ Pañal guardado") }
        }

        if (showDeleteConfirmation && sessionToDelete != null) {
            DeleteConfirmationDialog(sessionToDelete!!, cardBackgroundColor, primaryTextColor,
                onConfirm = { viewModel.deleteSession(sessionToDelete!!.id); showDeleteConfirmation = false; sessionToDelete = null; scope.launch { snackbarHostState.showSnackbar("🗑️ Eliminado") } },
                onDismiss = { showDeleteConfirmation = false; sessionToDelete = null })
        }

        if (showManualEntry) ManualEntryDialog(cardBackgroundColor, primaryTextColor, { showManualEntry = false }) { s ->
            viewModel.addManualSession(s); showManualEntry = false; scope.launch { snackbarHostState.showSnackbar("✅ Guardado") }
        }

        if (showDiaperHistory) DiaperHistoryDialog(state.diaperChanges, cardBackgroundColor, primaryTextColor, secondaryTextColor, { showDiaperHistory = false }) { id -> viewModel.deleteDiaperChange(id) }
    }
}

// ==========================================
// GRÁFICA DE TENDENCIA SEMANAL REPARADA Y ANIMADA
// ==========================================
@Composable
fun WeeklySleepGraph(history: List<SleepSession>, primaryTextColor: Color, secondaryTextColor: Color, barColor: Color, isDark: Boolean) {
    val last7Days = (6 downTo 0).map { i -> val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -i); cal.time }
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFmt = SimpleDateFormat("EEE", Locale("es", "ES"))
    val grouped = history.filter { it.status == SleepStatus.FINALIZADO }.groupBy { dateFmt.format(it.startTime) }

    val dataPoints = last7Days.map { date ->
        val sessions = grouped[dateFmt.format(date)] ?: emptyList()
        var totalMillis = 0L
        sessions.forEach { val diff = (it.endTime?.time ?: 0L) - it.startTime.time; if(diff > 0) totalMillis += diff }
        Pair(dayFmt.format(date).replaceFirstChar { it.uppercase() }.take(3), totalMillis / 3600000f)
    }

    val maxHours = (dataPoints.maxOfOrNull { it.second } ?: 12f).coerceAtLeast(8f)
    val barGradient = Brush.verticalGradient(colors = listOf(barColor.copy(alpha = 0.5f), barColor))

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        dataPoints.forEach { (day, hours) ->
            val targetHeight = if (maxHours > 0) (hours / maxHours).coerceAtMost(1f) else 0f

            var animationPlayed by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { animationPlayed = true }

            val animatedHeight by animateFloatAsState(
                targetValue = if (animationPlayed) targetHeight.coerceAtLeast(0.02f) else 0.02f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                label = "barAnimation"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
                if (hours > 0) {
                    Text(String.format(Locale.US, "%.1f", hours), color = primaryTextColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                } else {
                    Text("-", color = secondaryTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.width(16.dp).weight(1f).clip(RoundedCornerShape(50))
                        .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(animatedHeight).clip(RoundedCornerShape(50))
                            .background(if (hours > 0) barGradient else SolidColor(Color.Transparent))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(day, color = if (hours > 0) primaryTextColor else secondaryTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// MODALES (Edición, Manual, Pañales, Logros)
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
    if (showStartDatePicker) { val dps = rememberDatePickerState(initialSelectedDateMillis = startDate.time); DatePickerDialog(onDismissRequest = { showStartDatePicker = false }, confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { startDate = Date(it) }; showStartDatePicker = false }) { Text("OK") } }) { DatePicker(dps) } }
    if (showStartTimePicker) { val tps = rememberTimePickerState(initialHour = Calendar.getInstance().apply{time=startDate}.get(Calendar.HOUR_OF_DAY), initialMinute = Calendar.getInstance().apply{time=startDate}.get(Calendar.MINUTE)); AlertDialog(onDismissRequest = { showStartTimePicker = false }, confirmButton = { TextButton(onClick = { startDate = Calendar.getInstance().apply { time = startDate; set(Calendar.HOUR_OF_DAY, tps.hour); set(Calendar.MINUTE, tps.minute) }.time; showStartTimePicker = false }) { Text("OK") } }, text = { TimePicker(tps) }) }
    if (showEndDatePicker) { val dps = rememberDatePickerState(initialSelectedDateMillis = endDate.time); DatePickerDialog(onDismissRequest = { showEndDatePicker = false }, confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { endDate = Date(it) }; showEndDatePicker = false }) { Text("OK") } }) { DatePicker(dps) } }
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

// ==========================================
// RESTO DE FUNCIONES Y DETALLES
// ==========================================

@Composable
fun SessionDetailDialog(session: SleepSession, cardBg: Color, textPrimary: Color, onDismiss: () -> Unit, onDelete: (SleepSession) -> Unit, onEdit: (SleepSession) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cardBg).border(3.dp, Brush.linearGradient(listOf(PastelPink, PastelPurple)), RoundedCornerShape(28.dp))) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
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