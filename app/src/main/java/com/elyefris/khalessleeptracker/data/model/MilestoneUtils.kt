package com.elyefris.khalessleeptracker.data.model

import java.util.concurrent.TimeUnit
import java.util.Calendar

data class Milestone(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean
)

fun checkMilestones(history: List<SleepSession>): List<Milestone> {
    // REINICIO SEMANAL: Solo miramos los últimos 7 días
    val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)

    val recentSessions = history.filter {
        it.status == SleepStatus.FINALIZADO &&
                it.endTime != null &&
                it.startTime.time >= sevenDaysAgo
    }

    return listOf(
        Milestone(
            id = "perfect_night",
            emoji = "✨",
            title = "Noche de Paz",
            description = "Esta semana: Durmió más de 9h seguidas sin despertar.",
            isUnlocked = recentSessions.any { s ->
                s.type == SleepType.NOCHE && TimeUnit.MILLISECONDS.toHours(s.endTime!!.time - s.startTime.time) >= 9 && s.interruptions.isEmpty()
            }
        ),
        Milestone(
            id = "target_nap",
            emoji = "🌿",
            title = "Batería al 100%",
            description = "Esta semana: Logró una siesta de casi 2 horas.",
            isUnlocked = recentSessions.any { s ->
                s.type == SleepType.SIESTA && TimeUnit.MILLISECONDS.toMinutes(s.endTime!!.time - s.startTime.time) >= 110
            }
        ),
        Milestone(
            id = "ninja",
            emoji = "🦉",
            title = "Ninja del Sueño",
            description = "Esta semana: Se despertó y volvió a dormir en menos de 10 min.",
            isUnlocked = recentSessions.any { s ->
                s.interruptions.any { i -> i.backToSleepAt != null && TimeUnit.MILLISECONDS.toMinutes(i.backToSleepAt.time - i.wokeUpAt.time) < 10 }
            }
        ),
        Milestone(
            id = "early_bird",
            emoji = "🌅",
            title = "Madrugador",
            description = "Esta semana: Despertó listo para el día antes de las 6:30 AM.",
            isUnlocked = recentSessions.any { s ->
                if (s.type == SleepType.NOCHE && s.endTime != null) {
                    val cal = Calendar.getInstance().apply { time = s.endTime }
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val min = cal.get(Calendar.MINUTE)
                    hour < 6 || (hour == 6 && min <= 30)
                } else false
            }
        ),
        Milestone(
            id = "streak_week",
            emoji = "🔥",
            title = "Racha Semanal",
            description = "Esta semana: Registró 3 noches buenas de al menos 8 horas.",
            isUnlocked = recentSessions.count { s ->
                s.type == SleepType.NOCHE && TimeUnit.MILLISECONDS.toHours(s.endTime!!.time - s.startTime.time) >= 8
            } >= 3
        ),
        Milestone(
            id = "frequent_tracker",
            emoji = "📅",
            title = "Padres Constantes",
            description = "Esta semana: Más de 15 registros guardados.",
            isUnlocked = recentSessions.size >= 15
        )
    )
}