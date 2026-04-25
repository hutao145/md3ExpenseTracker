# 💰 MD3 Expense Tracker

<div align="center">

✨ **一款精致的 Material Design 3 Android 本地记账应用** ✨

`Kotlin` · `Jetpack Compose` · `Room` · `MVVM`

[🇬🇧 English](README_EN.md) | 🇨🇳 简体中文

---

🍜 记餐饮 · 🚗 记交通 · 🛍️ 记购物 · 💵 记收入 · 📊 看统计 · ☁️ 云备份 · 🤖 AI 分析

</div>

---

## 🌟 功能亮点

### 💸 收支管理 — 随手一记，清晰明了

- ➕ 支持**收入**和**支出**两种类型，一键切换
- 🏷️ **23+ 内置分类**（🍜 餐饮、🚗 交通、🛍️ 购物、💰 薪资、🎁 奖金...），每个分类都有精心适配的 Material Icon
- ✏️ 觉得分类不够用？支持**自定义分类**名称
- 🔗 可关联资产账户，记一笔账余额自动变动，再也不用手动算
- 📅 按日期自动分组，每天花了多少一目了然

### ✅ 批量操作 — 告别逐条删除的痛苦

- 👆 长按进入多选模式（带震动反馈，手感一流）
- 🗑️ 批量删除、📝 批量改分类，效率拉满

### 🔍 搜索与筛选 — 精准找到每一笔

- ⌨️ 输入关键字实时搜索分类和备注
- ⏮️ ⏭️ 按月前后翻页，快速定位
- 📆 自定义日期范围，看某段时间花了多少

### 💰 月预算 — 管住手，守住钱包

- 🎯 设定每月预算目标
- 📊 进度条直观展示花费比例，超支一眼就知道

### 📈 统计图表 — 数据可视化，花钱花在哪

- 📉 **趋势折线图** — 贝塞尔曲线丝滑绘制 + 渐变填充，1500ms 入场动画
- 🍩 **分类饼图** — 环形图展示各分类占比，Top 5 图例一目了然
- 📅 **月历总览** — 日历格式浏览整月收支，支出红色、收入绿色，今天蓝圈高亮
- 🔄 按周 / 月 / 年灵活切换统计周期
- 📱 支持横屏查看趋势详情，大屏体验更佳

### 🤖 AI 财务分析 — 智能洞察你的财务状况

- 🧠 接入 OpenAI 兼容 API，一键生成**财务全景透视报告**
- 📊 **健康评分** — 0-100 综合评分，直观了解财务状态
- 🔍 **深度分析** — 消费结构、异常风险、行为规律、趋势预测
- 📋 **行动清单** — AI 给出优先级排序的改善建议
- 📚 **历史记录** — 分析结果持久化保存，随时回顾对比
- ⏳ **后台分析** — 选择日期范围后在后台执行，不阻塞操作
- 📅 支持多种日期预设（近一周/30天/本月/本年）和自定义范围

### 🏦 资产管理 — 你的所有账户都在这里

- 📂 三种类型：💳 **资产** / 📉 **负债** / 🤝 **借出**
- 💎 汇总卡片一览：总资产、总负债、总借出
- ⚡ 记账时关联资产，余额自动更新
- 🔀 可在设置中开关资产页面，按需显示

### ☁️ 备份与同步 — 数据安全，高枕无忧

- 🌐 **WebDAV 备份** — 上传 / 下载 / 删除，支持自定义服务器路径（坚果云、NextCloud 等）
- 🔁 **记账后自动网络备份** — 开启后每次记账/自动记账自动上传到 WebDAV（入口已整合到「备份与恢复」页面）
- ⏰ **定时自动备份** — 自定义 1-72 小时间隔，WorkManager 后台调度，本地 + WebDAV 双重保障
- 📄 **本地 CSV** — UTF-8 BOM 导出（Excel 直接打开不乱码），兼容旧版导入
- 🧹 **导入去重保护** — 本地导入 / WebDAV 恢复自动去重，避免重复导入产生重复账单
- 🔄 **AutoAccounting 同步** — 对接 [AutoAccounting](https://github.com/AutoAccountingOrg/AutoAccounting)，支持前台自动拉取与手动同步，告别重复操作

### 🎨 主题系统 — 你的记账本你做主

- 🎨 **6 套精心调配的配色**：
  - 💜 默认紫 · 🌸 樱花粉 · 🌊 海湾蓝 · 🌿 原野绿 · 🍂 秋黄 · 🖤 中性黑
- 🎭 **动态取色**（Android 12+ Material You，壁纸变色跟着变）
- 🌗 **主题模式**：跟随系统 / ☀️ 浅色 / 🌙 深色
- ⬛ **AMOLED 纯黑模式**（省电护眼，夜猫子福音）

### 🔒 隐私与安全 — 你的账本只有你能看

- 🔢 **PIN 密码锁** — 4 位数字密码，5 次错误自动冷却 30 秒
- 👆 **生物识别解锁** — 支持指纹和面部识别（需设备支持）
- 🔐 启用后每次打开应用需验证身份

### 📱 桌面小组件 — 不打开 App 也能看

- 🔮 Jetpack Glance 构建，桌面直接显示本月支出
- ⚡ 一键快捷记账按钮，记账从未如此方便
- 🔄 每 30 分钟自动刷新 + 数据变动时即时更新

---

## 🛠️ 技术栈

| 层级 | 技术 | 版本 |
|:---:|:---:|:---:|
| 🗣️ 语言 | Kotlin | 2.0.21 |
| 🖼️ UI 框架 | Jetpack Compose | BOM 2024.12.01 |
| 🎨 设计规范 | Material Design 3 | via BOM |
| 🏗️ 架构 | MVVM | ViewModel + StateFlow |
| 💾 本地数据库 | Room | 2.6.1 |
| ⚙️ 代码生成 | KSP | 2.0.21-1.0.28 |
| ♻️ 生命周期 | AndroidX Lifecycle | 2.8.7 |
| 🌐 网络 | OkHttp (WebDAV + AI API) | 4.12.0 |
| ⏰ 后台任务 | WorkManager | 2.10.0 |
| 🔒 生物识别 | AndroidX Biometric | 1.1.0 |
| 📱 小组件 | Jetpack Glance | 1.1.1 |
| 🎯 图标 | Material Icons Extended | 1.7.6 |
| 📊 图表渲染 | ECharts (WebView) | 5.x |

---

## 📂 项目结构

```
com.example.expensetracker
├── 🚀 MainActivity                    # 入口，导航，小组件 Intent 处理
├── 📦 data/
│   ├── 💾 local/
│   │   ├── ExpenseEntity              # 收支记录实体
│   │   ├── AssetEntity                # 资产实体
│   │   ├── AiAnalysisEntity           # AI 分析记录实体
│   │   ├── ExpenseDao                 # 收支 DAO
│   │   ├── AssetDao                   # 资产 DAO
│   │   ├── AiAnalysisDao             # AI 分析 DAO
│   │   ├── ExpenseDatabase            # Room 数据库 (v6, 5 次迁移)
│   │   └── DailyExpenseSummary        # 每日汇总数据类
│   ├── 🌐 remote/
│   │   ├── AiApiClient                # AI API 客户端 (OpenAI 兼容)
│   │   ├── AutoAccountingService      # AutoAccounting HTTP 同步
│   │   └── WebDavClient               # WebDAV 客户端
│   └── ExpenseRepository              # 数据仓库层
├── ⏰ backup/
│   ├── BackupScheduler                # WorkManager 定时调度
│   └── ScheduledBackupWorker          # 后台备份任务
├── 🔒 security/
│   ├── PinManager                     # PIN 哈希与验证 (SHA-256)
│   ├── BiometricHelper                # 生物识别封装
│   └── AppLockManager                 # 应用锁状态管理
├── 🖼️ ui/
│   ├── 📱 screen/
│   │   ├── ExpenseListScreen          # 首页：收支列表、筛选、预算
│   │   ├── StatisticsScreen           # 统计：趋势图、饼图、月历
│   │   ├── AssetScreen                # 资产管理
│   │   ├── SettingsScreen             # 设置：主题、安全、AI 配置
│   │   ├── BackupScreen               # 备份：WebDAV、CSV、定时备份
│   │   ├── LockScreen                 # PIN 锁屏界面
│   │   ├── TrendDetailActivity        # 横屏趋势详情
│   │   └── AiAnalysisActivity         # AI 财务分析 (历史记录 + WebView 报告)
│   ├── 🧩 component/
│   │   ├── ExpenseFormSheet           # 收支表单 (新增/编辑统一)
│   │   ├── AssetFormSheet             # 资产表单 (新增/编辑统一)
│   │   └── SetPinDialog               # 设置 PIN 对话框
│   ├── 📊 model/                      # UI 数据模型
│   ├── 🧠 viewmodel/
│   │   └── ExpenseViewModel           # 全局 ViewModel
│   └── 🔧 util/
│       ├── AmountFormatter            # 金额格式化工具
│       └── CategoryIconHelper         # 分类图标映射
├── 📱 widget/
│   ├── ExpenseAppWidget               # Glance 小组件
│   └── ExpenseAppWidgetReceiver       # 小组件广播接收器
└── 🎨 theme/
    ├── Theme.kt                       # 6 套配色 + 动态颜色 + AMOLED
    ├── Color.kt                       # 247 个颜色常量
    └── Type.kt                        # MD3 字体排版
```

---

## 🚀 编译与运行

**环境要求：**
- 🛠️ Android Studio Ladybug 或更新版本
- ☕ JDK 17
- 📦 Android SDK 35，最低 API 26（Android 8.0）

```bash
# 🐛 Debug 构建
./gradlew assembleDebug

# 📦 Release 构建
./gradlew assembleRelease
```

---

## 🤖 AI 财务分析配置

本应用支持接入任何 **OpenAI 兼容 API** 来生成智能财务分析报告。

**配置步骤：**
1. 📱 打开设置 → AI 分析配置
2. 🔗 填写 API 端点（默认 `https://api.openai.com/v1/chat/completions`）
3. 🔑 填写 API Key
4. 🤖 选择模型（默认 `gpt-4o-mini`，支持任意兼容模型）

**使用方式：**
1. 进入 AI 财务分析页面，查看历史分析记录
2. 点击右下角 ➕ 按钮，选择分析日期范围
3. 后台自动调用 AI 生成全景透视报告
4. 分析完成后点击记录查看包含图表的交互式报告

---

## 🔄 AutoAccounting 集成

本应用支持对接 [AutoAccounting](https://github.com/AutoAccountingOrg/AutoAccounting) 实现**自动记账**，从此告别手动输入！

**使用步骤：**
1. 📲 在设备上安装并启动 AutoAccounting，启用 LSPosed 模块 hook 支付宝 / 微信
2. 📖 在本应用设置中开启 **前台自动同步账单**（默认开启，可随时关闭）
3. 🔄 应用回到前台时将通过 `http://127.0.0.1:52045` 自动拉取未同步账单
4. 👆 也可在首页手动点击同步按钮立即拉取

> ⚠️ **注意**：请勿将本记账应用添加到 AutoAccounting 的 LSPosed 作用域中，否则会导致崩溃。AutoAccounting 仅需 hook 支付类应用。

---

## 📄 License

本项目仅用于个人学习与交流。
