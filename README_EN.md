# 💰 MD3 Expense Tracker

<div align="center">

✨ **A sleek Material Design 3 Android local expense tracking app** ✨

`Kotlin` · `Jetpack Compose` · `Room` · `MVVM`

🇬🇧 English | [🇨🇳 简体中文](README.md)

---

🍜 Dining · 🚗 Transport · 🛍️ Shopping · 💵 Income · 📊 Stats · ☁️ Backup

</div>

---

## 🌟 Feature Highlights

### 💸 Income & Expense — Track every penny effortlessly

- ➕ Record both **income** and **expense**, switch with a single tap
- 🏷️ **23+ built-in categories** (🍜 Dining, 🚗 Transport, 🛍️ Shopping, 💰 Salary, 🎁 Bonus...) each with a carefully matched Material Icon
- ✏️ Need more? Create **custom categories** with any name
- 🔗 Link transactions to asset accounts — balances update automatically
- 📅 Auto-grouped by date with daily subtotals at a glance

### ✅ Batch Operations — No more deleting one by one

- 👆 Long-press to enter multi-select mode (with haptic feedback)
- 🗑️ Batch delete · 📝 Batch category change — maximum efficiency

### 🔍 Search & Filtering — Find any transaction instantly

- ⌨️ Real-time search across categories and notes
- ⏮️ ⏭️ Navigate month by month, jump to any period
- 📆 Custom date range filtering

### 💰 Monthly Budget — Keep your spending in check

- 🎯 Set a monthly budget target
- 📊 Visual progress bar — see at a glance if you're overspending

### 📈 Statistics & Charts — See where your money goes

- 📉 **Trend line chart** — smooth Bezier curves + gradient fill, 1500ms animated entrance
- 🍩 **Category donut chart** — ring chart showing expense breakdown with Top 5 legend
- 🔄 Flexible period switching: week / month / year
- 📱 Landscape mode for detailed trend viewing

### 🏦 Asset Management — All your accounts in one place

- 📂 Three types: 💳 **Assets** / 📉 **Liabilities** / 🤝 **Lent Out**
- 💎 Summary card: total assets, liabilities, and lent-out amounts
- ⚡ Link transactions to assets for automatic balance updates

### ☁️ Backup & Sync — Your data, safe and sound

- 🌐 **WebDAV backup** — upload / download / delete, custom server path (Nutcloud, NextCloud, etc.)
- 🔁 **Auto WebDAV backup after each entry** — when enabled, every manual/auto entry is uploaded automatically (entry moved into the **Backup & Restore** page)
- 📄 **Local CSV** — UTF-8 BOM export (opens in Excel without garbled text), backward-compatible import
- 🧹 **Import dedup protection** — local import / WebDAV restore now deduplicate records to prevent duplicate bills
- 🔄 **AutoAccounting sync** — connect to [AutoAccounting](https://github.com/AutoAccountingOrg/AutoAccounting), with both foreground auto-sync and manual sync

### 🎨 Theme System — Make it yours

- 🎨 **6 hand-crafted color schemes**:
  - 💜 Default Purple · 🌸 Sakura Pink · 🌊 Gulf Blue · 🌿 Field Green · 🍂 Autumn Yellow · 🖤 Neutral Black
- 🎭 **Dynamic color** (Android 12+ Material You — wallpaper colors, everywhere)
- 🌗 **Theme modes**: Follow System / ☀️ Light / 🌙 Dark
- ⬛ **AMOLED pure black mode** (battery-saving, eye-friendly, night owl approved)

### 📱 Home Screen Widget — No need to open the app

- 🔮 Built with Jetpack Glance, shows monthly spending right on your home screen
- ⚡ One-tap quick-add button — logging expenses has never been easier
- 🔄 Auto-refresh every 30 min + instant updates on data changes

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
|:---:|:---:|:---:|
| 🗣️ Language | Kotlin | 2.0.21 |
| 🖼️ UI Framework | Jetpack Compose | BOM 2024.12.01 |
| 🎨 Design System | Material Design 3 | via BOM |
| 🏗️ Architecture | MVVM | ViewModel + StateFlow |
| 💾 Database | Room | 2.6.1 |
| ⚙️ Code Gen | KSP | 2.0.21-1.0.28 |
| ♻️ Lifecycle | AndroidX Lifecycle | 2.8.7 |
| 🌐 Networking | OkHttp (WebDAV) | 4.12.0 |
| 📱 Widget | Jetpack Glance | 1.1.1 |
| 🎯 Icons | Material Icons Extended | 1.7.6 |

---

## 📂 Project Structure

```
com.example.expensetracker
├── 🚀 MainActivity                    # Entry point, navigation, widget intent
├── 📦 data/
│   ├── 💾 local/
│   │   ├── ExpenseEntity              # Expense/income entity
│   │   ├── AssetEntity                # Asset entity
│   │   ├── ExpenseDao                 # Expense DAO
│   │   ├── AssetDao                   # Asset DAO
│   │   ├── ExpenseDatabase            # Room DB (v5, 4 migrations)
│   │   └── DailyExpenseSummary        # Daily summary data class
│   ├── 🌐 remote/
│   │   ├── AutoAccountingService      # AutoAccounting HTTP sync
│   │   └── WebDavClient               # WebDAV client
│   └── ExpenseRepository              # Repository layer
├── 🖼️ ui/
│   ├── 📱 screen/
│   │   ├── ExpenseListScreen          # Home: list, filters, budget
│   │   ├── StatisticsScreen           # Stats: trend + donut chart
│   │   ├── AssetScreen                # Asset management
│   │   ├── SettingsScreen             # Settings: theme, test data
│   │   ├── BackupScreen               # Backup: WebDAV, CSV, sync
│   │   └── TrendDetailActivity        # Landscape trend detail
│   ├── 🧩 component/
│   │   ├── AddExpenseDialog           # Add transaction BottomSheet
│   │   ├── EditExpenseDialog          # Edit transaction BottomSheet
│   │   ├── AddAssetDialog             # Add asset dialog
│   │   └── EditAssetDialog            # Edit asset BottomSheet
│   ├── 📊 model/                      # UI data models
│   ├── 🧠 viewmodel/
│   │   └── ExpenseViewModel           # Shared ViewModel
│   └── 🔧 util/
│       └── CategoryIconHelper         # Category-to-icon mapping
├── 📱 widget/
│   ├── ExpenseAppWidget               # Glance widget
│   └── ExpenseAppWidgetReceiver       # Widget broadcast receiver
└── 🎨 theme/
    ├── Theme.kt                       # 6 palettes + dynamic color + AMOLED
    ├── Color.kt                       # 247 color constants
    └── Type.kt                        # MD3 typography scale
```

---

## 🚀 Build & Run

**Requirements:**
- 🛠️ Android Studio Ladybug or newer
- ☕ JDK 17
- 📦 Android SDK 35, min API 26 (Android 8.0)

```bash
# 🐛 Debug build
./gradlew assembleDebug

# 📦 Release build
./gradlew assembleRelease
```

---

## 🔄 AutoAccounting Integration

This app supports [AutoAccounting](https://github.com/AutoAccountingOrg/AutoAccounting) for **automated bill tracking** — say goodbye to manual input!

**Steps:**
1. 📲 Install and start AutoAccounting, enable its LSPosed module to hook Alipay / WeChat
2. 📖 In this app, enable **Auto-sync bills on app foreground** in Settings (enabled by default, can be turned off anytime)
3. 🔄 When the app returns to foreground, it will auto-fetch unsynced bills via `http://127.0.0.1:52045`
4. 👆 You can still tap the sync button on the home screen for an immediate manual sync

> ⚠️ **Note**: Do **not** add this expense tracker to AutoAccounting's LSPosed scope — that will cause crashes. AutoAccounting only needs to hook payment apps.

---

## 📄 License

This project is for personal learning purposes only.
