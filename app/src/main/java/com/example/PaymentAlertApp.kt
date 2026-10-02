package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class PaymentAlertApp : Application() {

    companion object {
        const val CHANNEL_SERVICE_ID = "payment_alert_service_channel"
        const val CHANNEL_ALERT_ID = "payment_alert_heads_up_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Channel for foreground keep-alive service
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Payment Alert Soundbox Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the payment alert soundbox active in the background"
                setShowBadge(false)
            }

            // Channel for high priority heads-up payment notifications
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Payment Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alerts for incoming UPI payments"
                enableVibration(true)
                enableLights(true)
            }

            notificationManager?.createNotificationChannel(serviceChannel)
            notificationManager?.createNotificationChannel(alertChannel)
        }
    }
}
