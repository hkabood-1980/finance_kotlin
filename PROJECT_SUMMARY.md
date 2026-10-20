# 📚 خلاصه پروژه مالی + درخواست کتاب آموزشی


کاربر می‌خواهد یک **کتاب آموزشی کامل و جز به جز** درباره‌ی:
- Android Studio
- Kotlin
- Jetpack Compose
- Room Database
- ساخت پروژه مالی واقعی

**با استفاده از کدهای همین پروژه (finance_kotlin) بعنوان مثال واقعی.**

**فرمت مورد نظر:** متن Markdown + فلوچارت Mermaid (تبدیل به PDF توسط کاربر)

## 🔗 لینک GitHub

https://github.com/hkabood-1980/finance_kotlin

## 📊 وضعیت پروژه

| بخش | وضعیت |
|-----|-------|
| ۱۰ صفحه کامل | ✅ |
| پارسر SMS (۳۰+ بانک) | ✅ |
| نسخه ۲.۸ | ✅ |
| آماده مایکت | ✅ |

## 🏗️ ساختار پروژه

app/src/main/java/com/hkaboodvand/finacemobile/
├── MainActivity.kt
├── data/
│ ├── db/
│ │ ├── entity/ (Account, Transaction, SmsQueue, SmsMapping)
│ │ ├── dao/ (AccountDao, TransactionDao, SmsDao)
│ │ └── AppDatabase.kt
│ ├── repository/FinanceRepository.kt
│ ├── prefs/AppPreferences.kt
│ └── sms/ (SmsParser, SmsReceiver, SmsSyncHelper)
└── ui/
├── navigation/AppNavigation.kt
├── theme/
└── screens/
├── HomeScreen.kt
├── AddCardScreen.kt
├── CardListScreen.kt
├── IncomeScreen.kt
├── ExpenseScreen.kt
├── TransferScreen.kt
├── SavingScreen.kt
├── ReportsScreen.kt
├── SmsScreen.kt
└── BackupScreen.kt

text

## 🛠️ تکنولوژی‌ها

- Kotlin 2.0.21
- Jetpack Compose + Material 3
- Room 2.6.1
- Navigation Compose 2.8.4
- DataStore 1.1.1
- Gson 2.11.0
- minSdk 29, targetSdk 34

## 📋 فهرست پیشنهادی کتاب (۵ جلد، ۲۵ فصل)

### 📖 جلد ۱: آشنایی با اندروید و Kotlin (۱۵۰ صفحه)
- فصل ۱: Android Studio چیست؟
- فصل ۲: نصب و راه‌اندازی
- فصل ۳: آشنایی با Gradle
- فصل ۴: زبان Kotlin از صفر
- فصل ۵: ساختار یه اپ اندروید

### 📖 جلد ۲: Jetpack Compose (۲۰۰ صفحه)
- فصل ۶: Compose چیست؟
- فصل ۷: Composableها
- فصل ۸: State و Recomposition
- فصل ۹: Layout (Row, Column, Box)
- فصل ۱۰: Material Design 3
- فصل ۱۱: Navigation
- فصل ۱۲: انیمیشن

### 📖 جلد ۳: Room Database (۱۵۰ صفحه)
- فصل ۱۳: پایگاه داده
- فصل ۱۴: Entity ها
- فصل ۱۵: DAO ها
- فصل ۱۶: Migration

### 📖 جلد ۴: پروژه مالی (۳۰۰ صفحه)
- فصل ۱۷: ساختار پروژه
- فصل ۱۸: HomeScreen قدم به قدم
- فصل ۱۹: AddCardScreen
- فصل ۲۰: IncomeScreen و ExpenseScreen
- فصل ۲۱: ReportsScreen (با نمودار)
- فصل ۲۲: BackupScreen
- فصل ۲۳: SmsParser

### 📖 جلد ۵: انتشار در مایکت (۵۰ صفحه)
- فصل ۲۴: ساخت APK
- فصل ۲۵: Keystore و آپلود

 
