package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.prefs.AppSettings
import com.example.service.PaymentKeepAliveService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val settings = AppSettings.getInstance(context)
            if (settings.isServiceEnabled) {
                PaymentKeepAliveService.start(context)
            }
        }
    }
}
