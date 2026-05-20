package com.example.webreader.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.webreader.MainActivity
import com.example.webreader.ui.BrowserViewModel

class TtsService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var silentAudioTrack: AudioTrack? = null
    private var mediaSession: MediaSession? = null

    companion object {
        private const val CHANNEL_ID = "tts_service_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.webreader.ACTION_START"
        const val ACTION_PAUSE = "com.example.webreader.ACTION_PAUSE"
        const val ACTION_STOP = "com.example.webreader.ACTION_STOP"
        const val EXTRA_TITLE = "extra_title"

        fun start(context: Context, title: String) {
            val intent = Intent(context, TtsService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TtsService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Đang đọc báo..."
                val notification = buildNotification(title)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                acquireWakeLock()
                startSilentAudio()
                setupMediaSession()
            }
            ACTION_PAUSE -> {
                BrowserViewModel.activeInstance?.pauseReading()
                stopSilentAudio()
                releaseMediaSession()
                releaseWakeLock()
                stopForeground(true)
                stopSelf()
            }
            ACTION_STOP -> {
                BrowserViewModel.activeInstance?.pauseReading()
                stopSilentAudio()
                releaseMediaSession()
                releaseWakeLock()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WebReader::TtsWakeLock"
            ).apply {
                setReferenceCounted(false)
            }
        }
        if (wakeLock?.isHeld == false) {
            // Acquire wake lock with a 2-hour timeout as a safety limit
            wakeLock?.acquire(2 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun startSilentAudio() {
        try {
            if (silentAudioTrack == null) {
                val sampleRate = 8000
                val numSamples = sampleRate * 1 // 1 second of silence
                val sizeInBytes = numSamples * 2 // 16-bit PCM = 2 bytes per sample
                
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                
                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(sizeInBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                
                val silentBuffer = ShortArray(numSamples) // filled with zeros
                track.write(silentBuffer, 0, silentBuffer.size)
                track.setLoopPoints(0, numSamples, -1) // loop infinitely
                track.play()
                
                silentAudioTrack = track
            }
        } catch (e: Exception) {
            android.util.Log.e("TtsService", "Lỗi khởi tạo âm thanh tĩnh: ${e.message}", e)
        }
    }

    private fun stopSilentAudio() {
        try {
            silentAudioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
            silentAudioTrack = null
        } catch (e: Exception) {
            android.util.Log.e("TtsService", "Lỗi dừng âm thanh tĩnh: ${e.message}", e)
        }
    }

    private fun setupMediaSession() {
        try {
            if (mediaSession == null) {
                mediaSession = MediaSession(this, "WebReaderMediaSession").apply {
                    val state = PlaybackState.Builder()
                        .setActions(
                            PlaybackState.ACTION_PLAY or
                            PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_STOP or
                            PlaybackState.ACTION_SKIP_TO_NEXT or
                            PlaybackState.ACTION_SKIP_TO_PREVIOUS
                        )
                        .setState(PlaybackState.STATE_PLAYING, 0L, 1.0f)
                        .build()
                    setPlaybackState(state)
                    
                    setCallback(object : MediaSession.Callback() {
                        override fun onPlay() {
                            BrowserViewModel.activeInstance?.resumeReading()
                            updateMediaSessionState(PlaybackState.STATE_PLAYING)
                        }

                        override fun onPause() {
                            BrowserViewModel.activeInstance?.pauseReading()
                            updateMediaSessionState(PlaybackState.STATE_PAUSED)
                        }

                        override fun onStop() {
                            BrowserViewModel.activeInstance?.pauseReading()
                            updateMediaSessionState(PlaybackState.STATE_STOPPED)
                        }

                        override fun onSkipToNext() {
                            BrowserViewModel.activeInstance?.playNext()
                        }

                        override fun onSkipToPrevious() {
                            BrowserViewModel.activeInstance?.playPrevious()
                        }
                    })
                    
                    isActive = true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TtsService", "Lỗi thiết lập MediaSession: ${e.message}", e)
        }
    }

    private fun updateMediaSessionState(stateInt: Int) {
        try {
            mediaSession?.apply {
                val state = PlaybackState.Builder()
                    .setActions(
                        PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_STOP or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS
                    )
                    .setState(stateInt, 0L, 1.0f)
                    .build()
                setPlaybackState(state)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun releaseMediaSession() {
        try {
            mediaSession?.apply {
                isActive = false
                release()
            }
            mediaSession = null
        } catch (e: Exception) {
            android.util.Log.e("TtsService", "Lỗi giải phóng MediaSession: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        stopSilentAudio()
        releaseMediaSession()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WebReader TTS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kênh thông báo cho bộ đọc WebReader dưới nền"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = Intent(this, TtsService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, TtsService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WebReader đang phát giọng đọc")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play) // System play icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Tạm dừng", pausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dừng", stopPendingIntent)

        return builder.build()
    }
}
