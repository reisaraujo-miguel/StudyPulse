package com.reisaraujo_miguel.studypulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.reisaraujo_miguel.studypulse.components.ChronometerGaugeScreen
import com.reisaraujo_miguel.studypulse.ui.theme.StudyPulseTheme

/**
 * MainActivity is the main activity of the application.
 */
class MainActivity : ComponentActivity() {
    /**
     * onCreate is called when the activity is created.
     *
     * @param savedInstanceState The saved instance state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyPulseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    /**
                     * @param innerPadding The inner padding provided by Scaffold.
                     */
                        innerPadding ->
                    ChronometerGaugeScreen(
                        contentPadding = innerPadding,
                    )
                }
            }
        }
    }
}
