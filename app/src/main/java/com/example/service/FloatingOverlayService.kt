package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.example.R
import com.example.data.model.PaymentNotification

class FloatingOverlayService : Service() {

    companion object {
        const val EXTRA_PAYMENT_JSON = "extra_payment_json"

        fun showOverlay(context: Context, payment: PaymentNotification) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                putExtra(EXTRA_PAYMENT_JSON, payment.toJson())
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable { removeOverlay() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jsonStr = intent?.getStringExtra(EXTRA_PAYMENT_JSON)
        val payment = jsonStr?.let { PaymentNotification.fromJson(it) }

        if (payment != null) {
            displayFloatingAlert(payment)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun displayFloatingAlert(payment: PaymentNotification) {
        removeOverlay()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 80 // slight offset from status bar
        }

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.layout_floating_alert, null).apply {
            findViewById<TextView>(R.id.tvOverlayAmount)?.text = "₹${payment.formattedAmount}"
            findViewById<TextView>(R.id.tvOverlaySender)?.text = payment.sender
            findViewById<TextView>(R.id.tvOverlayApp)?.text = payment.appSource
            findViewById<TextView>(R.id.tvOverlayAnnouncement)?.text = payment.announcementText

            setOnClickListener {
                removeOverlay()
            }
        }

        try {
            windowManager?.addView(overlayView, params)
            // Auto dismiss after 10 seconds
            handler.removeCallbacks(dismissRunnable)
            handler.postDelayed(dismissRunnable, 10000)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun removeOverlay() {
        handler.removeCallbacks(dismissRunnable)
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                // ignore
            }
            overlayView = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }
}
