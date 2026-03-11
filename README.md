# md3ExpenseTracker

中文 | English

一款基于 Material Design 3 的 Android 本地记账应用，采用 Jetpack Compose + MVVM 架构，支持收支管理、分类统计、趋势图、批量操作、备份与同步，并适配动态颜色与主题模式。

An Android local expense tracker built with Material Design 3, Jetpack Compose, and MVVM. It supports income/expense management, category statistics, trend charts, batch actions, backup/sync, and dynamic color + theme modes.

---

## 功能 Features

- 收支记录与分类管理（收入/支出、分类、备注）
- 账单按日期分组与范围筛选
- 趋势图与分类构成统计
- 批量操作：批量删除、批量改分类
- 资产管理：底部 Sheet 编辑与删除二次确认
- 备份与同步：WebDAV 备份、本地 CSV 导入导出、AutoAccounting 同步
- 主题与外观：动态颜色、系统/浅色/深色主题模式
- 桌面小组件（Glance）

- Income/expense entries with categories and notes
- Date grouping and range filters
- Trend chart and category breakdown
- Batch actions: delete and category edit
- Asset management with bottom sheet editor and in-sheet delete confirmation
- Backup & sync: WebDAV, local CSV import/export, AutoAccounting sync
- Theming: dynamic color, system/light/dark modes
- Home screen widget (Glance)

---

## 技术栈 Tech Stack

- Language: Kotlin
- UI: Jetpack Compose, Material Design 3
- Architecture: MVVM (ViewModel + StateFlow)
- Storage: Room, SharedPreferences
- Network: OkHttp (WebDAV), HttpURLConnection (AutoAccounting)
- Widget: Jetpack Glance

---

## 项目结构 Project Structure

```
com.example.expensetracker
├── MainActivity
├── data
│   ├── local
│   ├── remote
│   └── ExpenseRepository
├── ui
│   ├── screen
│   ├── component
│   ├── model
│   └── viewmodel
├── widget
└── theme
```

---

## 编译与运行 Build

- Android Studio Ladybug+
- JDK 17
- Android SDK 35, minSdk 26

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

---

## License

仅用于个人学习与交流。For personal learning only.
