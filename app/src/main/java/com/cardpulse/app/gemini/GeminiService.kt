package com.cardpulse.app.gemini

import com.cardpulse.app.config.AppConfig
import com.cardpulse.app.model.SpendRule
import com.cardpulse.app.model.Transaction
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig

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
}

data class FraudAnalysis(
    val verdict: String,        // SAFE / SUSPICIOUS / UNKNOWN
    val reason: String,
    val confidence: String      // HIGH / MEDIUM / LOW
)
