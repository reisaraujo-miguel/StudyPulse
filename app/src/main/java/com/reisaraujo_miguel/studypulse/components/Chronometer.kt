package com.reisaraujo_miguel.studypulse.components

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reisaraujo_miguel.studypulse.R
import java.util.Locale

/**
 * ChronometerGaugeScreen is a composable function that displays a gauge representing the
 * elapsed time in a study session.
 *
 * @param modifier The modifier to apply to this layout.
 * @param contentPadding The padding values to apply to the content.
 * @param studyMilestones The list of study milestones to display.
 * @param restReminders The list of rest reminders to display.
 * @param restAlarmFrequency The frequency of rest alarms.
 * @param viewModel The view model to use for this screen.
 */
@Composable
fun ChronometerGaugeScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    studyMilestones: List<Int> = listOf(25, 45, 65, 90),
    restReminders: List<Int> = listOf(15, 20, 25, 30),
    restAlarmFrequency: Int = 5,
    viewModel: ChronometerViewModel = viewModel()
) {
    val context = LocalContext.current
    val audioVibrationManager = remember(context) { AudioVibrationManager(context) }

    LaunchedEffect(studyMilestones, restReminders, restAlarmFrequency) {
        viewModel.studyMilestones = studyMilestones
        viewModel.restReminders = restReminders
        viewModel.restAlarmFrequency = restAlarmFrequency
    }

    LaunchedEffect(viewModel, audioVibrationManager) {
        viewModel.events.collect {
            /**
             * @param event The event to handle.
             */
                event ->
            when (event) {
                is ChronometerEvent.MilestoneAlert ->
                    audioVibrationManager.triggerAlert(event.isAlarm)

                ChronometerEvent.StartAlarm -> audioVibrationManager.startContinuousAlarm()
                ChronometerEvent.StopAlarm -> audioVibrationManager.stopContinuousAlarm()
            }
        }
    }

    DisposableEffect(audioVibrationManager) {
        onDispose {
            audioVibrationManager.stopContinuousAlarm()
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    val maxStudyTimeMillis = (studyMilestones.lastOrNull() ?: 90) * 60 * 1000f
    val maxRestTimeMillis = (restReminders.lastOrNull() ?: 30) * 60 * 1000f

    val gaugeProgress = uiState.gaugeProgress(maxStudyTimeMillis, maxRestTimeMillis)

    val currentColor = if (uiState.currentMode == SessionMode.STUDY) {
        lerp(Color(0xFFFF3D00), Color(0xFF00E676), gaugeProgress)
    } else {
        lerp(Color(0xFF0288D1), Color(0xFF7C4DFF), gaugeProgress)
    }

    val backgroundColor = if (uiState.isRunning) {
        currentColor.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.background
    }

    val milestoneMessage = uiState.milestoneMessage(studyMilestones, restReminders)

    if (uiState.showAlarmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlarmDialog() },
            title = { Text(stringResource(R.string.alarm_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.alarm_dialog_message,
                        uiState.currentMinutes()
                    )
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.switchToMode(SessionMode.STUDY) }) {
                    Text(stringResource(R.string.alarm_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAlarmDialog() }) {
                    Text(stringResource(R.string.alarm_dialog_dismiss))
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            DashboardHeader(
                totalRealMillis = uiState.totalRealMillis,
                totalStudyMillis = uiState.totalStudyMillis,
                totalRestMillis = uiState.totalRestMillis,
                chunksCount = uiState.chunksCount,
                color = backgroundColor
            )

            ChronometerGauge(
                progress = gaugeProgress,
                currentColor = currentColor,
                elapsedMillis = uiState.currentSessionMillis,
                modeLabel = if (uiState.currentMode == SessionMode.STUDY) {
                    stringResource(R.string.mode_study)
                } else {
                    stringResource(R.string.mode_rest)
                }
            )

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
                if (!uiState.isRunning &&
                    uiState.totalStudyMillis == 0L &&
                    uiState.totalRestMillis == 0L
                ) {
                    Button(
                        onClick = { viewModel.startStudy() },
                        modifier = Modifier.width(150.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_start_study))
                    }
                } else if (uiState.currentMode == SessionMode.STUDY) {
                    Button(
                        onClick = { viewModel.switchToMode(SessionMode.REST) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                        modifier = Modifier.width(150.dp)
                    ) {
                        Icon(Icons.Default.Bedtime, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_take_rest))
                    }
                } else {
                    Button(
                        onClick = { viewModel.switchToMode(SessionMode.STUDY) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.width(150.dp)
                    ) {
                        Icon(Icons.Default.School, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_back_to_study))
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.width(120.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_reset))
                }
            }
        }
    }
}

/**
 * DashboardHeader is a composable function that displays a header for the dashboard.
 *
 * @param totalRealMillis The total real time in milliseconds.
 * @param totalStudyMillis The total study time in milliseconds.
 * @param totalRestMillis The total rest time in milliseconds.
 * @param chunksCount The number of chunks.
 * @param color The background color.
 */
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
                    text = stringResource(R.string.label_total_real_time),
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
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(stringResource(R.string.label_study_time), formatTime(totalStudyMillis))
                MetricItem(stringResource(R.string.label_rest_time), formatTime(totalRestMillis))
                MetricItem(stringResource(R.string.label_chunks), "$chunksCount")
            }
        }
    }
}

/**
 * MetricItem is a composable function that displays a metric item.
 *
 * @param label The label for the metric.
 * @param value The value for the metric.
 */
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

/**
 * formatTime formats the given time in milliseconds to a string in the format "mm:ss" or
 * "hh:mm:ss".
 *
 * @param millis The time in milliseconds to format.
 */
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

/**
 * ChronometerGauge is a composable function that displays a gauge representing the elapsed time.
 *
 * @param progress The progress of the gauge.
 * @param currentColor The color of the current progress.
 * @param elapsedMillis The elapsed time in milliseconds.
 * @param modeLabel The label for the mode.
 * @param modifier The modifier to apply to this layout.
 * @param trackColor The color of the track.
 * @param strokeWidth The width of the stroke.
 */
@Composable
fun ChronometerGauge(
    progress: Float,
    currentColor: Color,
    elapsedMillis: Long,
    modeLabel: String,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    strokeWidth: Dp = 20.dp
) {
    val totalSeconds = elapsedMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = minOf(size.width - strokePx, (size.height - strokePx / 2f) * 2f)
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2f, strokePx / 2f)
            val strokeStyle = Stroke(width = strokePx, cap = StrokeCap.Round)

            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = strokeStyle
            )

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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * ChronometerPreview is a preview of the ChronometerGaugeScreen composable.
 */
@Preview
@Composable
fun ChronometerPreview() {
    ChronometerGaugeScreen(studyMilestones = listOf(1, 2))
}
