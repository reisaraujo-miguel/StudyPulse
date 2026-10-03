package com.reisaraujo_miguel.studypulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.reisaraujo_miguel.studypulse.components.ChronometerGaugeScreen
import com.reisaraujo_miguel.studypulse.ui.theme.StudyPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyPulseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChronometerGaugeScreen(
                        contentPadding = innerPadding,
                        //studyMilestones = listOf(1, 2),
                        //restReminders = listOf(1, 2),
                        //restAlarmFrequency = 1
                    )
                }
            }
        }
    }
}
