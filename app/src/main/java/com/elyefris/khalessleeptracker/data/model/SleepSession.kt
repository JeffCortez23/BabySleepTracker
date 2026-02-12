package com.elyefris.khalessleeptracker.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

enum class SleepType {
    SIESTA, NOCHE
}

enum class SleepStatus {
    DURMIENDO, DESPIERTO, FINALIZADO
}

data class SleepSession(
    // @DocumentId permite que Firestore inyecte el ID del documento aquí automáticamente
    @DocumentId val id: String = "",
    val startTime: Date = Date(), // Hora exacta de inicio
    val endTime: Date? = null,    // Null si sigue durmiendo
    val type: SleepType = SleepType.SIESTA,
    val status: SleepStatus = SleepStatus.DURMIENDO,
    val interruptions: List<Interruption> = emptyList(), // Lista de despertares
    val note: String = ""
)

// Clase para controlar cuando se despierta y se vuelve a dormir a mitad de la noche
data class Interruption(
    val wokeUpAt: Date = Date(),
    val backToSleepAt: Date? = null
)