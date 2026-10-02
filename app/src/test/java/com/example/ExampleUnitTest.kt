package com.example

import com.example.data.model.PaymentNotification
import com.example.parser.UPIPaymentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPhonePeParsing() {
        val packageName = "com.phonepe.app"
        val title = "PhonePe"
        val text = "Received ₹500 from Rahul Sharma"

        val isPayment = UPIPaymentParser.isPaymentNotification(packageName, title, text)
        assertTrue("Should detect PhonePe payment credit", isPayment)

        val payment = UPIPaymentParser.parse(
            packageName = packageName,
            title = title,
            text = text,
            subText = null,
            receiverName = "Kausthav",
            language = "bn"
        )

        assertNotNull(payment)
        assertEquals("Rahul Sharma", payment?.sender)
        assertEquals(500.0, payment?.amount ?: 0.0, 0.001)
        assertEquals("Kausthav", payment?.receiver)
        assertEquals("PhonePe", payment?.appSource)
        assertEquals(
            "Rahul Sharma আপনাকে ৫০০ টাকা পাঠালেন। গ্রহণ করেছেন Kausthav",
            payment?.announcementText
        )
    }

    @Test
    fun testGooglePayParsing() {
        val packageName = "com.google.android.apps.nfc.plugin"
        val title = "Google Pay"
        val text = "Rahul Sharma sent you ₹1250"

        val isPayment = UPIPaymentParser.isPaymentNotification(packageName, title, text)
        assertTrue("Should detect Google Pay credit", isPayment)

        val payment = UPIPaymentParser.parse(
            packageName = packageName,
            title = title,
            text = text,
            subText = null,
            receiverName = "Kausthav",
            language = "bn"
        )

        assertNotNull(payment)
        assertEquals("Rahul Sharma", payment?.sender)
        assertEquals(1250.0, payment?.amount ?: 0.0, 0.001)
        assertEquals("Google Pay", payment?.appSource)
        assertEquals(
            "Rahul Sharma আপনাকে ১২৫০ টাকা পাঠালেন। গ্রহণ করেছেন Kausthav",
            payment?.announcementText
        )
    }

    @Test
    fun testPaytmParsing() {
        val packageName = "net.one97.paytm"
        val title = "Paytm"
        val text = "Received ₹250 from Priya Patel on Paytm"

        val isPayment = UPIPaymentParser.isPaymentNotification(packageName, title, text)
        assertTrue("Should detect Paytm credit", isPayment)

        val payment = UPIPaymentParser.parse(
            packageName = packageName,
            title = title,
            text = text,
            subText = null,
            receiverName = "Kausthav",
            language = "en"
        )

        assertNotNull(payment)
        assertEquals("Priya Patel", payment?.sender)
        assertEquals(250.0, payment?.amount ?: 0.0, 0.001)
        assertEquals("Paytm", payment?.appSource)
        assertEquals(
            "Received 250 rupees from Priya Patel. Received by Kausthav",
            payment?.announcementText
        )
    }

    @Test
    fun testIgnoreDebitNotification() {
        val packageName = "com.phonepe.app"
        val title = "PhonePe"
        val text = "Paid ₹500 to Reliance Fresh"

        val isPayment = UPIPaymentParser.isPaymentNotification(packageName, title, text)
        org.junit.Assert.assertFalse("Debit/outgoing notifications must be ignored", isPayment)
    }
}
