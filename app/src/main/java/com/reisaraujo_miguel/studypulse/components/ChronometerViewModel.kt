package com.reisaraujo_miguel.studypulse.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * ChronometerViewModel is a ViewModel class that manages the state of the chronometer.
 */
class ChronometerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChronometerUiState())

    /**
     * uiState is a StateFlow that represents the current state of the chronometer.
     */
    val uiState: StateFlow<ChronometerUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChronometerEvent>(Channel.BUFFERED)

    /**
     * events is a Flow that represents the events that occur during the chronometer.
     */
    val events: Flow<ChronometerEvent> = _events.receiveAsFlow()

    private var timerJob: Job? = null

    /**
     * studyMilestones is a list of study milestones in minutes.
     */
    var studyMilestones: List<Int> = listOf(25, 45, 65, 90)

    /**
     * restReminders is a list of rest reminders in minutes.
     */
    var restReminders: List<Int> = listOf(15, 20, 25, 30)

    /**
     * restAlarmFrequency is the frequency of rest alarms in minutes.
     */
    var restAlarmFrequency: Int = 5

    /**
     * startStudy starts the study session.
     */
    fun startStudy() {
        if (_uiState.value.isRunning) return

        _uiState.update {
            /**
             * @param currentState The current UI state.
             */
                currentState ->
            currentState.copy(
                isRunning = true,
                chunksCount = if (currentState.chunksCount == 0) 1 else currentState.chunksCount
            )
        }
        startTimerJob()
    }

    /**
     * switchToMode switches to the specified mode.
     *
     * @param targetMode The target mode to switch to.
     */
    fun switchToMode(targetMode: SessionMode) {
        val current = _uiState.value
        if (current.currentMode == targetMode) return

        _events.trySend(ChronometerEvent.StopAlarm)

        _uiState.update {
            /**
             * @param currentState The current UI state.
             */
                currentState ->
            currentState.copy(
                currentMode = targetMode,
                currentSessionMillis = 0L,
                lastAlertMinuteTriggered = -1,
                showAlarmDialog = false,
                isRunning = true,
                chunksCount = (
                        if (targetMode == SessionMode.STUDY)
                            currentState.chunksCount + 1
                        else
                            currentState.chunksCount
                        )
            )
        }
        startTimerJob()
    }

    /**
     * reset resets the chronometer.
     */
    fun reset() {
        timerJob?.cancel()
        timerJob = null
        _events.trySend(ChronometerEvent.StopAlarm)

        _uiState.value = ChronometerUiState()
    }

    /**
     * dismissAlarmDialog dismisses the alarm dialog.
     */
    fun dismissAlarmDialog() {
        _events.trySend(ChronometerEvent.StopAlarm)
        _uiState.update { it.copy(showAlarmDialog = false) }
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var lastFrameTime = System.currentTimeMillis()

            while (_uiState.value.isRunning) {
                delay(200L.toDuration(DurationUnit.MILLISECONDS))
                val currentTime = System.currentTimeMillis()
                val delta = currentTime - lastFrameTime
                lastFrameTime = currentTime

                onTimerTick(delta)
            }
        }
    }

    /**
     * onTimerTick is called when the timer ticks.
     *
     * @param delta The time that has passed since the last tick in milliseconds.
     */
    fun onTimerTick(delta: Long) {
        _uiState.update {
            /**
             * @param state The current UI state.
             */
                state ->
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
            _uiState.update { it.copy(lastAlertMinuteTriggered = sessionMinutes) }
            if (sessionMinutes in studyMilestones) {
                viewModelScope.launch {
                    _events.send(ChronometerEvent.MilestoneAlert(isAlarm = false))
                }
            }
        } else {
            val nonAlarmReminders = restReminders.dropLast(1)
            val lastReminder = restReminders.lastOrNull()

            _uiState.update { it.copy(lastAlertMinuteTriggered = sessionMinutes) }

            if (nonAlarmReminders.isNotEmpty() && sessionMinutes in nonAlarmReminders) {
                viewModelScope.launch {
                    _events.send(ChronometerEvent.MilestoneAlert(isAlarm = false))
                }
            } else if (lastReminder != null &&
                sessionMinutes >= lastReminder &&
                restAlarmFrequency > 0 &&
                sessionMinutes % restAlarmFrequency == 0
            ) {
                _uiState.update { it.copy(showAlarmDialog = true) }
                viewModelScope.launch {
                    _events.send(ChronometerEvent.StartAlarm)
                }
            }
        }
    }

    /**
     * onCleared is called when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
