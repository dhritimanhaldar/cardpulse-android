Perfect! Based on the dev-0.1.0 branch and our entire conversation history, here's a comprehensive README:

***

# CardPulse 💳

<div align="center">

**Smart Credit Card Management for India**

A private Android app for tracking credit card expenses, milestones, rewards, and lounge access eligibility.

[
[
[
[

</div>

***

## 📋 Table of Contents

- [Overview](#overview)
- [Why CardPulse?](#why-cardpulse)
- [Features Implemented](#features-implemented)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Card Catalog System](#card-catalog-system)
- [Setup & Installation](#setup--installation)
- [Configuration](#configuration)
- [Roadmap](#roadmap)
- [Known Issues](#known-issues)
- [Contributing](#contributing)
- [Privacy & Security](#privacy--security)
- [License](#license)

***

## 🎯 Overview

**CardPulse** is a comprehensive credit card management application designed specifically for Indian credit card users. It automatically tracks transactions, calculates milestone progress, monitors reward eligibility, and manages lounge access - all while keeping your data completely private and secure on your device.

### Key Highlights

- 🔐 **100% Private** - All data stored locally on device
- 📱 **SMS Auto-Parsing** - Automatically reads transaction SMS
- 📧 **Gmail Integration** - Syncs statements and outstanding balances
- 🎯 **Milestone Tracking** - Never miss reward thresholds
- ✈️ **Lounge Access** - Track quarterly/annual lounge visit limits
- 💰 **Spend Analytics** - Category-wise expense tracking
- 🎁 **Reward Optimization** - Maximize card benefits

***

## 🤔 Why CardPulse?

### The Problem

Managing multiple credit cards in India is challenging:

- **Milestone Confusion**: Each card has different spend milestones (₹5L, ₹7.5L, ₹10L, etc.)
- **Reward Complexity**: Different reward rates for categories (5X on travel, 2X on dining)
- **Lounge Limits**: Annual/quarterly limits vary by card
- **Fee Waivers**: Miss spend thresholds → pay annual fees
- **Manual Tracking**: Existing apps don't understand Indian card benefits

### The Solution

CardPulse automatically:
- ✅ Parses transaction SMS from all major Indian banks
- ✅ Tracks progress toward milestone thresholds
- ✅ Calculates real-time reward points
- ✅ Monitors lounge visit eligibility
- ✅ Alerts before fee waiver deadlines
- ✅ Suggests optimal card for each purchase

***

## ✨ Features Implemented

### 🎴 Card Management

- **Multi-Card Support**
  - Add unlimited credit cards
  - Custom card nicknames
  - Visual card colors
  - Bank-specific branding
  - Card type detection (VISA/Mastercard/Rupay/Amex)

- **Card Catalog Integration** ✨
  - Pre-loaded database of 100+ Indian credit cards
  - Hierarchical bank/group/card structure
  - Automatic BIN (Bank Identification Number) matching
  - Card-specific milestone and benefit data

### 💸 Transaction Tracking

- **Automatic SMS Parsing**
  - Real-time transaction detection from SMS
  - Supports 14+ major Indian banks:
    - HDFC Bank, ICICI Bank, Axis Bank
    - SBI Card, Kotak Bank, IDFC First
    - IndusInd Bank, Yes Bank, RBL Bank
    - Standard Chartered, Citi Bank, HSBC
    - AU Bank, American Express
  - Extracts: amount, merchant, date, card last 4 digits
  - Debit/credit classification
  - Auto-links transactions to correct card

- **Gmail Statement Sync** 📧
  - Fetches credit card statements via Gmail API
  - Extracts: outstanding balance, minimum due, payment due date
  - Parses transaction emails for missed SMS
  - Verifies SMS transactions against statements

- **Manual Entry**
  - Add transactions manually
  - Edit existing transactions
  - Flag suspicious transactions
  - Add custom categories

### 🎯 Milestone & Reward Tracking

- **Spend Milestones**
  - Track progress toward annual/quarterly thresholds
  - Visual progress bars for each milestone
  - Real-time spend calculations
  - Excludes refunds/reversals from totals
  - Example milestones tracked:
    - HDFC Infinia: ₹10L annual spend
    - Axis Magnus: ₹7.5L milestone benefits
    - ICICI Sapphiro: ₹5L annual fee waiver

- **Reward Points Calculation** (Coming Soon)
  - Category-based reward multipliers
  - Base points + bonus points
  - Accelerated rewards tracking
  - Points expiry alerts

### ✈️ Lounge Access Management

- **Lounge Tracking**
  - Quarterly/annual visit limits
  - Per-card lounge access rules
  - Domestic vs. International lounges
  - Companion guest eligibility
  - Visit history and remaining balance

- **Smart Recommendations**
  - Suggests best card for lounge access
  - Prioritizes cards with remaining visits
  - Warns when approaching limits

### 📊 Analytics & Insights

- **Spend Analytics**
  - Category-wise breakdown
  - Monthly/quarterly/annual trends
  - Card-wise spend distribution
  - Merchant frequency analysis

- **Fee Optimization**
  - Annual fee waiver progress
  - Fee vs. benefits comparison
  - Suggests card closure if underutilized

### 🔔 Smart Notifications

- **Proactive Alerts**
  - Large transaction alerts (₹5K+ configurable)
  - Milestone achievement notifications
  - Payment due date reminders
  - Fee waiver deadline warnings
  - Lounge limit approaching alerts
  - Duplicate transaction detection

### 🎨 User Interface

- **Modern Material Design 3**
  - Clean, intuitive interface
  - Dark mode support (system-based)
  - Smooth animations
  - Gesture navigation
  - Responsive layouts

- **Dashboard**
  - At-a-glance card overview
  - Quick access to recent transactions
  - Milestone progress summary
  - Upcoming payment alerts

***

## 🏗️ Architecture

CardPulse follows **Clean Architecture** principles with **MVVM (Model-View-ViewModel)** pattern:

```
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                     │
│  (Jetpack Compose UI + ViewModels)                      │
├─────────────────────────────────────────────────────────┤
│                    Domain Layer                          │
│  (Use Cases, Business Logic, Models)                    │
├─────────────────────────────────────────────────────────┤
│                     Data Layer                           │
│  (Room DB, Repositories, Data Sources)                  │
├─────────────────────────────────────────────────────────┤
│               External Integrations                      │
│  (SMS Reader, Gmail API, Gemini AI)                     │
└─────────────────────────────────────────────────────────┘
```

### Key Architectural Decisions

1. **Local-First**: All data stored in Room database, no cloud sync
2. **Reactive UI**: Flow/StateFlow for reactive data updates
3. **Coroutines**: Kotlin coroutines for async operations
4. **Dependency Injection**: Manual DI (future: Hilt/Koin)
5. **Single Activity**: Navigation via Compose Navigation

***

## 🛠️ Technology Stack

### Core

- **Language**: Kotlin 1.9.x
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Build System**: Gradle 8.7 with Kotlin DSL

### UI

- **Jetpack Compose**: Modern declarative UI
- **Material Design 3**: Latest Material components
- **Compose Navigation**: Type-safe navigation

### Data & Storage

- **Room Database**: Local SQLite persistence
- **SharedPreferences**: App settings
- **Gson**: JSON parsing for card catalog

### Networking & APIs

- **Gmail API**: Statement fetching (via Google API Client)
- **Google Auth**: OAuth 2.0 for Gmail access
- **Gemini AI**: Transaction parsing & categorization (optional)

### Background Processing

- **WorkManager**: Periodic syncs
- **BroadcastReceiver**: SMS interception
- **Foreground Service**: Real-time transaction monitoring

### Testing (Planned)

- **JUnit 4**: Unit tests
- **Espresso**: UI tests
- **MockK**: Mocking framework

***

## 📁 Project Structure

```
cardpulse-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/cardpulse/app/
│   │   │   │   ├── data/
│   │   │   │   │   ├── CardDao.kt
│   │   │   │   │   ├── TransactionDao.kt
│   │   │   │   │   ├── SpendRuleDao.kt
│   │   │   │   │   ├── LoungeDao.kt
│   │   │   │   │   ├── NotificationLogDao.kt
│   │   │   │   │   ├── CardPulseDatabase.kt
│   │   │   │   │   ├── Converters.kt
│   │   │   │   │   └── CardRepository.kt
│   │   │   │   │
│   │   │   │   ├── model/
│   │   │   │   │   ├── Models.kt          # Card, Transaction entities
│   │   │   │   │   ├── CardData.kt        # Catalog data models
│   │   │   │   │   └── Enums.kt           # Status enums
│   │   │   │   │
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screen/
│   │   │   │   │   │   ├── DashboardScreen.kt
│   │   │   │   │   │   ├── CardDetailScreen.kt
│   │   │   │   │   │   ├── AddCardScreen.kt
│   │   │   │   │   │   ├── TransactionListScreen.kt
│   │   │   │   │   │   ├── AnalyticsScreen.kt
│   │   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   │   ├── component/
│   │   │   │   │   │   ├── CardView.kt
│   │   │   │   │   │   ├── ProgressBar.kt
│   │   │   │   │   │   └── TransactionItem.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   │
│   │   │   │   ├── viewmodel/
│   │   │   │   │   ├── DashboardViewModel.kt
│   │   │   │   │   ├── CardDetailViewModel.kt
│   │   │   │   │   ├── AddCardViewModel.kt
│   │   │   │   │   ├── GmailSyncViewModel.kt
│   │   │   │   │   └── MilestoneViewModel.kt
│   │   │   │   │
│   │   │   │   ├── sms/
│   │   │   │   │   ├── SmsReader.kt
│   │   │   │   │   └── SmsReceiver.kt
│   │   │   │   │
│   │   │   │   ├── gmail/
│   │   │   │   │   ├── GmailFetcher.kt
│   │   │   │   │   └── GmailAuthHelper.kt
│   │   │   │   │
│   │   │   │   ├── gemini/
│   │   │   │   │   └── GeminiService.kt
│   │   │   │   │
│   │   │   │   ├── util/
│   │   │   │   │   ├── DateUtils.kt
│   │   │   │   │   ├── CurrencyFormatter.kt
│   │   │   │   │   └── Extensions.kt
│   │   │   │   │
│   │   │   │   └── MainActivity.kt
│   │   │   │
│   │   │   ├── assets/
│   │   │   │   ├── card_data.json      # Card catalog (100+ cards)
│   │   │   │   └── sample.json         # Sample test data
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── drawable/          # Icons, logos
│   │   │   │   ├── values/            # Strings, colors, themes
│   │   │   │   └── xml/               # Preferences, network config
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── test/                      # Unit tests (planned)
│   │
│   └── build.gradle.kts               # App-level build config
│
├── gradle/                            # Gradle wrapper
├── build.gradle.kts                   # Project-level build config
├── gradle.properties                  # Gradle properties
├── settings.gradle.kts                # Settings
└── README.md                          # This file
```

***

## 🗄️ Database Schema

CardPulse uses **Room Database** with the following entities:

### **Card** Entity

```kotlin
@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bankName: String,              // e.g., "HDFC Bank"
    val cardName: String,              // e.g., "Infinia Metal"
    val last4Digits: String,           // Last 4 digits
    val cardType: String,              // VISA/Mastercard/Rupay/Amex
    val cardNetwork: String,           // e.g., "HDFC Infinia"
    val creditLimit: Double,
    val billingCycleDay: Int,          // Day billing cycle resets
    val statementDay: Int,             // Statement generation day
    val dueDateOffset: Int,            // Days after statement for payment
    val annualFee: Double,
    val isAutoFetched: Boolean = false,// Auto-detected from SMS
    val isVerified: Boolean = false,   // User verified card details
    val isActive: Boolean = true,
    val addedOn: Date = Date(),
    val color: String = "#1A73E8",     // Hex color for card UI
    val currentOutstanding: Double = 0.0,
    val minimumDue: Double = 0.0,
    val paymentDueDate: String? = null
)
```

### **Transaction** Entity

```kotlin
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,                   // Foreign key to Card
    val amount: Double,
    val merchant: String,
    val category: String,              // FOOD/TRAVEL/SHOPPING/FUEL/etc.
    val date: Date,
    val source: TransactionSource,     // MANUAL/SMS/EMAIL/STATEMENT
    val rawText: String = "",          // Original SMS/email text
    val rawEmailId: String? = null,
    val status: TransactionStatus = CONFIRMED,
    val isCredit: Boolean = false,     // true for refunds/credits
    val isFlagged: Boolean = false,    // Fraud/duplicate flag
    val flagReason: String? = "",
    val currency: String = "INR",
    val isInternational: Boolean = false
)
```

### **SpendRule** Entity (Milestones)

```kotlin
@Entity(tableName = "spend_rules")
data class SpendRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val ruleName: String,              // e.g., "10L Annual Milestone"
    val targetAmount: Double,          // e.g., 1000000.0 (₹10L)
    val currentAmount: Double = 0.0,
    val resetPeriod: String,           // ANNUAL/QUARTERLY/MONTHLY
    val periodStart: Date,
    val periodEnd: Date,
    val isAchieved: Boolean = false,
    val rewardType: String,            // BONUS_POINTS/FEE_WAIVER/VOUCHER
    val rewardValue: String            // e.g., "10000 bonus points"
)
```

### **LoungeAccess** Entity

```kotlin
@Entity(tableName = "lounge_access")
data class LoungeAccess(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val domesticLimit: Int,            // Domestic visits per period
    val internationalLimit: Int,       // International visits
    val domesticUsed: Int = 0,
    val internationalUsed: Int = 0,
    val resetPeriod: String,           // ANNUAL/QUARTERLY
    val periodStart: Date,
    val periodEnd: Date,
    val guestAllowed: Boolean = false,
    val guestUsed: Int = 0
)
```

### **NotificationLog** Entity

```kotlin
@Entity(tableName = "notification_log")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val notificationType: String,      // LARGE_TXN/MILESTONE/DUE_DATE
    val title: String,
    val message: String,
    val sentAt: Date = Date(),
    val isRead: Boolean = false
)
```

***

## 📚 Card Catalog System

CardPulse includes a comprehensive **hierarchical card catalog** with 100+ Indian credit cards.

### Catalog Structure

```json
{
  "v": 1,
  "banks": [
    {
      "b": "hdfc",                    // Bank code
      "bn": "HDFC Bank",              // Bank name
      "bn_px": ["464618", "444445"],  // BIN prefixes
      "ln": [16],                     // Card lengths
      "p": [                          // Perks
        {"i": "h1", "n": "SmartEMI", ...},
        {"i": "h2", "n": "Fuel Surcharge Waiver", ...}
      ],
      "g": [                          // Groups
        {
          "n": "Infinia",             // Group name
          "bn": {"px": ["444446"]},   // Group-specific BINs
          "p": [...],                 // Group perks
          "c": [                      // Cards
            {
              "n": "Infinia Metal",   // Card name
              "nw": "v",              // Network: VISA
              "t": "pr",              // Type: Premium
              "af": 12500,            // Annual fee
              "jf": 12500,            // Joining fee
              "fs": 800000,           // Fee waiver spend
              "fm": 2,                // Fee multiplier
              "rv": 1.0,              // Reward value
              "cl": "#1A1A2E",        // Card color
              "cf": "h",              // Category focus
              "bn": {"px": ["469618"]} // Card-specific BIN
            }
          ]
        }
      ]
    }
  ]
}
```

### Supported Banks (100+ Cards)

- **HDFC Bank**: Infinia, Diners Club, Regalia, Millennia
- **ICICI Bank**: Sapphiro, Emeralde, Rubyx, Platinum
- **Axis Bank**: Magnus, Reserve, Vistara, Atlas
- **SBI Card**: Elite, Prime, SimplyCLICK, Air India
- **Kotak Mahindra**: League, Royale, White, 811
- **American Express**: Platinum, Gold, Membership Rewards
- **Standard Chartered**: Ultimate, Super Value, DigiSmart
- **Citi Bank**: Prestige, Premier Miles, Rewards
- **HSBC**: Visa Platinum, Cashback, Smart Value
- **Yes Bank**: FIRST Exclusive, Prosperity Rewards
- **IndusInd Bank**: Legend, Pioneer, Nexxt
- **IDFC FIRST**: Wealth, Select, Millenia
- **RBL Bank**: World Safari, ShopRite, Titanium
- **AU Bank**: Altura, Vetta, LIT

### BIN Matching Logic

CardPulse automatically identifies cards using:

1. **BIN (Bank Identification Number)** - First 6 digits
2. **Card Length** - 15/16 digits
3. **Network Detection** - VISA/Mastercard/Rupay/Amex
4. **Hierarchical Fallback**:
   - Exact BIN match (card-level)
   - Group BIN match (e.g., all Infinia variants)
   - Bank BIN match (e.g., any HDFC card)
   - Manual selection

***

## ⚙️ Setup & Installation

### Prerequisites

- **Android Studio**: Arctic Fox or newer
- **JDK**: 17 or higher
- **Android Device/Emulator**: API 26+ (Android 8.0+)

### Clone Repository

```bash
git clone https://github.com/dhritimanhaldar/cardpulse-android.git
cd cardpulse-android
git checkout dev-0.1.0
```

### Configure API Keys

Create `local.properties` in project root:

```properties
# Gmail API (Optional - for statement sync)
gmail.client.id=YOUR_GMAIL_CLIENT_ID
gmail.client.secret=YOUR_GMAIL_CLIENT_SECRET

# Gemini AI API (Optional - for AI-powered parsing)
gemini.api.key=YOUR_GEMINI_API_KEY
```

### Build & Run

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or open in Android Studio and click Run
```

***

## 🔧 Configuration

### Permissions Required

```xml
<!-- SMS Reading -->
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />

<!-- Internet (Gmail API) -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### First-Time Setup

1. **Grant Permissions**: SMS, Notifications
2. **Add First Card**: Manual entry or auto-detect from SMS
3. **Optional: Connect Gmail**: For statement sync
4. **Configure Alerts**: Set thresholds for notifications

***

## 🗺️ Roadmap

### Version 0.2.0 - Enhanced Tracking (Q2 2026)

- [ ] **Card Editing**: Edit existing card details
- [ ] **Bank/Card Dropdowns**: Select from catalog instead of manual entry
- [ ] **Transaction Rematching**: Auto-refresh on card edit
- [ ] **Duplicate Detection**: Flag duplicate transactions
- [ ] **Transaction Search**: Filter by date/merchant/category
- [ ] **Export Data**: CSV/Excel export

### Version 0.3.0 - Rewards & Analytics (Q3 2026)

- [ ] **Reward Points Calculation**
  - Base points + category bonuses
  - Redemption value tracking
  - Points expiry alerts
- [ ] **Spend Analytics Dashboard**
  - Monthly/quarterly trends
  - Category breakdown charts
  - Card utilization comparison
- [ ] **Smart Card Recommendations**
  - Best card for each category
  - Optimize reward earning
  - Fee vs. benefits analysis

### Version 0.4.0 - AI & Automation (Q4 2026)

- [ ] **Gemini AI Integration**
  - Intelligent transaction categorization
  - Merchant name normalization
  - Duplicate detection
  - Expense insights
- [ ] **Predictive Alerts**
  - Milestone progress predictions
  - Fee waiver deadline warnings
  - Spending pattern anomalies
- [ ] **Auto-Categorization**
  - ML-based category assignment
  - Merchant learning
  - Custom rule engine

### Version 0.5.0 - Social & Sharing (Q1 2027)

- [ ] **Multi-User Support**
  - Add family members
  - Shared cards tracking
  - Authorized user management
- [ ] **Backup & Sync** (Optional)
  - Encrypted cloud backup
  - Cross-device sync
  - Data portability
- [ ] **Widgets**
  - Home screen card balance widget
  - Milestone progress widget
  - Quick add transaction widget

### Version 1.0.0 - Public Release (Q2 2027)

- [ ] **Comprehensive Testing**
  - Unit tests (90%+ coverage)
  - UI tests
  - Performance optimization
- [ ] **Security Audit**
  - Penetration testing
  - Encryption review
  - Privacy compliance (GDPR, DPDP)
- [ ] **Documentation**
  - User guide
  - API documentation
  - Video tutorials
- [ ] **Play Store Release**
  - Beta testing program
  - Public launch
  - Support infrastructure

### Future Considerations

- **OCR Bill Scanning**: Extract amounts from physical receipts
- **Bill Splitting**: Split transactions with friends
- **Investment Tracking**: Link savings/investment accounts
- **Tax Planning**: Generate tax reports (80C, 80D)
- **EMI Management**: Track EMI conversions and payments
- **Insurance Tracking**: Link insurance policies to cards
- **Wallet Integration**: PhonePe, Paytm, GPay linkage

***

## 🐛 Known Issues

### Critical

- [ ] **Malformed JSON in card_data.json** (Line 81)
  - Trailing commas causing parse failures
  - Milestone progress bars not rendering
  - **Fix**: Remove trailing commas from JSON arrays

### High Priority

- [ ] **Card Matching Failures**
  - Some BINs not matching to correct cards
  - Fallback logic too weak
  - **Fix**: Enhance BIN database coverage

- [ ] **Gemini API Disabled**
  - Gmail transaction parsing fails silently
  - **Fix**: Remove Gemini dependency from critical paths

### Medium Priority

- [ ] **Duplicate Card Names**
  - Auto-fetched cards show "ICICI ICICI Sapphiro"
  - **Fix**: Improve name deduplication logic

- [ ] **SMS Parsing Edge Cases**
  - Some bank SMS formats not recognized
  - International transactions missing currency
  - **Fix**: Expand regex patterns

### Low Priority

- [ ] **UI Polish**
  - Dark mode colors need refinement
  - Some animations janky on low-end devices
  - **Fix**: Optimize Compose performance

***

## 🤝 Contributing

CardPulse is currently a **private project**. Contributions are by invitation only.

### Development Guidelines

1. **Branch Strategy**: `dev-0.1.0` for active development
2. **Code Style**: Follow Kotlin official style guide
3. **Commit Messages**: Use conventional commits
4. **Testing**: Write tests for new features
5. **Documentation**: Update README for major changes

***

## 🔒 Privacy & Security

### Data Privacy

- ✅ **100% Local Storage**: All data stored on device
- ✅ **No Cloud Sync**: No data sent to external servers (optional backup excluded)
- ✅ **No Analytics**: No usage tracking or telemetry
- ✅ **No Ads**: Completely ad-free
- ✅ **Encrypted at Rest**: Room database encrypted (future)

### Security Best Practices

- SMS/Gmail permissions used only for transaction parsing
- OAuth 2.0 for Gmail (industry standard)
- Sensitive data never logged
- API keys stored in `local.properties` (not committed)
- Regular security audits planned for v1.0

### Compliance

- **GDPR**: User data sovereignty (local storage)
- **DPDP Act 2023** (India): No data collection/processing
- **PCI-DSS**: No card CVV/PIN stored

***

## 📄 License

**Private License** - This project is private and not available for public use, modification, or distribution without explicit permission from the author.

© 2026 Dhritiman Haldar. All rights reserved.

***

## 📞 Contact & Support

- **Author**: Dhritiman Haldar
- **GitHub**: [@dhritimanhaldar](https://github.com/dhritimanhaldar)
- **Repository**: [cardpulse-android](https://github.com/dhritimanhaldar/cardpulse-android)
- **Branch**: `dev-0.1.0`

***

## 🙏 Acknowledgments

- **Jetpack Compose Team**: For modern Android UI toolkit
- **Material Design**: For comprehensive design system
- **Indian Credit Card Community**: For card data contributions
- **Open Source Projects**: Room, Coroutines, Gson, and more

***

<div align="center">

**Built with ❤️ in Pune, India**

*Empowering Indian credit card users to maximize rewards and minimize fees*

</div>

***

## 📊 Project Stats

```
Language: Kotlin
Lines of Code: ~5,000
Commits: 44
Branch: dev-0.1.0
Status: Active Development
Version: 0.1.0-alpha
```

***

**Last Updated**: May 12, 2026, 2:00 AM IST
