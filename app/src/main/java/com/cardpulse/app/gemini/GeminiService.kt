package com.cardpulse.app.gemini

import android.util.Log
import com.cardpulse.app.config.AppConfig
import com.cardpulse.app.model.SpendRule
import com.cardpulse.app.model.Transaction
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import org.json.JSONObject

class GeminiService {

    private val model = GenerativeModel(
        modelName = AppConfig.GEMINI_MODEL,
        apiKey = AppConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.2f
            maxOutputTokens = 1024
        }
    )

    // ─── Extract spend rules from card T&C text ────────────────
    suspend fun extractSpendRules(cardName: String, termsText: String): String {
        val prompt = """
            You are a credit card benefits analyst.
            Given the following terms and conditions for "$cardName", extract all spend-based milestone rules.
            For each rule, return:
            - Rule name
            - Spend target amount in INR
            - Reward (points / cashback / voucher)
            - Cycle (monthly / quarterly / annual)
            
            Terms:
            $termsText
            
            Return as a clean numbered list only. No extra explanation.
        """.trimIndent()

        return try {
            model.generateContent(prompt).text ?: "No rules extracted"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    // ─── Analyse transaction for fraud signals ─────────────────
    suspend fun analyseTransaction(transaction: Transaction, recentTxns: List<Transaction>): FraudAnalysis {
        val recentSummary = recentTxns.takeLast(5).joinToString("\n") {
            "- ₹${it.amount} at ${it.merchant} on ${it.date}"
        }

        val prompt = """
            You are a fraud detection assistant for an Indian credit card app.
            
            Analyse this transaction and tell if it seems suspicious:
            Amount: ₹${transaction.amount}
            Merchant: ${transaction.merchant}
            Category: ${transaction.category}
            International: ${transaction.isInternational}
            Currency: ${transaction.currency}
            
            Recent transactions on this card:
            $recentSummary
            
            Reply in this exact format:
            VERDICT: SAFE or SUSPICIOUS
            REASON: one sentence reason
            CONFIDENCE: HIGH or MEDIUM or LOW
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt).text ?: ""
            parseFraudResponse(response)
        } catch (e: Exception) {
            FraudAnalysis(verdict = "UNKNOWN", reason = e.message ?: "Error", confidence = "LOW")
        }
    }

    // ─── Get card information and benefits summary ─────────────
    suspend fun getCardInfo(cardName: String, bankName: String): String {
        val prompt = """
            Give a brief factual summary of the "$cardName" credit card by $bankName in India.
            Include: annual fee, key rewards rate, lounge access, and top 3 benefits.
            Keep it under 100 words. Use INR values only.
        """.trimIndent()

        return try {
            model.generateContent(prompt).text ?: "No information available"
        } catch (e: Exception) {
            "Could not fetch card info: ${e.message}"
        }
    }

    // ─── Suggest spending category ─────────────────────────────
    suspend fun suggestCategory(merchant: String, amount: Double): String {
        val prompt = """
            For an Indian credit card app, what spending category best fits:
            Merchant: $merchant
            Amount: ₹$amount
            
            Choose exactly one from: FOOD, TRAVEL, SHOPPING, FUEL, ENTERTAINMENT, UTILITIES, HEALTH, EDUCATION, INSURANCE, OTHER
            Reply with the category word only.
        """.trimIndent()

        return try {
            model.generateContent(prompt).text?.trim() ?: "OTHER"
        } catch (e: Exception) {
            "OTHER"
        }
    }

    private fun parseFraudResponse(response: String): FraudAnalysis {
        val lines = response.lines()
        var verdict = "UNKNOWN"
        var reason = "Could not analyse"
        var confidence = "LOW"

        for (line in lines) {
            when {
                line.startsWith("VERDICT:") -> verdict = line.removePrefix("VERDICT:").trim()
                line.startsWith("REASON:") -> reason = line.removePrefix("REASON:").trim()
                line.startsWith("CONFIDENCE:") -> confidence = line.removePrefix("CONFIDENCE:").trim()
            }
        }
        return FraudAnalysis(verdict, reason, confidence)
    }

    // ─── Gemini parse email transaction ────────────────────────
    suspend fun parseEmailTransaction(
        subject: String,
        from: String,
        bodySnippet: String  // first 800 chars of stripped body
    ): GeminiTransactionResult {
        val prompt = """
You are a financial email parser for Indian bank transaction alerts.

Analyze this email and extract transaction details. Respond ONLY in this exact JSON format with no other text:
{
  "isTransaction": true/false,
  "amount": <number or null>,
  "merchant": "<merchant name or null>",
  "last4": "<last 4 digits of card or null>",
  "bankName": "<bank name or null>",
  "isCredit": true/false,
  "category": "<one of: Food & Dining, Shopping, Travel, Entertainment, Healthcare, Utilities, Fuel, Finance, Others>"
}

Set isTransaction=false if this is a promotional, OTP, or non-transaction email.

Email:
From: $from
Subject: $subject
Body: ${bodySnippet.take(800)}
""".trimIndent()

        return try {
            val response = model.generateContent(prompt).text ?: return emptyResult()
            val json = JSONObject(
                response.trim()
                    .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            )
            GeminiTransactionResult(
                isTransaction = json.optBoolean("isTransaction", false),
                amount = json.optDouble("amount").takeIf { !it.isNaN() },
                merchant = json.optString("merchant").takeIf { it != "null" && it.isNotBlank() },
                last4 = json.optString("last4").takeIf { it != "null" && it.length == 4 },
                bankName = json.optString("bankName").takeIf { it != "null" && it.isNotBlank() },
                isCredit = json.optBoolean("isCredit", false),
                category = json.optString("category").takeIf { it != "null" && it.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "parseEmailTransaction error: ${e.message}")
            emptyResult()
        }
    }

    private fun emptyResult() = GeminiTransactionResult(
        isTransaction = false, amount = null, merchant = null,
        last4 = null, bankName = null, isCredit = false, category = null
    )
}

data class FraudAnalysis(
    val verdict: String,        // SAFE / SUSPICIOUS / UNKNOWN
    val reason: String,
    val confidence: String      // HIGH / MEDIUM / LOW
)

data class GeminiTransactionResult(
    val amount: Double?,
    val merchant: String?,
    val last4: String?,
    val bankName: String?,
    val isCredit: Boolean,
    val category: String?,
    val isTransaction: Boolean  // false if email is not a transaction at all
)
