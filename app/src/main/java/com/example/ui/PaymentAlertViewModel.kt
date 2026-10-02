package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PaymentNotification
import com.example.data.prefs.AppSettings
import com.example.parser.UPIPaymentParser
import com.example.service.FloatingOverlayService
import com.example.service.PaymentKeepAliveService
import com.example.tts.PaymentTTSManager
import com.example.ui.overlay.PaymentAlertOverlayActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentAlertViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = AppSettings.getInstance(application)
    private val ttsManager = PaymentTTSManager.getInstance(application)

    val isServiceEnabled = MutableStateFlow(settings.isServiceEnabled)
    val receiverName = MutableStateFlow(settings.receiverName)
    val ttsLanguage = MutableStateFlow(settings.ttsLanguage)
    val boostVolume = MutableStateFlow(settings.boostVolume)
    val showLockscreenOverlay = MutableStateFlow(settings.showLockscreenOverlay)
    val showFloatingOverlay = MutableStateFlow(settings.showFloatingOverlay)
    val vibrateOnAlert = MutableStateFlow(settings.vibrateOnAlert)
    val speechRate = MutableStateFlow(settings.speechRate)

    val hasSeenWelcome = MutableStateFlow(settings.hasSeenWelcomePopup)
    val recentPayments: StateFlow<List<PaymentNotification>> = settings.recentPaymentsFlow

    val isNotificationAccessGranted = MutableStateFlow(false)
    val isOverlayPermissionGranted = MutableStateFlow(false)
    val isBatteryOptimizationIgnored = MutableStateFlow(false)

    init {
        refreshPermissionStates()
    }

    fun refreshPermissionStates() {
        val context = getApplication<Application>()

        // 1. Notification Listener Access
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        isNotificationAccessGranted.value = enabledListeners.contains(context.packageName)

        // 2. Overlay Permission
        isOverlayPermissionGranted.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

        // 3. Battery Optimization
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isBatteryOptimizationIgnored.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            true
        }
    }

    fun toggleService(enabled: Boolean) {
        settings.isServiceEnabled = enabled
        isServiceEnabled.value = enabled
        val context = getApplication<Application>()
        if (enabled) {
            PaymentKeepAliveService.start(context)
        } else {
            PaymentKeepAliveService.stop(context)
        }
    }

    fun updateReceiverName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            settings.receiverName = trimmed
            receiverName.value = trimmed
        }
    }

    fun updateLanguage(lang: String) {
        settings.ttsLanguage = lang
        ttsLanguage.value = lang
    }

    fun toggleBoostVolume(enabled: Boolean) {
        settings.boostVolume = enabled
        boostVolume.value = enabled
    }

    fun toggleLockscreenOverlay(enabled: Boolean) {
        settings.showLockscreenOverlay = enabled
        showLockscreenOverlay.value = enabled
    }

    fun toggleFloatingOverlay(enabled: Boolean) {
        settings.showFloatingOverlay = enabled
        showFloatingOverlay.value = enabled
    }

    fun toggleVibrate(enabled: Boolean) {
        settings.vibrateOnAlert = enabled
        vibrateOnAlert.value = enabled
    }

    fun updateSpeechRate(rate: Float) {
        settings.speechRate = rate
        speechRate.value = rate
    }

    fun setWelcomeSeen(seen: Boolean) {
        settings.hasSeenWelcomePopup = seen
        hasSeenWelcome.value = seen
    }

    fun clearHistory() {
        settings.clearHistory()
    }

    /**
     * Simulates full live payment alert flow (voice announcement + lockscreen overlay + history + floating overlay)
     */
    fun simulatePaymentAlert(
        sender: String,
        amount: Double,
        appSource: String
    ) {
        val context = getApplication<Application>()
        val packageName = when (appSource) {
            "PhonePe" -> UPIPaymentParser.PKG_PHONEPE
            "Google Pay" -> UPIPaymentParser.PKG_GPAY
            "Paytm" -> UPIPaymentParser.PKG_PAYTM
            else -> UPIPaymentParser.PKG_BHIM
        }

        val formattedAmount = if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", amount)
        }

        val announcement = PaymentNotification.buildAnnouncement(
            sender = sender,
            amount = amount,
            receiver = receiverName.value,
            language = ttsLanguage.value
        )

        val payment = PaymentNotification(
            sender = sender,
            amount = amount,
            formattedAmount = formattedAmount,
            receiver = receiverName.value,
            appSource = appSource,
            packageName = packageName,
            rawText = "Payment received from $sender of ₹$formattedAmount via $appSource",
            announcementText = announcement,
            language = ttsLanguage.value
        )

        // Save to history
        settings.addPayment(payment)

        // Speak
        ttsManager.speakPayment(
            text = announcement,
            language = ttsLanguage.value,
            boostVolume = boostVolume.value,
            speechRate = speechRate.value
        )

        // Open Lockscreen / Overlay Activity
        if (showLockscreenOverlay.value) {
            val intent = Intent(context, PaymentAlertOverlayActivity::class.java).apply {
                putExtra(PaymentAlertOverlayActivity.EXTRA_PAYMENT_JSON, payment.toJson())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }

        // Floating Overlay
        if (showFloatingOverlay.value) {
            FloatingOverlayService.showOverlay(context, payment)
        }
    }

    /**
     * Replays or tests voice announcement only
     */
    fun testVoiceAnnouncement(
        sender: String,
        amount: Double
    ) {
        val announcement = PaymentNotification.buildAnnouncement(
            sender = sender,
            amount = amount,
            receiver = receiverName.value,
            language = ttsLanguage.value
        )

        ttsManager.speakPayment(
            text = announcement,
            language = ttsLanguage.value,
            boostVolume = boostVolume.value,
            speechRate = speechRate.value
        )
    }

    fun replayPayment(payment: PaymentNotification) {
        ttsManager.speakPayment(
            text = payment.announcementText,
            language = payment.language,
            boostVolume = boostVolume.value,
            speechRate = speechRate.value
        )
    }

    override fun onCleared() {
        ttsManager.shutdown()
        super.onCleared()
    }
}
