package com.elyefris.khalessleeptracker.data.repository

import com.elyefris.khalessleeptracker.data.model.DiaperChange
import com.elyefris.khalessleeptracker.data.model.DiaperType
import com.elyefris.khalessleeptracker.data.model.SleepSession
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface SleepRepository {
    fun getLastSession(): Flow<SleepSession?>
    fun getHistory(): Flow<List<SleepSession>>
    suspend fun startSleep(typeString: String)
    suspend fun finishSleep()
    suspend fun wakeUp()
    suspend fun backToSleep()

    // Funciones de modificación
    suspend fun deleteSession(sessionId: String)
    suspend fun addManualSession(session: SleepSession)
    suspend fun updateSession(session: SleepSession) // NUEVA: Para editar

    // Funciones de Pañales
    fun getDiaperChanges(): Flow<List<DiaperChange>>
    suspend fun addDiaperChange(type: DiaperType, notes: String, timestamp: Date)
    suspend fun deleteDiaperChange(diaperId: String)
}