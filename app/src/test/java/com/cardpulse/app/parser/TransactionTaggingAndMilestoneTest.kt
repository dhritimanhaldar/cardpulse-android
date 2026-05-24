package com.cardpulse.app.parser

import com.cardpulse.app.data.TransactionDedupe
import com.cardpulse.app.model.Milestone
import com.cardpulse.app.model.Perk
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import com.cardpulse.app.model.UptoLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionTaggingAndMilestoneTest {

    @Test
    fun smsParserBuildsTaggedSpend() {
        val result = SmsTransactionParser.parse(
            smsBody = "INR 500 spent at HP Petrol Pump on card XX1234",
            sender = "VM-ICICIB"
        )

        requireNotNull(result)
        assertEquals(LedgerTransactionKind.SPEND, result.transactionKind)
        assertTrue(result.tags.contains("fuel"))
        assertTrue(result.tags.contains("offline"))
    }

    @Test
    fun inferUsefulTagsFromMerchantAndText() {
        assertTrue(TransactionTagger.infer("spent at petrol pump POS", "HP Petrol", LedgerTransactionKind.SPEND).tags.containsAll(listOf("fuel", "offline")))
        assertTrue(TransactionTagger.infer("online order", "Swiggy", LedgerTransactionKind.SPEND).tags.containsAll(listOf("dining", "online")))
        assertTrue(TransactionTagger.infer("electricity bill paid", "BESCOM", LedgerTransactionKind.SPEND).tags.contains("utilities"))
        assertTrue(TransactionTagger.infer("premium paid", "LIC", LedgerTransactionKind.SPEND).tags.contains("insurance"))
    }

    @Test
    fun dedupesSmsAndEmailForSameLogicalTransaction() {
        val sms = txn(source = TransactionSource.SMS, rawId = null, merchant = "HP Petrol Pump", date = 1_000_000)
        val email = txn(source = TransactionSource.GMAIL, rawId = "email-1", merchant = "HP PETROL", date = 1_000_000 + 120_000)

        assertTrue(TransactionDedupe.isSameLogicalTransaction(sms, email))
        assertEquals(sms, TransactionDedupe.richerOf(sms, email))
    }

    @Test
    fun percentageRewardUsesSpendDenominator() {
        val fivePercent = Perk(
            i = "p1",
            n = "5% cashback up to Rs 100",
            rt = "c",
            cy = "m",
            up = UptoLimit(t = "rw", v = 100)
        )
        val twoPercent = Perk(
            i = "p2",
            n = "2% cashback up to Rs 500",
            rt = "c",
            cy = "m",
            up = UptoLimit(t = "rw", v = 500)
        )

        assertEquals(2000.0, MilestoneProgressCalculator.computePerkDenominator(fivePercent), 0.01)
        assertEquals(25000.0, MilestoneProgressCalculator.computePerkDenominator(twoPercent), 0.01)
    }

    @Test
    fun minimumTransactionThresholdAndRefundAffectProgress() {
        val perk = Perk(
            i = "fuel",
            n = "Fuel rewards 5% up to Rs 100",
            rt = "c",
            cy = "m",
            mn = 50,
            up = UptoLimit(t = "rw", v = 100)
        )
        val spend49 = txn(amount = 49.0, merchant = "HP Petrol", tags = "fuel,offline")
        val spend100 = txn(amount = 100.0, merchant = "HP Petrol", tags = "fuel,offline")
        val refund30 = txn(amount = 30.0, merchant = "HP Petrol", tags = "fuel,offline", kind = LedgerTransactionKind.REFUND)

        val progress = MilestoneProgressCalculator.computeForPerk(
            perk = perk,
            transactions = listOf(spend49, spend100, refund30),
            cycleStartMillis = 0
        )

        assertEquals(70.0, progress.currentAmount, 0.01)
    }

    @Test
    fun simpleSpendMilestoneUsesThresholdDenominator() {
        val milestone = Milestone(
            i = "lounge",
            n = "Lounge access",
            ta = 20000,
            rw = "Lounge on Rs 20,000 spend",
            rt = "l",
            cy = "m"
        )

        val progress = MilestoneProgressCalculator.computeForMilestone(
            milestone = milestone,
            transactions = listOf(txn(amount = 5000.0, merchant = "Amazon")),
            cycleStartMillis = 0
        )

        assertEquals(20000.0, progress.denominator, 0.01)
        assertEquals(0.25f, progress.progress, 0.01f)
    }

    private fun txn(
        amount: Double = 100.0,
        source: TransactionSource = TransactionSource.SMS,
        rawId: String? = null,
        merchant: String = "Amazon",
        date: Long = 1_000_000,
        tags: String = "",
        kind: LedgerTransactionKind = LedgerTransactionKind.SPEND
    ): Transaction {
        return Transaction(
            id = 1,
            cardId = 10,
            amount = amount,
            merchant = merchant,
            category = "Others",
            date = date,
            source = source,
            rawText = merchant,
            rawEmailId = rawId,
            status = TransactionStatus.CONFIRMED,
            isCredit = TransactionKindClassifier.isCreditLike(kind),
            transactionKind = kind.name,
            tags = tags
        )
    }
}
