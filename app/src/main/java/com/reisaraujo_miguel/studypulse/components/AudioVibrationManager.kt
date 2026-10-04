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

/**
 * AudioVibrationManager is a utility class that provides methods to trigger an alert sound and
 * start a continuous vibration pattern.
 *
 * @param context The application context.
 */
class AudioVibrationManager(
    private val context: Context,
) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * triggerAlert triggers the alert sound and vibration pattern.
     *
     * @param isAlarm indicates whether the alert is for an alarm or a notification.
     */
    fun triggerAlert(
        isAlarm: Boolean
    ) {
        // 1. Play Short Notification / Alert Sound
        var alertPlayer: MediaPlayer? = null
        try {
            val soundUri = if (isAlarm) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            if (soundUri != null) {
                val player = MediaPlayer()
                alertPlayer = player
                player.setDataSource(context, soundUri)
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(
                            if (isAlarm)
                                AudioAttributes.USAGE_ALARM
                            else AudioAttributes.USAGE_NOTIFICATION
                        )
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                player.setOnCompletionListener {
                    /**
                     * @param player The MediaPlayer instance being released.
                     */
                        player ->
                    try {
                        player.release()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error releasing MediaPlayer", e)
                    }
                }
                player.prepare()
                player.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alert sound", e)
            try {
                alertPlayer?.release()
            } catch (releaseEx: Exception) {
                Log.e(TAG, "Error releasing alert MediaPlayer on exception", releaseEx)
            }
        }

        // 2. Trigger Haptic Pattern
        try {
            val vibe = getVibrator()
            if ((vibe != null) && vibe.hasVibrator()) {
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering vibration", e)
        }
    }

    /**
     * startContinuousAlarm starts the continuous alarm sound and vibration pattern.
     */
    fun startContinuousAlarm() {
        stopContinuousAlarm()

        // 1. Play Looping Alarm Sound
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            if (soundUri != null) {
                val player = MediaPlayer()
                mediaPlayer = player
                player.setDataSource(context, soundUri)
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                player.isLooping = true
                player.prepare()
                player.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting continuous alarm sound", e)
            stopContinuousAlarm()
        }

        // 2. Start Looping Vibration Pattern
        try {
            vibrator = getVibrator()
            if ((vibrator != null) && (vibrator?.hasVibrator() == true)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 500, 200, 500, 200)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0)
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 500, 200, 500, 200), 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting continuous vibration", e)
        }
    }

    /**
     * stopContinuousAlarm stops the continuous alarm sound and vibration pattern.
     */
    fun stopContinuousAlarm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibrator", e)
        } finally {
            vibrator = null
        }
    }

    companion object {
        private const val TAG = "AudioVibrationManager"
    }
}
