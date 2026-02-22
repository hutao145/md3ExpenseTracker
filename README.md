# 📒 MD3 Expense Tracker

一款基于 **Jetpack Compose + Material Design 3** 构建的现代 Android 记账应用，支持收支管理、预算追踪、数据统计、桌面微件等功能。

---

## ✨ 功能特性

### 📊 收支管理
- 记录**支出**与**收入**，支持分类（餐饮、交通、购物、娱乐、医疗、教育等）
- 支持添加备注、自定义日期
- **单击编辑**、**长按多选批量删除**

### 📅 月度视图
- 按月浏览账单，支持左右切换月份
- 分类汇总统计，直观展示每月各分类占比
- 设置**本月预算**，进度条实时展示预算消耗情况

### 🔍 搜索与筛选
- 按**分类/备注**关键词搜索
- 按**自定义日期区间**筛选账单

### 📈 数据统计
- **柱状图**：展示月度支出 / 收入趋势
- **饼图**：按分类分布可视化

### 🏠 桌面微件（Jetpack Glance）
- 桌面直接查看**本月支出总额**
- 点击 **[+]** 按钮，无需打开 App 即可快速跳转到记账页面
- 记账后微件自动刷新数据

### 📤 数据导出
- 支持将账单数据导出为 **CSV 文件**

### 🎨 界面与体验
- 全面适配 **Material Design 3**，支持动态取色
- **沉浸式状态栏 / 导航栏**，内容延伸至全屏
- 流畅的**过渡动画**（列表项、多选模式切换、底部栏滑入滑出）
- 长按进入**多选模式**，支持批量删除及全选动画

---

## 🛠 技术栈

| 技术 | 用途 |
|---|---|
| Kotlin | 主开发语言 |
| Jetpack Compose | 声明式 UI |
| Material Design 3 | 设计系统 |
| Room | 本地数据库 |
| ViewModel + StateFlow | UI 状态管理 |
| Jetpack Glance | 桌面微件 |
| Kotlin Coroutines | 异步处理 |

---

## 📦 项目结构

```
app/src/main/java/com/example/expensetracker/
├── data/
│   ├── local/          # Room DAO 与数据库实体
│   └── ExpenseRepository.kt
├── ui/
│   ├── component/      # 通用 Compose 组件（添加/编辑对话框等）
│   ├── model/          # UI 数据模型
│   ├── screen/         # 各页面（记账列表、统计、设置）
│   └── viewmodel/      # ViewModel
└── widget/             # Glance 桌面微件
```

---

## 🚀 快速开始

1. Clone 本仓库
2. 用 **Android Studio** 打开项目
3. 连接设备或启动模拟器（Android 8.0+）
4. 点击 **Run** 即可

---

## 📸 截图

> *（可在此处添加应用截图）*

---

## 📄 License

MIT License