const fs = await import("node:fs/promises");
const path = await import("node:path");
const { Presentation, PresentationFile } = await import("@oai/artifact-tool");

const W = 1280;
const H = 720;

const DECK_ID = "md3expense-software-intro-full";
const OUT_DIR = "E:\\AI\\ai projects\\md3ExpenseTracker\\outputs\\md3expense-software-intro-full";
const SCRATCH_DIR = path.resolve(path.join("E:\\AI\\ai projects\\md3ExpenseTracker\\tmp\\slides", DECK_ID));
const PREVIEW_DIR = path.join(SCRATCH_DIR, "preview");
const INSPECT_PATH = path.join(SCRATCH_DIR, "inspect.ndjson");

const C = {
  bg: "#FFFCF5",
  blobA: "#F3EDFF",
  blobB: "#EAF2FF",
  blobC: "#FFF1D9",
  panel: "#FFFFFFE8",
  panelSoft: "#FFFFFFCC",
  border: "#EADDC7",
  text: "#1C1B2E",
  sub: "#4B5563",
  mute: "#7A8798",
  gold: "#F59E0B",
  purple: "#8B5CF6",
  teal: "#14B8A6",
  red: "#EF4444",
  blue: "#2563EB",
};

const FONT = {
  title: "Poppins",
  body: "Lato",
  mono: "Aptos Mono",
};

const TOTAL = 15;
const inspectRecords = [];

function line(fill = "#00000000", width = 0) {
  return { style: "solid", fill, width };
}

function shape(slide, geometry, left, top, width, height, fill = "#00000000", stroke = "#00000000", sw = 0) {
  return slide.shapes.add({ geometry, position: { left, top, width, height }, fill, line: line(stroke, sw) });
}

function text(slide, no, value, left, top, width, height, opt = {}) {
  const box = shape(slide, "rect", left, top, width, height, opt.fill || "#00000000", opt.stroke || "#00000000", opt.sw || 0);
  box.text = value;
  box.text.fontSize = opt.size ?? 18;
  box.text.bold = opt.bold ?? false;
  box.text.color = opt.color ?? C.text;
  box.text.typeface = opt.face ?? FONT.body;
  box.text.alignment = opt.align ?? "left";
  box.text.verticalAlignment = opt.valign ?? "top";
  box.text.insets = { left: 0, right: 0, top: 0, bottom: 0 };

  inspectRecords.push({
    kind: "textbox",
    slide: no,
    role: opt.role || "text",
    text: String(value),
    textChars: String(value).length,
    textLines: String(value).split(/\n/).length,
    bbox: [left, top, width, height],
  });
  return box;
}

function bg(slide) {
  slide.background.fill = C.bg;
  shape(slide, "ellipse", -170, -150, 560, 430, C.blobA);
  shape(slide, "ellipse", 920, -160, 520, 420, C.blobB);
  shape(slide, "ellipse", -120, 505, 500, 310, C.blobC);
}

function glass(slide, x, y, w, h) {
  shape(slide, "roundRect", x, y, w, h, C.panel, C.border, 1);
}

function header(slide, no, kicker) {
  text(slide, no, kicker, 70, 30, 640, 22, { size: 12, bold: true, face: FONT.mono, color: C.purple, role: "header" });
  text(slide, no, `${String(no).padStart(2, "0")} / ${String(TOTAL).padStart(2, "0")}`, 1050, 30, 170, 22, { size: 12, bold: true, face: FONT.mono, color: C.purple, align: "right", role: "page" });
  shape(slide, "rect", 64, 58, 1152, 2, "#E7E2F6");
}

function title(slide, no, t, s) {
  text(slide, no, t, 72, 84, 950, 70, { size: 48, bold: true, face: FONT.title, color: C.text, role: "title" });
  if (s) {
    text(slide, no, s, 74, 165, 960, 54, { size: 22, face: FONT.body, color: C.sub, role: "subtitle" });
  }
}

function bulletCard(slide, no, x, y, w, h, t, b, accent) {
  glass(slide, x, y, w, h);
  shape(slide, "rect", x, y, 8, h, accent);
  text(slide, no, t, x + 20, y + 15, w - 30, 28, { size: 20, bold: true, face: FONT.title, color: C.text, role: "card-title" });
  text(slide, no, b, x + 20, y + 52, w - 34, h - 68, { size: 16, face: FONT.body, color: C.sub, role: "card-body" });
}

function metricCard(slide, no, x, y, w, h, v, l, n, accent) {
  glass(slide, x, y, w, h);
  shape(slide, "rect", x, y, w, 6, accent);
  text(slide, no, v, x + 16, y + 14, w - 24, 40, { size: 34, bold: true, face: FONT.title, color: C.text, role: "metric-value" });
  text(slide, no, l, x + 16, y + 58, w - 24, 24, { size: 15, bold: true, face: FONT.body, color: C.sub, role: "metric-label" });
  text(slide, no, n, x + 16, y + 84, w - 24, 32, { size: 12, face: FONT.body, color: C.mute, role: "metric-note" });
}

function slide1(p) {
  const no = 1;
  const s = p.slides.add();
  bg(s);
  glass(s, 52, 72, 1176, 578);
  header(s, no, "MD3 EXPENSE TRACKER · 软件介绍完整版");

  text(s, no, "MD3 Expense Tracker", 86, 118, 760, 64, { size: 58, bold: true, face: FONT.title, color: C.text, role: "cover-title" });
  text(s, no, "Android 本地记账应用\n产品与技术深度介绍", 88, 188, 760, 118, { size: 45, bold: true, face: FONT.title, color: C.purple, role: "cover-subtitle" });
  text(s, no, "从业务价值、功能体系、技术架构到工程保障，完整呈现项目能力与可持续演进路径。", 90, 322, 760, 72, { size: 22, face: FONT.body, color: C.sub, role: "cover-desc" });

  glass(s, 90, 418, 560, 88);
  text(s, no, "Kotlin · Jetpack Compose · Room · MVVM · WorkManager", 112, 450, 520, 28, { size: 17, bold: true, face: FONT.mono, color: C.gold, role: "cover-stack" });

  glass(s, 860, 148, 312, 430);
  text(s, no, "本次重点", 890, 182, 220, 32, { size: 24, bold: true, face: FONT.title, color: C.text, role: "focus-title" });
  text(s, no, "• 产品定位与用户价值\n• 功能模块与使用闭环\n• AI 与自动化能力\n• 数据安全与备份韧性\n• 架构设计与工程质量\n• 后续路线图", 892, 232, 240, 300, { size: 19, face: FONT.body, color: C.sub, role: "focus-body" });
}

function slide2Agenda(p) {
  const no = 2;
  const s = p.slides.add();
  bg(s);
  header(s, no, "目录");
  title(s, no, "演示目录", "按照“价值 → 能力 → 实现 → 规划”四条主线展开");

  const items = [
    "01 产品背景与目标",
    "02 用户痛点与价值主张",
    "03 功能能力地图",
    "04 核心功能深挖（记账/统计/AI/备份/安全）",
    "05 关键交互闭环",
    "06 技术架构与数据模型",
    "07 工程能力与稳定性",
    "08 路线图与总结",
  ];

  glass(s, 72, 250, 1136, 360);
  items.forEach((it, i) => {
    const col = i < 4 ? 0 : 1;
    const row = i % 4;
    const x = 96 + col * 560;
    const y = 282 + row * 78;
    shape(s, "ellipse", x, y + 8, 10, 10, col === 0 ? C.purple : C.gold);
    text(s, no, it, x + 20, y, 510, 32, { size: 20, face: FONT.body, color: C.text, role: "agenda-item" });
  });
}

function slide3Background(p) {
  const no = 3;
  const s = p.slides.add();
  bg(s);
  header(s, no, "项目背景");
  title(s, no, "项目背景与产品目标", "以“本地优先 + 智能辅助”为核心，降低个人财务管理门槛");

  bulletCard(s, no, 72, 254, 360, 338, "背景动因", "用户有记账需求，但常见产品存在学习成本高、长期坚持难、数据控制弱等问题。", C.gold);
  bulletCard(s, no, 460, 254, 360, 338, "产品目标", "把日常“记录-分析-行动”串成闭环，让用户既能快速记账，也能从数据中得到可执行决策。", C.purple);
  bulletCard(s, no, 848, 254, 360, 338, "定位策略", "以 Android 本地记账为基础盘，叠加预算、统计、AI 分析、备份与安全能力，形成完整产品面。", C.teal);
}

function slide4PainValue(p) {
  const no = 4;
  const s = p.slides.add();
  bg(s);
  header(s, no, "用户价值");
  title(s, no, "用户痛点与价值主张", "每个痛点对应明确产品能力，避免“功能堆叠但无价值”");

  glass(s, 72, 250, 1136, 360);
  text(s, no, "痛点", 102, 276, 250, 30, { size: 20, bold: true, face: FONT.title, color: C.purple, role: "table-h1" });
  text(s, no, "价值主张", 436, 276, 320, 30, { size: 20, bold: true, face: FONT.title, color: C.purple, role: "table-h2" });
  text(s, no, "落地能力", 822, 276, 300, 30, { size: 20, bold: true, face: FONT.title, color: C.purple, role: "table-h3" });
  shape(s, "rect", 96, 312, 1088, 1, "#EDE7F6");

  const rows = [
    ["记录效率低", "缩短录入路径，降低心智负担", "分类快捷录入、批量操作、自然语言记账"],
    ["复盘难度高", "把流水变成可解释的结构化信息", "趋势图、饼图、月历、周/月/年统计"],
    ["数据安全焦虑", "提供本地可控与云端可恢复双保障", "Room 本地库、WebDAV 备份、CSV 导出"],
    ["执行缺乏方向", "从“看到问题”升级到“给出建议”", "AI 健康评分、风险提示、行动清单"],
  ];
  rows.forEach((r, i) => {
    const y = 332 + i * 74;
    text(s, no, r[0], 102, y, 290, 44, { size: 18, bold: true, face: FONT.body, color: C.text, role: "pain" });
    text(s, no, r[1], 436, y, 340, 44, { size: 17, face: FONT.body, color: C.sub, role: "value" });
    text(s, no, r[2], 822, y, 340, 44, { size: 17, face: FONT.body, color: C.sub, role: "ability" });
    if (i < rows.length - 1) shape(s, "rect", 96, y + 56, 1088, 1, "#F0EAE1");
  });
}

function slide5CapabilityMap(p) {
  const no = 5;
  const s = p.slides.add();
  bg(s);
  header(s, no, "能力全景");
  title(s, no, "产品能力地图", "覆盖记账主链路、数据保障和智能分析三大维度");

  const cards = [
    ["收支管理", "收入/支出双类型、23+分类、自定义分类、按日分组"],
    ["批量与筛选", "多选删除、批量改分类、关键词搜索、日期范围过滤"],
    ["预算控制", "月预算设置、进度提示、超支感知"],
    ["统计可视化", "趋势折线、分类占比、月历总览、横屏详情"],
    ["资产管理", "资产/负债/借出三类型、余额联动更新"],
    ["AI 分析", "健康评分、异常识别、行为洞察、行动建议"],
    ["备份同步", "WebDAV 上传下载、CSV 导入导出、定时备份"],
    ["隐私安全", "PIN + 生物识别、应用锁、失败冷却机制"],
  ];

  cards.forEach((c, i) => {
    const col = i % 4;
    const row = Math.floor(i / 4);
    const x = 72 + col * 288;
    const y = 252 + row * 178;
    bulletCard(s, no, x, y, 270, 160, c[0], c[1], [C.gold, C.purple, C.teal, C.blue][col]);
  });
}

function slide6CoreBusiness(p) {
  const no = 6;
  const s = p.slides.add();
  bg(s);
  header(s, no, "业务核心");
  title(s, no, "收支管理与资产联动", "“每一笔记录”与“资产余额”自动关联，减少手工对账成本");

  bulletCard(s, no, 72, 252, 364, 330, "记录能力", "支持收入/支出切换、分类与备注录入、日期选择、批量处理。\n\n首页按天分组，单日收支总额实时汇总。", C.gold);
  bulletCard(s, no, 458, 252, 364, 330, "资产联动", "支持资产/负债/借出三种账户类型。\n\n记账时可关联账户，金额自动同步到账户余额。", C.purple);
  bulletCard(s, no, 844, 252, 364, 330, "状态管理", "ExpenseViewModel 聚合列表、筛选、预算、主题、安全等状态，统一驱动 UI。", C.teal);

  metricCard(s, no, 72, 600, 260, 94, "23+", "内置分类", "支持用户自定义扩展", C.gold);
  metricCard(s, no, 350, 600, 260, 94, "2", "收支类型", "收入/支出一键切换", C.purple);
  metricCard(s, no, 628, 600, 260, 94, "3", "资产类型", "资产/负债/借出", C.teal);
  metricCard(s, no, 906, 600, 302, 94, "100%", "按日聚合展示", "快速回溯当天流水", C.blue);
}

function slide7StatsBudget(p) {
  const no = 7;
  const s = p.slides.add();
  bg(s);
  header(s, no, "统计分析");
  title(s, no, "统计分析与预算控制", "从流水记录升级到财务洞察，帮助用户做出更稳健的消费决策");

  glass(s, 72, 248, 760, 360);
  const chart = s.charts.add("bar");
  chart.position = { left: 96, top: 286, width: 710, height: 262 };
  chart.title = "统计能力覆盖（示意）";
  chart.categories = ["日汇总", "分类占比", "趋势分析", "预算追踪", "周期切换", "横屏详情"];
  const ser = chart.series.add("完备度");
  ser.values = [95, 92, 90, 88, 86, 80];
  chart.hasLegend = true;
  chart.legend.position = "bottom";
  chart.barOptions.direction = "column";

  inspectRecords.push({ kind: "chart", slide: no, role: "bar", bbox: [96, 286, 710, 262] });

  bulletCard(s, no, 854, 248, 354, 360, "关键收益", "• 预算进度可视化，超支风险可提前感知\n• 周/月/年切换，适配短期与长期复盘\n• 趋势图 + 分类占比，定位异常消费更直观", C.purple);
}

function slide8AI(p) {
  const no = 8;
  const s = p.slides.add();
  bg(s);
  header(s, no, "智能能力");
  title(s, no, "AI 分析与自然语言记账", "支持 OpenAI 兼容 API，兼顾“深度洞察”与“快速录入”两类场景");

  glass(s, 72, 248, 548, 360);
  text(s, no, "AI 分析链路", 96, 276, 240, 28, { size: 20, bold: true, face: FONT.title, color: C.purple, role: "ai-title" });
  text(s, no, "1. 选择时间范围\n2. 汇总 period / months / categories / assets\n3. 调用兼容 API 生成结构化解读\n4. 输出健康评分、风险提示、行动建议\n5. 分析结果持久化保存并可回顾", 96, 314, 500, 260, { size: 18, face: FONT.body, color: C.sub, role: "ai-flow" });

  glass(s, 648, 248, 560, 170);
  text(s, no, "自然语言记账", 672, 276, 220, 28, { size: 20, bold: true, face: FONT.title, color: C.gold, role: "nl-title" });
  text(s, no, "用户输入一句自然语言后，解析器输出严格 JSON items 列表，支持多笔拆分、字段容错与资产名关联。", 672, 314, 520, 84, { size: 17, face: FONT.body, color: C.sub, role: "nl-body" });

  glass(s, 648, 438, 560, 170);
  text(s, no, "接口稳健性", 672, 466, 220, 28, { size: 20, bold: true, face: FONT.title, color: C.teal, role: "api-title" });
  text(s, no, "AiApiClient 统一处理 baseUrl 规范化、模型列表拉取、超时设置（60/120/60s）、错误消息解析与模型优先级排序。", 672, 504, 520, 84, { size: 17, face: FONT.body, color: C.sub, role: "api-body" });
}

function slide9BackupSync(p) {
  const no = 9;
  const s = p.slides.add();
  bg(s);
  header(s, no, "数据韧性");
  title(s, no, "备份恢复与同步机制", "本地导出 + 云端备份 + 自动清理，保证“可恢复、可持续、可运维”");

  bulletCard(s, no, 72, 252, 360, 330, "WebDAV 能力", "支持连接测试、文件列表拉取、上传/下载/删除。\n支持自定义服务地址和目录路径。", C.gold);
  bulletCard(s, no, 456, 252, 360, 330, "本地能力", "支持 CSV 导出与导入。\n导出使用 UTF-8 BOM，兼容 Excel 直接打开。", C.purple);
  bulletCard(s, no, 840, 252, 368, 330, "自动化机制", "WorkManager 定时备份（1-72h）。\n自动备份支持防抖与互斥锁，并保留最新 3 份 auto_backup。", C.teal);

  metricCard(s, no, 72, 600, 356, 94, "1-72h", "可配置备份周期", "PeriodicWorkRequestBuilder", C.gold);
  metricCard(s, no, 456, 600, 356, 94, "3 份", "自动备份保留策略", "旧文件自动删除", C.purple);
  metricCard(s, no, 840, 600, 368, 94, "双路径", "本地+云端", "降低单点故障风险", C.teal);
}

function slide10Security(p) {
  const no = 10;
  const s = p.slides.add();
  bg(s);
  header(s, no, "安全与隐私");
  title(s, no, "隐私安全与访问控制", "围绕“数据保护 + 身份校验 + 风险缓释”构建信任底座");

  glass(s, 72, 252, 1136, 358);
  text(s, no, "安全模块", 98, 282, 220, 28, { size: 20, bold: true, face: FONT.title, color: C.purple, role: "sec-h1" });
  text(s, no, "实现细节", 416, 282, 340, 28, { size: 20, bold: true, face: FONT.title, color: C.purple, role: "sec-h2" });
  text(s, no, "价值", 848, 282, 220, 28, { size: 20, bold: true, face: FONT.title, color: C.purple, role: "sec-h3" });
  shape(s, "rect", 96, 318, 1088, 1, "#EBE5F8");

  const rows = [
    ["PIN 管理", "PinManager 使用 16 字节随机盐 + SHA-256 哈希，不保存明文 PIN。", "降低本地凭据泄露风险"],
    ["生物识别", "BiometricHelper 封装系统认证能力，支持指纹/面部（设备支持前提）。", "提升解锁便捷性与安全性"],
    ["应用锁状态", "AppLockManager 管理开关状态、验证流程与冷却策略。", "防止未授权访问账本"],
    ["配置隔离", "安全配置与主题/备份配置分层存储，避免耦合。", "提高可维护性与可审计性"],
  ];

  rows.forEach((r, i) => {
    const y = 340 + i * 66;
    text(s, no, r[0], 98, y, 270, 42, { size: 17, bold: true, face: FONT.body, color: C.text, role: "sec-cell" });
    text(s, no, r[1], 416, y, 390, 42, { size: 16, face: FONT.body, color: C.sub, role: "sec-cell" });
    text(s, no, r[2], 848, y, 320, 42, { size: 16, face: FONT.body, color: C.sub, role: "sec-cell" });
    if (i < rows.length - 1) shape(s, "rect", 96, y + 50, 1088, 1, "#F1EBE2");
  });
}

function slide11Flow(p) {
  const no = 11;
  const s = p.slides.add();
  bg(s);
  header(s, no, "业务闭环");
  title(s, no, "关键交互闭环", "从“记一笔”到“调策略”，构建可持续的个人财务管理循环");

  const steps = [
    ["记录", "输入金额/分类/备注，关联资产账户"],
    ["归集", "按日与按类自动汇总，形成结构化账单"],
    ["监控", "预算进度与异常波动实时可见"],
    ["复盘", "趋势图与类别占比帮助定位问题"],
    ["行动", "AI 生成建议并形成后续执行清单"],
  ];

  steps.forEach((st, i) => {
    const x = 52 + i * 246;
    glass(s, x, 360, 224, 190);
    shape(s, "ellipse", x + 18, 380, 36, 36, C.purple);
    text(s, no, `${i + 1}`, x + 32, 389, 10, 14, { size: 14, bold: true, face: FONT.mono, color: "#FFFFFF", role: "step-num" });
    text(s, no, st[0], x + 64, 382, 130, 30, { size: 24, bold: true, face: FONT.title, color: C.text, role: "step-title" });
    text(s, no, st[1], x + 18, 430, 188, 96, { size: 16, face: FONT.body, color: C.sub, role: "step-body" });
    if (i < steps.length - 1) {
      shape(s, "rightArrow", x + 224, 440, 20, 18, C.gold);
    }
  });
}

function slide12Architecture(p) {
  const no = 12;
  const s = p.slides.add();
  bg(s);
  header(s, no, "技术架构");
  title(s, no, "技术架构分层", "MVVM + Repository + Room + Compose，形成清晰的数据与状态流");

  const cols = [
    ["表现层 UI", ["Compose Screen", "组件化表单", "统计图与小组件"]],
    ["状态与业务", ["ExpenseViewModel", "StateFlow 聚合", "过滤/预算/AI 状态"]],
    ["数据与基础设施", ["Repository", "Room/DAO", "WebDAV/AI/WorkManager"]],
  ];

  cols.forEach((c, i) => {
    const x = 78 + i * 386;
    const accent = [C.gold, C.purple, C.teal][i];
    glass(s, x, 260, 360, 360);
    shape(s, "rect", x, 260, 360, 8, accent);
    text(s, no, c[0], x + 22, 288, 300, 30, { size: 23, bold: true, face: FONT.title, color: C.text, role: "arch-title" });
    c[1].forEach((item, j) => {
      shape(s, "ellipse", x + 24, 338 + j * 82, 10, 10, accent);
      text(s, no, item, x + 44, 330 + j * 82, 292, 34, { size: 18, face: FONT.body, color: C.sub, role: "arch-item" });
    });
  });

  shape(s, "rightArrow", 438, 426, 24, 18, C.purple);
  shape(s, "rightArrow", 824, 426, 24, 18, C.purple);
}

function slide13Database(p) {
  const no = 13;
  const s = p.slides.add();
  bg(s);
  header(s, no, "数据模型");
  title(s, no, "数据库模型与演进", "Room v6 + 5 次迁移，保证历史数据平滑升级");

  glass(s, 72, 248, 1136, 360);
  const versions = [
    ["v1→v2", "expenses 增加 category 字段"],
    ["v2→v3", "expenses 增加 type（收入/支出）"],
    ["v3→v4", "新增 assets 表支持资产管理"],
    ["v4→v5", "expenses 增加 assetId 关联"],
    ["v5→v6", "新增 ai_analyses 表持久化分析记录"],
  ];
  versions.forEach((v, i) => {
    const y = 282 + i * 64;
    shape(s, "ellipse", 96, y + 10, 12, 12, [C.gold, C.purple, C.teal, C.blue, C.red][i]);
    text(s, no, v[0], 118, y, 140, 26, { size: 18, bold: true, face: FONT.mono, color: C.text, role: "db-v" });
    text(s, no, v[1], 280, y, 900, 30, { size: 18, face: FONT.body, color: C.sub, role: "db-desc" });
  });

  glass(s, 72, 628, 360, 70);
  text(s, no, "核心实体：ExpenseEntity", 92, 650, 320, 24, { size: 15, bold: true, face: FONT.body, color: C.text, role: "entity" });
  glass(s, 460, 628, 360, 70);
  text(s, no, "核心实体：AssetEntity", 480, 650, 320, 24, { size: 15, bold: true, face: FONT.body, color: C.text, role: "entity" });
  glass(s, 848, 628, 360, 70);
  text(s, no, "核心实体：AiAnalysisEntity", 868, 650, 320, 24, { size: 15, bold: true, face: FONT.body, color: C.text, role: "entity" });
}

function slide14Engineering(p) {
  const no = 14;
  const s = p.slides.add();
  bg(s);
  header(s, no, "工程质量");
  title(s, no, "工程能力与质量保障", "技术栈完整、模块职责清晰，支撑持续迭代和规模扩展");

  glass(s, 72, 248, 680, 360);
  text(s, no, "关键依赖（摘选）", 96, 276, 220, 28, { size: 20, bold: true, face: FONT.title, color: C.purple, role: "deps-title" });
  text(s, no, "• Kotlin 2.0.21\n• Compose BOM 2024.12.01\n• Room 2.6.1\n• Lifecycle 2.8.7\n• WorkManager 2.10.0\n• OkHttp 4.12.0\n• AndroidX Biometric 1.1.0\n• Jetpack Glance 1.1.1", 96, 316, 640, 270, { size: 18, face: FONT.body, color: C.sub, role: "deps-list" });

  glass(s, 776, 248, 432, 360);
  const chart = s.charts.add("pie");
  chart.position = { left: 810, top: 300, width: 360, height: 250 };
  chart.title = "模块复杂度占比（示意）";
  const se = chart.series.add("占比");
  se.categories = ["UI层", "业务层", "数据层", "安全与备份", "系统集成"];
  se.values = [30, 25, 20, 15, 10];
  chart.hasLegend = true;
  chart.legend.position = "bottom";

  inspectRecords.push({ kind: "chart", slide: no, role: "pie", bbox: [810, 300, 360, 250] });
}

function slide15Roadmap(p) {
  const no = 15;
  const s = p.slides.add();
  bg(s);
  header(s, no, "路线图");
  title(s, no, "路线图与总结", "以稳定能力为底座，持续提升智能化、自动化与跨生态协同");

  glass(s, 72, 248, 1136, 340);
  const roadmap = [
    ["Q2", "体验优化", "优化记账路径、筛选效率与可视化细节"],
    ["Q3", "智能增强", "提升 AI 分析准确性与解释可读性"],
    ["Q4", "生态协同", "增强与外部账单来源的同步能力"],
    ["Q1", "平台扩展", "探索多端协同与更强数据洞察能力"],
  ];
  roadmap.forEach((r, i) => {
    const x = 92 + i * 282;
    bulletCard(s, no, x, 280, 252, 280, r[0], `${r[1]}\n\n${r[2]}`, [C.gold, C.purple, C.teal, C.blue][i]);
  });

  glass(s, 72, 606, 1136, 94);
  text(s, no, "总结：MD3 Expense Tracker 已具备“记账基础能力 + 数据洞察能力 + 安全备份能力 + 智能分析能力”的完整闭环，具备持续演进为个人财务助手的产品基础。", 96, 634, 1090, 52, { size: 20, face: FONT.body, color: C.text, role: "summary" });
}

function createDeck() {
  const p = Presentation.create({ slideSize: { width: W, height: H } });
  slide1(p);
  slide2Agenda(p);
  slide3Background(p);
  slide4PainValue(p);
  slide5CapabilityMap(p);
  slide6CoreBusiness(p);
  slide7StatsBudget(p);
  slide8AI(p);
  slide9BackupSync(p);
  slide10Security(p);
  slide11Flow(p);
  slide12Architecture(p);
  slide13Database(p);
  slide14Engineering(p);
  slide15Roadmap(p);
  return p;
}

async function ensureDirs() {
  await fs.mkdir(OUT_DIR, { recursive: true });
  await fs.mkdir(SCRATCH_DIR, { recursive: true });
  await fs.mkdir(PREVIEW_DIR, { recursive: true });
}

async function saveBlob(blob, outputPath) {
  const bytes = new Uint8Array(await blob.arrayBuffer());
  await fs.writeFile(outputPath, bytes);
}

async function writeInspect(p) {
  const lines = [
    JSON.stringify({ kind: "deck", id: DECK_ID, slideCount: p.slides.count, slideSize: { width: W, height: H } }),
    ...inspectRecords.map((r) => JSON.stringify(r)),
  ].join("\n") + "\n";
  await fs.writeFile(INSPECT_PATH, lines, "utf8");
}

async function renderExport(p) {
  await ensureDirs();
  await writeInspect(p);

  for (let i = 0; i < p.slides.items.length; i += 1) {
    const png = await p.export({ slide: p.slides.items[i], format: "png", scale: 1 });
    await saveBlob(png, path.join(PREVIEW_DIR, `slide-${String(i + 1).padStart(2, "0")}.png`));
  }

  const pptx = await PresentationFile.exportPptx(p);
  const out = path.join(OUT_DIR, "output.pptx");
  await pptx.save(out);
  return out;
}

const deck = createDeck();
const outPath = await renderExport(deck);
console.log(outPath);
