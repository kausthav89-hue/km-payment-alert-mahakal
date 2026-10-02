package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.PaymentAlertApp
import com.example.R
import com.example.data.model.PaymentNotification
import com.example.data.prefs.AppSettings
import com.example.parser.UPIPaymentParser
import com.example.tts.PaymentTTSManager
import com.example.ui.overlay.PaymentAlertOverlayActivity

class PaymentNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "PaymentNotificationSvc"
    }

    private lateinit var settings: AppSettings
    private lateinit var ttsManager: PaymentTTSManager

    override fun onCreate() {
        super.onCreate()
        settings = AppSettings.getInstance(this)
        ttsManager = PaymentTTSManager.getInstance(this)
        Log.d(TAG, "PaymentNotificationListenerService created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        // Check if master switch is ON
        if (!settings.isServiceEnabled) {
            Log.d(TAG, "Service is disabled in settings. Skipping notification.")
            return
        }

        val packageName = sbn.packageName ?: return

        // Ignore notifications posted by our own app to prevent loops
        if (packageName == applicationContext.packageName) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val fullText = listOfNotNull(title, text, bigText, subText).joinToString(" ")
        Log.d(TAG, "Notification received from [$packageName]: $fullText")

        // Check if this notification matches UPI payment credit criteria
        if (!UPIPaymentParser.isPaymentNotification(packageName, title, fullText)) {
            return
        }

        Log.i(TAG, "Matching UPI payment notification detected from $packageName")

        // Parse payment details
        val payment = UPIPaymentParser.parse(
            packageName = packageName,
            title = title,
            text = bigText ?: text,
            subText = subText,
            receiverName = settings.receiverName,
            language = settings.ttsLanguage
        )

        if (payment != null) {
            handleIncomingPayment(payment)
        } else {
            Log.w(TAG, "Could not parse payment from text: $fullText")
        }
    }

    /**
     * Executes the soundbox alert flow: Screen Wake -> Vibration -> TTS -> Lock Screen Heads-Up -> History
     */
    fun handleIncomingPayment(payment: PaymentNotification) {
        Log.i(TAG, "Triggering soundbox alert for: ${payment.sender}, Amount: ${payment.amount}")

        // 1. Wake the screen immediately
        wakeScreen()

        // 2. Vibrate
        if (settings.vibrateOnAlert) {
            triggerVibration()
        }

        // 3. Save to history
        settings.addPayment(payment)

        // 4. Speak voice announcement loudly
        ttsManager.speakPayment(
            text = payment.announcementText,
            language = payment.language,
            boostVolume = settings.boostVolume,
            speechRate = settings.speechRate
        )

        // 5. Show Lockscreen Overlay / Heads-up Activity
        if (settings.showLockscreenOverlay) {
            showLockscreenAlert(payment)
        }

        // 6. Show Floating Window Overlay if phone is unlocked and permission granted
        if (settings.showFloatingOverlay) {
            FloatingOverlayService.showOverlay(this, payment)
        }
    }

    private fun wakeScreen() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "PaymentAlert:SoundboxWakeLock"
            )
            wakeLock?.acquire(10000L) // 10 seconds
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 200, 100, 200, 100, 300),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 200, 100, 200, 100, 300),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 200, 100, 200, 100, 300), -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun showLockscreenAlert(payment: PaymentNotification) {
        val intent = Intent(this, PaymentAlertOverlayActivity::class.java).apply {
            putExtra(PaymentAlertOverlayActivity.EXTRA_PAYMENT_JSON, payment.toJson())
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            payment.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Post high priority notification with Full Screen Intent (essential for heads-up and lock screen on Android 10+)
        val notification = NotificationCompat.Builder(this, PaymentAlertApp.CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("₹${payment.formattedAmount} Received from ${payment.sender}")
            .setContentText(payment.announcementText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(payment.id.hashCode(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post alert notification: ${e.message}")
        }

        // Also start activity directly
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start overlay activity directly: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // no-op
    }

    override fun onDestroy() {
        Log.d(TAG, "PaymentNotificationListenerService destroyed")
        super.onDestroy()
    }
}
