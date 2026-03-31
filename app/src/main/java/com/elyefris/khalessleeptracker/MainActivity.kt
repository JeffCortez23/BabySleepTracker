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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.elyefris.khalessleeptracker.data.model.DiaperChange
import com.elyefris.khalessleeptracker.data.model.DiaperType
import com.elyefris.khalessleeptracker.data.model.SleepSession
import com.elyefris.khalessleeptracker.data.model.SleepStatus
import com.elyefris.khalessleeptracker.data.model.SleepType
import com.elyefris.khalessleeptracker.data.repository.FirebaseSleepRepository
import com.elyefris.khalessleeptracker.ui.components.LiquidCard
import com.elyefris.khalessleeptracker.ui.theme.*
import com.elyefris.khalessleeptracker.utils.calculateRealSleepTime
import com.elyefris.khalessleeptracker.utils.formatSleepDuration
import com.elyefris.khalessleeptracker.viewmodel.SleepViewModel
import com.elyefris.khalessleeptracker.data.model.Interruption
import com.elyefris.khalessleeptracker.data.model.checkMilestones
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
                @Suppress("UNCHECKED_CAST")
                return SleepViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KhalesSleepTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: SleepViewModel) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val backgroundBrush = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF240046)))
    } else {
        Brush.linearGradient(listOf(PastelBlue, PastelPurple, PastelPink))
    }

    // CONTRASTES MEJORADOS (Gris Pizarra elegante)
    val primaryTextColor = if (isDark) Color(0xFFF7FAFC) else Color(0xFF2D3748)
    val secondaryTextColor = if (isDark) Color(0xFFA0AEC0) else Color(0xFF718096)
    val cardBackgroundColor = if (isDark) Color(0xFF1A202C).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)

    var selectedSession by remember { mutableStateOf<SleepSession?>(null) }
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
            while (true) {
                currentTimeMillis = System.currentTimeMillis()
                delay(60000L)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallFloatingActionButton(
                        onClick = { showDiaperDialog = true },
                        containerColor = if (isDark) Color(0xFF2D3748) else Color.White.copy(alpha = 0.9f)
                    ) { Text("🫧", fontSize = 20.sp) } // Burbujas en lugar de pañal

                    SmallFloatingActionButton(
                        onClick = { showDiaperHistory = true },
                        containerColor = if (isDark) Color(0xFF2D3748) else Color.White.copy(alpha = 0.9f)
                    ) { Text("📋", fontSize = 20.sp) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallFloatingActionButton(
                        onClick = { showManualEntry = true },
                        containerColor = if (isDark) Color(0xFF2D3748) else Color.White.copy(alpha = 0.9f)
                    ) { Text("📝", fontSize = 20.sp) }

                    SmallFloatingActionButton(
                        onClick = { showAchievements = true },
                        containerColor = if (isDark) Color(0xFF2D3748) else Color.White.copy(alpha = 0.9f)
                    ) { Icon(imageVector = Icons.Default.Star, contentDescription = "Logros", tint = Color(0xFFECC94B)) }
                }
            }

            // --- 1. TARJETA PRINCIPAL ---
            LiquidCard(modifier = Modifier.size(width = 340.dp, height = 240.dp)) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (state.session?.status) {
                            SleepStatus.DURMIENDO -> if (state.session?.type == SleepType.SIESTA) "Siesta Activa 🌤️" else "Dulces Sueños 🌜"
                            SleepStatus.DESPIERTO -> "En Pausa ✨"
                            else -> "Listo para dormir"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    state.session?.let { session ->
                        if (session.status != SleepStatus.FINALIZADO) {
                            val diffMillis = currentTimeMillis - session.startTime.time
                            val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis) % 60

                            Text(
                                text = "${hours}h ${minutes}m",
                                fontSize = 52.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF6B46C1) // Purpura más profundo para contraste
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            Text(
                                text = "Desde: ${formatter.format(session.startTime)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryTextColor
                            )
                        } else {
                            Text("🧸", fontSize = 72.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } ?: run {
                        Text("🧸", fontSize = 72.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            // --- 2. HISTORIAL ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Historial de Sueño",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                if (selectedDate == null) {
                    DateListView(
                        history = state.history,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        onDateClick = { selectedDate = it }
                    )
                } else {
                    SessionsOfDayView(
                        history = state.history,
                        selectedDate = selectedDate!!,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        onBack = { selectedDate = null },
                        onSessionClick = { selectedSession = it }
                    )
                }
            }

            // --- 3. BOTONES ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                if (state.session == null || state.session?.status == SleepStatus.FINALIZADO) {
                    Button(
                        onClick = { viewModel.startNap() },
                        colors = ButtonDefaults.buttonColors(containerColor = if(isDark) Color(0xFF4A5568) else PastelCream),
                        elevation = ButtonDefaults.buttonElevation(4.dp),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌤️", fontSize = 24.sp)
                            Text("Siesta", color = if(isDark) Color.White else Color(0xFF2D3748), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { viewModel.startNight() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF805AD5)), // Morado suave pero visible
                        elevation = ButtonDefaults.buttonElevation(4.dp),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌜", fontSize = 24.sp)
                            Text("Noche", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    if (state.session?.status == SleepStatus.DURMIENDO) {
                        Button(
                            onClick = { viewModel.wakeUp() },
                            colors = ButtonDefaults.buttonColors(containerColor = PastelPink),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth().fillMaxHeight()
                        ) {
                            Text("¡Despertó! ✨", color = Color(0xFF2D3748), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                    } else if (state.session?.status == SleepStatus.DESPIERTO) {
                        Button(
                            onClick = { viewModel.backToSleep() },
                            colors = ButtonDefaults.buttonColors(containerColor = PastelBlue),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Reanudar", color = Color(0xFF2D3748), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Volvió a dormir", color = Color(0xFF2D3748).copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.finishSleep() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E).copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Finalizar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Guardar", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- POPUPS ---
        if (selectedSession != null) {
            SessionDetailDialog(
                session = selectedSession!!,
                cardBg = cardBackgroundColor,
                textPrimary = primaryTextColor,
                onDismiss = { selectedSession = null },
                onDelete = { session ->
                    sessionToDelete = session
                    showDeleteConfirmation = true
                }
            )
        }

        if (showAchievements) {
            AchievementsDialog(
                history = state.history,
                isDark = isDark,
                cardBg = cardBackgroundColor,
                textPrimary = primaryTextColor,
                onDismiss = { showAchievements = false }
            )
        }

        if (showDiaperDialog) {
            DiaperDialog(
                cardBg = cardBackgroundColor,
                textPrimary = primaryTextColor,
                onDismiss = { showDiaperDialog = false },
                onAddDiaper = { type ->
                    viewModel.addDiaperChange(type, "", Date())
                    showDiaperDialog = false
                }
            )
        }

        if (showDeleteConfirmation && sessionToDelete != null) {
            DeleteConfirmationDialog(
                session = sessionToDelete!!,
                isDark = isDark,
                cardBg = cardBackgroundColor,
                textPrimary = primaryTextColor,
                onConfirm = {
                    viewModel.deleteSession(sessionToDelete!!.id)
                    showDeleteConfirmation = false
                    selectedSession = null
                    sessionToDelete = null
                },
                onDismiss = {
                    showDeleteConfirmation = false
                    sessionToDelete = null
                }
            )
        }

        if (showManualEntry) {
            ManualEntryDialog(
                isDark = isDark,
                cardBg = cardBackgroundColor,
                textPrimary = primaryTextColor,
                onDismiss = { showManualEntry = false },
                onSave = { session ->
                    viewModel.addManualSession(session)
                    showManualEntry = false
                }
            )
        }

        if (showDiaperHistory) {
            DiaperHistoryDialog(
                diaperChanges = state.diaperChanges,
                cardBg = cardBackgroundColor,
                textPrimary = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                onDismiss = { showDiaperHistory = false },
                onDelete = { diaperId ->
                    viewModel.deleteDiaperChange(diaperId)
                }
            )
        }
    }
}

// ==========================================
// VISTAS DE HISTORIAL
// ==========================================
@Composable
fun DateListView(history: List<SleepSession>, primaryTextColor: Color, secondaryTextColor: Color, onDateClick: (String) -> Unit) {
    val groupedByDate = history.filter { it.status == SleepStatus.FINALIZADO }
        .groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.startTime) }
        .toSortedMap(compareByDescending { it })

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
        items(groupedByDate.keys.toList()) { dateKey ->
            val sessions = groupedByDate[dateKey] ?: emptyList()
            val date = sessions.first().startTime
            val dateFmt = SimpleDateFormat("EEEE dd MMMM yyyy", Locale("es", "ES")).format(date)
            var totalMillis = 0L
            sessions.forEach {
                val (h, m) = calculateRealSleepTime(it)
                totalMillis += (h * 60L + m) * 60000L
            }
            val totalHours = totalMillis / 3600000L
            val totalMinutes = (totalMillis % 3600000L) / 60000L

            LiquidCard(modifier = Modifier.fillMaxWidth().clickable { onDateClick(dateKey) }) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(dateFmt.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = primaryTextColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☁️ Total: ${totalHours}h ${totalMinutes}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF805AD5))
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
    val sessions = history.filter { it.status == SleepStatus.FINALIZADO }
        .filter { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.startTime) == selectedDate }
        .sortedByDescending { it.startTime }

    Column {
        LiquidCard(modifier = Modifier.fillMaxWidth().clickable { onBack() }) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⬅", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Volver a fechas", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = primaryTextColor)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sessions) { session ->
                LiquidCard(modifier = Modifier.fillMaxWidth().clickable { onSessionClick(session) }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (session.type == SleepType.SIESTA) "🌤️" else "🌜", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(session.startTime)
                            val endFmt = session.endTime?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it) } ?: "..."
                            Text("$timeFmt - $endFmt", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
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
// MODALES RESTAURADOS Y ESTILIZADOS
// ==========================================

@Composable
fun AchievementsDialog(history: List<SleepSession>, isDark: Boolean, cardBg: Color, textPrimary: Color, onDismiss: () -> Unit) {
    val milestones = remember(history) { checkMilestones(history) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cardBg)
                .border(3.dp, Brush.linearGradient(listOf(Color(0xFFECC94B), Color(0xFFDD6B20))), RoundedCornerShape(28.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Logros 🏆", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(350.dp)
                ) {
                    items(milestones) { milestone ->
                        val itemBg = if (milestone.isUnlocked) {
                            if(isDark) Color(0xFF2B6CB0).copy(alpha=0.3f) else PastelBlue.copy(alpha=0.4f)
                        } else {
                            if(isDark) Color.Black.copy(alpha=0.3f) else Color.LightGray.copy(alpha=0.2f)
                        }

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(itemBg)
                                .border(1.dp, if (milestone.isUnlocked) PastelBlue else Color.Transparent, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = milestone.emoji,
                                fontSize = 32.sp,
                                color = if (milestone.isUnlocked) Color.Unspecified else Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = milestone.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (milestone.isUnlocked) textPrimary else Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = milestone.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (milestone.isUnlocked) textPrimary.copy(alpha=0.8f) else Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF805AD5))) {
                    Text("Cerrar", color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryDialog(isDark: Boolean, cardBg: Color, textPrimary: Color, onDismiss: () -> Unit, onSave: (SleepSession) -> Unit) {
    var selectedType by remember { mutableStateOf(SleepType.NOCHE) }
    var startDate by remember { mutableStateOf(Date(System.currentTimeMillis() - 8 * 3600000)) } // 8 horas atrás por defecto
    var endDate by remember { mutableStateOf(Date()) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("EEE dd MMM yyyy", Locale("es", "ES"))
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).clip(RoundedCornerShape(28.dp)).background(cardBg)
                .border(3.dp, Brush.linearGradient(listOf(PastelBlue, PastelPurple)), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📝 Registro Manual", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { selectedType = SleepType.SIESTA },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == SleepType.SIESTA) PastelCream else Color.Gray.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) { Text("🌤️ Siesta", color = if(selectedType == SleepType.SIESTA) Color(0xFF2D3748) else Color.Gray) }

                    Button(
                        onClick = { selectedType = SleepType.NOCHE },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == SleepType.NOCHE) Color(0xFF805AD5) else Color.Gray.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) { Text("🌜 Noche", color = Color.White) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Inicio", style = MaterialTheme.typography.titleSmall, color = textPrimary, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(dateFormat.format(startDate), fontSize = 12.sp, color = textPrimary)
                    }
                    OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(timeFormat.format(startDate), fontSize = 12.sp, color = textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Fin", style = MaterialTheme.typography.titleSmall, color = textPrimary, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showEndDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(dateFormat.format(endDate), fontSize = 12.sp, color = textPrimary)
                    }
                    OutlinedButton(onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(timeFormat.format(endDate), fontSize = 12.sp, color = textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar", color = textPrimary) }
                    Button(
                        onClick = {
                            if (endDate.time > startDate.time) {
                                onSave(SleepSession(startTime = startDate, endTime = endDate, type = selectedType, status = SleepStatus.FINALIZADO))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF805AD5)),
                        modifier = Modifier.weight(1f)
                    ) { Text("Guardar", color = Color.White) }
                }
            }
        }
    }

    // Pickers simplificados
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.time)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { startDate = Date(it) }; showStartDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = datePickerState) }
    }
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = { TextButton(onClick = { val cal = Calendar.getInstance().apply { time = startDate; set(Calendar.HOUR_OF_DAY, timePickerState.hour); set(Calendar.MINUTE, timePickerState.minute) }; startDate = cal.time; showStartTimePicker = false }) { Text("OK") } },
            text = { TimePicker(state = timePickerState) }
        )
    }
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate.time)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { endDate = Date(it) }; showEndDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = datePickerState) }
    }
    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = { TextButton(onClick = { val cal = Calendar.getInstance().apply { time = endDate; set(Calendar.HOUR_OF_DAY, timePickerState.hour); set(Calendar.MINUTE, timePickerState.minute) }; endDate = cal.time; showEndTimePicker = false }) { Text("OK") } },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

// ==========================================
// REGISTRO RÁPIDO DE PAÑAL (Emojis Estéticos)
// ==========================================

@Composable
fun DiaperDialog(cardBg: Color, textPrimary: Color, onDismiss: () -> Unit, onAddDiaper: (DiaperType) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cardBg)
                .border(3.dp, Brush.linearGradient(listOf(PastelBlue, PastelPink)), RoundedCornerShape(28.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("¿Qué pasó? 🫧", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Selecciona para guardar automáticamente", color = textPrimary.copy(alpha=0.6f), fontSize = 12.sp)

                Spacer(modifier = Modifier.height(24.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { onAddDiaper(DiaperType.ORINA) },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelBlue),
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("💧 Orina", fontSize = 20.sp, color = Color(0xFF2D3748), fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { onAddDiaper(DiaperType.POPO) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDDFD6)), // Tono beige/arena
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("🤎 Sólido", fontSize = 20.sp, color = Color(0xFF5C4033), fontWeight = FontWeight.Bold) } // Corazón marrón + "Sólido" es más estético

                    Button(
                        onClick = { onAddDiaper(DiaperType.AMBOS) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE9D8FD)), // Morado muy suave
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("💧🤎 Ambos", fontSize = 20.sp, color = Color(0xFF44337A), fontWeight = FontWeight.Bold) }
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
                        val itemColor = when(change.type) {
                            DiaperType.ORINA -> PastelBlue.copy(alpha=0.3f)
                            DiaperType.POPO -> Color(0xFFEDDFD6).copy(alpha=0.5f)
                            DiaperType.AMBOS -> Color(0xFFE9D8FD).copy(alpha=0.5f)
                        }
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(itemColor).padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(when(change.type){ DiaperType.ORINA -> "💧"; DiaperType.POPO -> "🤎"; DiaperType.AMBOS -> "💧🤎" }, fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(16.dp))
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
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF805AD5))) { Text("Cerrar", color = Color.White) }
            }
        }
    }
}

// Resto de los diálogos (SessionDetailDialog, DeleteConfirmationDialog, TimelineEvent)
// se mantienen igual a la versión anterior ya que ya habían sido optimizados.

@Composable
fun SessionDetailDialog(session: SleepSession, cardBg: Color, textPrimary: Color, onDismiss: () -> Unit, onDelete: (SleepSession) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cardBg).border(3.dp, Brush.linearGradient(listOf(PastelPink, PastelPurple)), RoundedCornerShape(28.dp))) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onDelete(session) }) { Text("🗑️", fontSize = 24.sp) }
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
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF805AD5)), shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Cerrar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(session: SleepSession, isDark: Boolean, cardBg: Color, textPrimary: Color, onConfirm: () -> Unit, onDismiss: () -> Unit) {
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
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)).border(2.dp, color, CircleShape).align(Alignment.TopCenter)) {
                Text(text = icon, fontSize = 16.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)) {
            Text(text = time, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.ExtraBold)
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = textColor)
        }
    }
}