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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

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

    val primaryTextColor = if (isDark) Color.White.copy(alpha = 0.9f) else Color.DarkGray.copy(alpha = 0.8f)
    val secondaryTextColor = if (isDark) Color.LightGray else Color.DarkGray.copy(alpha = 0.6f)
    val cardBackgroundColor = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)

    var selectedSession by remember { mutableStateOf<SleepSession?>(null) }
    var showAchievements by remember { mutableStateOf(false) }
    var showDiaperDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<SleepSession?>(null) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SmallFloatingActionButton(
                    onClick = { showDiaperDialog = true },
                    containerColor = if (isDark) Color(0xFF2E2E2E) else Color.White.copy(alpha = 0.9f)
                ) {
                    Text("🧷", fontSize = 20.sp)
                }

                SmallFloatingActionButton(
                    onClick = { showAchievements = true },
                    containerColor = if (isDark) Color(0xFF2E2E2E) else Color.White.copy(alpha = 0.9f)
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = "Logros", tint = Color(0xFFFFD700))
                }
            }

            // --- 1. TARJETA PRINCIPAL ---
            LiquidCard(
                modifier = Modifier.size(width = 340.dp, height = 240.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (state.session?.status) {
                            SleepStatus.DURMIENDO -> if (state.session?.type == SleepType.SIESTA) "Hora de la Siesta ☀️" else "Dulces Sueños 🌙"
                            SleepStatus.DESPIERTO -> "¡Se despertó! 👀"
                            else -> "Lista para dormir"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (state.session?.status) {
                            SleepStatus.DURMIENDO -> if (state.session?.type == SleepType.SIESTA) "👶🏻☀️" else "👶🏻💤"
                            SleepStatus.DESPIERTO -> "👀❗"
                            else -> "🧸"
                        },
                        fontSize = 72.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    state.session?.startTime?.let { date ->
                        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        Text(
                            text = "Desde: ${formatter.format(date)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if(isDark) Color.White else Color.Black.copy(alpha = 0.7f)
                        )
                        if (state.session?.type == SleepType.SIESTA && state.session?.status == SleepStatus.DURMIENDO) {
                            Text(text = "Meta: 2 horas", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                        }
                    }
                }
            }

            // --- 2. HISTORIAL CON NAVEGACIÓN JERÁRQUICA ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Historial Completo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                if (selectedDate == null) {
                    // VISTA DE FECHAS
                    DateListView(
                        history = state.history,
                        isDark = isDark,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        onDateClick = { selectedDate = it }
                    )
                } else {
                    // VISTA DE SESIONES DE UN DÍA ESPECÍFICO
                    SessionsOfDayView(
                        history = state.history,
                        selectedDate = selectedDate!!,
                        isDark = isDark,
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
                        colors = ButtonDefaults.buttonColors(containerColor = if(isDark) Color(0xFFFFF7ED) else PastelCream),
                        elevation = ButtonDefaults.buttonElevation(4.dp),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("☀️", fontSize = 24.sp)
                            Text("Siesta", color = Color.DarkGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { viewModel.startNight() },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelPurple),
                        elevation = ButtonDefaults.buttonElevation(4.dp),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌙", fontSize = 24.sp)
                            Text("Noche", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    if (state.session?.status == SleepStatus.DURMIENDO) {
                        Button(
                            onClick = { viewModel.wakeUp() },
                            colors = ButtonDefaults.buttonColors(containerColor = PastelPink),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text("¡Despertó!", color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.backToSleep() },
                            colors = ButtonDefaults.buttonColors(containerColor = PastelBlue),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text("Volvió a dormir", color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }

                    Button(
                        onClick = { viewModel.finishSleep() },
                        colors = ButtonDefaults.buttonColors(containerColor = if(isDark) Color.DarkGray else Color.White.copy(alpha=0.6f)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.width(90.dp).fillMaxHeight()
                    ) {
                        Text("Fin", color = Color.Red.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- POPUPS ---
        if (selectedSession != null) {
            SessionDetailDialog(
                session = selectedSession!!,
                isDark = isDark,
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
                isDark = isDark,
                cardBg = cardBackgroundColor,
                textPrimary = primaryTextColor,
                onDismiss = { showDiaperDialog = false },
                onAddDiaper = { type, notes ->
                    viewModel.addDiaperChange(type, notes)
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
    }
}

// ==========================================
// COMPONENTES PARA HISTORIAL JERÁRQUICO
// ==========================================

@Composable
fun DateListView(
    history: List<SleepSession>,
    isDark: Boolean,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    onDateClick: (String) -> Unit
) {
    // Agrupar sesiones por fecha
    val groupedByDate = history
        .filter { it.status == SleepStatus.FINALIZADO }
        .groupBy {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.startTime)
        }
        .toSortedMap(compareByDescending { it })

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        items(groupedByDate.keys.toList()) { dateKey ->
            val sessions = groupedByDate[dateKey] ?: emptyList()
            val date = sessions.first().startTime
            val dateFmt = SimpleDateFormat("EEEE dd MMMM yyyy", Locale("es", "ES")).format(date)

            LiquidCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDateClick(dateKey) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = dateFmt.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Text(
                            text = "${sessions.size} registro${if(sessions.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor
                        )
                    }
                    Text("➜", fontSize = 24.sp, color = primaryTextColor)
                }
            }
        }
    }
}

@Composable
fun SessionsOfDayView(
    history: List<SleepSession>,
    selectedDate: String,
    isDark: Boolean,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    onBack: () -> Unit,
    onSessionClick: (SleepSession) -> Unit
) {
    val sessions = history
        .filter { it.status == SleepStatus.FINALIZADO }
        .filter { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.startTime) == selectedDate }
        .sortedByDescending { it.startTime }

    Column {
        // Botón de regreso
        LiquidCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBack() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⬅", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Volver a fechas",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lista de sesiones del día
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sessions) { session ->
                LiquidCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSessionClick(session) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = if (session.type == SleepType.SIESTA) "☀️" else "🌙", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(session.startTime)
                            Text(
                                text = timeFmt,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor
                            )

                            // Calcular tiempo REAL de sueño
                            val (hours, minutes) = calculateRealSleepTime(session)
                            val durationText = formatSleepDuration(hours, minutes)

                            Row {
                                Text(
                                    text = "Duración real: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = secondaryTextColor
                                )
                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelPurple
                                )
                            }

                            if (session.interruptions.isNotEmpty()) {
                                Text(
                                    text = "${session.interruptions.size} despertar${if(session.interruptions.size != 1) "es" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PastelPink
                                )
                            }
                        }
                        Text("➜", fontSize = 20.sp, color = primaryTextColor)
                    }
                }
            }
        }
    }
}

// ==========================================
// DIÁLOGO DE PAÑALES
// ==========================================

@Composable
fun DiaperDialog(
    isDark: Boolean,
    cardBg: Color,
    textPrimary: Color,
    onDismiss: () -> Unit,
    onAddDiaper: (DiaperType, String) -> Unit
) {
    var selectedType by remember { mutableStateOf<DiaperType?>(null) }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(cardBg)
                .border(3.dp, Brush.linearGradient(listOf(PastelBlue, PastelPink)), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🧷 Cambio de Pañal 🧷", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(24.dp))

                // Opciones de tipo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DiaperTypeButton(
                        emoji = "💧",
                        label = "Orina",
                        isSelected = selectedType == DiaperType.ORINA,
                        onClick = { selectedType = DiaperType.ORINA }
                    )
                    DiaperTypeButton(
                        emoji = "💩",
                        label = "Popó",
                        isSelected = selectedType == DiaperType.POPO,
                        onClick = { selectedType = DiaperType.POPO }
                    )
                    DiaperTypeButton(
                        emoji = "💧💩",
                        label = "Ambos",
                        isSelected = selectedType == DiaperType.AMBOS,
                        onClick = { selectedType = DiaperType.AMBOS }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        selectedType?.let { type ->
                            onAddDiaper(type, notes)
                        }
                    },
                    enabled = selectedType != null,
                    colors = ButtonDefaults.buttonColors(containerColor = PastelPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Registrar", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = textPrimary)
                }
            }
        }
    }
}

@Composable
fun DiaperTypeButton(
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) PastelBlue.copy(alpha = 0.3f) else Color.Transparent)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) PastelBlue else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(emoji, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

// ==========================================
// OTROS COMPONENTES (sin cambios)
// ==========================================

@Composable
fun AchievementsDialog(
    history: List<SleepSession>,
    isDark: Boolean,
    cardBg: Color,
    textPrimary: Color,
    onDismiss: () -> Unit
) {
    val milestones = remember(history) { checkMilestones(history) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(cardBg)
                .border(3.dp, Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500))), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏆 Hitos de Khale 🏆", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(350.dp)
                ) {
                    items(milestones) { milestone ->
                        val itemBg = if (milestone.isUnlocked) {
                            if(isDark) Color(0xFF1E3A5F) else PastelBlue.copy(alpha=0.3f)
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
                                style = MaterialTheme.typography.labelLarge,
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
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PastelPurple)) {
                    Text("Cerrar", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SessionDetailDialog(
    session: SleepSession,
    isDark: Boolean,
    cardBg: Color,
    textPrimary: Color,
    onDismiss: () -> Unit,
    onDelete: (SleepSession) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(cardBg)
                .border(
                    width = 3.dp,
                    brush = Brush.linearGradient(listOf(PastelPink, PastelPurple)),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header con botón de eliminar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(40.dp))
                    Text(
                        text = "✨ Detalles del Sueño ✨",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = { onDelete(session) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            text = "🗑️",
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val dateFmt = SimpleDateFormat("EEEE dd MMMM", Locale("es", "ES")).format(session.startTime)
                Text(
                    text = dateFmt.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    color = PastelPurple,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mostrar tiempo REAL de sueño
                val (hours, minutes) = calculateRealSleepTime(session)
                val durationText = formatSleepDuration(hours, minutes)

                LiquidCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tiempo Real de Sueño",
                            style = MaterialTheme.typography.labelLarge,
                            color = textPrimary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PastelPurple
                        )
                        if (session.interruptions.isNotEmpty()) {
                            Text(
                                text = "(${session.interruptions.size} despertar${if(session.interruptions.size != 1) "es" else ""} registrados)",
                                style = MaterialTheme.typography.bodySmall,
                                color = textPrimary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

                // 1. INICIO
                TimelineEvent(
                    time = timeFormat.format(session.startTime),
                    title = "Se durmió",
                    icon = if (session.type == SleepType.SIESTA) "☀️" else "🌙",
                    color = PastelBlue,
                    isFirst = true,
                    isLast = session.interruptions.isEmpty() && session.endTime == null,
                    textColor = textPrimary
                )

                // 2. INTERRUPCIONES
                session.interruptions.forEach { interruption ->
                    TimelineEvent(
                        time = timeFormat.format(interruption.wokeUpAt),
                        title = "Despertó",
                        icon = "👀",
                        color = PastelPink,
                        isFirst = false,
                        isLast = false,
                        textColor = textPrimary
                    )

                    interruption.backToSleepAt?.let { backTime ->
                        TimelineEvent(
                            time = timeFormat.format(backTime),
                            title = "Volvió a dormir",
                            icon = "💤",
                            color = PastelBlue,
                            isFirst = false,
                            isLast = false,
                            textColor = textPrimary
                        )
                    }
                }

                // 3. FIN
                session.endTime?.let { end ->
                    TimelineEvent(
                        time = timeFormat.format(end),
                        title = "Despertó Definitivo",
                        icon = "🏁",
                        color = PastelPurple,
                        isFirst = false,
                        isLast = true,
                        textColor = textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PastelPurple),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Cerrar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

// ==========================================
// DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN
// ==========================================

@Composable
fun DeleteConfirmationDialog(
    session: SleepSession,
    isDark: Boolean,
    cardBg: Color,
    textPrimary: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(cardBg)
                .border(
                    width = 3.dp,
                    brush = Brush.linearGradient(listOf(Color.Red.copy(alpha = 0.6f), Color.Red.copy(alpha = 0.3f))),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️ Confirmar Eliminación",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "¿Estás seguro de que deseas eliminar este registro?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Información del registro a eliminar
                val dateFmt = SimpleDateFormat("EEE dd MMM, hh:mm a", Locale("es", "ES")).format(session.startTime)
                val typeEmoji = if (session.type == SleepType.SIESTA) "☀️" else "🌙"
                val (hours, minutes) = calculateRealSleepTime(session)
                val duration = formatSleepDuration(hours, minutes)

                LiquidCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = typeEmoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = dateFmt.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = duration,
                                style = MaterialTheme.typography.bodySmall,
                                color = textPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color.DarkGray else Color.LightGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = if (isDark) Color.White else Color.Black)
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineEvent(
    time: String,
    title: String,
    icon: String,
    color: Color,
    isFirst: Boolean,
    isLast: Boolean,
    textColor: Color
) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(18.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f))
                        .align(Alignment.TopCenter)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.LightGray.copy(alpha = 0.5f))
                        .align(Alignment.TopCenter)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f))
                    .border(2.dp, color, CircleShape)
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    text = icon,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}
