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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

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
        viewModel.events.collect { event ->
            when (event) {
                is ChronometerEvent.MilestoneAlert -> audioVibrationManager.triggerAlert(event.isAlarm)
                ChronometerEvent.StartAlarm -> audioVibrationManager.startContinuousAlarm()
                ChronometerEvent.StopAlarm -> audioVibrationManager.stopContinuousAlarm()
            }
        }
    }

    DisposableEffect(Unit) {
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

    val backgroundColor =
        if (uiState.isRunning) currentColor.copy(alpha = 0.10f) else MaterialTheme.colorScheme.background

    val milestoneMessage = uiState.milestoneMessage(studyMilestones, restReminders)

    if (uiState.showAlarmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlarmDialog() },
            title = { Text("Time to Study! 📚") },
            text = { Text("You've rested for ${uiState.currentMinutes()} minutes. Ready to get back to work?") },
            confirmButton = {
                Button(onClick = { viewModel.switchToMode(SessionMode.STUDY) }) {
                    Text("Back to Study")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAlarmDialog() }) {
                    Text("Ignore (+5 min)")
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
                .padding(24.dp)
                .padding(bottom = 24.dp),
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
                modeLabel = if (uiState.currentMode == SessionMode.STUDY) "Study" else "Rest"
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
                if (!uiState.isRunning && uiState.totalStudyMillis == 0L && uiState.totalRestMillis == 0L) {
                    Button(
                        onClick = { viewModel.startStudy() },
                        modifier = Modifier.width(150.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Study")
                    }
                } else if (uiState.currentMode == SessionMode.STUDY) {
                    Button(
                        onClick = { viewModel.switchToMode(SessionMode.REST) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                        modifier = Modifier.width(150.dp)
                    ) {
                        Icon(Icons.Default.Bedtime, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take a Rest")
                    }
                } else {
                    Button(
                        onClick = { viewModel.switchToMode(SessionMode.STUDY) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.width(150.dp)
                    ) {
                        Icon(Icons.Default.School, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back to Study")
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.reset() },
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
                modifier = Modifier.fillMaxWidth(),
                thickness = DividerDefaults.Thickness,
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
            val diameter = size.width - strokePx
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
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

@Preview
@Composable
fun ChronometerPreview() {
    ChronometerGaugeScreen(studyMilestones = listOf(1, 2))
}
