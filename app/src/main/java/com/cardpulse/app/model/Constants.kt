package com.cardpulse.app.model

object CardTypes {
    const val ENTRY_LEVEL = "en"
    const val PREMIUM = "pr"
    const val PLATINUM = "p"
    const val SUPER_PREMIUM = "sp"
    const val CASHBACK = "cb"
    const val LIFETIME_FREE = "lt"
    const val CORPORATE = "co"
    const val SECURED = "se"
    const val FINTECH = "ft"  // NEW in v2
    
    val ALL_TYPES = listOf(ENTRY_LEVEL, PREMIUM, PLATINUM, SUPER_PREMIUM, 
                           CASHBACK, LIFETIME_FREE, CORPORATE, SECURED, FINTECH)
}

object CardNetworks {
    const val VISA = "v"
    const val MASTERCARD = "mc"
    const val RUPAY = "rp"
    const val AMEX = "amex"
    const val DINERS_CLUB = "dc"
    
    val ALL_NETWORKS = listOf(VISA, MASTERCARD, RUPAY, AMEX, DINERS_CLUB)
}

object RewardTypes {
    const val POINTS = "p"
    const val CASHBACK = "c"
    const val MILES = "mi"
    const val VOUCHER = "v"
    const val WAIVER = "w"
    const val LOUNGE = "l"
    const val FEE = "f"
    const val INSURANCE = "i"
    const val GOLF = "g"
    const val CONCIERGE = "co"
    const val SERVICE = "s"      // NEW in v2
    const val DISCOUNT = "d"     // NEW in v2
    const val BOGO = "b"         // NEW in v2
    
    val ALL_TYPES = listOf(POINTS, CASHBACK, MILES, VOUCHER, WAIVER, LOUNGE,
                           FEE, INSURANCE, GOLF, CONCIERGE, SERVICE, DISCOUNT, BOGO)
}

object CycleTypes {
    const val ONE_TIME = "o"
    const val MONTHLY = "m"
    const val QUARTERLY = "q"
    const val ANNUAL = "a"
    
    val ALL_TYPES = listOf(ONE_TIME, MONTHLY, QUARTERLY, ANNUAL)
}

object Caveats {
    const val C1 = "c1"   // No rewards on fuel spends
    const val C2 = "c2"   // No rewards on wallet loads
    const val C3 = "c3"   // No rewards on rent payments
    const val C4 = "c4"   // No rewards on government or utility payments
    const val C5 = "c5"   // Unlimited lounge access
    const val C6 = "c6"   // Domestic lounge access only
    const val C7 = "c7"   // Reward redemption attracts GST
    const val C8 = "c8"   // Foreign markup applies
    const val C9 = "c9"   // Priority Pass included
    const val C10 = "c10" // Reward points expire after a fixed period
    const val C11 = "c11" // Fees vary by variant
    const val C12 = "c12" // Excludes insurance premium spends
    const val C13 = "c13" // Excludes educational payments
    const val C14 = "c14" // Excludes EMI transactions
    const val C15 = "c15" // Excludes cash-equivalent spends
    const val C16 = "c16" // NEW: Reward capped per statement cycle
    const val C17 = "c17" // NEW: Co-brand merchant exclusive
    const val C18 = "c18" // NEW: Network exclusive benefit
    const val C19 = "c19" // NEW: High income criteria applies
    const val C20 = "c20" // NEW: Card discontinued for new applicants
    const val C21 = "c21" // NEW: UPI transactions earn rewards
    const val C22 = "c22" // NEW: Prepaid/credit-line hybrid product
    const val C23 = "c23" // NEW: Spa/wellness benefits available
    const val C24 = "c24" // NEW: BOGO offers via specific partners
    const val C25 = "c25" // NEW: Issued via NBFC / partner-bank
    
    val CAVEAT_DESCRIPTIONS = mapOf(
        C1 to "No rewards on fuel spends",
        C2 to "No rewards on wallet loads",
        C3 to "No rewards on rent payments",
        C4 to "No rewards on government or utility payments",
        C5 to "Unlimited lounge access",
        C6 to "Domestic lounge access only",
        C7 to "Reward redemption attracts GST",
        C8 to "Foreign markup applies",
        C9 to "Priority Pass included",
        C10 to "Reward points expire after a fixed period",
        C11 to "Fees vary by variant",
        C12 to "Excludes insurance premium spends",
        C13 to "Excludes educational payments",
        C14 to "Excludes EMI transactions",
        C15 to "Excludes cash-equivalent spends",
        C16 to "Reward capped per statement cycle",
        C17 to "Co-brand merchant exclusive (e.g., Swiggy/Flipkart only)",
        C18 to "Network exclusive benefit (Visa/MC-only)",
        C19 to "High income criteria applies",
        C20 to "Card discontinued for new applicants",
        C21 to "UPI transactions earn rewards (RuPay-on-UPI cards)",
        C22 to "Prepaid/credit-line hybrid product",
        C23 to "Spa/wellness benefits available",
        C24 to "BOGO offers via specific partners",
        C25 to "Issued via NBFC / partner-bank"
    )
}

object ConfidenceLevels {
    const val HIGH = "h"
    const val MEDIUM = "m"
    const val LOW = "l"
}
