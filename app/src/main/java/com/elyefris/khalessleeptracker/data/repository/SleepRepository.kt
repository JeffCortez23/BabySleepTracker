package com.elyefris.khalessleeptracker.data.repository

import com.elyefris.khalessleeptracker.data.model.SleepSession
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    fun getLastSession(): Flow<SleepSession?>

    // --- NUEVO: Función para obtener la lista ---
    fun getHistory(): Flow<List<SleepSession>>

    suspend fun startSleep(type: String)
    suspend fun wakeUp()
    suspend fun backToSleep()
    suspend fun finishSleep()
}