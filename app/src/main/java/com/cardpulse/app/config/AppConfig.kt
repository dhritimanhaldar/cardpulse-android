package com.cardpulse.app.config

object AppConfig {

    const val GEMINI_API_KEY = "AIzaSyBbCnCTG5B0385_E-cuAKmTTLzHKg87fWE"
    const val GEMINI_MODEL = "gemini-1.5-flash"

    const val FIREBASE_PROJECT_ID = "cardpulse-all"
    const val FIREBASE_APP_ID = "1:121179538388:android:3d1a90fafa998f8c220c41"

    const val GOOGLE_WEB_CLIENT_ID =
        "121179538388-vpdcj6rvmfqqgrmntfpsoj2ebo685evi.apps.googleusercontent.com"

    const val GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
    const val GMAIL_FETCH_LIMIT = 50
    const val GMAIL_LOOKBACK_DAYS = 30L

    const val TRANSACTION_DUPLICATE_WINDOW_MINUTES = 10L
    const val FRAUD_LARGE_AMOUNT_THRESHOLD = 10000.0
    const val FRAUD_FOREIGN_CURRENCY_FLAG = true
    const val NOTIFICATION_CHANNEL_ID = "cardpulse_alerts"
    const val NOTIFICATION_CHANNEL_NAME = "CardPulse Alerts"
}
