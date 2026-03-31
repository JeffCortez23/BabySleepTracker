package com.elyefris.khalessleeptracker.data.model

import java.util.concurrent.TimeUnit

data class Milestone(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean
)

// Función que revisa el historial y desbloquea logros
fun checkMilestones(history: List<SleepSession>): List<Milestone> {
    // Solo miramos sesiones terminadas
    val finishedSessions = history.filter { it.status == SleepStatus.FINALIZADO && it.endTime != null }

    return listOf(
        // LOGRO 1: NOCHE PERFECTA
        Milestone(
            id = "perfect_night",
            emoji = "✨",
            title = "Noche de Paz",
            description = "Durmió más de 9h de corrido sin despertar.",
            isUnlocked = finishedSessions.any { session ->
                if (session.type == SleepType.NOCHE && session.endTime != null) {
                    val durationHours = TimeUnit.MILLISECONDS.toHours(session.endTime.time - session.startTime.time)
                    durationHours >= 9 && session.interruptions.isEmpty()
                } else false
            }
        ),

        // LOGRO 2: SIESTA OBJETIVO
        Milestone(
            id = "target_nap",
            emoji = "🌿",
            title = "Batería al 100%",
            description = "Una siesta sólida de casi 2 horas (1h 50m+).",
            isUnlocked = finishedSessions.any { session ->
                if (session.type == SleepType.SIESTA && session.endTime != null) {
                    val durationMin = TimeUnit.MILLISECONDS.toMinutes(session.endTime.time - session.startTime.time)
                    durationMin >= 110
                } else false
            }
        ),

        // LOGRO 3: NINJA
        Milestone(
            id = "ninja",
            emoji = "🦉",
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

        // LOGRO 4: RACHA
        Milestone(
            id = "streak",
            emoji = "💫",
            title = "En Racha",
            description = "Acumula 3 noches perfectas en el historial.",
            isUnlocked = finishedSessions.count { session ->
                val duration = if (session.endTime != null) session.endTime.time - session.startTime.time else 0
                session.type == SleepType.NOCHE && session.interruptions.isEmpty() && TimeUnit.MILLISECONDS.toHours(duration) >= 8
            } >= 3
        ),

        // NUEVO LOGRO 5: OSO HIBERNANDO
        Milestone(
            id = "hibernating_bear",
            emoji = "🐻",
            title = "Oso Hibernando",
            description = "Una noche de sueño profundo de más de 11 horas.",
            isUnlocked = finishedSessions.any { session ->
                if (session.type == SleepType.NOCHE && session.endTime != null) {
                    val durationHours = TimeUnit.MILLISECONDS.toHours(session.endTime.time - session.startTime.time)
                    durationHours >= 11
                } else false
            }
        ),

        // NUEVO LOGRO 6: SIESTA RELÁMPAGO
        Milestone(
            id = "power_nap",
            emoji = "⚡",
            title = "Siesta Relámpago",
            description = "Una siesta corta y revitalizante de entre 20 y 45 min.",
            isUnlocked = finishedSessions.any { session ->
                if (session.type == SleepType.SIESTA && session.endTime != null) {
                    val durationMin = TimeUnit.MILLISECONDS.toMinutes(session.endTime.time - session.startTime.time)
                    durationMin in 20..45
                } else false
            }
        ),

        // NUEVO LOGRO 7: CONSTANCIA
        Milestone(
            id = "frequent_tracker",
            emoji = "📅",
            title = "Constancia Pura",
            description = "Has registrado más de 30 sesiones de sueño.",
            isUnlocked = finishedSessions.size >= 30
        )
    )
}