package com.reisaraujo_miguel.studypulse.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ChronometerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChronometerUiState())
    val uiState: StateFlow<ChronometerUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChronometerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var timerJob: Job? = null

    var studyMilestones: List<Int> = listOf(25, 45, 65, 90)
    var restReminders: List<Int> = listOf(15, 20, 25, 30)
    var restAlarmFrequency: Int = 5

    fun startStudy() {
        if (_uiState.value.isRunning) return

        _uiState.update { currentState ->
            currentState.copy(
                isRunning = true,
                chunksCount = if (currentState.chunksCount == 0) 1 else currentState.chunksCount
            )
        }
        startTimerJob()
    }

    fun switchToMode(targetMode: SessionMode) {
        val current = _uiState.value
        if (current.currentMode == targetMode) return

        _events.trySend(ChronometerEvent.StopAlarm)

        _uiState.update { currentState ->
            currentState.copy(
                currentMode = targetMode,
                currentSessionMillis = 0L,
                lastAlertMinuteTriggered = -1,
                showAlarmDialog = false,
                isRunning = true,
                chunksCount = if (targetMode == SessionMode.STUDY) currentState.chunksCount + 1 else currentState.chunksCount
            )
        }
        startTimerJob()
    }

    fun reset() {
        timerJob?.cancel()
        timerJob = null
        _events.trySend(ChronometerEvent.StopAlarm)

        _uiState.value = ChronometerUiState()
    }

    fun dismissAlarmDialog() {
        _events.trySend(ChronometerEvent.StopAlarm)
        _uiState.update { it.copy(showAlarmDialog = false) }
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var lastFrameTime = System.currentTimeMillis()

            while (_uiState.value.isRunning) {
                delay(200L.toDuration(DurationUnit.MILLISECONDS)) // Update timer every 200ms (5 FPS UI tick is plenty smooth and light on CPU)
                val currentTime = System.currentTimeMillis()
                val delta = currentTime - lastFrameTime
                lastFrameTime = currentTime

                onTimerTick(delta)
            }
        }
    }

    private fun onTimerTick(delta: Long) {
        _uiState.update { state ->
            val updatedSessionMillis = state.currentSessionMillis + delta
            val updatedStudyMillis = if (state.currentMode == SessionMode.STUDY) {
                state.totalStudyMillis + delta
            } else {
                state.totalStudyMillis
            }
            val updatedRestMillis = if (state.currentMode == SessionMode.REST) {
                state.totalRestMillis + delta
            } else {
                state.totalRestMillis
            }

            state.copy(
                currentSessionMillis = updatedSessionMillis,
                totalStudyMillis = updatedStudyMillis,
                totalRestMillis = updatedRestMillis
            )
        }

        checkMilestonesAndAlarms()
    }

    private fun checkMilestonesAndAlarms() {
        val state = _uiState.value
        val sessionMinutes = state.currentMinutes()

        if (sessionMinutes == state.lastAlertMinuteTriggered) return

        if (state.currentMode == SessionMode.STUDY) {
            if (sessionMinutes in studyMilestones) {
                _uiState.update { it.copy(lastAlertMinuteTriggered = sessionMinutes) }
                viewModelScope.launch {
                    _events.send(ChronometerEvent.MilestoneAlert(isAlarm = false))
                }
            }
        } else {
            val nonAlarmReminders = restReminders.dropLast(1)
            val lastReminder = restReminders.lastOrNull()

            if (nonAlarmReminders.isNotEmpty() && sessionMinutes in nonAlarmReminders) {
                _uiState.update { it.copy(lastAlertMinuteTriggered = sessionMinutes) }
                viewModelScope.launch {
                    _events.send(ChronometerEvent.MilestoneAlert(isAlarm = false))
                }
            } else if (lastReminder != null && sessionMinutes >= lastReminder && sessionMinutes % restAlarmFrequency == 0) {
                _uiState.update {
                    it.copy(
                        lastAlertMinuteTriggered = sessionMinutes,
                        showAlarmDialog = true
                    )
                }
                viewModelScope.launch {
                    _events.send(ChronometerEvent.StartAlarm)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
