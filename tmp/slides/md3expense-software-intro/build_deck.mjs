const fs = await import("node:fs/promises");
const path = await import("node:path");
const { Presentation, PresentationFile } = await import("@oai/artifact-tool");

const W = 1280;
const H = 720;

const DECK_ID = "md3expense-software-intro";
const OUT_DIR = "E:\\AI\\ai projects\\md3ExpenseTracker\\outputs\\md3expense-software-intro";
const SCRATCH_DIR = path.resolve(path.join("E:\\AI\\ai projects\\md3ExpenseTracker\\tmp\\slides", DECK_ID));
const PREVIEW_DIR = path.join(SCRATCH_DIR, "preview");
const INSPECT_PATH = path.join(SCRATCH_DIR, "inspect.ndjson");

const C = {
  bg: "#F5F8FF",
  panel: "#FFFFFF",
  panelSoft: "#EEF4FF",
  text: "#0F172A",
  sub: "#334155",
  mute: "#64748B",
  accent: "#2563EB",
  accent2: "#0EA5E9",
  good: "#16A34A",
  warn: "#EA580C",
  border: "#DCE5F3",
};

const FONT = {
  title: "Poppins",
  body: "Lato",
  mono: "Aptos Mono",
};

const inspectRecords = [];

const SLIDES = [
  {
    kicker: "MD3 EXPENSE TRACKER",
    title: "MD3 Expense Tracker\nAndroid 本地记账软件介绍",
    subtitle: "面向个人用户的高颜值、强功能、可持续迭代的财务管理应用",
    tag: "Kotlin · Compose · Room · MVVM",
    notes: "开场聚焦产品定位与核心价值。",
  },
  {
    kicker: "产品定位",
    title: "为什么要做这款记账软件",
    subtitle: "目标是把“记录、分析、决策”做成低门槛的每日财务闭环",
    cards: [
      ["记录成本高", "传统记账步骤繁琐，容易中断，长期坚持困难。"],
      ["复盘效率低", "数据分散在流水中，缺少趋势与结构化洞察。"],
      ["安全感不足", "用户既要云同步，也要本地可控与隐私保护。"],
    ],
    notes: "先讲痛点，再过渡到解决方案。",
  },
  {
    kicker: "核心能力",
    title: "核心能力全景",
    subtitle: "覆盖从日常记账到长期资产管理的关键场景",
    metrics: [
      ["23+", "内置分类", "含餐饮/交通/购物/薪资等"],
      ["6", "主题配色", "支持动态取色与深浅模式"],
      ["1-72h", "自动备份周期", "WorkManager 后台调度"],
      ["30min", "小组件刷新", "桌面直达本月支出"],
    ],
    notes: "强调能力广度与体验细节。",
  },
  {
    kicker: "智能分析",
    title: "AI 财务分析能力",
    subtitle: "接入 OpenAI 兼容 API，输出健康评分、风险提示与行动清单",
    cards: [
      ["健康评分", "基于收支结构和波动特征生成 0-100 综合评分。"],
      ["深度洞察", "识别异常消费、行为模式与阶段性变化趋势。"],
      ["行动建议", "给出可执行、按优先级排序的改善方案。"],
    ],
    notes: "说明 AI 价值不止结论，更在于行动闭环。",
  },
  {
    kicker: "数据安全",
    title: "隐私与数据安全设计",
    subtitle: "本地优先 + 多路径备份 + 身份验证，确保数据可控可靠",
    cards: [
      ["本地数据库", "使用 Room 持久化核心数据，离线可用、响应稳定。"],
      ["双通道备份", "支持 WebDAV 与本地 CSV 导出，降低单点风险。"],
      ["应用锁", "PIN + 生物识别，防止未授权访问账本。"],
    ],
    notes: "强调用户对“可恢复”和“可防护”的真实需求。",
  },
  {
    kicker: "系统架构",
    title: "技术架构与模块分层",
    subtitle: "单 Activity + MVVM，按数据、业务、界面职责解耦",
    notes: "说明结构清晰、便于维护和演进。",
  },
  {
    kicker: "工程实现",
    title: "技术栈与工程质量",
    subtitle: "现代 Android 技术体系，兼顾开发效率与稳定性",
    chartTitle: "核心技术模块覆盖度（示意）",
    notes: "通过图表强调技术构成的完整性。",
  },
  {
    kicker: "业务流程",
    title: "典型使用流程",
    subtitle: "从记录到复盘的闭环路径，帮助用户形成财务管理习惯",
    steps: ["快速记账", "分类归集", "预算追踪", "统计复盘", "AI 建议执行"],
    notes: "用流程展示产品的连续价值。",
  },
  {
    kicker: "总结与规划",
    title: "当前成果与后续路线",
    subtitle: "在稳定可用基础上持续增强智能化和生态连接能力",
    cards: [
      ["已具备", "完整记账能力、统计分析、资产管理、备份同步与安全防护。"],
      ["下一步", "优化多设备体验，增强智能提醒与异常事件预警。"],
      ["长期方向", "构建更主动的个人财务助手，实现自动化决策支持。"],
    ],
    notes: "收束全局，给出明确发展方向。",
  },
];

function line(fill = "#00000000", width = 0) {
  return { style: "solid", fill, width };
}

function addShape(slide, geo, left, top, width, height, fill = "#00000000", stroke = "#00000000", strokeW = 0) {
  return slide.shapes.add({
    geometry: geo,
    position: { left, top, width, height },
    fill,
    line: line(stroke, strokeW),
  });
}

function addText(slide, slideNo, text, left, top, width, height, opts = {}) {
  const box = addShape(slide, "rect", left, top, width, height, opts.fill || "#00000000", opts.stroke || "#00000000", opts.strokeW || 0);
  box.text = text;
  box.text.fontSize = opts.size ?? 20;
  box.text.bold = opts.bold ?? false;
  box.text.color = opts.color ?? C.text;
  box.text.typeface = opts.face ?? FONT.body;
  box.text.alignment = opts.align ?? "left";
  box.text.verticalAlignment = opts.valign ?? "top";
  box.text.insets = { left: 0, right: 0, top: 0, bottom: 0 };

  inspectRecords.push({
    kind: "textbox",
    slide: slideNo,
    role: opts.role || "text",
    text: String(text),
    textChars: String(text).length,
    textLines: String(text).split(/\n/).length,
    bbox: [left, top, width, height],
  });
  return box;
}

function addBg(slide) {
  slide.background.fill = C.bg;
  addShape(slide, "ellipse", 980, -120, 480, 380, "#DBEAFE", "#00000000", 0);
  addShape(slide, "ellipse", -140, 500, 420, 280, "#E0F2FE", "#00000000", 0);
}

function addHeader(slide, slideNo, kicker) {
  addText(slide, slideNo, kicker, 68, 26, 500, 24, { size: 12, bold: true, face: FONT.mono, color: C.accent, role: "header" });
  addText(slide, slideNo, `${String(slideNo).padStart(2, "0")} / 09`, 1110, 26, 100, 24, { size: 12, bold: true, face: FONT.mono, color: C.accent, align: "right", role: "header-page" });
  addShape(slide, "rect", 64, 56, 1152, 2, C.border, "#00000000", 0);
}

function addTitle(slide, slideNo, title, subtitle) {
  addText(slide, slideNo, title, 68, 84, 860, 110, { size: 44, bold: true, face: FONT.title, color: C.text, role: "title" });
  if (subtitle) {
    addText(slide, slideNo, subtitle, 70, 205, 860, 56, { size: 21, face: FONT.body, color: C.sub, role: "subtitle" });
  }
}

function addCard(slide, slideNo, x, y, w, h, title, body, accent = C.accent) {
  addShape(slide, "roundRect", x, y, w, h, C.panel, C.border, 1.2);
  addShape(slide, "rect", x, y, 7, h, accent, "#00000000", 0);
  addText(slide, slideNo, title, x + 22, y + 16, w - 32, 26, { size: 17, bold: true, face: FONT.title, color: C.text, role: "card-title" });
  addText(slide, slideNo, body, x + 22, y + 52, w - 36, h - 70, { size: 16, face: FONT.body, color: C.sub, role: "card-body" });
}

function addMetric(slide, slideNo, x, y, w, h, value, label, note, color) {
  addShape(slide, "roundRect", x, y, w, h, C.panel, C.border, 1.2);
  addShape(slide, "rect", x, y, w, 6, color, "#00000000", 0);
  addText(slide, slideNo, value, x + 18, y + 20, w - 30, 48, { size: 34, bold: true, face: FONT.title, color: C.text, role: "metric-value" });
  addText(slide, slideNo, label, x + 18, y + 72, w - 30, 26, { size: 16, face: FONT.body, color: C.sub, role: "metric-label" });
  addText(slide, slideNo, note, x + 18, y + 100, w - 30, 20, { size: 12, face: FONT.body, color: C.mute, role: "metric-note" });
}

function addNotes(slide, text) {
  slide.speakerNotes.setText(text);
}

function slide1(p) {
  const n = 1;
  const s = p.slides.add();
  const d = SLIDES[0];
  addBg(s);
  addShape(s, "roundRect", 58, 72, 1164, 576, "#FFFFFFD9", "#D7E3F4", 1);
  addHeader(s, n, d.kicker);
  addTitle(s, n, d.title, d.subtitle);
  addShape(s, "roundRect", 72, 286, 560, 88, C.panelSoft, "#CFE0FF", 1);
  addText(s, n, d.tag, 94, 315, 520, 30, { size: 18, bold: true, face: FONT.mono, color: C.accent, role: "tag" });
  addShape(s, "roundRect", 72, 400, 1128, 220, C.panel, C.border, 1.2);
  addText(s, n, "一句话定位", 96, 426, 220, 28, { size: 16, bold: true, face: FONT.title, color: C.accent, role: "定位标签" });
  addText(s, n, "MD3 Expense Tracker 是一款以本地优先为核心、兼具智能分析能力的 Android 记账应用，帮助用户把日常流水沉淀为可执行的财务决策。", 96, 462, 1080, 118, { size: 24, face: FONT.body, color: C.text, role: "定位说明" });
  addNotes(s, d.notes);
}

function slideCards(p, n, palette = [C.accent, C.accent2, C.good]) {
  const s = p.slides.add();
  const d = SLIDES[n - 1];
  addBg(s);
  addHeader(s, n, d.kicker);
  addTitle(s, n, d.title, d.subtitle);
  const y = 300;
  const w = 356;
  d.cards.forEach((c, i) => {
    addCard(s, n, 68 + i * (w + 22), y, w, 300, c[0], c[1], palette[i % palette.length]);
  });
  addNotes(s, d.notes);
}

function slideMetrics(p) {
  const n = 3;
  const s = p.slides.add();
  const d = SLIDES[2];
  addBg(s);
  addHeader(s, n, d.kicker);
  addTitle(s, n, d.title, d.subtitle);

  const colors = [C.accent, C.accent2, C.good, C.warn];
  d.metrics.forEach((m, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    addMetric(s, n, 68 + col * 574, 298 + row * 170, 550, 152, m[0], m[1], m[2], colors[i]);
  });
  addNotes(s, d.notes);
}

function slideArchitecture(p) {
  const n = 6;
  const s = p.slides.add();
  const d = SLIDES[5];
  addBg(s);
  addHeader(s, n, d.kicker);
  addTitle(s, n, d.title, d.subtitle);

  const cols = [
    { t: "数据层", items: ["Room 数据库", "DAO + Repository", "WebDAV / AI / 同步服务"] },
    { t: "业务层", items: ["ExpenseViewModel", "预算与统计聚合", "备份与安全策略"] },
    { t: "表现层", items: ["Compose Screens", "组件化 UI", "桌面小组件"] },
  ];

  cols.forEach((col, i) => {
    const x = 82 + i * 392;
    addShape(s, "roundRect", x, 312, 352, 280, C.panel, C.border, 1.2);
    addText(s, n, col.t, x + 22, 332, 300, 32, { size: 21, bold: true, face: FONT.title, color: C.accent, role: "架构列标题" });
    col.items.forEach((item, idx) => {
      addShape(s, "ellipse", x + 24, 378 + idx * 62, 10, 10, C.accent2, "#00000000", 0);
      addText(s, n, item, x + 44, 370 + idx * 62, 286, 28, { size: 17, face: FONT.body, color: C.text, role: "架构项" });
    });
  });
  addNotes(s, d.notes);
}

function slideTechChart(p) {
  const n = 7;
  const s = p.slides.add();
  const d = SLIDES[6];
  addBg(s);
  addHeader(s, n, d.kicker);
  addTitle(s, n, d.title, d.subtitle);

  addShape(s, "roundRect", 68, 292, 1144, 324, C.panel, C.border, 1.2);

  const chart = s.charts.add("bar");
  chart.position = { left: 100, top: 330, width: 740, height: 245 };
  chart.title = d.chartTitle;
  chart.categories = ["UI", "数据存储", "后台任务", "网络同步", "安全能力", "智能分析"];

  const series = chart.series.add("成熟度");
  series.values = [92, 88, 85, 80, 84, 78];

  chart.hasLegend = true;
  chart.legend.position = "bottom";

  chart.barOptions.direction = "column";

  addShape(s, "roundRect", 872, 332, 322, 244, C.panelSoft, "#CFDBF9", 1);
  addText(s, n, "技术亮点", 896, 354, 200, 30, { size: 20, bold: true, face: FONT.title, color: C.accent, role: "tech-highlights-title" });
  addText(s, n, "• Kotlin + Compose 提升开发效率\n• Room + Flow 支持响应式数据更新\n• WorkManager 实现可靠后台任务\n• 生物识别与 PIN 提升访问安全", 896, 392, 274, 168, { size: 16, face: FONT.body, color: C.text, role: "tech-highlights-body" });

  inspectRecords.push({ kind: "chart", slide: n, role: "bar-chart", bbox: [100, 330, 740, 245] });
  addNotes(s, d.notes);
}

function slideFlow(p) {
  const n = 8;
  const s = p.slides.add();
  const d = SLIDES[7];
  addBg(s);
  addHeader(s, n, d.kicker);
  addTitle(s, n, d.title, d.subtitle);

  const y = 396;
  const stepW = 210;
  d.steps.forEach((step, idx) => {
    const x = 62 + idx * 238;
    addShape(s, "roundRect", x, y, stepW, 132, C.panel, C.border, 1.2);
    addShape(s, "ellipse", x + 18, y + 18, 34, 34, C.accent, "#00000000", 0);
    addText(s, n, `${idx + 1}`, x + 30, y + 26, 14, 18, { size: 14, bold: true, face: FONT.mono, color: "#FFFFFF", role: "流程序号" });
    addText(s, n, step, x + 64, y + 20, 136, 32, { size: 18, bold: true, face: FONT.title, color: C.text, role: "流程标题" });
    addText(s, n, idx === 0 ? "随手录入收支" : idx === 1 ? "自动归类与关联" : idx === 2 ? "预算进度可视化" : idx === 3 ? "趋势与结构复盘" : "形成行动清单", x + 20, y + 60, 172, 56, { size: 14, face: FONT.body, color: C.sub, role: "流程说明" });

    if (idx < d.steps.length - 1) {
      addShape(s, "rightArrow", x + 210, y + 54, 26, 20, C.accent2, "#00000000", 0);
    }
  });
  addNotes(s, d.notes);
}

function createDeck() {
  const p = Presentation.create({ slideSize: { width: W, height: H } });
  slide1(p);
  slideCards(p, 2);
  slideMetrics(p);
  slideCards(p, 4, [C.accent2, C.good, C.warn]);
  slideCards(p, 5, [C.good, C.accent, C.accent2]);
  slideArchitecture(p);
  slideTechChart(p);
  slideFlow(p);
  slideCards(p, 9, [C.accent, C.accent2, C.good]);
  return p;
}

async function ensureDirs() {
  await fs.mkdir(OUT_DIR, { recursive: true });
  await fs.mkdir(SCRATCH_DIR, { recursive: true });
  await fs.mkdir(PREVIEW_DIR, { recursive: true });
}

async function saveBlobToFile(blob, filePath) {
  const bytes = new Uint8Array(await blob.arrayBuffer());
  await fs.writeFile(filePath, bytes);
}

async function writeInspect(p) {
  const lines = [
    JSON.stringify({ kind: "deck", id: DECK_ID, slideCount: p.slides.count, slideSize: { width: W, height: H } }),
    ...inspectRecords.map((r) => JSON.stringify(r)),
  ].join("\n") + "\n";
  await fs.writeFile(INSPECT_PATH, lines, "utf8");
}

async function renderAndExport(p) {
  await ensureDirs();
  await writeInspect(p);

  for (let i = 0; i < p.slides.items.length; i += 1) {
    const slide = p.slides.items[i];
    const png = await p.export({ slide, format: "png", scale: 1 });
    await saveBlobToFile(png, path.join(PREVIEW_DIR, `slide-${String(i + 1).padStart(2, "0")}.png`));
  }

  const pptx = await PresentationFile.exportPptx(p);
  const out = path.join(OUT_DIR, "output.pptx");
  await pptx.save(out);
  return out;
}

const presentation = createDeck();
const outPath = await renderAndExport(presentation);
console.log(outPath);
