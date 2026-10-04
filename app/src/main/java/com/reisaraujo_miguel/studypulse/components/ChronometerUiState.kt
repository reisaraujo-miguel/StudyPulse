package com.reisaraujo_miguel.studypulse.components

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
                        "Get off your phone! You just started!"

                    studyMilestones.size >= 2 && minutes < studyMilestones[1] ->
                        "Keep on the good work!"

                    studyMilestones.size >= 3 && minutes < studyMilestones[2] ->
                        "You deserve a break :)"

                    studyMilestones.size >= 4 && minutes < studyMilestones[3] ->
                        "You are on fire 🔥"

                    else -> "We highly recommend you take a break"
                }
            }
        } else {
            if (restReminders.isEmpty()) {
                "Resting…"
            } else {
                when {
                    minutes < restReminders[0] ->
                        "Relax and recharge your energy ☕"

                    restReminders.size >= 2 && minutes < restReminders[1] ->
                        "Halfway through your rest time!"

                    restReminders.size >= 3 && minutes < restReminders[2] ->
                        "Start wrapping up your rest…"

                    restReminders.size >= 4 && minutes < restReminders[3] ->
                        "Get ready to jump back to study! 📚"

                    else -> "Rest time complete! Back to work!"
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
