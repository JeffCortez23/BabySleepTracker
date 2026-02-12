package com.elyefris.khalessleeptracker

import com.elyefris.khalessleeptracker.data.model.SleepSession
import com.elyefris.khalessleeptracker.data.model.SleepStatus
import com.elyefris.khalessleeptracker.data.model.SleepType
import java.util.concurrent.TimeUnit

data class Milestone(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean
)

// Función mágica que revisa el historial y desbloquea logros
fun checkMilestones(history: List<SleepSession>): List<Milestone> {
    // Solo miramos sesiones terminadas
    val finishedSessions = history.filter { it.status == SleepStatus.FINALIZADO && it.endTime != null }

    return listOf(
        // LOGRO 1: NOCHE PERFECTA (Más de 9 horas sin despertares)
        Milestone(
            id = "perfect_night",
            emoji = "🌟",
            title = "Noche de Paz",
            description = "Durmió más de 9h de corrido sin despertar.",
            isUnlocked = finishedSessions.any { session ->
                if (session.type == SleepType.NOCHE && session.endTime != null) {
                    val durationHours = TimeUnit.MILLISECONDS.toHours(session.endTime.time - session.startTime.time)
                    durationHours >= 9 && session.interruptions.isEmpty()
                } else false
            }
        ),

        // LOGRO 2: SIESTA OBJETIVO (Cerca de las 2 horas)
        Milestone(
            id = "target_nap",
            emoji = "🔋",
            title = "Batería al 100%",
            description = "Una siesta sólida de casi 2 horas (1h 50m+).",
            isUnlocked = finishedSessions.any { session ->
                if (session.type == SleepType.SIESTA && session.endTime != null) {
                    val durationMin = TimeUnit.MILLISECONDS.toMinutes(session.endTime.time - session.startTime.time)
                    durationMin >= 110 // 1 hora 50 min
                } else false
            }
        ),

        // LOGRO 3: NINJA (Se volvió a dormir rápido)
        Milestone(
            id = "ninja",
            emoji = "🥷",
            title = "Ninja del Sueño",
            description = "Se despertó, pero volvió a dormir en menos de 10 min.",
            isUnlocked = finishedSessions.any { session ->
                session.interruptions.any { interruption ->
                    if (interruption.backToSleepAt != null) {
                        val awakeTime = interruption.backToSleepAt.time - interruption.wokeUpAt.time
                        TimeUnit.MILLISECONDS.toMinutes(awakeTime) < 10
                    } else false
                }
            }
        ),

        // LOGRO 4: RACHA (3 Noches buenas seguidas - Lógica simplificada: 3 noches perfectas en el historial)
        Milestone(
            id = "streak",
            emoji = "🔥",
            title = "En Racha",
            description = "Acumula 3 noches perfectas en el historial.",
            isUnlocked = finishedSessions.count { session ->
                val duration = if (session.endTime != null) session.endTime.time - session.startTime.time else 0
                session.type == SleepType.NOCHE && session.interruptions.isEmpty() && TimeUnit.MILLISECONDS.toHours(duration) >= 8
            } >= 3
        )
    )
}