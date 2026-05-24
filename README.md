# CardPulse

**Version:** 0.1.0  
**Branch:** `dev-0.1.0`  
**Package:** `com.cardpulse.app`  
**Min SDK:** 26 (Android 8.0)  
**Target SDK:** 35

## 📱 Overview

CardPulse is a private Android application designed to intelligently track credit card expenses, milestones, rewards, and lounge eligibility. The app automatically detects your credit cards from Gmail statements and SMS alerts, parses transactions, and tracks your progress toward card-specific rewards and benefits.

### Key Features

- 🎯 **Milestone & Reward Tracking**: Track spending milestones for each card with real-time progress
- 💳 **Auto Card Detection**: Automatically detect credit cards from Gmail statements
- 📊 **Smart Transaction Parsing**: Extract transactions from SMS and Gmail with intelligent categorization
- 🏦 **Multi-Bank Support**: Comprehensive support for major Indian banks (HDFC, ICICI, SBI, Axis, AU Bank, Yes Bank, etc.)
- ✈️ **Lounge Access Tracking**: Track lounge visit eligibility and quarterly limits
- 🎁 **Perk Management**: Monitor card-specific perks, benefits, and their validity periods
- 🔔 **Smart Notifications**: Get alerts for unverified cards, payment due dates, and milestone achievements
- 🎨 **Modern UI**: Clean Material Design 3 interface with dark theme support
- 🔐 **Privacy First**: All data stays on device, no external servers

***

## 🏗️ Architecture

### Tech Stack

**Core**
- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Manual DI (ViewModelFactory)

**Data Layer**
- **Local Database**: Room SQLite
- **Preferences**: SharedPreferences
- **JSON Parsing**: Gson

**Authentication & Cloud**
- **Authentication**: Firebase Auth (Google Sign-In)
- **Gmail Integration**: Gmail API v1
- **Google Services**: Play Services Auth

**Background Processing**
- **Coroutines**: kotlinx-coroutines-android
- **Flow**: StateFlow for reactive UI
- **WorkManager**: Periodic SMS/Gmail sync

**Permissions**
- `READ_SMS` - Transaction parsing from bank SMS
- `INTERNET` - Gmail API access
- `ACCESS_NETWORK_STATE` - Network connectivity
- Gmail OAuth scopes: `gmail.readonly`, `gmail.labels`

***

## 📁 Project Structure

```
app/
├── src/main/
│   ├── java/com/cardpulse/app/
│   │   ├── data/                    # Data layer
│   │   │   ├── CardCatalogLoader.kt       # Loads card catalog from JSON
│   │   │   ├── CardRepository.kt          # Card data operations
│   │   │   ├── TransactionRepository.kt   # Transaction data operations
│   │   │   ├── CardPulseDatabase.kt       # Room database
│   │   │   ├── CardDao.kt                 # Card database access
│   │   │   ├── TransactionDao.kt          # Transaction database access
│   │   │   ├── SmsTransactionParser.kt    # SMS parsing logic
│   │   │   ├── GmailStatementParser.kt    # Gmail email parsing
│   │   │   └── MilestoneCalculator.kt     # Milestone progress calculation
│   │   │
│   │   ├── model/                   # Data models
│   │   │   ├── Models.kt                  # Card, Transaction entities
│   │   │   ├── Notification.kt            # Notification model
│   │   │   └── Milestone.kt               # Milestone/Perk model
│   │   │
│   │   ├── ui/                      # UI layer
│   │   │   ├── screen/
│   │   │   │   ├── DashboardScreen.kt     # Main card list screen
│   │   │   │   ├── CardDetailScreen.kt    # Card detail with milestones
│   │   │   │   ├── AddCardScreen.kt       # Add/Edit card form
│   │   │   │   ├── SignInHubActivity.kt   # Gmail authentication
│   │   │   │   ├── LoadingScreen.kt       # Dynamic loading with steps
│   │   │   │   └── NotificationsSheet.kt  # Notifications drawer
│   │   │   │
│   │   │   ├── components/
│   │   │   │   ├── AppDrawer.kt           # Side navigation drawer
│   │   │   │   ├── CardItem.kt            # Card list item
│   │   │   │   ├── MilestoneCard.kt       # Milestone progress card
│   │   │   │   └── FilterChip.kt          # Active filter display
│   │   │   │
│   │   │   └── theme/
│   │   │       ├── Color.kt               # Color definitions
│   │   │       ├── Theme.kt               # Material theme setup
│   │   │       └── Type.kt                # Typography definitions
│   │   │
│   │   ├── viewmodel/               # ViewModels
│   │   │   ├── DashboardViewModel.kt      # Dashboard state & logic
│   │   │   ├── CardDetailViewModel.kt     # Card detail state
│   │   │   ├── AuthViewModel.kt           # Authentication state
│   │   │   └── AddCardViewModel.kt        # Add/Edit card state
│   │   │
│   │   └── MainActivity.kt          # Single activity app
│   │
│   ├── assets/
│   │   └── card_data.json           # Card catalog (banks, variants, perks)
│   │
│   ├── res/
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   ├── colors.xml
│   │   │   └── themes.xml
│   │   └── drawable/                # Icons and images
│   │
│   └── AndroidManifest.xml
│
├── google-services.json             # Firebase configuration
└── build.gradle.kts                 # App-level Gradle config
```

***

## 💾 Database Schema

### Card Entity
```kotlin
@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val bankName: String,              // e.g., "HDFC", "ICICI"
    val cardName: String,              // e.g., "Infinia", "Sapphiro"
    val last4Digits: String,           // Last 4 digits for identification
    val cardType: String,              // "Credit" or "Debit"
    val cardNetwork: String,           // "Visa", "Mastercard", "RuPay", "Amex"
    val creditLimit: Double,           // Total credit limit
    val billingCycleDay: Int,          // Day of month (1-31)
    val statementDay: Int,             // Statement generation day
    val dueDateOffset: Int,            // Days after statement
    val annualFee: Double,             // Annual fee amount
    val isAutoFetched: Boolean,        // True if auto-detected from Gmail
    val isVerified: Boolean,           // True if user verified details
    val isActive: Boolean,             // False if card closed/inactive
    val addedOn: Long,                 // Timestamp of card addition
    val color: String,                 // Hex color for card visual
    val currentOutstanding: Double?,   // Current outstanding amount
    val minimumDue: Double?,           // Minimum due amount
    val paymentDueDate: String?        // Due date string
)
```

### Transaction Entity
```kotlin
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val cardId: Int,                   // Foreign key to Card
    val amount: Double,                // Transaction amount
    val merchant: String,              // Merchant name
    val category: String,              // "Dining", "Shopping", "Travel", etc.
    val timestamp: Long,               // Transaction timestamp
    val description: String,           // Full SMS/Email text
    val source: String,                // "SMS" or "Gmail"
    val isVerified: Boolean            // User confirmed accuracy
)
```

***

## 🎯 Card Catalog JSON Structure

The app uses `card_data.json` to store comprehensive card information:

```json
{
  "v": "1",
  "banks": [
    {
      "b": "HDFC",
      "bn": {"px": ["512345"], "ln": [16]},
      "g": [
        {
          "n": "Infinia",
          "bn": {"px": ["512347"], "ln": [16]},
          "p": [
            {
              "i": "m1",
              "n": "Travel Voucher",
              "rt": "v",
              "cy": "a",
              "mn": 800000
            }
          ],
          "c": [
            {
              "n": "Travel Rewards",
              "rt": "p",
              "cy": "m",
              "up": {"t": "rw", "v": "5%"}
            }
          ]
        }
      ]
    }
  ]
}
```

**Field Abbreviations:**
- `rt`: Reward Type (`p`=Points, `c`=Cashback, `l`=Lounge, `v`=Voucher, `f`=Fuel, `w`=Waiver, `i`=Insurance, `mi`=Miles)
- `cy`: Cycle (`m`=Monthly, `q`=Quarterly, `a`=Annual, `o`=One-time)
- `mn`: Milestone Amount (spend target)
- `vp`: Visits Per cycle (lounge visits)
- `up`: Upgrade benefits

***

## 🔧 Key Features Explained

### 1. Auto Card Detection
- Scans Gmail for credit card statements from last 6 months
- Extracts: Bank name, Card variant, Last 4 digits, Credit limit, Outstanding balance
- Creates unverified cards that user can review and confirm
- Matches cards against catalog to load milestones automatically

### 2. Transaction Parsing

**SMS Parsing:**
- Monitors SMS from bank short codes
- Extracts: Amount, Merchant, Card last 4 digits, Date
- Auto-categorizes based on merchant keywords
- Handles multiple Indian bank SMS formats

**Gmail Parsing:**
- Reads transaction alert emails
- Extracts outstanding balance, due dates
- Parses statement attachments (future feature)

**Current Issues (to be fixed):**
- Card payment SMS incorrectly treated as expenses
- Many "Unknown" merchants
- Duplicate transactions
- Zero-amount transactions saved

### 3. Milestone Tracking

**Types:**
- **Spend Milestones**: Reach ₹X spending for reward
- **Category Perks**: Enhanced rewards on specific categories
- **Membership Benefits**: Lounge access, insurance, etc.

**Progress Calculation:**
- Tracks relevant transactions per milestone
- Respects milestone validity periods (monthly/quarterly/annual)
- Shows progress bar with current vs target
- Lists transactions contributing to each milestone

### 4. Smart Filtering & Notifications

**Filters:**
- By verification status (unverified cards)
- By bank
- By card type/network
- Payment due soon
- Auto-detected vs manual cards

**Notifications:**
- Unverified auto-detected cards
- Payment due within 7 days
- Milestone achievements
- Outstanding balance alerts

### 5. Side Drawer Navigation

**Sections:**
- **Profile**: User info, Gmail account
- **Notifications**: Clickable notifications that apply filters
- **Filters**: Checkbox-based multi-select filters
- **Settings**: Sign out option

**Access:**
- Hamburger menu (☰) in top bar
- Swipe from left edge (gesture enabled)
- Notification/Filter icons

***

## 🚀 Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 35
- Firebase account
- Google Cloud Console project

### Firebase Setup

1. **Create Firebase Project:**
  - Go to [Firebase Console](https://console.firebase.google.com)
  - Create new project: `cardpulse-all`
  - Enable Google Analytics (optional)

2. **Add Android App:**
  - Package name: `com.cardpulse.app`
  - SHA-1: Generate using `./gradlew signingReport`
  - Download `google-services.json` to `app/` folder

3. **Enable Authentication:**
  - Firebase Console → Authentication → Sign-in method
  - Enable **Google** provider
  - Configure OAuth consent screen

4. **Gmail API Setup:**
  - Go to [Google Cloud Console](https://console.cloud.google.com)
  - Enable **Gmail API**
  - Configure OAuth 2.0 scopes:
    - `https://www.googleapis.com/auth/gmail.readonly`
    - `https://www.googleapis.com/auth/gmail.labels`

### Build & Run

```bash
# Clone repository
git clone https://github.com/dhritimanhaldar/cardpulse-android.git
cd cardpulse-android

# Checkout dev branch
git checkout dev-0.1.0

# Add google-services.json (provided separately)
# Place in app/ directory

# Build project
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Or open in Android Studio and run
```

***

## 🔑 Required Permissions

**Runtime Permissions:**
- **READ_SMS**: Required for parsing bank transaction SMS
- **INTERNET**: Gmail API and Firebase Auth

**OAuth Permissions:**
- **Gmail readonly**: Read statements and transaction emails
- **Gmail labels**: Organize emails (future feature)

**Permissions Requested At:**
- Gmail login: During first authentication
- SMS: On app first launch or when accessing dashboard

***

## 📊 Current Limitations (v0.1.0)

### Known Issues
1. **Transaction Parsing:**
  - Card payments incorrectly shown as expenses
  - High number of "Unknown" merchants
  - Duplicate transactions appearing
  - Zero-amount transactions not filtered
  - Poor merchant name extraction from SMS

2. **Gmail Integration:**
  - No HTML table parsing for statements
  - Limited email type detection
  - No attachment parsing

3. **UI/UX:**
  - Loading screen needs better state management
  - No transaction editing/deletion
  - No bulk operations
  - Limited error handling display

4. **Features:**
  - No export to CSV/Excel
  - No spending analytics/charts
  - No budget tracking
  - No bill reminders
  - No multi-user support

### Planned Improvements (v0.2.0)
- [ ] Enhanced transaction classifier (exclude payments)
- [ ] Bank-specific SMS pattern registry
- [ ] Intelligent merchant normalization
- [ ] Deduplication system
- [ ] Two-phase transaction processing
- [ ] Quality scoring for transactions
- [ ] User correction learning system
- [ ] HTML email parsing
- [ ] Spending analytics dashboard
- [ ] Export functionality

***

## 🧪 Testing

### Manual Testing Checklist

**Authentication:**
- [ ] Gmail sign-in works
- [ ] Sign-out clears session
- [ ] Loading screen shows 4 steps
- [ ] Dashboard loads after auth

**Card Management:**
- [ ] Auto-detect cards from Gmail
- [ ] Manual card addition
- [ ] Card editing updates immediately
- [ ] Card color selection works
- [ ] Bank dropdown shows no duplicates

**Transaction Parsing:**
- [ ] SMS transactions parsed
- [ ] Gmail transactions parsed
- [ ] Transactions appear in list
- [ ] Categories assigned correctly
- [ ] No zero-amount transactions

**Milestones:**
- [ ] Milestones load from catalog
- [ ] Progress bars show correctly
- [ ] Transactions grouped per milestone
- [ ] Validity periods decoded (no "p/l/i" bugs)
- [ ] "Milestone" and "Rewards" labels correct

**Navigation:**
- [ ] Side drawer opens with hamburger menu
- [ ] Swipe from left opens drawer
- [ ] Filters apply correctly
- [ ] Notifications clickable
- [ ] Filter chip shows active filters
- [ ] Clear filter works

**UI:**
- [ ] Card visual always visible
- [ ] No constant "CardPulse" banner
- [ ] Verification banner shows for auto-cards
- [ ] Edit button works
- [ ] Dark theme supported

***

## 🤝 Contributing

This is a **private repository** for personal use. However, the architecture and patterns can be referenced for similar projects.

### Code Style
- **Language**: Kotlin with idiomatic conventions
- **Formatting**: ktlint compatible
- **Architecture**: MVVM + Repository pattern
- **Compose**: Declarative UI with state hoisting
- **Coroutines**: Structured concurrency with proper scope management

### Git Workflow
- **main**: Production-ready releases
- **dev-0.1.0**: Active development branch
- **feature/**: Feature-specific branches
- **bugfix/**: Bug fix branches

***

## 📄 License

**Private Project** - All rights reserved.  
Not licensed for public use or distribution.

***

## 👨‍💻 Developer

**Dhritiman Haldar**  
📍 Pune, Maharashtra, India  
📧 [Your Email]  
🔗 [GitHub](https://github.com/dhritimanhaldar)

***

## 📝 Version History

### v0.1.0 (Current - dev-0.1.0) - May 2026
**Features:**
- ✅ Gmail authentication with Google Sign-In
- ✅ Auto card detection from Gmail statements
- ✅ SMS transaction parsing (basic)
- ✅ Manual card management (add/edit/view)
- ✅ Card catalog with 50+ card variants
- ✅ Milestone tracking with progress bars
- ✅ Side drawer navigation
- ✅ Notifications system
- ✅ Multi-filter support
- ✅ Dynamic loading screen
- ✅ Material 3 UI with dark theme

**Known Issues:**
- ⚠️ Card payments shown as expenses
- ⚠️ Many "Unknown" merchants
- ⚠️ Transaction duplicates
- ⚠️ Poor merchant name extraction

### v0.0.1 (Initial) - March 2026
- Basic project setup
- Firebase integration
- Room database schema

***

## 🔮 Roadmap

### v0.2.0 - Transaction Intelligence (June 2026)
- Enhanced SMS/Gmail parser
- Transaction type classification
- Deduplication engine
- Merchant normalization
- Bank-specific patterns
- Quality scoring

### v0.3.0 - Analytics & Insights (July 2026)
- Spending analytics dashboard
- Category-wise breakdown
- Monthly trends
- Milestone achievement history
- Custom date ranges

### v0.4.0 - Smart Features (August 2026)
- Budget tracking
- Bill reminders
- Due date notifications
- Reward optimization suggestions
- Card recommendation engine

### v1.0.0 - Production Release (September 2026)
- CSV/Excel export
- Multiple account support
- Backup & restore
- Widget support
- Play Store release

***

## ❓ FAQ

**Q: Is my data secure?**  
A: Yes. All data is stored locally on your device. Gmail access is read-only and used only for fetching statements. No data is sent to external servers.

**Q: Which banks are supported?**  
A: Major Indian banks including HDFC, ICICI, SBI, Axis, AU Bank, Yes Bank, Kotak, IndusInd, HSBC, Standard Chartered, and more. See `card_data.json` for full list.

**Q: Why are card payments showing as transactions?**  
A: This is a known bug in v0.1.0. The parser doesn't yet distinguish between spending and payments. This will be fixed in v0.2.0 with transaction type classification.

**Q: Can I edit auto-detected cards?**  
A: Yes. Tap the Edit button on the card detail screen to modify any field. This also marks the card as verified.

**Q: Why do I see duplicate transactions?**  
A: The current parser doesn't deduplicate across SMS and email sources. The deduplication system will be implemented in v0.2.0.

**Q: How are milestones calculated?**  
A: Milestones are loaded from the card catalog JSON. Progress is calculated by summing relevant transactions (by category, validity period) and comparing to the milestone target.

**Q: Can I add cards not in the catalog?**  
A: Yes. You can add any card manually. However, milestone tracking requires the card to be in the catalog. Future versions will support custom milestones.

**Q: Why isn't Gmail detecting my cards?**  
A: Ensure:
- Gmail API is enabled in Google Cloud Console
- OAuth consent configured correctly
- Correct scopes granted (`gmail.readonly`)
- Statements are in Gmail inbox (not archived)
- Bank sends statements via email

**Q: How do I report bugs?**  
A: This is a private project. For the developer's reference, use logcat output and screenshot the issue.

***

## 📚 Additional Documentation

- [Card Catalog Format](docs/CARD_CATALOG.md) _(to be created)_
- [SMS Parsing Patterns](docs/SMS_PATTERNS.md) _(to be created)_
- [Database Schema](docs/DATABASE.md) _(to be created)_
- [API Documentation](docs/API.md) _(to be created)_

***

## 🙏 Acknowledgments

- **Material Design 3** for UI components
- **Firebase** for authentication
- **Gmail API** for email access
- **Jetpack Compose** for modern Android UI
- **Room** for local database persistence

***

**Last Updated:** May 15, 2026  
**Branch:** dev-0.1.0  
**Status:** Active Development 🚧