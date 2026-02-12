package com.elyefris.khalessleeptracker.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

enum class DiaperType {
    ORINA,      // Wet diaper
    POPO,       // Dirty diaper
    AMBOS       // Both
}

data class DiaperChange(
    @DocumentId val id: String = "",
    val timestamp: Date = Date(),
    val type: DiaperType = DiaperType.ORINA,
    val notes: String = ""
)
