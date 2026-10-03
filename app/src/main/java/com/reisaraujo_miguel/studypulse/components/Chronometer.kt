package com.reisaraujo_miguel.studypulse.components

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

enum class SessionMode {
    STUDY, REST
}

@Composable
fun ChronometerGaugeScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    studyMilestones: List<Int> = listOf(25, 45, 65, 90),
    restReminders: List<Int> = listOf(15, 20, 25, 30),
    restAlarmFrequency: Int = 5
) {
    val context = LocalContext.current

    var currentMode by remember { mutableStateOf(SessionMode.STUDY) }
    var isRunning by remember { mutableStateOf(false) }

    var totalStudyMillis by remember { mutableLongStateOf(0L) }
    var totalRestMillis by remember { mutableLongStateOf(0L) }

    var currentSessionMillis by remember { mutableLongStateOf(0L) }
    var chunksCount by remember { mutableIntStateOf(0) }

    // Alert & Alarm Tracking
    var lastAlertMinuteTriggered by remember { mutableIntStateOf(-1) }
    var showAlarmDialog by remember { mutableStateOf(false) }

    val maxStudyTimeMillis = (studyMilestones.last() * 60 * 1000f).coerceAtLeast(1f)
    val maxRestTimeMillis = (restReminders.last() * 60 * 1000f).coerceAtLeast(1f)

    val currentMaxMillis =
        if (currentMode == SessionMode.STUDY) maxStudyTimeMillis else maxRestTimeMillis

    val gaugeProgress = (currentSessionMillis / currentMaxMillis).coerceAtMost(1f)

    // Gauge Colors based on mode
    val currentColor = if (currentMode == SessionMode.STUDY) {
        lerp(Color(0xFFFF3D00), Color(0xFF00E676), gaugeProgress) // Red -> Green
    } else {
        lerp(Color(0xFF0288D1), Color(0xFF7C4DFF), gaugeProgress) // Light Blue -> Purple
    }

    val currentMinutes = (currentSessionMillis / (1000 * 60)).toInt()

    // Subtle background tint
    val backgroundColor =
        if (isRunning) currentColor.copy(alpha = 0.10f) else MaterialTheme.colorScheme.background

    // Milestone Messages
    val milestoneMessage = if (currentMode == SessionMode.STUDY) {
        when {
            currentMinutes <= 0 -> ""
            currentMinutes < studyMilestones[0] -> "Get off your phone! You just started!"
            currentMinutes < studyMilestones[1] -> "Keep on the good work!"
            currentMinutes < studyMilestones[2] -> "You deserve a break :)"
            currentMinutes < studyMilestones[3] -> "You are on fire 🔥"
            else -> "We highly recommend you take a break"
        }
    } else {
        when {
            currentMinutes <= 0 -> ""
            currentMinutes < restReminders[0] -> "Relax and recharge your energy ☕"
            currentMinutes < restReminders[1] -> "Halfway through your rest time!"
            currentMinutes < restReminders[2] -> "Start wrapping up your rest..."
            currentMinutes < restReminders[3] -> "Get ready to jump back to study! 📚"
            else -> "Rest time complete! Back to work!"
        }
    }

    // Helper: Mode Transition Logic
    fun switchToMode(targetMode: SessionMode) {
        if (currentMode != targetMode) {
            currentMode = targetMode
            currentSessionMillis = 0L
            lastAlertMinuteTriggered = -1
            showAlarmDialog = false
            isRunning = true

            if (targetMode == SessionMode.STUDY) {
                chunksCount++
            }
        }
    }


    LaunchedEffect(isRunning, currentMode) {
        if (isRunning) {
            var lastFrameTime = System.currentTimeMillis()
            while (isRunning) {
                delay(50L.toDuration(DurationUnit.MILLISECONDS))
                val currentTime = System.currentTimeMillis()
                val delta = currentTime - lastFrameTime
                lastFrameTime = currentTime

                currentSessionMillis += delta

                if (currentMode == SessionMode.STUDY) {
                    totalStudyMillis += delta

                    val sessionMinutes = (currentSessionMillis / (1000 * 60)).toInt()

                    if (sessionMinutes != lastAlertMinuteTriggered) {
                        if (sessionMinutes in studyMilestones) {
                            lastAlertMinuteTriggered = sessionMinutes
                            triggerAlert(context, isAlarm = false)
                        }
                    }
                } else {
                    totalRestMillis += delta

                    // Rest Mode Milestone Alerts & Alarms
                    val sessionMinutes = (currentSessionMillis / (1000 * 60)).toInt()

                    if (sessionMinutes != lastAlertMinuteTriggered) {
                        // Alerts
                        if (sessionMinutes in restReminders.subList(0, restReminders.lastIndex)) {
                            lastAlertMinuteTriggered = sessionMinutes
                            triggerAlert(context, isAlarm = false)
                        }

                        // Alarm
                        else if (sessionMinutes >= restReminders.last() && sessionMinutes % restAlarmFrequency == 0) {
                            lastAlertMinuteTriggered = sessionMinutes
                            showAlarmDialog = true
                        }
                    }
                }
            }
        }
    }

    PlayRestAlarm(context, showAlarmDialog)
    if (showAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmDialog = false },
            title = { Text("Time to Study! 📚") },
            text = { Text("You've rested for $currentMinutes minutes. Ready to get back to work?") },
            confirmButton = {
                Button(onClick = { switchToMode(SessionMode.STUDY) }) {
                    Text("Back to Study")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmDialog = false }) {
                    Text("Ignore (+5 min)")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(contentPadding)
              .padding(24.dp)
              .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            DashboardHeader(
                totalRealMillis = totalStudyMillis + totalRestMillis,
                totalStudyMillis = totalStudyMillis,
                totalRestMillis = totalRestMillis,
                chunksCount = chunksCount,
                color = backgroundColor
            )

            ChronometerGauge(
                progress = gaugeProgress,
                currentColor = currentColor,
                elapsedMillis = currentSessionMillis,
                modeLabel = if (currentMode == SessionMode.STUDY) "Study" else "Rest"
            )

            // Milestone Message Display
            Text(
                text = milestoneMessage,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )


            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Initial Start or Mode Switcher (Study <-> Rest)
                if (!isRunning && totalStudyMillis == 0L && totalRestMillis == 0L) {
                    Button(
                        onClick = {
                            isRunning = true
                            chunksCount = 1
                        },
                        modifier = Modifier.width(150.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Study")
                    }
                } else if (currentMode == SessionMode.STUDY) {
                    // In Study Mode -> Show "Take a Rest"
                    Button(
                        onClick = { switchToMode(SessionMode.REST) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                        modifier = Modifier.width(150.dp)
                    ) {
                        Icon(Icons.Default.Bedtime, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take a Rest")
                    }
                } else {
                    // In Rest Mode -> Show "Back to Study"
                    Button(
                        onClick = { switchToMode(SessionMode.STUDY) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.width(150.dp)
                    ) {
                        Icon(Icons.Default.School, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back to Study")
                    }
                }

                // Full Reset Button
                OutlinedButton(
                    onClick = {
                        isRunning = false
                        currentMode = SessionMode.STUDY
                        totalStudyMillis = 0L
                        totalRestMillis = 0L
                        currentSessionMillis = 0L
                        chunksCount = 0
                        lastAlertMinuteTriggered = -1
                        showAlarmDialog = false
                    },
                    modifier = Modifier.width(120.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
fun PlayRestAlarm(context: Context, enabled: Boolean) {
    DisposableEffect(enabled) {
        var mediaPlayer: MediaPlayer? = null
        var vibrator: Vibrator? = null

        if (enabled) {
            // 1. Play looping audio stream
            try {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, soundUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    isLooping = true // <--- LOOPS SOUND INFINITELY
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Start continuous vibration pattern
            try {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager =
                        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    manager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 500, 200, 500, 200)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0)
                    // 3rd parameter (0) tells Android to loop starting at index 0 of the array
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200), 0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Runs immediately when showAlarmDialog changes to false
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                vibrator?.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun DashboardHeader(
    totalRealMillis: Long,
    totalStudyMillis: Long,
    totalRestMillis: Long,
    chunksCount: Int,
    color: Color
) {
    Surface(
        color = color,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Real Time",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(totalRealMillis),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem("Study Time", formatTime(totalStudyMillis))
                MetricItem("Rest Time", formatTime(totalRestMillis))
                MetricItem("Chunks", "$chunksCount")
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun triggerAlert(context: Context, isAlarm: Boolean) {
    try {
        val soundUri = if (isAlarm) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        MediaPlayer().apply {
            setDataSource(context, soundUri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            prepare()
            start()
            setOnCompletionListener { release() }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // 2. Play Haptic/Vibration Pattern
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings =
                if (isAlarm) longArrayOf(0, 500, 200, 500, 200, 500) else longArrayOf(
                    0,
                    300,
                    150,
                    300
                )
            val amplitudes =
                if (isAlarm) intArrayOf(0, 255, 0, 255, 0, 255) else intArrayOf(0, 200, 0, 200)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000L)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun ChronometerGauge(
    progress: Float,
    currentColor: Color,
    elapsedMillis: Long,
    modeLabel: String,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFFE0E0E0),
    strokeWidth: Dp = 20.dp
) {
    // Format total time into MM:SS
    val totalSeconds = elapsedMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val formattedTime = "%02d:%02d".format(locale = Locale.US, minutes, seconds)

    Box(
        modifier = modifier
          .fillMaxWidth()
          .aspectRatio(2f),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(
                width = size.width - strokePx,
                height = size.width - strokePx
            )
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val strokeStyle = Stroke(width = strokePx, cap = StrokeCap.Round)

            // Background Track Arc (Static 180° semi-circle)
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = strokeStyle
            )

            // Active Progress Arc (Fills up to 180° max, changes color)
            drawArc(
                color = currentColor,
                startAngle = 180f,
                sweepAngle = 180f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = strokeStyle
            )
        }

        // Center Time Text Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = formattedTime,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = modeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
    }
}

@Preview
@Composable
fun ChronometerPreview() {
    ChronometerGaugeScreen(studyMilestones = listOf(1, 2))
}
