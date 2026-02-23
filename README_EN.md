# 📒 MD3 Expense Tracker

<div align="right">
  <a href="README.md">简体中文</a> | <strong>English</strong>
</div>

A modern Android expense tracking app built on **Material Design 3**, featuring clean UI, local-first storage, and seamless integration with [AutoAccounting](https://github.com/AutoAccountingOrg/AutoAccounting) for automated bill syncing.

---

## ✨ Features

| Feature | Description |
|---|---|
| 💰 Income & Expense | Log transactions with amount, category, and notes |
| 📅 Daily Grouping | Transactions grouped by date with daily totals |
| 🏷️ Categories | Built-in categories: dining, transport, shopping and more |
| 📊 Statistics | Home card showing balance, monthly income/expense and category breakdown |
| 📆 Date Range Filter | Filter transactions by custom date range |
| 🔍 Search | Search by category or notes keywords |
| 💼 Monthly Budget | Set a monthly budget with a visual progress bar |
| ✅ Batch Operations | Long-press to enter selection mode, supports batch delete |
| 📤 CSV Export | Export data to CSV for analysis in Excel |
| 🔄 AutoAccounting Sync | Pull bills from Alipay / WeChat via the AutoAccounting module |

---

## 🔄 AutoAccounting Integration

This app supports syncing with [AutoAccounting](https://github.com/AutoAccountingOrg/AutoAccounting) via its local HTTP server (`http://127.0.0.1:52045`).

**Steps:**

1. Install and start AutoAccounting on your device, enable its LSPosed module to hook Alipay / WeChat
2. Open this app and tap the **🔄 sync button** next to the total balance on the home screen
3. The app will fetch unsynced bills and save them to the local database, showing a Snackbar with the result

> ⚠️ **Note**: Do **not** add this expense tracker app to AutoAccounting's LSPosed scope — that will cause crashes. AutoAccounting only needs to hook payment apps like Alipay and WeChat.

---

## 🛠️ Tech Stack

| Layer | Tech |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design System | Material Design 3 |
| Local Storage | Room |
| Architecture | MVVM (ViewModel + StateFlow) |
| Networking | HttpURLConnection (no extra dependencies) |

---

## 📁 Project Structure

```
com.example.expensetracker
├── MainActivity
├── data
│   ├── local
│   │   ├── ExpenseEntity
│   │   ├── ExpenseDao
│   │   └── ExpenseDatabase
│   ├── remote
│   │   └── AutoAccountingService
│   └── ExpenseRepository
├── ui
│   ├── screen
│   │   ├── ExpenseListScreen
│   │   └── StatisticsScreen
│   ├── component
│   ├── model
│   └── viewmodel
│       └── ExpenseViewModel
└── theme
```

---

## 🚀 Build & Run

**Requirements:**
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35, min API 26 (Android 8.0)

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

---

## 📦 Dependencies

| Library | Version |
|---|---|
| Kotlin | 2.0.21 |
| Compose BOM | 2024.12.01 |
| Material 3 | via BOM |
| AndroidX Room | 2.6.1 |
| Lifecycle / ViewModel | 2.8.7 |
| KSP | 2.0.21-1.0.28 |

---

## 📄 License

This project is for personal learning purposes.
