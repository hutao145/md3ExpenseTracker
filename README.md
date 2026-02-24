# 📒 MD3 记账本

<div align="right">
  <strong>简体中文</strong> | <a href="README_EN.md">English</a>
</div>

一款基于 **Material Design 3** 设计规范的 Android 本地记账应用，采用现代 Jetpack 技术栈构建，支持收支管理、分类统计、日期筛选、WebDAV 云端备份以及与 [AutoAccounting（自动记账）](https://github.com/AutoAccountingOrg/AutoAccounting) 模块的账单同步。

---

## ✨ 核心功能

| 功能 | 说明 |
|---|---|
| 💰 收支记录 | 支持支出 / 收入两种类型，输入金额、分类和备注 |
| 📅 按天分组展示 | 账单列表按日期分组，每组显示当日总收支 |
| 🏷️ 分类管理 | 内置多种常见消费分类（餐饮、交通、购物等） |
| 📊 统计摘要 | 首页卡片展示总结余、当月收支与分类占比 |
| 📆 日期范围筛选 | 自由选择时间范围查看历史账单 |
| 🔍 全文搜索 | 按分类或备注关键字快速检索 |
| 💼 月度预算 | 设置月预算并以进度条直观展示消耗情况 |
| ✅ 批量操作 | 长按进入多选模式，支持批量删除 |
| ☁️ WebDAV 云端备份 | 备份账单 CSV 到 WebDAV 服务器，支持多版本历史浏览、恢复与删除 |
| 💾 本地备份 | 导出 / 导入 CSV，方便在 Excel 中进行二次分析 |
| 🔄 自动记账同步 | 与 AutoAccounting 模块联动，自动拉取支付宝 / 微信账单并入库 |
| 🪟 桌面小组件 | 支持 Glance 桌面 Widget，快速查看结余与新增记录 |

---

## 📸 界面预览

> 采用 Material Design 3 动态色彩方案，风格清爽现代，全程 MD3 规范动画过渡效果。

---

## 🛠️ 技术栈

| 层次 | 选型 |
|---|---|
| 开发语言 | Kotlin |
| UI 框架 | Jetpack Compose |
| 设计系统 | Material Design 3 |
| 本地存储 | Room |
| 凭据持久化 | SharedPreferences |
| 架构模式 | MVVM（ViewModel + StateFlow） |
| 网络请求 | OkHttp 4（WebDAV 备份）/ HttpURLConnection（AutoAccounting 同步） |
| 桌面小组件 | Jetpack Glance |

---

## 📁 项目结构

```
com.example.expensetracker
├── MainActivity                    # 应用唯一 Activity
├── data
│   ├── local
│   │   ├── ExpenseEntity           # Room 数据实体
│   │   ├── ExpenseDao              # 数据访问接口
│   │   └── ExpenseDatabase         # Room 数据库
│   ├── remote
│   │   ├── WebDavClient            # WebDAV 网络客户端（上传/下载/列举/删除）
│   │   └── AutoAccountingService   # AutoAccounting HTTP 通信
│   └── ExpenseRepository           # 数据仓库
├── ui
│   ├── screen
│   │   ├── ExpenseListScreen       # 主页（账单列表）
│   │   ├── StatisticsScreen        # 统计页
│   │   ├── BackupScreen            # 备份与恢复（WebDAV + 本地）
│   │   └── SettingsScreen          # 设置页
│   ├── component                   # 通用 UI 组件（弹窗、卡片等）
│   ├── model                       # UI 展示模型
│   └── viewmodel
│       └── ExpenseViewModel        # 状态管理与业务逻辑
├── widget                          # Glance 桌面 Widget
└── theme                           # Material 3 主题配置
```

---

## ☁️ WebDAV 备份说明

本应用支持将账单数据备份到任意 WebDAV 服务器（例如 坚果云、Nextcloud、NAS 等）。

**功能特性：**

- 备份为带时间戳的 CSV 文件（如 `backup_20260224_113106.csv`），支持多版本共存
- 点击「恢复」后弹出文件列表，显示文件名、时间和大小，可选择历史快照恢复
- 支持删除云端指定备份文件，列表删除时有 MD3 弹性退场动画
- 服务器配置（地址、账号、密码、目录路径）自动持久化，重启应用无需重新填写

**使用步骤：**

1. 进入「设置」→「备份与恢复」→「WebDAV」标签页
2. 填写服务器地址、用户名、密码和备份目录路径，点击「测试连接」验证
3. 点击「立即备份」上传当前账单
4. 点击「恢复」浏览历史备份列表，选择想要的版本点击恢复

---

## 🔄 AutoAccounting 同步说明

本应用支持与 [AutoAccounting](https://github.com/AutoAccountingOrg/AutoAccounting) 联动，通过其本地 HTTP 服务（`http://127.0.0.1:52045`）拉取账单数据。

**使用步骤：**

1. 在手机上安装并启动 AutoAccounting，启用其 Hook 微信 / 支付宝的 LSPosed 模块
2. 打开本应用，点击首页「总结余」右侧的 **🔄 同步按钮**
3. 应用会自动拉取未同步账单并写入本地数据库，完成后通过 Snackbar 展示同步结果

> ⚠️ **注意**：请勿在 LSPosed 中将本记账应用列入 AutoAccounting 的作用域，否则会导致应用崩溃。AutoAccounting 只需 Hook 支付宝、微信等支付应用即可。

---

## 🚀 编译与运行

**环境要求：**
- Android Studio Ladybug 或以上
- JDK 17
- Android SDK 35，最低支持 API 26（Android 8.0）

**构建命令：**

```bash
# Debug 包
./gradlew assembleDebug

# Release 包
./gradlew assembleRelease
```

---

## 📦 主要依赖

| 组件 | 版本 |
|---|---|
| Kotlin | 2.0.21 |
| Compose BOM | 2024.12.01 |
| Material 3 | 跟随 BOM |
| AndroidX Room | 2.6.1 |
| Lifecycle / ViewModel | 2.8.7 |
| OkHttp | 4.12.0 |
| Jetpack Glance | 1.1.1 |
| KSP | 2.0.21-1.0.28 |

---

## 📄 License

本项目仅作个人学习使用。