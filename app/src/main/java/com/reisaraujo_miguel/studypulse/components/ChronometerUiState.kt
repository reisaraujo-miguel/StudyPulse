package com.reisaraujo_miguel.studypulse.components

enum class SessionMode {
    STUDY, REST
}

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
    val totalRealMillis: Long
        get() = totalStudyMillis + totalRestMillis

    fun currentMinutes(): Int = (currentSessionMillis / (1000 * 60)).toInt()

    fun gaugeProgress(maxStudyTimeMillis: Float, maxRestTimeMillis: Float): Float {
        val currentMax =
            if (currentMode == SessionMode.STUDY) maxStudyTimeMillis else maxRestTimeMillis
        return (currentSessionMillis / currentMax).coerceAtMost(1f)
    }

    fun milestoneMessage(studyMilestones: List<Int>, restReminders: List<Int>): String {
        val minutes = currentMinutes()
        if (minutes <= 0) return ""

        return if (currentMode == SessionMode.STUDY) {
            when {
                studyMilestones.isNotEmpty() && minutes < studyMilestones[0] -> "Get off your phone! You just started!"
                studyMilestones.size >= 2 && minutes < studyMilestones[1] -> "Keep on the good work!"
                studyMilestones.size >= 3 && minutes < studyMilestones[2] -> "You deserve a break :)"
                studyMilestones.size >= 4 && minutes < studyMilestones[3] -> "You are on fire 🔥"
                else -> "We highly recommend you take a break"
            }
        } else {
            when {
                restReminders.isNotEmpty() && minutes < restReminders[0] -> "Relax and recharge your energy ☕"
                restReminders.size >= 2 && minutes < restReminders[1] -> "Halfway through your rest time!"
                restReminders.size >= 3 && minutes < restReminders[2] -> "Start wrapping up your rest..."
                restReminders.size >= 4 && minutes < restReminders[3] -> "Get ready to jump back to study! 📚"
                else -> "Rest time complete! Back to work!"
            }
        }
    }
}

sealed interface ChronometerEvent {
    data class MilestoneAlert(val isAlarm: Boolean) : ChronometerEvent
    data object StartAlarm : ChronometerEvent
    data object StopAlarm : ChronometerEvent
}
