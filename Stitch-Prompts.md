# 社区便民报修服务管理系统 — Stitch AI 设计 Prompt 合集

> **风格基调：白色毛玻璃 + 细边框 + 柔和渐变背景**
> 参考产品：Linear.app / Arc Browser / macOS Sonoma 设置页 / Vercel Dashboard
> 
> 使用方法：打开 https://stitch.google.com ，逐个复制下面的 Prompt 生成设计稿。
> 建议先生成「主布局」搭好框架，再逐个生成子页面。

---

## 🎨 全局设计规范（所有 Prompt 共享）

### 核心视觉语言 —— 毛玻璃质感

这是你最需要强调给 Stitch 的：

```
Material aesthetic: GLASSMORPHISM throughout.
- All cards/panels/dialogs are frosted glass with backdrop blur
- Card background: rgba(255, 255, 255, 0.65) to rgba(255, 255, 255, 0.85)
- backdrop-filter: blur(16px) to blur(24px)
- Every card has a subtle 1px border: rgba(255, 255, 255, 0.6) or rgba(15, 23, 42, 0.06)
- No heavy drop shadows. Use very soft layered shadow:
    0 1px 2px rgba(0,0,0,0.02), 0 4px 16px rgba(15, 23, 42, 0.04)
```

### 背景（让毛玻璃效果能被看见）

```
Page background: soft diffused gradient with organic blurred color blobs.
  Base: #F8FAFC (very pale slate)
  Overlay: large blurred circles ("aurora blobs") in soft pastel colors,
           positioned off-screen corners so they peek through:
    · Top-left blob: #DBEAFE (light blue)
    · Bottom-right blob: #EDE9FE (light violet)
    · Top-right blob: #DCFCE7 (light green)
  Blur radius: 200-300px, opacity 0.5
  This creates depth so glass cards feel frosted on top.
```

### 配色

| 用途 | 色值 | 说明 |
|------|------|------|
| **主品牌色** | `#3B82F6` | 柔和科技蓝，不刺眼 |
| **主品牌 Hover** | `#2563EB` | 稍深蓝 |
| **强调色** | `#8B5CF6` | 柔和紫，用于小面积点缀 |
| **成功** | `#10B981` | 薄荷绿 |
| **警告** | `#F59E0B` | 琥珀色 |
| **危险** | `#EF4444` | 柔和红 |
| **主文字** | `#0F172A` | slate-900 |
| **次文字** | `#64748B` | slate-500 |
| **辅助文字** | `#94A3B8` | slate-400 |
| **细边框** | `rgba(15, 23, 42, 0.06)` | 1px 极淡 |
| **分割线** | `rgba(15, 23, 42, 0.08)` | 比边框再淡一点 |

### 圆角与间距

```
Border radius:
  - Cards / Panels: 16px
  - Buttons / Inputs: 10px
  - Tags / Badges: 8px (rounded pill)
  - Avatar: 9999px (fully round)

Spacing baseline: 4px grid
  - Gap between major sections: 24px
  - Internal card padding: 20-24px
  - Form field gap: 16px
```

### 按钮与交互

```
Primary button: frosted glass white with 1px blue border + blue text,
  hover: slight blue background tint. Or subtle blue gradient fill.
Secondary button: transparent with 1px rgba border, gray text.
All interactive elements have 200ms smooth transition.
Table rows: transparent normally, very subtle rgba(59,130,246,0.06) on hover.
No heavy zebra striping — keep clean.
```

---

## Prompt 1 — 登录注册页

```
Design a login page for "社区便民报修服务管理系统" (Community Repair Service System).
STYLE: GLASSMORPHISM — frosted white panels, soft gradient backdrop, 1px subtle borders.

BACKGROUND: Large canvas with organic blurred color blobs — soft light blue (#DBEAFE) top-left, soft light violet (#EDE9FE) bottom-right, soft light green (#DCFCE7) middle-right. All heavily blurred (250px radius), low opacity, aurora-like.

CENTER CARD (frosted glass):
  - backdrop-blur: 20px
  - background: rgba(255,255,255,0.72)
  - border: 1px solid rgba(255,255,255,0.7) AND 1px solid rgba(15,23,42,0.04) (double border illusion)
  - border-radius: 20px
  - padding: 40px 44px
  - shadow: 0 1px 2px rgba(0,0,0,0.02), 0 8px 32px rgba(15,23,42,0.06)
  
  Inside the card:
  - Top: small gradient circular icon (repair wrench, blue→violet gradient 40x40px)
  - System name: "社区便民报修服务管理系统" — slate-900, 20px, semi-bold
  - Subtitle: "请登录以继续使用" — slate-400, 14px
  
  Form (16px gap between rows):
  - Label "账号" (slate-500, 13px)
  - Input: frosted white bg, 1px rgba border, 10px radius, padding 12px 14px, icon prefix (user icon in slate-400), placeholder "用户名或手机号"
  - Label "密码" 
  - Input: same style, lock icon prefix, show-password toggle (eye icon) on right inside
  
  Primary button: full-width
    · Style: subtle gradient fill from #3B82F6 to #8B5CF6, white text, semi-bold
    · 12px vertical padding, 10px radius
    · Subtle glow/shadow in brand color
  
  Bottom area:
  · Left link "注册新账号" (slate-500)  · Center dot  · Right link "忘记密码？" (slate-500)
  · Demo hint pill at very bottom: light bg #EFF6FF, text "演示: admin/123456"

NO heavy solid colors. Everything airy, frosted, light, premium.
Think Arc Browser new tab page or Linear login.
```

---

## Prompt 2 — 主布局（侧边栏 + 顶栏 + 内容区）

```
Design a SaaS admin dashboard shell layout.
STYLE: FULL GLASSMORPHISM — every panel is frosted white with 1px borders, on soft aurora gradient background.

ENTIRE PAGE BACKGROUND:
  Base #F8FAFC with three large blurred color blobs:
  · Top-left: soft blue #DBEAFE (blur 300px, opacity 0.6)
  · Bottom-right: soft violet #EDE9FE (blur 300px, opacity 0.5)
  · Bottom-left: soft mint #DCFCE7 (blur 250px, opacity 0.4)

THREE PANELS — ALL FROSTED GLASS:

1. LEFT SIDEBAR (240px wide, full height)
   - backdrop-blur: 20px
   - background: rgba(255,255,255,0.55) — more transparent than content area
   - border-right: 1px solid rgba(15,23,42,0.06)
   
   Logo area (top, 64px height):
   · Small gradient circle (36px) with wrench icon inside
   · "社区便民报修" — slate-900, 15px, semi-bold
   · Subtitle "维修管理系统" — slate-400, 11px
   
   Menu items (grouped with thin 1px dividers rgba(15,23,42,0.06)):
   · Label "工作台" (slate-400, 11px, uppercase tracking-wide)
     - 统计概览看板 · 报修类目管理
   · Label "工单管理"
     - 工单审核 · 派单调度 · 工单大厅 · 我的报修 · 我的工单 · 工单验收 · 服务评价
   · Label "系统管理"  
     - 评价管理 · 统计分析 · 用户管理 · 个人中心
   
   Active item state:
     · Very subtle blue background tint rgba(59,130,246,0.08)
     · Left accent bar: 3px gradient #3B82F6 → #8B5CF6
     · Text: slate-900, semi-bold
   
   Inactive item:
     · Text: slate-500, 13px
     · Hover: background rgba(15,23,42,0.03), text slate-700
     · Icon: slate-400, 16px

2. TOP HEADER (56px height, spanning right of sidebar)
   - backdrop-blur: 16px
   - background: rgba(255,255,255,0.6)
   - border-bottom: 1px solid rgba(15,23,42,0.06)
   
   Left: breadcrumb "工作台 / 统计概览看板" — slate-400, 13px (not bold, subtle)
   
   Right section:
   · Bell icon (slate-400) with tiny 4px red dot indicator
   · User pill: avatar circle (28px, gradient) + name "张管理员" (slate-700) + small dropdown arrow (slate-400)
   · Role tag: small frosted pill background rgba(239,68,68,0.1), text "管理员" #EF4444

3. MAIN CONTENT AREA (rest of space, scrollable)
   - padding: 24px
   - cards inside: glass panels with 16px radius, 1px border, backdrop blur

FEEL: premium, weightless, layered depth through transparency.
Imagine macOS Ventura System Settings or Linear.app dashboard.
```

---

## Prompt 3 — Dashboard 概览看板

```
Design a dashboard overview page inside a glassmorphism admin layout.
The page sits on the soft aurora gradient background with glass panels.

PAGE TITLE AREA:
  - "统计概览看板" — slate-900, 22px, semi-bold
  - Subtitle below: "欢迎回来，以下是您的系统概览" — slate-400, 13px

1. WELCOME BANNER (full-width glass card, 20px radius, backdrop-blur 20px, 1px rgba border)
   - Left: large frosted circular avatar (64px) — shows first char of name on soft gradient circle
   - Middle column:
     · Greeting "下午好，张管理员 👋" — 18px, semi-bold, slate-900
     · Info line below — slate-400, 13px: "系统管理员 · 138****8888 · 上次登录 2026-11-10"
   - Right: two buttons — "用户管理" (glass style: 1px blue border + blue text) + "个人中心" (transparent outline)

2. FOUR KPI CARDS (4 cards in a row, equal width, glass panels, 16px radius)
   Each card contains:
   - Top-left: small frosted circle (40px) with colored icon inside (backdrop-blur, rgba bg)
     · Card 1  "居民用户" — green circle rgba(16,185,129,0.1), icon 🏠
     · Card 2  "维修人员" — amber circle rgba(245,158,11,0.1), icon 🔧  
     · Card 3  "系统管理员" — red circle rgba(239,68,68,0.1), icon ⚙️
     · Card 4  "今日新增工单" — blue circle rgba(59,130,246,0.1), icon 📋
   - Large number: slate-900, 28px, bold (e.g., 156, 23, 5, 18)
   - Label below number: slate-400, 13px
   - Tiny trend line at bottom: "较上月 +12" in green, with mini up-arrow sparkline

3. QUICK ACTIONS GRID (glass card, 6 action tiles inside 3-col grid)
   - Section header: "快速操作" — 15px, semi-bold, slate-700, with subtle divider below
   - Each tile: frosted circle icon (44px, colored rgba bg) + text label below, 72px column spacing
     Row 1: 提交报修(blue) · 审核工单(green) · 派单调度(amber)
     Row 2: 我的报修(violet) · 用户管理(teal) · 评价管理(pink)

ALL ELEMENTS: frosted, transparent, 1px borders, no heavy shadows.
Layered depth through varying backdrop-blur intensity and transparency levels.
```

---

## Prompt 4 — 工单列表页（搜索 + 表格）

```
Design a data table list page for repair orders, inside glassmorphism admin layout.
Everything is frosted glass panels on aurora gradient background.

TOP AREA — SEARCH CARD (glass panel, 16px radius, backdrop-blur 16px, 1px rgba border):
  - Row of 5 filter controls, each has label above + glass input below:
    · "工单状态" — multi-select with chips/pills instead of checkboxes
    · "报修类目" — single select cascader
    · "紧急程度" — select (普通/紧急)
    · "关键字" — text input with search icon prefix
    · "提交时间" — date range picker with shortcut pills (今天·本周·本月)
  - Action buttons row below, right-aligned:
    · "查询" — glassmorphism primary: subtle blue tint background + 1px blue border + blue text
    · "重置" — transparent with 1px rgba border + slate-500 text

TOOLBAR (above table, inside same card or separate glass strip):
  · Left: primary gradient button "➕ 提交报修" (blue→violet, white text, subtle glow)
  · Right: three small frosted icon buttons — refresh · column settings · density

TABLE AREA (glass panel, 16px radius, 1px border, backdrop-blur 20px):
  Columns: | 工单编号 | 报修标题 | 报修类目 | 维修地址 | 联系人 | 紧急 | 状态 | 维修人员 | 提交时间 | 操作 |
  
  Table visual details (VERY IMPORTANT for glass feel):
  - NO table outer border lines. NO cell borders. NO heavy zebra striping.
  - Header row: transparent bg, text slate-400 uppercase 11px tracking-wide, bottom 1px rgba divider
  - Data rows: pure transparent, 48px height, smooth transition
  - Row hover: EXTREMELY subtle tint rgba(59,130,246,0.04), no border change
  
  Column specifics:
  - 工单编号: monospaced, slate-500, clickable blue on hover
  - 状态列: FROSTED PILL BADGES (NOT solid!)
    · Each status pill: rgba background (e.g., rgba(59,130,246,0.08) for blue states), 1px same-color border, matching color text, 8px radius
    · Examples: 待审核 → amber tint pill, 维修中 → orange tint pill, 已完成 → green tint pill
  - 紧急: tiny red pill "紧急" or gray text "普通"
  - 操作列: subtle text buttons — slate-500 normally, blue on hover
  
PAGINATION (bottom right of table area):
  Glass style: frosted pill for page numbers, outlined prev/next
  "共 156 条" on left, then page size, pages, jump-to-page

CRITICAL: No solid fills anywhere. All colored elements use rgba tints on white.
Border lines are always 1px, 12-16px border-radius everywhere.
```

---

## Prompt 5 — 工单提交页（表单）

```
Design a form page for "提交报修工单" inside glassmorphism admin layout.
Soft aurora gradient background, all panels are frosted glass.

PAGE HEADER:
  - Breadcrumb: slate-400, 12px
  - Title: "提交报修" — slate-900, 22px, semi-bold, with small ➕ glass-icon circle
  - Subtitle: "填写故障信息，提交后进入管理员审核流程" — slate-400, 13px

MAIN FORM CARD (glass panel, 20px radius, backdrop-blur 20px, 1px rgba border, max-width 760px, centered):
  Inside: vertical form with 24px gap between rows, label above field on left (slate-500, 13px)
  
  All inputs/follows this GLASS INPUT STYLE:
    background: rgba(255,255,255,0.6), backdrop-blur 8px
    border: 1px solid rgba(15,23,42,0.08)
    border-radius: 10px
    padding: 12px 14px
    Focus state: 1px solid #3B82F6, very subtle blue glow box-shadow
  
  Form fields:
  a) 报修类目 — Cascader glass input
  b) 报修标题 — Text glass input with character counter
  c) 故障描述 — Textarea glass, 5 rows, counter
  d) 维修地址 — Text glass input  
  e) 紧急程度 — Two frosted pill buttons side-by-side: 🔵 普通 (active filled tint) / 🟠 紧急 (active amber tint)
  f) 期望上门时间 — Glass date picker with shortcut pills
  g) 现场照片 — Upload area: frosted "+" card (120x120px, dashed 1px border) + up to 3 frosted image preview cards
  h) 联系人 / 联系电话 — side-by-side glass inputs

FORM FOOTER (glass sticky bar or inline at bottom):
  - 取消 button: glass outlined style
  - 提交 button: gradient filled blue→violet, white text, subtle glow shadow
  - Helper text between: slate-400, 12px "提交后可在「我的报修」查看状态，支持审核前修改或撤销"

Optional rejection alert (if resubmit mode):
  Glass panel with red tint rgba(239,68,68,0.05), 1px red-tinted border, rejection reason text in slate-600.
```

---

## Prompt 6 — 工单详情页

```
Design a single repair order detail page inside glassmorphism admin layout.
Aurora gradient background, all panels frosted glass with 1px borders.

1. HEAD CARD (glass panel, 20px radius, full width)
   - Left:
     · Monospace order number "WO20261110001" — slate-400, 14px, tracking-wide
     · Large status pill — FROSTED: rgba(orange tint) bg, 1px orange border, orange text, 10px radius, "维修中"
     · Small extra pill if urgent: red tint frosted "紧急"
   - Right: action buttons right-aligned — glass outlined "返回", gradient filled "修改工单", red-tint glass "撤销"

2. FLOW TIMELINE CARD (glass panel)
   - Title "工单流转状态" — 15px semi-bold slate-700
   - Horizontal step indicator: 6-7 circles in a row with connecting frosted lines
     · Completed steps: filled with soft green tint rgba(16,185,129,0.15), green check icon, green text
     · Current step: filled with soft blue tint rgba(59,130,246,0.15), blue ring pulse animation hint
     · Future steps: 1px circle outline rgba(15,23,42,0.1), slate-400 text
     Connecting line between steps: 2px frosted rgba gradient
   
3. INFO DESCRIPTIONS — 3 glass cards in a subtle grid (2 cols on large screens, stacked on mobile)
   Each card: 16px radius, glass panel, section title with colored left-bar accent (3px gradient), 
   then description list items:
   
   Card A "基本信息":
     · Label/Value pairs with very subtle 1px rgba dividers between each row
     · Label: slate-400 13px, Value: slate-900 14px
   
   Card B "报修人信息": same style as Card A
   
   Card C "故障信息":
     · Description text paragraph
     · Photo gallery row: 2-3 frosted image cards (rounded 12px) with very subtle border
   
4. RIGHT SIDEBAR — "操作日志" vertical timeline (glass panel, narrower width)
   - Vertical dots with connecting frosted line (rgba 2px)
   - Each entry: small timestamp (slate-400 11px) + action text (slate-700 13px)
   - Dot at each timestamp: filled colored circle (blue for current, gray for past)

CRITICAL GLASS RULES: No solid background fills. 
Use rgba tints on white/transparent for all colored elements.
Everything has 1px subtle borders. 16-20px radius on all cards.
```

---

## Prompt 7 — 统计分析页

```
Design a data analytics dashboard page inside glassmorphism admin layout.
Soft aurora gradient background, every panel frosted glass with 1px rgba borders.

FILTER BAR (compact glass strip, 14px radius):
  - Date range picker + shortcut pills (近7天 · 近30天 · 近90天 · 近1年)
  - Granularity select (按日/周/月) + Category level select + Top N select
  - Refresh (glass outlined) + Reset buttons

FOUR KPI CARDS (4 equal glass cards in a row, 16px radius, 20px backdrop-blur):
  Each card:
  - Top-left colored frosted icon circle (44px, rgba tinted: blue/green/orange/violet)
  - Large metric number: slate-900, 32px, bold
  - Unit label: slate-400, 13px
  - Bottom section: tiny frosted mini-sparkline or circular progress ring + trend text
  - Cards: "工单总量 1,256" · "维修完成率 94.2%" · "平均评分 4.6/5.0" · "平均处理时长 2.3天"

CHART GRID (2x2 grid of glass chart cards, 16px radius each):
  Each chart card: title top-left, "查看详情" text link top-right (slate-400 12px)
  Charts themselves have TRANSPARENT backgrounds, frosted white labels
  
  Top-Left — 报修趋势: Line/area chart, soft blue gradient area fill rgba(59,130,246,0.15), smooth bezier curve
  Top-Right — 类目占比: Donut chart with thin lines (1px), frosted center showing total count, legend right side with colored dot indicators
  Bottom-Left — 状态分布: Horizontal soft bar chart, each bar is frosted rounded pill (10px radius), colored by state
  Bottom-Right — 维修人员工作量排名: Horizontal bars ranking top 10, left has mini avatar circles, bars are frosted colored pills

CHART COLORS (soft, muted — not neon):
  #3B82F6 blue, #10B981 emerald, #F59E0B amber, #8B5CF6 violet, #EC4899 pink, #14B8A6 teal

CRITICAL: Charts must feel integrated into the glass panels. No heavy grid lines, no solid color backgrounds.
Chart axes and labels use slate-400, thin 1px lines.
```

---

## Prompt 8 — 用户管理页

```
Design a "用户管理" CRUD table page inside glassmorphism admin layout.
Aurora gradient background, all panels are frosted glass.

SEARCH AREA (glass strip):
  - Row of controls, glass inputs: keyword (用户名/姓名/手机号), role select (全部/居民/维修/管理员), status select (启用/禁用)
  - Right: 查询 (glass blue outlined) + 重置 buttons

TOOLBAR:
  - Primary gradient filled button "➕ 新增用户" (blue→violet)
  - Hint text below: slate-400 12px "居民账号自助注册，此入口仅创建维修人员和管理员"

USER TABLE AREA (glass card, 16px radius, 1px border, backdrop-blur 20px):
  NO table borders. NO cell borders. NO zebra striping.
  
  Columns: | 用户ID | 用户名 | 真实姓名 | 手机号 | 角色 | 状态 | 锁定 | 最后登录 | 创建时间 | 操作 |
  
  Table details:
  - Header row: transparent, slate-400 uppercase 11px tracking-wide, bottom 1px rgba divider
  - Row hover: EXTREMELY subtle rgba(59,130,246,0.04) tint
  - Role column: FROSTED PILL BADGES (not solid):
    · 居民 → green tint pill rgba(16,185,129,0.1), green border, green text
    · 维修人员 → amber tint pill rgba(245,158,11,0.1), amber border, amber text
    · 管理员 → red tint pill rgba(239,68,68,0.1), red border, red text
  - Status column: frosted toggle switch — ON green tint rgba bg, OFF gray
  - Lock column: tiny amber tinted frosted pill "锁定中" or "-"
  - Action column (right-fixed): text buttons slate-500 → blue on hover: 详情 · 编辑 · 角色 · 重置密码

DIALOGS / DRAWERS (also glass!):
  - Edit User dialog: centered glass panel, 20px radius, backdrop-blur 24px, 1px border, soft aurora showing through behind
  - User Detail Drawer: right-side slide-in glass panel, frosted, 1px border on left edge
  
PAGINATION (bottom right): frosted glass pill buttons for pages, outlined prev/next.

EVERYTHING: frosted white, rgba tints for colors, 1px borders, 16px+ radius, transparent backgrounds beneath all cards.
```

---

## ✨ Stitch 使用小贴士

### 生成顺序
1. **Prompt 2（主布局）先做** — 侧边栏 + 顶栏搭好，所有页面共用
2. **Prompt 1（登录页）独立** — 它不走主布局，单独生成
3. 然后 3 → 4 → 5 → 6 → 7 → 8 逐个页面来

### 让 Stitch 正确理解毛玻璃
在每个 prompt 里你可以**手动加强**这几句话（如果 AI 生成的效果不对）：
```
EVERY panel, card, dialog, button must use glassmorphism:
- Background: rgba(255, 255, 255, 0.7)
- backdrop-filter: blur(20px)  
- border: 1px solid rgba(255, 255, 255, 0.6)
- NO solid opaque backgrounds anywhere
- NO heavy drop shadows
```

### 如果背景看不出毛玻璃效果
毛玻璃需要**有颜色的背景**才能体现。如果 Stitch 给了纯白背景，补一句：
```
Please add a soft aurora gradient background with large blurred color blobs
(light blue top-left, light violet bottom-right) so the glass effect is visible.
```

### 迭代技巧
不满意就直接描述改：
- "侧边栏透明度太高了，让它稍微不透一点"
- "状态标签颜色太重了，换成更淡的 rgba"
- "表格 hover 效果太明显了，要更克制"
- "圆角统一改成 14px"

---

生成好设计稿后截图给我，或者告诉我哪些地方需要微调，我帮你迭代 👌
