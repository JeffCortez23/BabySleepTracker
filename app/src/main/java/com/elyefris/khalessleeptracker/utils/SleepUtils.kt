package com.elyefris.khalessleeptracker.utils

import com.elyefris.khalessleeptracker.data.model.SleepSession
import java.util.concurrent.TimeUnit

/**
 * Calcula el tiempo REAL de sueño excluyendo interrupciones
 */
fun calculateRealSleepTime(session: SleepSession): Pair<Long, Long> {
    val endTime = session.endTime ?: return Pair(0, 0)
    
    // Tiempo total desde inicio hasta fin
    val totalTimeMillis = endTime.time - session.startTime.time
    
    // Calcular tiempo despierto durante interrupciones
    var awakeTimeMillis = 0L
    
    session.interruptions.forEach { interruption ->
        val backToSleep = interruption.backToSleepAt
        if (backToSleep != null) {
            // Si volvió a dormir, contar el tiempo que estuvo despierto
            awakeTimeMillis += backToSleep.time - interruption.wokeUpAt.time
        } else {
            // Si no volvió a dormir, contar desde que despertó hasta el final
            awakeTimeMillis += endTime.time - interruption.wokeUpAt.time
        }
    }
    
    // Tiempo real = Tiempo total - Tiempo despierto
    val realSleepTimeMillis = totalTimeMillis - awakeTimeMillis
    
    val hours = TimeUnit.MILLISECONDS.toHours(realSleepTimeMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(realSleepTimeMillis) % 60
    
    return Pair(hours, minutes)
}

/**
 * Formatea el tiempo de sueño en texto legible
 */
fun formatSleepDuration(hours: Long, minutes: Long): String {
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
