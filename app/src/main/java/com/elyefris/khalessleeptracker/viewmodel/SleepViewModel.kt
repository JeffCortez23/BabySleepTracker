package com.elyefris.khalessleeptracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elyefris.khalessleeptracker.data.model.DiaperChange
import com.elyefris.khalessleeptracker.data.model.DiaperType
import com.elyefris.khalessleeptracker.data.model.SleepSession
import com.elyefris.khalessleeptracker.data.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SleepUiState(
    val session: SleepSession? = null,
    val history: List<SleepSession> = emptyList(),
    val diaperChanges: List<DiaperChange> = emptyList(),
    val isLoading: Boolean = true
)

class SleepViewModel(
    private val repository: SleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepUiState())
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    init {
        // 1. Escuchar la sesión actual
        viewModelScope.launch {
            repository.getLastSession().collect { session ->
                _uiState.update { it.copy(session = session, isLoading = false) }
            }
        }

        // 2. Escuchar el historial completo
        viewModelScope.launch {
            repository.getHistory().collect { historyList ->
                _uiState.update { it.copy(history = historyList) }
            }
        }

        // 3. Escuchar cambios de pañales
        viewModelScope.launch {
            repository.getDiaperChanges().collect { changes ->
                _uiState.update { it.copy(diaperChanges = changes) }
            }
        }
    }

    // --- Funciones de sueño ---
    fun startNap() = viewModelScope.launch { repository.startSleep("SIESTA") }
    fun startNight() = viewModelScope.launch { repository.startSleep("NOCHE") }
    fun wakeUp() = viewModelScope.launch { repository.wakeUp() }
    fun backToSleep() = viewModelScope.launch { repository.backToSleep() }
    fun finishSleep() = viewModelScope.launch { repository.finishSleep() }
    fun deleteSession(sessionId: String) = viewModelScope.launch { repository.deleteSession(sessionId) }

    // --- Funciones de pañales ---
    fun addDiaperChange(type: DiaperType, notes: String = "") = viewModelScope.launch {
        repository.addDiaperChange(type, notes)
    }
    fun deleteDiaperChange(diaperId: String) = viewModelScope.launch {
        repository.deleteDiaperChange(diaperId)
    }
}
