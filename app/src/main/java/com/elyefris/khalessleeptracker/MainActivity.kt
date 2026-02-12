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
import com.elyefris.khalessleeptracker.data.model.SleepSession
import com.elyefris.khalessleeptracker.data.model.SleepStatus
import com.elyefris.khalessleeptracker.data.model.SleepType
import com.elyefris.khalessleeptracker.data.repository.FirebaseSleepRepository
import com.elyefris.khalessleeptracker.ui.components.LiquidCard
import com.elyefris.khalessleeptracker.ui.theme.*
import com.elyefris.khalessleeptracker.viewmodel.SleepViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// Nota: Asegúrate de que este import sea correcto según tu proyecto
import com.elyefris.khalessleeptracker.checkMilestones

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

    // Colores dinámicos
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
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                SmallFloatingActionButton(
                    onClick = { showAchievements = true },
                    containerColor = if (isDark) Color(0xFF2E2E2E) else Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.CenterEnd)
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

            // --- 2. HISTORIAL ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Historial Reciente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    val historyToShow = state.history.filter { it.status == SleepStatus.FINALIZADO }

                    items(historyToShow) { session ->
                        LiquidCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSession = session }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = if (session.type == SleepType.SIESTA) "☀️" else "🌙", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    val dateFmt = SimpleDateFormat("EEE dd, hh:mm a", Locale("es", "ES")).format(session.startTime)
                                    Text(
                                        text = dateFmt.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryTextColor
                                    )
                                    session.endTime?.let { end ->
                                        val diffMillis = end.time - session.startTime.time
                                        val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
                                        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis) % 60
                                        val durationText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
                                        Text(text = "Duración: $durationText", style = MaterialTheme.typography.bodyMedium, color = secondaryTextColor)
                                    }
                                }
                            }
                        }
                    }
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
                onDismiss = { selectedSession = null }
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
    }
}

// ==========================================
// COMPONENTES UI
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
                Text(
                    text = "✨ Detalles del Sueño ✨",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                val dateFmt = SimpleDateFormat("EEEE dd MMMM", Locale("es", "ES")).format(session.startTime)
                Text(
                    text = dateFmt.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    color = PastelPurple,
                    fontWeight = FontWeight.Bold
                )

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

// --- FUNCIÓN DE LÍNEA DE TIEMPO PERFECTA ---
@Composable
fun TimelineEvent(
    time: String,
    title: String,
    icon: String,
    color: Color,
    isFirst: Boolean, // <--- NUEVO PARÁMETRO
    isLast: Boolean,
    textColor: Color
) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        // COLUMNA IZQUIERDA (48dp para asegurar buen espacio)
        Box(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            // 1. LÍNEA SUPERIOR (Entra por arriba) - Si NO es el primero
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(18.dp) // Llega justo al centro del icono
                        .background(Color.LightGray.copy(alpha = 0.5f))
                        .align(Alignment.TopCenter)
                )
            }

            // 2. LÍNEA INFERIOR (Sale por abajo) - Si NO es el último
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 18.dp) // Empieza en el centro del icono
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.LightGray.copy(alpha = 0.5f))
                        .align(Alignment.TopCenter)
                )
            }

            // 3. EL ICONO (Encima de todo)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)) // Fondo suave
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

        // COLUMNA DERECHA (Textos)
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