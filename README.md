# MD3 Expense Tracker

<div align="center">

**一款基于 Material Design 3 的 Android 本地记账应用**

Kotlin | Jetpack Compose | Room | MVVM

[English](README_EN.md) | 简体中文

</div>

---

## 功能概览

### 收支管理
- 支持记录**收入**和**支出**两种类型
- 23+ 内置分类（餐饮、交通、购物、薪资、奖金等），每个分类配有 Material Icons
- 支持**自定义分类**名称
- 可关联资产账户，自动同步余额变动
- 按日期分组展示，显示每日小计

### 批量操作
- 长按进入多选模式（带触觉反馈）
- 批量删除、批量修改分类

### 搜索与筛选
- 按分类名称和备注内容实时搜索
- 按月导航（上月/下月/本月）
- 自定义日期范围筛选

### 月预算
- 设置月度预算目标
- 进度条可视化展示当月花费占比

### 统计图表
- **趋势折线图** — 贝塞尔曲线平滑绘制，渐变填充，1500ms 动画
- **分类饼图** — 环形图展示各分类支出占比，显示 Top 5 图例
- 支持按周/月/年切换统计周期
- 横屏趋势详情页

### 资产管理
- 三种资产类型：**资产** / **负债** / **借出**
- 汇总卡片展示总资产、总负债、总借出
- 收支记录关联资产后自动调整余额

### 备份与同步
- **WebDAV 备份** — 上传/下载/删除备份文件，支持自定义服务器路径
- **本地 CSV** — 导出含 UTF-8 BOM（Excel 兼容），支持导入旧版 4 列格式
- **AutoAccounting 同步** — 对接 [AutoAccounting](https://github.com/AutoAccountingOrg/AutoAccounting) 自动从支付宝/微信拉取账单

### 主题系统
- **6 套配色方案**：默认紫 / 樱花粉 / 海湾蓝 / 原野绿 / 秋黄 / 中性黑
- **动态取色**（Android 12+ Material You）
- **主题模式**：跟随系统 / 浅色 / 深色
- **AMOLED 纯黑模式**

### 桌面小组件
- Jetpack Glance 构建，显示本月支出总额
- 一键快捷记账按钮
- 每 30 分钟自动刷新 + 数据变动时即时更新

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.0.21 |
| UI 框架 | Jetpack Compose | BOM 2024.12.01 |
| 设计规范 | Material Design 3 | via BOM |
| 架构 | MVVM | ViewModel + StateFlow |
| 本地数据库 | Room | 2.6.1 |
| 代码生成 | KSP | 2.0.21-1.0.28 |
| 生命周期 | AndroidX Lifecycle | 2.8.7 |
| 网络 | OkHttp (WebDAV) | 4.12.0 |
| 小组件 | Jetpack Glance | 1.1.1 |
| 图标 | Material Icons Extended | 1.7.6 |

---

## 项目结构

```
com.example.expensetracker
├── MainActivity                    # 入口，导航，小组件 Intent 处理
├── data/
│   ├── local/
│   │   ├── ExpenseEntity           # 收支记录实体
│   │   ├── AssetEntity             # 资产实体
│   │   ├── ExpenseDao              # 收支 DAO
│   │   ├── AssetDao                # 资产 DAO
│   │   ├── ExpenseDatabase         # Room 数据库 (v5, 4 次迁移)
│   │   └── DailyExpenseSummary     # 每日汇总数据类
│   ├── remote/
│   │   ├── AutoAccountingService   # AutoAccounting HTTP 同步
│   │   └── WebDavClient            # WebDAV 客户端
│   └── ExpenseRepository           # 数据仓库层
├── ui/
│   ├── screen/
│   │   ├── ExpenseListScreen       # 首页：收支列表、筛选、预算
│   │   ├── StatisticsScreen        # 统计：趋势图、饼图
│   │   ├── AssetScreen             # 资产管理
│   │   ├── SettingsScreen          # 设置：主题、测试数据
│   │   ├── BackupScreen            # 备份：WebDAV、CSV、AutoAccounting
│   │   └── TrendDetailActivity     # 横屏趋势详情
│   ├── component/
│   │   ├── AddExpenseDialog        # 新增收支 BottomSheet
│   │   ├── EditExpenseDialog       # 编辑收支 BottomSheet
│   │   ├── AddAssetDialog          # 新增资产对话框
│   │   └── EditAssetDialog         # 编辑资产 BottomSheet
│   ├── model/                      # UI 数据模型
│   ├── viewmodel/
│   │   └── ExpenseViewModel        # 全局 ViewModel
│   └── util/
│       └── CategoryIconHelper      # 分类图标映射
├── widget/
│   ├── ExpenseAppWidget            # Glance 小组件
│   └── ExpenseAppWidgetReceiver    # 小组件广播接收器
└── theme/
    ├── Theme.kt                    # 6 套配色 + 动态颜色 + AMOLED
    ├── Color.kt                    # 247 个颜色常量
    └── Type.kt                     # MD3 字体排版
```

---

## 编译与运行

**环境要求：**
- Android Studio Ladybug 或更新版本
- JDK 17
- Android SDK 35，最低 API 26（Android 8.0）

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease
```

---

## AutoAccounting 集成

本应用支持对接 [AutoAccounting](https://github.com/AutoAccountingOrg/AutoAccounting) 实现自动记账。

**使用步骤：**
1. 在设备上安装并启动 AutoAccounting，启用 LSPosed 模块 hook 支付宝/微信
2. 打开本应用，进入备份页面点击同步按钮
3. 应用将通过 `http://127.0.0.1:52045` 拉取未同步的账单

> **注意**：请勿将本记账应用添加到 AutoAccounting 的 LSPosed 作用域中，否则会导致崩溃。AutoAccounting 仅需 hook 支付类应用。

---

## License

本项目仅用于个人学习与交流。
