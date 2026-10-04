package com.reisaraujo_miguel.studypulse.components

import com.reisaraujo_miguel.studypulse.R

/**
 * SessionMode represents the current mode of the study session.
 */
enum class SessionMode {
    /**
     * The study mode.
     */
    STUDY,

    /**
     * The rest mode.
     */
    REST
}

/**
 * ChronometerUiState represents the current state of the chronometer.
 *
 * @property currentMode The current mode of the study session.
 * @property isRunning Whether the chronometer is currently running.
 * @property totalStudyMillis The total time spent in study mode in milliseconds.
 * @property totalRestMillis The total time spent in rest mode in milliseconds.
 * @property currentSessionMillis The current session time in milliseconds.
 * @property chunksCount The number of chunks completed.
 * @property lastAlertMinuteTriggered The last minute at which an alert was triggered.
 * @property showAlarmDialog Whether to show the alarm dialog.
 */
data class ChronometerUiState(
    val currentMode: SessionMode = SessionMode.STUDY,
    val isRunning: Boolean = false,
    val totalStudyMillis: Long = 0L,
    val totalRestMillis: Long = 0L,
    val currentSessionMillis: Long = 0L,
    val chunksCount: Int = 0,
    val lastAlertMinuteTriggered: Int = -1,
    val showAlarmDialog: Boolean = false
) {
    /**
     * totalRealMillis The total real time in milliseconds.
     */
    val totalRealMillis: Long
        get() = totalStudyMillis + totalRestMillis

    /**
     * currentMinutes The current session time in minutes.
     */
    fun currentMinutes(): Int = (currentSessionMillis / (1000 * 60)).toInt()

    /**
     * gaugeProgress The progress of the gauge.
     *
     * @param maxStudyTimeMillis The maximum time in study mode in milliseconds.
     * @param maxRestTimeMillis The maximum time in rest mode in milliseconds.
     */
    fun gaugeProgress(maxStudyTimeMillis: Float, maxRestTimeMillis: Float): Float {
        val currentMax =
            if (currentMode == SessionMode.STUDY) maxStudyTimeMillis else maxRestTimeMillis
        if (currentMax <= 0f) return 0f
        return (currentSessionMillis / currentMax).coerceIn(0f, 1f)
    }

    /**
     * milestoneMessage The message to display for a milestone.
     *
     * @param studyMilestones The list of study milestones.
     * @param restReminders The list of rest reminders.
     */
    fun milestoneMessage(studyMilestones: List<Int>, restReminders: List<Int>): String {
        val minutes = currentMinutes()
        if (minutes <= 0) return ""

        return if (currentMode == SessionMode.STUDY) {
            if (studyMilestones.isEmpty()) {
                "Keep on studying!"
            } else {
                when {
                    minutes < studyMilestones[0] ->
                        R.string.msg_study_just_started.toString()

                    studyMilestones.size >= 2 && minutes < studyMilestones[1] ->
                        R.string.msg_study_keep_working.toString()

                    studyMilestones.size >= 3 && minutes < studyMilestones[2] ->
                        R.string.msg_study_deserve_break.toString()

                    studyMilestones.size >= 4 && minutes < studyMilestones[3] ->
                        R.string.msg_study_on_fire.toString()

                    else -> R.string.msg_study_take_break.toString()
                }
            }
        } else {
            if (restReminders.isEmpty()) {
                "Resting…"
            } else {
                when {
                    minutes < restReminders[0] ->
                        R.string.msg_rest_relax.toString()

                    restReminders.size >= 2 && minutes < restReminders[1] ->
                        R.string.msg_rest_halfway.toString()

                    restReminders.size >= 3 && minutes < restReminders[2] ->
                        R.string.msg_rest_wrapping_up.toString()

                    restReminders.size >= 4 && minutes < restReminders[3] ->
                        R.string.msg_rest_get_ready.toString()

                    else -> R.string.msg_rest_complete.toString()
                }
            }
        }
    }
}

/**
 * ChronometerEvent represents an event that occurs during the chronometer.
 */
sealed interface ChronometerEvent {
    /**
     * MilestoneAlert represents an alert event for a milestone.
     *
     * @property isAlarm Whether the alert is for an alarm.
     */
    data class MilestoneAlert(val isAlarm: Boolean) : ChronometerEvent

    /**
     * StartAlarm represents an event to start the alarm.
     */
    data object StartAlarm : ChronometerEvent

    /**
     * StopAlarm represents an event to stop the alarm.
     */
    data object StopAlarm : ChronometerEvent
}
