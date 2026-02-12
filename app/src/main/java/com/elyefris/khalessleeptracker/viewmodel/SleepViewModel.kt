package com.elyefris.khalessleeptracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elyefris.khalessleeptracker.data.model.SleepSession
import com.elyefris.khalessleeptracker.data.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SleepUiState(
    val session: SleepSession? = null,
    val history: List<SleepSession> = emptyList(), // NUEVO: Lista de historial
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

        // 2. NUEVO: Escuchar el historial
        viewModelScope.launch {
            repository.getHistory().collect { historyList ->
                _uiState.update { it.copy(history = historyList) }
            }
        }
    }

    // --- Funciones de botones (igual que antes) ---
    fun startNap() = viewModelScope.launch { repository.startSleep("SIESTA") }
    fun startNight() = viewModelScope.launch { repository.startSleep("NOCHE") }
    fun wakeUp() = viewModelScope.launch { repository.wakeUp() }
    fun backToSleep() = viewModelScope.launch { repository.backToSleep() }
    fun finishSleep() = viewModelScope.launch { repository.finishSleep() }
}