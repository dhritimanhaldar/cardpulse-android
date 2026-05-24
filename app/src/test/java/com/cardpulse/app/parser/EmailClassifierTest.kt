package com.cardpulse.app.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EmailClassifierTest {

    @Test
    fun classifiesBankTransactionAlerts() {
        assertKind(
            EmailKind.TRANSACTION_ALERT,
            from = "ICICI Bank Alerts",
            subject = "Transaction alert for your ICICI Bank Credit Card"
        )
        assertKind(
            EmailKind.TRANSACTION_ALERT,
            from = "AU Bank Credit Card Alerts",
            subject = "AU Bank Credit Card Transaction Alert"
        )
        assertKind(
            EmailKind.TRANSACTION_ALERT,
            from = "IDFC FIRST Bank",
            subject = "Debit Alert: Your IDFC FIRST Bank Credit Card"
        )
        assertKind(
            EmailKind.TRANSACTION_ALERT,
            from = "YES BANK Alerts",
            subject = "YES BANK - Transaction Alert"
        )
    }

    @Test
    fun classifiesPaymentAlertsSeparately() {
        assertKind(
            EmailKind.PAYMENT_ALERT,
            from = "AU Bank Credit Card Alerts",
            subject = "AU Bank Credit Card Payment Alert"
        )
    }

    @Test
    fun classifiesMonthlyStatements() {
        assertKind(
            EmailKind.STATEMENT,
            from = "IDFC FIRST Bank",
            subject = "Your LIC Select Credit Card Statement"
        )
        assertKind(
            EmailKind.STATEMENT,
            from = "AU Small Finance Bank",
            subject = "Your Vetta Credit Card Statement -May 2026 is here!"
        )
        assertKind(
            EmailKind.STATEMENT,
            from = "SBI Credit Card Statement",
            subject = "Your Paytm SBI Card SELECT Monthly Statement -May 2026"
        )
        assertKind(
            EmailKind.STATEMENT,
            from = "ICICI Bank",
            subject = "ICICI Bank Credit Card Statement for the period March 23 2026 to April 22 2026"
        )
        assertKind(
            EmailKind.STATEMENT,
            from = "YES BANK",
            subject = "Your YES_BANK_Klick Credit Card E-Statement"
        )
    }

    @Test
    fun ignoresNoisyNonLedgerEmails() {
        assertKind(
            EmailKind.PROMOTION,
            from = "AU Credit Cards",
            subject = "Limited time offer: Increase Credit Limit"
        )
        assertKind(
            EmailKind.PROMOTION,
            from = "SBI Card",
            subject = "Gift your loved ones a complimentary Add-on SBI Credit Card"
        )
        assertKind(
            EmailKind.CARD_SERVICE,
            from = "AU BANK",
            subject = "Important: Terms and conditions on your AU Credit Card"
        )
        assertKind(
            EmailKind.UNRELATED,
            from = "Cleartrip Account",
            subject = "Transaction Alert"
        )
        assertNotEquals(
            EmailKind.TRANSACTION_ALERT,
            EmailClassifier.classify(
                from = "Airtel",
                subject = "Your mobile bill is ready",
                body = "Transaction summary for this month"
            ).kind
        )
    }

    @Test
    fun infersPaymentsAndSpendForLedgerRows() {
        assertEquals(
            LedgerTransactionKind.SPEND,
            TransactionKindClassifier.infer("Debit Alert: INR 500 spent at AMAZON on your credit card")
        )
        assertEquals(
            LedgerTransactionKind.PAYMENT,
            TransactionKindClassifier.infer("Payment received INR 10000 towards your credit card")
        )
        assertEquals(
            LedgerTransactionKind.REFUND,
            TransactionKindClassifier.infer("Refund of INR 799 credited back to your credit card")
        )
    }

    private fun assertKind(expected: EmailKind, from: String, subject: String, body: String = "") {
        assertEquals(expected, EmailClassifier.classify(from = from, subject = subject, body = body).kind)
    }
}
