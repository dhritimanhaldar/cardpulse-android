package com.cardpulse.app.data

import android.content.Context
import android.util.Log
import com.cardpulse.app.config.AppConfig
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Date
import javax.net.ssl.HttpsURLConnection

class GmailFetcher(private val context: Context) {

    private val scope = "oauth2:${AppConfig.GMAIL_SCOPE} ${AppConfig.GMAIL_LABELS_SCOPE}"

    suspend fun fetchTransactionEmails(cardLast4: List<String>): List<RawEmailData> =
        withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                    ?: return@withContext emptyList()
                val token = GoogleAuthUtil.getToken(context, account.account!!, scope)
                val lookbackMs = AppConfig.GMAIL_LOOKBACK_DAYS * 24 * 60 * 60 * 1000L
                val afterDate = (System.currentTimeMillis() - lookbackMs) / 1000
                val query = buildString {
                    append("(")
                    // Transaction-specific subject terms
                    append("subject:(\"transaction alert\" OR \"debit alert\" OR \"credit alert\" OR \"payment alert\" OR \"amount debited\" OR \"has been debited\" OR \"has been credited\" OR \"spent on\" OR \"purchase of\" OR \"payment of\" OR \"transaction on\")")
                    append(") after:$afterDate")
                }
                fetchEmailsForQuery(token, query)
            } catch (e: Exception) {
                Log.e("GmailFetcher", "Fetch error: ${e.message}")
                emptyList()
            }
        }

    suspend fun fetchStatementEmails(): List<RawEmailData> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext emptyList()
            val token = GoogleAuthUtil.getToken(context, account.account!!, scope)
            val lookbackMs = AppConfig.GMAIL_LOOKBACK_DAYS * 24 * 60 * 60 * 1000L
            val afterDate = (System.currentTimeMillis() - lookbackMs) / 1000
            val query = buildString {
                append("(")
                append("subject:(statement OR \"credit card statement\" OR e-statement OR \"bill generated\") ")
                append("OR (\"credit limit\" OR \"available credit\" OR \"total amount due\" OR \"payment due date\")")
                append(") after:$afterDate")
            }
            fetchEmailsForQuery(token, query)
        } catch (e: Exception) {
            Log.e("GmailFetcher", "Statement fetch error: ${e.message}")
            emptyList()
        }
    }

    private fun fetchEmailsForQuery(token: String, query: String): List<RawEmailData> {
        val ids = listMessageIds(token, query)
        return ids.take(AppConfig.GMAIL_FETCH_LIMIT).mapNotNull { id ->
            fetchEmailBody(token, id)?.also { email ->
                Log.d("GmailFetcher", "Email from: ${email.from} | Subject: ${email.subject}")
            }
        }
    }

    private fun listMessageIds(token: String, query: String): List<String> {
        val url = URL("https://gmail.googleapis.com/gmail/v1/users/me/messages?q=${
            java.net.URLEncoder.encode(query, "UTF-8")}&maxResults=${AppConfig.GMAIL_FETCH_LIMIT}")
        val conn = url.openConnection() as HttpsURLConnection
        conn.setRequestProperty("Authorization", "Bearer $token")
        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        val messages = json.optJSONArray("messages") ?: return emptyList()
        return (0 until messages.length()).map { messages.getJSONObject(it).getString("id") }
    }

    private fun fetchEmailBody(token: String, messageId: String): RawEmailData? {
        return try {
            val url = URL("https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId?format=full")
            val conn = url.openConnection() as HttpsURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val subject = extractHeader(json, "Subject") ?: ""
            val from = extractHeader(json, "From") ?: ""
            val dateHeader = extractHeader(json, "Date") ?: ""
            val body = extractBody(json)
            RawEmailData(messageId, subject, from, dateHeader, body)
        } catch (e: Exception) {
            Log.e("GmailFetcher", "Email fetch error for $messageId: ${e.message}")
            null
        }
    }

    private fun extractHeader(json: JSONObject, name: String): String? {
        val headers = json.optJSONObject("payload")?.optJSONArray("headers") ?: return null
        for (i in 0 until headers.length()) {
            val h = headers.getJSONObject(i)
            if (h.getString("name").equals(name, ignoreCase = true)) return h.getString("value")
        }
        return null
    }

    private fun extractBody(json: JSONObject): String {
        val payload = json.optJSONObject("payload") ?: return ""
        val directBody = payload.optJSONObject("body")?.optString("data")
        if (!directBody.isNullOrEmpty()) return stripHtml(decodeBase64(directBody))
        val parts = payload.optJSONArray("parts") ?: return ""
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.optString("mimeType") == "text/plain") {
                val data = part.optJSONObject("body")?.optString("data")
                if (!data.isNullOrEmpty()) return stripHtml(decodeBase64(data))
            }
        }
        // Fallback: try text/html part and strip tags
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.optString("mimeType") == "text/html") {
                val data = part.optJSONObject("body")?.optString("data")
                if (!data.isNullOrEmpty()) return stripHtml(decodeBase64(data))
            }
        }
        return ""
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>"), " ")
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>"), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&rsquo;|&lsquo;|&#39;"), "'")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    private fun decodeBase64(encoded: String): String {
        return try {
            String(android.util.Base64.decode(encoded.replace('-', '+').replace('_', '/'), android.util.Base64.DEFAULT))
        } catch (e: Exception) { "" }
    }
}

data class RawEmailData(
    val messageId: String,
    val subject: String,
    val from: String,
    val dateHeader: String,
    val body: String
)
