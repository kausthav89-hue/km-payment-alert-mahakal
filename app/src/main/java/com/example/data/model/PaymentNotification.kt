package com.example.data.model

import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PaymentNotification(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val amount: Double,
    val formattedAmount: String,
    val receiver: String,
    val appSource: String, // "PhonePe", "Google Pay", "Paytm", "BHIM", "UPI"
    val packageName: String,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val announcementText: String,
    val language: String // "bn" or "en"
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("sender", sender)
        json.put("amount", amount)
        json.put("formattedAmount", formattedAmount)
        json.put("receiver", receiver)
        json.put("appSource", appSource)
        json.put("packageName", packageName)
        json.put("rawText", rawText)
        json.put("timestamp", timestamp)
        json.put("announcementText", announcementText)
        json.put("language", language)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): PaymentNotification? {
            return try {
                val json = JSONObject(jsonStr)
                PaymentNotification(
                    id = json.optString("id", UUID.randomUUID().toString()),
                    sender = json.optString("sender", "Unknown"),
                    amount = json.optDouble("amount", 0.0),
                    formattedAmount = json.optString("formattedAmount", "0"),
                    receiver = json.optString("receiver", "Kausthav"),
                    appSource = json.optString("appSource", "UPI"),
                    packageName = json.optString("packageName", ""),
                    rawText = json.optString("rawText", ""),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    announcementText = json.optString("announcementText", ""),
                    language = json.optString("language", "bn")
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Converts standard Western digits (0-9) to Bengali digits (০-৯)
         */
        fun toBengaliDigits(input: String): String {
            val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
            val sb = StringBuilder()
            for (ch in input) {
                if (ch in '0'..'9') {
                    sb.append(bengaliDigits[ch - '0'])
                } else {
                    sb.append(ch)
                }
            }
            return sb.toString()
        }

        /**
         * Builds announcement text as requested:
         * Bengali: "[Sender Name] আপনাকে [Amount] টাকা পাঠালেন। গ্রহণ করেছেন [Receiver Name]"
         * English: "Received [Amount] rupees from [Sender Name]. Received by [Receiver Name]"
         */
        fun buildAnnouncement(
            sender: String,
            amount: Double,
            receiver: String,
            language: String
        ): String {
            val isWholeNumber = amount % 1.0 == 0.0
            val amountFormatted = if (isWholeNumber) {
                amount.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", amount)
            }

            return if (language == "bn") {
                val bengaliAmount = toBengaliDigits(amountFormatted)
                "$sender আপনাকে $bengaliAmount টাকা পাঠালেন। গ্রহণ করেছেন $receiver"
            } else {
                "Received $amountFormatted rupees from $sender. Received by $receiver"
            }
        }
    }
}
