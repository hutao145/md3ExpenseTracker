const fs = await import("node:fs/promises");
const path = await import("node:path");
const { Presentation, PresentationFile } = await import("@oai/artifact-tool");

const W = 1280;
const H = 720;

const DECK_ID = "md3expense-software-intro-polished";
const OUT_DIR = "E:\\AI\\ai projects\\md3ExpenseTracker\\outputs\\md3expense-software-intro-polished";
const SCRATCH_DIR = path.resolve(path.join("E:\\AI\\ai projects\\md3ExpenseTracker\\tmp\\slides", DECK_ID));
const PREVIEW_DIR = path.join(SCRATCH_DIR, "preview");
const INSPECT_PATH = path.join(SCRATCH_DIR, "inspect.ndjson");

const C = {
  bg0: "#FFFDF8",
  bg1: "#F6F3FF",
  bg2: "#EEF5FF",
  panel: "#FFFFFFE8",
  panel2: "#FFFFFFCC",
  border: "#E8DCC6",
  text: "#1E1B2E",
  sub: "#4B5563",
  mute: "#7C8798",
  gold: "#F59E0B",
  gold2: "#FBBF24",
  purple: "#8B5CF6",
  teal: "#14B8A6",
  pink: "#EC4899",
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
    title: "MD3 Expense Tracker",
    subtitle: "Android 本地记账软件介绍",
    desc: "面向个人用户的高颜值、强功能、可持续迭代的财务管理应用",
    foot: "Kotlin · Jetpack Compose · Room · MVVM",
  },
  {
    kicker: "产品定位",
    title: "为什么要做这款记账软件",
    subtitle: "把记录、分析、决策做成低门槛的日常财务闭环",
    cards: [
      ["记录成本高", "很多记账软件录入路径复杂，导致用户难以长期坚持。"],
      ["复盘效率低", "只有流水没有洞察，用户无法快速理解消费结构。"],
      ["安全诉求强", "用户既希望云备份，也希望本地数据完全可控。"],
    ],
  },
  {
    kicker: "核心能力",
    title: "核心能力全景",
    subtitle: "覆盖记账、预算、统计、资产、安全与同步等关键场景",
    metrics: [
      ["23+", "内置分类", "餐饮/交通/购物/薪资等"],
      ["6", "主题方案", "支持动态取色与深浅模式"],
      ["1-72h", "自动备份", "后台任务按周期执行"],
      ["30min", "组件刷新", "桌面直接查看本月支出"],
    ],
  },
  {
    kicker: "智能分析",
    title: "AI 财务分析能力",
    subtitle: "从“看数据”进化到“给建议”，提升决策效率",
    cards: [
      ["健康评分", "基于收支结构和波动特征输出 0-100 综合评分。"],
      ["深度洞察", "识别异常消费、习惯模式和阶段性风险点。"],
      ["行动清单", "给出优先级明确、可立即执行的改善建议。"],
    ],
  },
  {
    kicker: "数据安全",
    title: "隐私与数据安全设计",
    subtitle: "本地优先 + 双路径备份 + 身份认证，保障账本资产安全",
    cards: [
      ["本地优先", "核心数据存储于本地 Room，离线也能稳定使用。"],
      ["双通道备份", "支持 WebDAV 与 CSV 导入导出，提升可恢复能力。"],
      ["访问保护", "PIN 与生物识别联动，防止未授权访问。"],
    ],
  },
  {
    kicker: "系统架构",
    title: "技术架构与模块分层",
    subtitle: "单 Activity + MVVM，按职责拆分为数据、业务、表现三层",
  },
  {
    kicker: "工程实现",
    title: "技术栈与工程质量",
    subtitle: "现代 Android 技术体系，兼顾开发效率、稳定性与可维护性",
  },
  {
    kicker: "业务流程",
    title: "典型使用流程",
    subtitle: "从记录到复盘再到优化建议，形成可持续财务管理习惯",
    steps: ["快速记账", "分类归集", "预算追踪", "统计复盘", "AI建议执行"],
  },
  {
    kicker: "总结与规划",
    title: "当前成果与后续路线",
    subtitle: "在稳定可用基础上持续增强智能化和生态协同能力",
    cards: [
      ["已完成", "记账、统计、资产管理、备份同步与应用安全能力已形成闭环。"],
      ["短期计划", "继续优化多设备体验与异常消费提醒能力。"],
      ["长期方向", "构建更主动的个人财务助手，强化自动化决策支持。"],
    ],
  },
];

function line(fill = "#00000000", width = 0) {
  return { style: "solid", fill, width };
}

function addShape(slide, geometry, left, top, width, height, fill = "#00000000", stroke = "#00000000", strokeW = 0) {
  return slide.shapes.add({ geometry, position: { left, top, width, height }, fill, line: line(stroke, strokeW) });
}

function addText(slide, slideNo, text, left, top, width, height, opt = {}) {
  const s = addShape(slide, "rect", left, top, width, height, opt.fill || "#00000000", opt.stroke || "#00000000", opt.strokeW || 0);
  s.text = text;
  s.text.fontSize = opt.size ?? 18;
  s.text.bold = opt.bold ?? false;
  s.text.color = opt.color ?? C.text;
  s.text.typeface = opt.face ?? FONT.body;
  s.text.alignment = opt.align ?? "left";
  s.text.verticalAlignment = opt.valign ?? "top";
  s.text.insets = { left: 0, right: 0, top: 0, bottom: 0 };

  inspectRecords.push({
    kind: "textbox",
    slide: slideNo,
    role: opt.role || "text",
    text: String(text),
    textChars: String(text).length,
    textLines: String(text).split(/\n/).length,
    bbox: [left, top, width, height],
  });
  return s;
}

function addBackground(slide) {
  slide.background.fill = C.bg0;
  addShape(slide, "ellipse", -180, -140, 560, 420, C.bg1);
  addShape(slide, "ellipse", 930, -170, 520, 420, C.bg2);
  addShape(slide, "ellipse", -120, 500, 500, 300, "#FFF2D8");
}

function addGlassPanel(slide, x, y, w, h) {
  addShape(slide, "roundRect", x, y, w, h, C.panel, C.border, 1);
}

function addHeader(slide, no, kicker) {
  addText(slide, no, kicker, 72, 30, 540, 22, { size: 12, bold: true, face: FONT.mono, color: C.purple, role: "header" });
  addText(slide, no, `${String(no).padStart(2, "0")} / 09`, 1086, 30, 130, 22, { size: 12, bold: true, face: FONT.mono, color: C.purple, align: "right", role: "page" });
  addShape(slide, "rect", 64, 58, 1152, 2, "#E9E4F8");
}

function addTitle(slide, no, title, subtitle) {
  addText(slide, no, title, 72, 90, 900, 78, { size: 50, bold: true, face: FONT.title, color: C.text, role: "title" });
  if (subtitle) {
    addText(slide, no, subtitle, 74, 184, 900, 42, { size: 22, face: FONT.body, color: C.sub, role: "subtitle" });
  }
}

function addCard(slide, no, x, y, w, h, t, b, accent) {
  addGlassPanel(slide, x, y, w, h);
  addShape(slide, "rect", x, y, 8, h, accent);
  addText(slide, no, t, x + 24, y + 16, w - 34, 26, { size: 20, bold: true, face: FONT.title, color: C.text, role: "card-title" });
  addText(slide, no, b, x + 24, y + 54, w - 36, h - 70, { size: 17, face: FONT.body, color: C.sub, role: "card-body" });
}

function addMetric(slide, no, x, y, w, h, v, l, n, accent) {
  addGlassPanel(slide, x, y, w, h);
  addShape(slide, "rect", x, y, w, 6, accent);
  addText(slide, no, v, x + 20, y + 18, w - 30, 44, { size: 36, bold: true, face: FONT.title, color: C.text, role: "metric-value" });
  addText(slide, no, l, x + 20, y + 66, w - 30, 24, { size: 16, bold: true, face: FONT.body, color: C.sub, role: "metric-label" });
  addText(slide, no, n, x + 20, y + 95, w - 30, 20, { size: 12, face: FONT.body, color: C.mute, role: "metric-note" });
}

function cover(p) {
  const no = 1;
  const d = SLIDES[0];
  const s = p.slides.add();
  addBackground(s);
  addGlassPanel(s, 52, 72, 1176, 578);
  addHeader(s, no, d.kicker);
  addText(s, no, d.title, 86, 120, 760, 70, { size: 58, bold: true, face: FONT.title, color: C.text, role: "cover-title" });
  addText(s, no, d.subtitle, 88, 196, 760, 54, { size: 48, bold: true, face: FONT.title, color: C.purple, role: "cover-subtitle" });
  addText(s, no, d.desc, 90, 274, 760, 62, { size: 24, face: FONT.body, color: C.sub, role: "cover-desc" });

  addGlassPanel(s, 90, 370, 520, 90);
  addText(s, no, d.foot, 112, 404, 470, 26, { size: 18, bold: true, face: FONT.mono, color: C.gold, role: "cover-foot" });

  addShape(s, "roundRect", 860, 156, 310, 420, C.panel2, C.border, 1);
  addText(s, no, "产品价值", 892, 188, 220, 34, { size: 24, bold: true, face: FONT.title, color: C.text, role: "cover-side-title" });
  addText(s, no, "• 本地优先\n• 智能分析\n• 安全可控\n• 持续迭代", 892, 236, 220, 200, { size: 20, face: FONT.body, color: C.sub, role: "cover-side-body" });
}

function cardSlide(p, no, accents = [C.gold, C.purple, C.teal]) {
  const d = SLIDES[no - 1];
  const s = p.slides.add();
  addBackground(s);
  addHeader(s, no, d.kicker);
  addTitle(s, no, d.title, d.subtitle);

  const y = 290;
  const w = 360;
  d.cards.forEach((c, i) => {
    addCard(s, no, 68 + i * (w + 18), y, w, 314, c[0], c[1], accents[i % accents.length]);
  });
}

function metricSlide(p) {
  const no = 3;
  const d = SLIDES[2];
  const s = p.slides.add();
  addBackground(s);
  addHeader(s, no, d.kicker);
  addTitle(s, no, d.title, d.subtitle);

  const ac = [C.gold, C.purple, C.teal, C.pink];
  d.metrics.forEach((m, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    addMetric(s, no, 68 + col * 574, 292 + row * 164, 554, 146, m[0], m[1], m[2], ac[i]);
  });
}

function architectureSlide(p) {
  const no = 6;
  const d = SLIDES[5];
  const s = p.slides.add();
  addBackground(s);
  addHeader(s, no, d.kicker);
  addTitle(s, no, d.title, d.subtitle);

  const cols = [
    { t: "数据层", c: C.gold, items: ["Room 数据库", "DAO + Repository", "WebDAV / AI / 同步"] },
    { t: "业务层", c: C.purple, items: ["ExpenseViewModel", "预算与统计聚合", "备份与安全策略"] },
    { t: "表现层", c: C.teal, items: ["Compose Screen", "组件化 UI", "Glance 小组件"] },
  ];

  cols.forEach((col, i) => {
    const x = 76 + i * 386;
    addGlassPanel(s, x, 292, 360, 302);
    addShape(s, "rect", x, 292, 360, 8, col.c);
    addText(s, no, col.t, x + 24, 320, 180, 32, { size: 24, bold: true, face: FONT.title, color: C.text, role: "arch-title" });
    col.items.forEach((it, j) => {
      addShape(s, "ellipse", x + 26, 372 + j * 64, 10, 10, col.c);
      addText(s, no, it, x + 46, 365 + j * 64, 292, 30, { size: 18, face: FONT.body, color: C.sub, role: "arch-item" });
    });
  });
}

function techSlide(p) {
  const no = 7;
  const d = SLIDES[6];
  const s = p.slides.add();
  addBackground(s);
  addHeader(s, no, d.kicker);
  addTitle(s, no, d.title, d.subtitle);

  addGlassPanel(s, 68, 286, 1144, 330);
  const chart = s.charts.add("bar");
  chart.position = { left: 92, top: 328, width: 736, height: 250 };
  chart.title = "核心技术模块成熟度（示意）";
  chart.categories = ["UI", "数据存储", "后台任务", "网络同步", "安全能力", "智能分析"];
  const ser = chart.series.add("成熟度");
  ser.values = [92, 90, 86, 82, 88, 79];
  chart.hasLegend = true;
  chart.legend.position = "bottom";
  chart.barOptions.direction = "column";

  addGlassPanel(s, 856, 328, 330, 250);
  addText(s, no, "工程亮点", 884, 352, 220, 30, { size: 24, bold: true, face: FONT.title, color: C.purple, role: "tech-title" });
  addText(s, no, "• Kotlin + Compose 提升迭代效率\n• Room + Flow 保障数据一致性\n• WorkManager 提供稳定后台任务\n• PIN + 生物识别强化访问安全", 884, 396, 272, 168, { size: 17, face: FONT.body, color: C.sub, role: "tech-body" });

  inspectRecords.push({ kind: "chart", slide: no, role: "bar-chart", bbox: [92, 328, 736, 250] });
}

function flowSlide(p) {
  const no = 8;
  const d = SLIDES[7];
  const s = p.slides.add();
  addBackground(s);
  addHeader(s, no, d.kicker);
  addTitle(s, no, d.title, d.subtitle);

  const y = 390;
  const w = 220;
  d.steps.forEach((step, i) => {
    const x = 50 + i * 246;
    addGlassPanel(s, x, y, w, 150);
    addShape(s, "ellipse", x + 18, y + 18, 34, 34, C.purple);
    addText(s, no, `${i + 1}`, x + 31, y + 27, 10, 16, { size: 14, bold: true, face: FONT.mono, color: "#FFFFFF", role: "step-num" });
    addText(s, no, step, x + 62, y + 20, 140, 30, { size: 20, bold: true, face: FONT.title, color: C.text, role: "step-title" });

    const desc = i === 0 ? "随手录入收支" : i === 1 ? "自动归类关联" : i === 2 ? "预算进度可视化" : i === 3 ? "趋势结构复盘" : "形成行动清单";
    addText(s, no, desc, x + 20, y + 68, 180, 44, { size: 16, face: FONT.body, color: C.sub, role: "step-desc" });
    if (i < d.steps.length - 1) {
      addShape(s, "rightArrow", x + w, y + 62, 24, 20, C.gold);
    }
  });
}

function createDeck() {
  const p = Presentation.create({ slideSize: { width: W, height: H } });
  cover(p);
  cardSlide(p, 2);
  metricSlide(p);
  cardSlide(p, 4, [C.purple, C.gold, C.teal]);
  cardSlide(p, 5, [C.teal, C.gold, C.purple]);
  architectureSlide(p);
  techSlide(p);
  flowSlide(p);
  cardSlide(p, 9, [C.gold, C.purple, C.teal]);
  return p;
}

async function ensureDirs() {
  await fs.mkdir(OUT_DIR, { recursive: true });
  await fs.mkdir(SCRATCH_DIR, { recursive: true });
  await fs.mkdir(PREVIEW_DIR, { recursive: true });
}

async function saveBlob(blob, out) {
  const bytes = new Uint8Array(await blob.arrayBuffer());
  await fs.writeFile(out, bytes);
}

async function writeInspect(p) {
  const lines = [JSON.stringify({ kind: "deck", id: DECK_ID, slideCount: p.slides.count, slideSize: { width: W, height: H } }), ...inspectRecords.map((r) => JSON.stringify(r))].join("\n") + "\n";
  await fs.writeFile(INSPECT_PATH, lines, "utf8");
}

async function renderExport(p) {
  await ensureDirs();
  await writeInspect(p);

  for (let i = 0; i < p.slides.items.length; i += 1) {
    const png = await p.export({ slide: p.slides.items[i], format: "png", scale: 1 });
    await saveBlob(png, path.join(PREVIEW_DIR, `slide-${String(i + 1).padStart(2, "0")}.png`));
  }

  const blob = await PresentationFile.exportPptx(p);
  const out = path.join(OUT_DIR, "output.pptx");
  await blob.save(out);
  return out;
}

const deck = createDeck();
const out = await renderExport(deck);
console.log(out);
