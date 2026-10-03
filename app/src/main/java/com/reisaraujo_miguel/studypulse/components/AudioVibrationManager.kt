package com.reisaraujo_miguel.studypulse.components

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AudioVibrationManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun triggerAlert(isAlarm: Boolean) {
        // 1. Play Short Notification / Alert Sound
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
                        .setUsage(if (isAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                prepare()
                start()
                setOnCompletionListener { mp ->
                    try {
                        mp.release()
                    } catch (e: Exception) {
                        Log.e("AudioVibrationManager", "Error releasing MediaPlayer", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioVibrationManager", "Error playing alert sound", e)
        }

        // 2. Trigger Haptic Pattern
        try {
            val vibe = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = if (isAlarm) {
                    longArrayOf(0, 500, 200, 500, 200, 500)
                } else {
                    longArrayOf(0, 300, 150, 300)
                }
                val amplitudes = if (isAlarm) {
                    intArrayOf(0, 255, 0, 255, 0, 255)
                } else {
                    intArrayOf(0, 200, 0, 200)
                }
                vibe.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibe.vibrate(1000L)
            }
        } catch (e: Exception) {
            Log.e("AudioVibrationManager", "Error triggering vibration", e)
        }
    }

    fun startContinuousAlarm() {
        stopContinuousAlarm()

        // 1. Play Looping Alarm Sound
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AudioVibrationManager", "Error starting continuous alarm sound", e)
        }

        // 2. Start Looping Vibration Pattern
        try {
            vibrator = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 500, 200, 500, 200)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 200, 500, 200), 0)
            }
        } catch (e: Exception) {
            Log.e("AudioVibrationManager", "Error starting continuous vibration", e)
        }
    }

    fun stopContinuousAlarm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AudioVibrationManager", "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AudioVibrationManager", "Error stopping vibrator", e)
        } finally {
            vibrator = null
        }
    }
}
