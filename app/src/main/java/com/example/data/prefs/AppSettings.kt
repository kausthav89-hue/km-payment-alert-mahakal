package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.PaymentNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _recentPaymentsFlow = MutableStateFlow<List<PaymentNotification>>(emptyList())
    val recentPaymentsFlow: StateFlow<List<PaymentNotification>> = _recentPaymentsFlow.asStateFlow()

    init {
        _recentPaymentsFlow.value = loadPayments()
    }

    companion object {
        private const val PREFS_NAME = "payment_alert_prefs"
        private const val KEY_SERVICE_ENABLED = "is_service_enabled"
        private const val KEY_RECEIVER_NAME = "receiver_name"
        private const val KEY_TTS_LANGUAGE = "tts_language" // "bn" or "en"
        private const val KEY_BOOST_VOLUME = "boost_volume"
        private const val KEY_SHOW_LOCKSCREEN = "show_lockscreen"
        private const val KEY_SHOW_FLOATING_OVERLAY = "show_floating_overlay"
        private const val KEY_VIBRATE = "vibrate_on_alert"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_WELCOME_SEEN = "welcome_seen"
        private const val KEY_PAYMENTS_LIST = "saved_payments_json"
        private const val MAX_HISTORY_ITEMS = 50

        @Volatile
        private var instance: AppSettings? = null

        fun getInstance(context: Context): AppSettings {
            return instance ?: synchronized(this) {
                instance ?: AppSettings(context.applicationContext).also { instance = it }
            }
        }
    }

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    var receiverName: String
        get() = prefs.getString(KEY_RECEIVER_NAME, "Kausthav") ?: "Kausthav"
        set(value) = prefs.edit().putString(KEY_RECEIVER_NAME, value.trim()).apply()

    var ttsLanguage: String
        get() = prefs.getString(KEY_TTS_LANGUAGE, "bn") ?: "bn"
        set(value) = prefs.edit().putString(KEY_TTS_LANGUAGE, value).apply()

    var boostVolume: Boolean
        get() = prefs.getBoolean(KEY_BOOST_VOLUME, true)
        set(value) = prefs.edit().putBoolean(KEY_BOOST_VOLUME, value).apply()

    var showLockscreenOverlay: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LOCKSCREEN, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_LOCKSCREEN, value).apply()

    var showFloatingOverlay: Boolean
        get() = prefs.getBoolean(KEY_SHOW_FLOATING_OVERLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_FLOATING_OVERLAY, value).apply()

    var vibrateOnAlert: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE, value).apply()

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    var hasSeenWelcomePopup: Boolean
        get() = prefs.getBoolean(KEY_WELCOME_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_WELCOME_SEEN, value).apply()

    fun addPayment(payment: PaymentNotification) {
        val current = loadPayments().toMutableList()
        current.add(0, payment)
        if (current.size > MAX_HISTORY_ITEMS) {
            current.removeAt(current.size - 1)
        }
        savePayments(current)
        _recentPaymentsFlow.value = current
    }

    fun clearHistory() {
        savePayments(emptyList())
        _recentPaymentsFlow.value = emptyList()
    }

    private fun loadPayments(): List<PaymentNotification> {
        val jsonStr = prefs.getString(KEY_PAYMENTS_LIST, null) ?: return emptyList()
        val list = mutableListOf<PaymentNotification>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val itemStr = jsonArray.getString(i)
                PaymentNotification.fromJson(itemStr)?.let { list.add(it) }
            }
        } catch (e: Exception) {
            // ignore
        }
        return list
    }

    private fun savePayments(list: List<PaymentNotification>) {
        val jsonArray = JSONArray()
        for (item in list) {
            jsonArray.put(item.toJson())
        }
        prefs.edit().putString(KEY_PAYMENTS_LIST, jsonArray.toString()).apply()
    }
}
