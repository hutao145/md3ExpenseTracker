# MD3 Expense Tracker

<div align="center">

**A Material Design 3 Android local expense tracking app**

Kotlin | Jetpack Compose | Room | MVVM

English | [简体中文](README.md)

</div>

---

## Features

### Income & Expense Management
- Record both **income** and **expense** transactions
- 23+ built-in categories (dining, transport, shopping, salary, bonus, etc.) with Material Icons
- **Custom category** names supported
- Link transactions to asset accounts with automatic balance sync
- Daily grouping with per-day subtotals

### Batch Operations
- Long-press to enter multi-select mode (with haptic feedback)
- Batch delete and batch category change

### Search & Filtering
- Real-time search by category name and note content
- Month navigation (previous / next / current)
- Custom date range filter

### Monthly Budget
- Set a monthly budget target
- Visual progress bar showing current spending ratio

### Statistics & Charts
- **Trend line chart** — smooth Bezier curves, gradient fill, 1500ms animation
- **Category donut chart** — ring chart showing expense breakdown, Top 5 legend
- Period selector: week / month / year
- Landscape trend detail view

### Asset Management
- Three asset types: **Asset** / **Liability** / **Lent Out**
- Summary card with total assets, liabilities, and lent-out amounts
- Automatic balance adjustment when transactions are linked

### Backup & Sync
- **WebDAV backup** — upload / download / delete backup files, configurable server path
- **Local CSV** — export with UTF-8 BOM (Excel-compatible), import supports legacy 4-column format
- **AutoAccounting sync** — pull bills from Alipay / WeChat via [AutoAccounting](https://github.com/AutoAccountingOrg/AutoAccounting)

### Theme System
- **6 color schemes**: Default Purple / Sakura Pink / Gulf Blue / Field Green / Autumn Yellow / Neutral Black
- **Dynamic color** (Android 12+ Material You)
- **Theme modes**: System / Light / Dark
- **AMOLED pure black mode**

### Home Screen Widget
- Built with Jetpack Glance, showing current month's total expense
- Quick-add button for fast transaction entry
- Auto-refresh every 30 minutes + instant update on data changes

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 2.0.21 |
| UI Framework | Jetpack Compose | BOM 2024.12.01 |
| Design System | Material Design 3 | via BOM |
| Architecture | MVVM | ViewModel + StateFlow |
| Local Database | Room | 2.6.1 |
| Code Generation | KSP | 2.0.21-1.0.28 |
| Lifecycle | AndroidX Lifecycle | 2.8.7 |
| Networking | OkHttp (WebDAV) | 4.12.0 |
| Widget | Jetpack Glance | 1.1.1 |
| Icons | Material Icons Extended | 1.7.6 |

---

## Project Structure

```
com.example.expensetracker
├── MainActivity                    # Entry point, navigation, widget intent handling
├── data/
│   ├── local/
│   │   ├── ExpenseEntity           # Expense/income entity
│   │   ├── AssetEntity             # Asset entity
│   │   ├── ExpenseDao              # Expense DAO
│   │   ├── AssetDao                # Asset DAO
│   │   ├── ExpenseDatabase         # Room database (v5, 4 migrations)
│   │   └── DailyExpenseSummary     # Daily summary data class
│   ├── remote/
│   │   ├── AutoAccountingService   # AutoAccounting HTTP sync
│   │   └── WebDavClient            # WebDAV client
│   └── ExpenseRepository           # Repository layer
├── ui/
│   ├── screen/
│   │   ├── ExpenseListScreen       # Home: transaction list, filters, budget
│   │   ├── StatisticsScreen        # Statistics: trend chart, donut chart
│   │   ├── AssetScreen             # Asset management
│   │   ├── SettingsScreen          # Settings: theme, test data
│   │   ├── BackupScreen            # Backup: WebDAV, CSV, AutoAccounting
│   │   └── TrendDetailActivity     # Landscape trend detail
│   ├── component/
│   │   ├── AddExpenseDialog        # Add transaction BottomSheet
│   │   ├── EditExpenseDialog       # Edit transaction BottomSheet
│   │   ├── AddAssetDialog          # Add asset dialog
│   │   └── EditAssetDialog         # Edit asset BottomSheet
│   ├── model/                      # UI data models
│   ├── viewmodel/
│   │   └── ExpenseViewModel        # Shared ViewModel
│   └── util/
│       └── CategoryIconHelper      # Category-to-icon mapping
├── widget/
│   ├── ExpenseAppWidget            # Glance widget
│   └── ExpenseAppWidgetReceiver    # Widget broadcast receiver
└── theme/
    ├── Theme.kt                    # 6 palettes + dynamic color + AMOLED
    ├── Color.kt                    # 247 color constants
    └── Type.kt                     # MD3 typography scale
```

---

## Build & Run

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

## AutoAccounting Integration

This app supports syncing with [AutoAccounting](https://github.com/AutoAccountingOrg/AutoAccounting) for automated bill tracking.

**Steps:**
1. Install and start AutoAccounting on your device, enable its LSPosed module to hook Alipay / WeChat
2. Open this app, go to the Backup screen and tap the sync button
3. The app will fetch unsynced bills via `http://127.0.0.1:52045`

> **Note**: Do **not** add this expense tracker app to AutoAccounting's LSPosed scope — that will cause crashes. AutoAccounting only needs to hook payment apps like Alipay and WeChat.

---

## License

This project is for personal learning purposes only.
