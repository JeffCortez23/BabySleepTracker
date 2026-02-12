package com.elyefris.khalessleeptracker.data.repository

import com.elyefris.khalessleeptracker.data.model.DiaperChange
import com.elyefris.khalessleeptracker.data.model.DiaperType
import com.elyefris.khalessleeptracker.data.model.SleepSession
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    fun getLastSession(): Flow<SleepSession?>
    fun getHistory(): Flow<List<SleepSession>>

    suspend fun startSleep(type: String)
    suspend fun wakeUp()
    suspend fun backToSleep()
    suspend fun finishSleep()
    suspend fun deleteSession(sessionId: String)
    suspend fun addManualSession(session: SleepSession)

    // Funciones de pañales
    fun getDiaperChanges(): Flow<List<DiaperChange>>
    suspend fun addDiaperChange(type: DiaperType, notes: String = "")
    suspend fun deleteDiaperChange(diaperId: String)
}
