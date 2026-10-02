package com.example.parser

import com.example.data.model.PaymentNotification
import java.util.regex.Pattern

object UPIPaymentParser {

    // Package names of popular UPI payment apps
    const val PKG_PHONEPE = "com.phonepe.app"
    const val PKG_PHONEPE_BUSINESS = "com.phonepe.app.business"
    const val PKG_GPAY = "com.google.android.apps.nfc.plugin"
    const val PKG_PAYTM = "net.one97.paytm"
    const val PKG_PAYTM_BUSINESS = "com.paytm.business"
    const val PKG_BHIM = "in.org.npci.upiapp"
    const val PKG_AMAZON_PAY = "in.amazon.mShop.android.shopping"
    const val PKG_WHATSAPP = "com.whatsapp"

    // Supported package names set
    val SUPPORTED_PACKAGES = setOf(
        PKG_PHONEPE,
        PKG_PHONEPE_BUSINESS,
        PKG_GPAY,
        PKG_PAYTM,
        PKG_PAYTM_BUSINESS,
        PKG_BHIM,
        PKG_AMAZON_PAY,
        PKG_WHATSAPP
    )

    // Regex for amounts (with ₹, Rs, Rs., INR, or Bengali টাকা)
    // Matches: ₹500, Rs. 500, Rs 1,250.50, INR 500, 500 টাকা, ₹৫০০
    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:[₹]|Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:টাকা|[₹]|Rs\.?)|(?:[₹]|টাকা)\s*([০-৯,]+(?:\.[০-৯]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    // Keywords that indicate money was SENT or DEBITED (which we MUST ignore!)
    private val DEBIT_KEYWORDS = listOf(
        "paid to",
        "sent to",
        "debited",
        "debited by",
        "payment sent",
        "transferred to",
        "payment of rs.*to",
        "পাঠিয়েছেন", // if user sent
        "debit card",
        "request received",
        "requested money",
        "failed",
        "declined"
    )

    // Keywords indicating money was RECEIVED / CREDITED
    private val CREDIT_KEYWORDS = listOf(
        "received",
        "credited",
        "sent you",
        "paid you",
        "accepted",
        "money received",
        "payment received",
        "পেয়েছেন",
        "পাঠালেন",
        "ক্রেডিট",
        "deposit"
    )

    /**
     * Determines whether the given notification is likely a payment credit notification.
     */
    fun isPaymentNotification(packageName: String, title: String?, text: String?): Boolean {
        val combined = "${title.orEmpty()} ${text.orEmpty()}".lowercase()

        // Check if package is known payment app OR combined text explicitly mentions UPI / bank payment
        val isTargetApp = SUPPORTED_PACKAGES.contains(packageName) ||
                combined.contains("phonepe") ||
                combined.contains("paytm") ||
                combined.contains("gpay") ||
                combined.contains("google pay") ||
                combined.contains("upi") ||
                combined.contains("bhim")

        if (!isTargetApp && !combined.contains("upi")) {
            return false
        }

        // Must not be a debit or payment request
        for (debitKey in DEBIT_KEYWORDS) {
            if (combined.contains(debitKey)) {
                // If it contains "paid to" or "sent to", it's outgoing money
                return false
            }
        }

        // Must contain at least one credit keyword or a currency symbol with amount
        val hasCreditWord = CREDIT_KEYWORDS.any { combined.contains(it) }
        val hasAmount = AMOUNT_PATTERN.matcher(combined).find()

        return (hasCreditWord && hasAmount) || (isTargetApp && hasAmount && combined.contains("from"))
    }

    /**
     * Parse notification text and extract Sender, Amount, and App source.
     */
    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        subText: String?,
        receiverName: String,
        language: String
    ): PaymentNotification? {
        val fullText = listOfNotNull(title, text, subText)
            .joinToString(" ")
            .trim()

        if (fullText.isEmpty()) return null

        val appSource = identifyAppSource(packageName, fullText)
        val amount = extractAmount(fullText) ?: return null
        val sender = extractSender(fullText, title, text)

        val formattedAmount = if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", amount)
        }

        val announcement = PaymentNotification.buildAnnouncement(
            sender = sender,
            amount = amount,
            receiver = receiverName,
            language = language
        )

        return PaymentNotification(
            sender = sender,
            amount = amount,
            formattedAmount = formattedAmount,
            receiver = receiverName,
            appSource = appSource,
            packageName = packageName,
            rawText = fullText,
            announcementText = announcement,
            language = language
        )
    }

    /**
     * Identify human-readable payment app name
     */
    fun identifyAppSource(packageName: String, text: String): String {
        val lowerText = text.lowercase()
        return when {
            packageName.contains("phonepe") || lowerText.contains("phonepe") -> "PhonePe"
            packageName.contains("google.android.apps.nfc") || lowerText.contains("google pay") || lowerText.contains("gpay") -> "Google Pay"
            packageName.contains("paytm") || lowerText.contains("paytm") -> "Paytm"
            packageName.contains("npci.upiapp") || lowerText.contains("bhim") -> "BHIM UPI"
            packageName.contains("whatsapp") || lowerText.contains("whatsapp") -> "WhatsApp Pay"
            packageName.contains("amazon") || lowerText.contains("amazon pay") -> "Amazon Pay"
            else -> "UPI"
        }
    }

    /**
     * Extract numerical amount from notification string
     */
    fun extractAmount(text: String): Double? {
        val matcher = AMOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            // Group 1: standard digits after ₹/Rs
            val grp1 = matcher.group(1)
            if (!grp1.isNullOrBlank()) {
                val cleaned = grp1.replace(",", "").trim()
                return cleaned.toDoubleOrNull()
            }
            // Group 2: standard digits before টাকা/₹
            val grp2 = matcher.group(2)
            if (!grp2.isNullOrBlank()) {
                val cleaned = grp2.replace(",", "").trim()
                return cleaned.toDoubleOrNull()
            }
            // Group 3: Bengali digits
            val grp3 = matcher.group(3)
            if (!grp3.isNullOrBlank()) {
                val englishDigits = fromBengaliDigits(grp3.replace(",", "").trim())
                return englishDigits.toDoubleOrNull()
            }
        }

        // Fallback: search for any standalone number following ₹ or Rs
        val fallbackPattern = Pattern.compile("""(?:[₹]|Rs\.?|INR)\s*([\d,]+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE)
        val fbMatcher = fallbackPattern.matcher(text)
        if (fbMatcher.find()) {
            val numStr = fbMatcher.group(1)?.replace(",", "")?.trim()
            return numStr?.toDoubleOrNull()
        }

        return null
    }

    /**
     * Extract Sender name accurately from notification text
     */
    fun extractSender(fullText: String, title: String?, text: String?): String {
        val body = text.orEmpty()
        val heading = title.orEmpty()

        // 1. Google Pay format: "Rahul Sharma sent you ₹500" or "Rahul Sharma paid you ₹500"
        val gpayPattern = Pattern.compile("""^([A-Za-z0-9\s.]+?)\s+(?:sent you|paid you|has sent you)""", Pattern.CASE_INSENSITIVE)
        var matcher = gpayPattern.matcher(body)
        if (matcher.find()) {
            val name = cleanSenderName(matcher.group(1))
            if (isValidName(name)) return name
        }
        matcher = gpayPattern.matcher(heading)
        if (matcher.find()) {
            val name = cleanSenderName(matcher.group(1))
            if (isValidName(name)) return name
        }

        // 2. PhonePe / Paytm format: "Received ₹500 from Rahul Sharma" or "...received from Rahul Sharma"
        val fromPattern = Pattern.compile(
            """(?:received from|received\s+[₹Rs.0-9,]+\s+from|from)\s+([A-Za-z0-9\s.']+?)(?:\s+(?:on|via|using|ref|UPI|in|to|\.|\$|\n)|$)""",
            Pattern.CASE_INSENSITIVE
        )
        matcher = fromPattern.matcher(body)
        if (matcher.find()) {
            val name = cleanSenderName(matcher.group(1))
            if (isValidName(name)) return name
        }

        matcher = fromPattern.matcher(fullText)
        if (matcher.find()) {
            val name = cleanSenderName(matcher.group(1))
            if (isValidName(name)) return name
        }

        // 3. Bengali format: "Rahul Sharma থেকে ₹৫০০ পেয়েছেন" or "Rahul Sharma আপনাকে..."
        val bengaliFromPattern = Pattern.compile("""^([^\s]+(?:\s+[^\s]+)*?)\s*(?:থেকে|আপনাকে)""")
        val bnMatcher = bengaliFromPattern.matcher(body)
        if (bnMatcher.find()) {
            val name = cleanSenderName(bnMatcher.group(1))
            if (isValidName(name)) return name
        }

        // 4. If title itself is a person's name (and not app name like "PhonePe" or "Payment Received")
        if (heading.isNotBlank() && !isGenericTitle(heading)) {
            val cleanTitle = cleanSenderName(heading)
            if (isValidName(cleanTitle)) return cleanTitle
        }

        return "Customer"
    }

    private fun cleanSenderName(raw: String?): String {
        if (raw == null) return ""
        return raw.trim()
            .replace(Regex("""^(?:Dear|Mr\.?|Mrs\.?|Ms\.?)\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(?:on|via|using|ref|UPI|a\/c|account|bank|wallet).*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[.,;:]+$"""), "")
            .trim()
    }

    private fun isValidName(name: String): Boolean {
        if (name.isBlank() || name.length < 2 || name.length > 50) return false
        val lower = name.lowercase()
        val blacklisted = listOf(
            "phonepe", "paytm", "gpay", "google pay", "bhim",
            "payment", "received", "credited", "upi", "money",
            "notification", "bank", "account", "cashback", "alert"
        )
        return blacklisted.none { lower == it || lower.startsWith("$it ") }
    }

    private fun isGenericTitle(title: String): Boolean {
        val lower = title.lowercase()
        return lower.contains("phonepe") ||
                lower.contains("paytm") ||
                lower.contains("google pay") ||
                lower.contains("payment received") ||
                lower.contains("money received") ||
                lower.contains("upi") ||
                lower.contains("alert") ||
                lower.contains("credited")
    }

    private fun fromBengaliDigits(input: String): String {
        val bengaliDigits = "০১২৩৪৫৬৭৮৯"
        val sb = StringBuilder()
        for (ch in input) {
            val idx = bengaliDigits.indexOf(ch)
            if (idx != -1) {
                sb.append(idx)
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
