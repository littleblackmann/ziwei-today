const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, HeadingLevel, BorderStyle, WidthType,
  ShadingType, VerticalAlign, PageNumber, LevelFormat, ExternalHyperlink,
  TableOfContents, Bookmark, InternalHyperlink
} = require("docx");
const fs = require("fs");

// ── 色票 ────────────────────────────────────────────────
const C_PRIMARY   = "5C2D91"; // 深紫
const C_GOLD      = "8B6914"; // 金色文字
const C_BG_LIGHT  = "F5F0FF"; // 淺紫背景
const C_BG_GOLD   = "FFF8E1"; // 淺金背景
const C_BG_WARN   = "FFF3E0"; // 提醒背景
const C_BORDER    = "CCCCCC";
const C_GRAY      = "666666";

// ── 輔助函式 ─────────────────────────────────────────────
const border = (color = C_BORDER) => ({ style: BorderStyle.SINGLE, size: 1, color });
const allBorders = (color = C_BORDER) => ({
  top: border(color), bottom: border(color),
  left: border(color), right: border(color)
});
const noBorder = () => ({ style: BorderStyle.NONE, size: 0, color: "FFFFFF" });
const allNoBorders = () => ({
  top: noBorder(), bottom: noBorder(), left: noBorder(), right: noBorder()
});

function h1(text, bookmarkId) {
  const textRun = new TextRun({ text, bold: true, size: 32, font: "微軟正黑體", color: C_PRIMARY });
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 360, after: 120 },
    border: { bottom: { style: BorderStyle.SINGLE, size: 8, color: C_PRIMARY, space: 4 } },
    children: bookmarkId
      ? [new Bookmark({ id: bookmarkId, children: [textRun] })]
      : [textRun],
  });
}

function h2(text, bookmarkId) {
  const textRun = new TextRun({ text, bold: true, size: 26, font: "微軟正黑體", color: C_PRIMARY });
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 240, after: 80 },
    children: bookmarkId
      ? [new Bookmark({ id: bookmarkId, children: [textRun] })]
      : [textRun],
  });
}

function h3(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    spacing: { before: 180, after: 60 },
    children: [new TextRun({ text, bold: true, size: 24, font: "微軟正黑體", color: "333333" })],
  });
}

function body(text, opts = {}) {
  return new Paragraph({
    spacing: { before: 60, after: 60 },
    children: [new TextRun({
      text, size: 22, font: "微軟正黑體",
      color: opts.color || "333333",
      bold: opts.bold || false,
    })],
  });
}

function bullet(text, level = 0) {
  return new Paragraph({
    numbering: { reference: "bullets", level },
    spacing: { before: 40, after: 40 },
    children: [new TextRun({ text, size: 22, font: "微軟正黑體", color: "333333" })],
  });
}

function numbered(text, level = 0) {
  return new Paragraph({
    numbering: { reference: "numbers", level },
    spacing: { before: 40, after: 40 },
    children: [new TextRun({ text, size: 22, font: "微軟正黑體", color: "333333" })],
  });
}

function space(n = 1) {
  return Array.from({ length: n }, () =>
    new Paragraph({ spacing: { before: 0, after: 0 }, children: [new TextRun("")] })
  );
}

// 高亮提示框（單欄表格模擬）
function noteBox(title, lines, bgColor = C_BG_LIGHT) {
  const rows = [
    new TableRow({
      children: [new TableCell({
        borders: allBorders(C_PRIMARY),
        shading: { fill: bgColor, type: ShadingType.CLEAR },
        margins: { top: 120, bottom: 80, left: 180, right: 180 },
        width: { size: 8640, type: WidthType.DXA },
        children: [
          new Paragraph({
            spacing: { before: 0, after: 60 },
            children: [new TextRun({ text: title, bold: true, size: 22, font: "微軟正黑體", color: C_PRIMARY })],
          }),
          ...lines.map(l => new Paragraph({
            spacing: { before: 0, after: 0 },
            children: [new TextRun({ text: l, size: 22, font: "微軟正黑體", color: "333333" })],
          })),
        ],
      })]
    })
  ];
  return new Table({
    width: { size: 8640, type: WidthType.DXA },
    columnWidths: [8640],
    rows,
  });
}

// 步驟卡（橫向兩格：數字 + 內容）
function stepCard(num, title, desc) {
  return new Table({
    width: { size: 8640, type: WidthType.DXA },
    columnWidths: [720, 7920],
    rows: [
      new TableRow({
        children: [
          new TableCell({
            borders: allBorders(C_PRIMARY),
            shading: { fill: C_PRIMARY, type: ShadingType.CLEAR },
            width: { size: 720, type: WidthType.DXA },
            margins: { top: 120, bottom: 120, left: 120, right: 120 },
            verticalAlign: VerticalAlign.CENTER,
            children: [new Paragraph({
              alignment: AlignmentType.CENTER,
              children: [new TextRun({ text: String(num), bold: true, size: 28, font: "微軟正黑體", color: "FFFFFF" })],
            })],
          }),
          new TableCell({
            borders: allBorders(C_PRIMARY),
            shading: { fill: C_BG_LIGHT, type: ShadingType.CLEAR },
            width: { size: 7920, type: WidthType.DXA },
            margins: { top: 100, bottom: 80, left: 180, right: 120 },
            children: [
              new Paragraph({
                spacing: { before: 0, after: 40 },
                children: [new TextRun({ text: title, bold: true, size: 23, font: "微軟正黑體", color: C_PRIMARY })],
              }),
              new Paragraph({
                spacing: { before: 0, after: 0 },
                children: [new TextRun({ text: desc, size: 21, font: "微軟正黑體", color: "444444" })],
              }),
            ],
          }),
        ]
      })
    ]
  });
}

// 功能對照表
function featureTable(rows) {
  const headerRow = new TableRow({
    children: [
      new TableCell({
        borders: allBorders(C_PRIMARY),
        shading: { fill: C_PRIMARY, type: ShadingType.CLEAR },
        width: { size: 2880, type: WidthType.DXA },
        margins: { top: 80, bottom: 80, left: 160, right: 120 },
        children: [new Paragraph({ children: [new TextRun({ text: "功能", bold: true, size: 22, font: "微軟正黑體", color: "FFFFFF" })] })],
      }),
      new TableCell({
        borders: allBorders(C_PRIMARY),
        shading: { fill: C_PRIMARY, type: ShadingType.CLEAR },
        width: { size: 5760, type: WidthType.DXA },
        margins: { top: 80, bottom: 80, left: 160, right: 120 },
        children: [new Paragraph({ children: [new TextRun({ text: "說明", bold: true, size: 22, font: "微軟正黑體", color: "FFFFFF" })] })],
      }),
    ]
  });

  const dataRows = rows.map(([feat, desc], i) => new TableRow({
    children: [
      new TableCell({
        borders: allBorders(),
        shading: { fill: i % 2 === 0 ? "F9F6FF" : "FFFFFF", type: ShadingType.CLEAR },
        width: { size: 2880, type: WidthType.DXA },
        margins: { top: 80, bottom: 80, left: 160, right: 120 },
        children: [new Paragraph({ children: [new TextRun({ text: feat, bold: true, size: 22, font: "微軟正黑體", color: C_PRIMARY })] })],
      }),
      new TableCell({
        borders: allBorders(),
        shading: { fill: i % 2 === 0 ? "F9F6FF" : "FFFFFF", type: ShadingType.CLEAR },
        width: { size: 5760, type: WidthType.DXA },
        margins: { top: 80, bottom: 80, left: 160, right: 120 },
        children: [new Paragraph({ children: [new TextRun({ text: desc, size: 22, font: "微軟正黑體", color: "333333" })] })],
      }),
    ]
  }));

  return new Table({
    width: { size: 8640, type: WidthType.DXA },
    columnWidths: [2880, 5760],
    rows: [headerRow, ...dataRows],
  });
}

// ── 文件主體 ─────────────────────────────────────────────
const doc = new Document({
  numbering: {
    config: [
      {
        reference: "bullets",
        levels: [{
          level: 0, format: LevelFormat.BULLET, text: "•",
          alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 720, hanging: 360 } } }
        }, {
          level: 1, format: LevelFormat.BULLET, text: "◦",
          alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 1080, hanging: 360 } } }
        }]
      },
      {
        reference: "numbers",
        levels: [{
          level: 0, format: LevelFormat.DECIMAL, text: "%1.",
          alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 720, hanging: 360 } } }
        }]
      },
    ]
  },
  styles: {
    default: {
      document: { run: { font: "微軟正黑體", size: 22 } }
    },
    paragraphStyles: [
      {
        id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 32, bold: true, font: "微軟正黑體", color: C_PRIMARY },
        paragraph: { spacing: { before: 360, after: 120 }, outlineLevel: 0 }
      },
      {
        id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 26, bold: true, font: "微軟正黑體", color: C_PRIMARY },
        paragraph: { spacing: { before: 240, after: 80 }, outlineLevel: 1 }
      },
      {
        id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 24, bold: true, font: "微軟正黑體", color: "333333" },
        paragraph: { spacing: { before: 180, after: 60 }, outlineLevel: 2 }
      },
    ]
  },

  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 }, // A4
        margin: { top: 1440, right: 1300, bottom: 1440, left: 1300 }
      }
    },
    headers: {
      default: new Header({
        children: [new Paragraph({
          border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: C_PRIMARY, space: 4 } },
          children: [
            new TextRun({ text: "今日紫微", bold: true, size: 20, font: "微軟正黑體", color: C_PRIMARY }),
            new TextRun({ text: "  使用說明", size: 20, font: "微軟正黑體", color: C_GRAY }),
          ]
        })]
      })
    },
    footers: {
      default: new Footer({
        children: [new Paragraph({
          alignment: AlignmentType.CENTER,
          border: { top: { style: BorderStyle.SINGLE, size: 4, color: C_BORDER, space: 4 } },
          children: [
            new TextRun({ text: "第 ", size: 18, font: "微軟正黑體", color: C_GRAY }),
            new TextRun({ children: [PageNumber.CURRENT], size: 18, font: "微軟正黑體", color: C_GRAY }),
            new TextRun({ text: " 頁", size: 18, font: "微軟正黑體", color: C_GRAY }),
          ]
        })]
      })
    },

    children: [
      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      // 封面
      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      ...space(6),
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 0, after: 40 },
        children: [new TextRun({ text: "✦", size: 72, font: "微軟正黑體", color: C_PRIMARY })],
      }),
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 0, after: 80 },
        children: [new TextRun({ text: "今日紫微", bold: true, size: 72, font: "微軟正黑體", color: C_PRIMARY })],
      }),
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 0, after: 240 },
        children: [new TextRun({ text: "使用說明書", size: 40, font: "微軟正黑體", color: C_GOLD })],
      }),
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 0, after: 40 },
        children: [new TextRun({ text: "結合紫微斗數 × AI 智慧解讀，每天用幾分鐘了解自己的運勢", size: 24, font: "微軟正黑體", color: C_GRAY })],
      }),
      ...space(2),
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 0, after: 0 },
        children: [new TextRun({ text: "版本 v1.1.0", size: 20, font: "微軟正黑體", color: C_GRAY })],
      }),
      new Paragraph({ children: [new TextRun("")], pageBreakBefore: true }),

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      // 目錄
      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      new TableOfContents("目　錄", { hyperlink: true, headingStyleRange: "1-2" }),
      new Paragraph({ children: [new TextRun("")], pageBreakBefore: true }),

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      // 第一章  認識今日紫微
      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      h1("一、認識今日紫微", "ch1"),
      body("今日紫微是一款結合傳統「紫微斗數」與現代 AI 的個人化運勢 App。"),
      body("它可以："),
      bullet("依照您（或家人）的出生年月日時，自動排出專屬命盤"),
      bullet("每天計算「流日」運勢，分析今日的星曜狀態"),
      bullet("透過 AI（GPT 語言模型）用自然語言為您撰寫個人化的運勢解讀"),
      bullet("針對八大主題分別解讀：整體運勢、財運、事業、感情、健康、人際、出行、心情"),
      ...space(1),

      noteBox("📌 使用前請先了解", [
        "本 App 的運勢解讀僅供文化參考與生活提醒，不構成醫療、投資或法律建議。",
        "身體不適請就醫，財務問題請諮詢專業人士。",
        "命理文化有其趣味，請以平常心看待，不必過度依賴。",
      ], C_BG_WARN),
      ...space(1),

      h2("App 需要什麼才能運作？"),
      featureTable([
        ["紫微命盤", "在手機本機計算，不需網路，不上傳任何資料"],
        ["AI 運勢解讀", "需要網路，並且需要您自備一組 OpenAI API Key（第三章有詳細教學）"],
        ["家人資料", "姓名與出生資料只儲存在您的手機，不會上傳給開發者"],
      ]),
      ...space(1),

      new Paragraph({ children: [new TextRun("")], pageBreakBefore: true }),

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      // 第二章  第一次使用
      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      h1("二、第一次使用步驟", "ch2"),
      body("第一次打開 App，請依照以下順序完成設定："),
      ...space(1),

      h2("步驟 1 — 閱讀並同意免責聲明"),
      body("App 啟動時會顯示「免責聲明」頁面，請您先閱讀所有內容，確認了解後點選「我同意，開始使用」。"),
      body("如果不同意，可點「暫不使用」，App 將不會繼續進入主畫面。"),
      ...space(1),

      h2("步驟 2 — 新增第一位家人"),
      body("同意免責聲明後，畫面上會出現「新增第一位家人」的提示。請點選按鈕，並填入以下資料："),
      featureTable([
        ["姓名", "填入稱呼即可，例如「媽媽」、「小明」"],
        ["性別", "選擇男或女（影響命盤計算）"],
        ["出生年月日", "使用滾輪選擇西元年、月、日"],
        ["出生時辰", "選擇最接近的時辰（2小時一格），不確定可選「不確定」"],
      ]),
      ...space(1),
      noteBox("💡 小提示", [
        "出生時辰影響「命宮」位置，如果真的不知道，可先選「不確定（午時）」，之後再到家人管理修改。",
        "資料儲存在您的手機，不會傳送給任何人。",
      ], C_BG_LIGHT),
      ...space(1),

      h2("步驟 3 — 設定 OpenAI API Key"),
      body("新增家人後，請先前往「設定」（右上角齒輪圖示）→「AI 設定」，設定您的 OpenAI API Key。"),
      body("沒有 API Key 時，系統會提示您設定，否則無法產生 AI 運勢解讀。"),
      body("▶ 詳細申請教學請參閱【第三章：申請 OpenAI API Key】"),
      ...space(1),

      h2("步驟 4 — 開始第一次詢問"),
      body("回到首頁，點選「開始今日詢問」，選擇您想了解的主題，App 就會自動計算並產生今日的運勢解讀。"),
      ...space(1),

      new Paragraph({ children: [new TextRun("")], pageBreakBefore: true }),

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      // 第三章  申請 API Key
      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      h1("三、申請 OpenAI API Key（圖解教學）", "ch3"),
      noteBox("為什麼要自備 API Key？", [
        "1. 您的命盤資料只傳到您自己的 OpenAI 帳號，不經過任何第三方伺服器。",
        "2. 使用費用由您自己決定上限，不會被任何人代收。",
        "3. 每次查詢約美金 0.001 元以下，每天問 10 題也只花不到 1 元臺幣。",
      ], C_BG_GOLD),
      ...space(1),

      body("請依照以下四個步驟完成設定："),
      ...space(1),
      stepCard(1, "前往 OpenAI 官網註冊帳號",
        "在手機或電腦瀏覽器開啟 https://auth.openai.com/create-account，用 Email 或 Google 帳號完成註冊，並依指示完成手機驗證。"),
      ...space(1),
      stepCard(2, "進入 API Platform 並儲值",
        "登入後開啟 https://platform.openai.com，點選左側「Settings → Billing」，加入信用卡並儲值（建議首次儲值 5–10 美元）。系統採先儲值再使用，費用極低。"),
      ...space(1),
      stepCard(3, "建立一支 API Key",
        "進入 https://platform.openai.com/api-keys，點右上角「Create new secret key」。名稱可填「今日紫微」，Permissions 選 All，建立後畫面會顯示 sk-... 開頭的字串，請立刻複製（離開頁面後就無法再看到）。"),
      ...space(1),
      stepCard(4, "回到 App 貼上 Key",
        "開啟今日紫微 → 右上角齒輪「設定」→「AI 設定」→「OpenAI API Key」欄位，長按選擇「貼上」，再按右側「儲存」。看到「API Key 已設定」即完成。"),
      ...space(1),

      noteBox("⚠️ 注意事項", [
        "API Key 就像密碼，請不要分享給其他人或傳送到不明網站。",
        "如果 Key 遺失或洩露，請登入 OpenAI 官網刪除舊的 Key 並重新建立。",
        "如要更換 Key，在設定頁清除後重新貼上新的 Key 再儲存即可。",
      ], C_BG_WARN),
      ...space(1),

      new Paragraph({ children: [new TextRun("")], pageBreakBefore: true }),

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      // 第四章  日常使用
      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      h1("四、日常使用說明", "ch4"),

      h2("4-1　首頁介紹"),
      body("首頁是 App 的主要操作畫面，包含以下區域："),
      featureTable([
        ["日期顯示", "顯示今天的日期，讓您確認是今日的運勢"],
        ["家人卡片列", "橫向滑動選擇要查詢的家人，被選中的名字會以紫色顯示"],
        ["開始今日詢問", "點選後進入主題選擇頁，開始查詢運勢"],
        ["查看詢問記錄", "查看這位家人過去的運勢查詢紀錄"],
        ["右上角人像圖示", "進入「家人管理」，新增或編輯家人資料"],
        ["右上角齒輪圖示", "進入「設定」頁面"],
      ]),
      ...space(1),

      h2("4-2　選擇運勢主題"),
      body("點選「開始今日詢問」後，會出現 8 個主題供您選擇："),
      featureTable([
        ["🌅 整體運勢", "今天的綜合運勢概況"],
        ["💰 財運", "錢財、收支方面的運勢"],
        ["💼 事業工作", "工作、職場、進修的運勢"],
        ["💕 感情", "伴侶、感情方面的運勢"],
        ["🌿 健康", "身體狀況、保養提醒"],
        ["🤝 人際", "朋友、貴人、社交運勢"],
        ["🚪 出行", "外出、移動、變動運勢"],
        ["✨ 心情", "今日心境、內在福氣"],
      ]),
      body("點選任一主題後，App 會自動計算命盤並請 AI 撰寫個人化解讀，通常在幾秒內完成。"),
      ...space(1),

      h2("4-3　查看運勢結果"),
      body("AI 解讀完成後，畫面會顯示："),
      bullet("運勢等級（大吉 / 中吉 / 平 / 小凶 / 凶）"),
      bullet("今日重點摘要"),
      bullet("具體建議事項"),
      bullet("今日關鍵詞標籤"),
      ...space(1),
      body("每天可以查詢多個主題，每個主題當天只會計算一次，結果會自動儲存到「詢問記錄」。"),
      ...space(1),

      h2("4-4　查看詢問記錄"),
      body("點選首頁的「查看詢問記錄」，可以瀏覽這位家人過去所有的查詢結果，方便回顧與比對。"),
      ...space(1),

      new Paragraph({ children: [new TextRun("")], pageBreakBefore: true }),

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      // 第五章  家人管理
      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      h1("五、家人管理", "ch5"),
      body("本 App 支援多位家人的命盤管理，讓您一次查詢全家人的運勢。"),
      ...space(1),

      h2("新增家人"),
      body("點選首頁右上角的「人像圖示」→ 右下角「+」按鈕，填入姓名、性別、出生年月日與時辰後儲存。"),
      ...space(1),

      h2("編輯家人資料"),
      body("在家人管理頁面，點選任一家人的卡片，即可修改資料。若出生時辰當初填錯，也可以在這裡更正。"),
      ...space(1),

      h2("設定主帳號"),
      body("家人列表中，每人卡片右側有「設為主帳號」按鈕。主帳號的名字旁邊會顯示金色星星，"),
      body("主帳號預設會在首頁優先顯示。"),
      ...space(1),

      new Paragraph({ children: [new TextRun("")], pageBreakBefore: true }),

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      // 第六章  設定
      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      h1("六、設定說明", "ch6"),
      body("點選右上角齒輪圖示進入設定頁，可調整以下選項："),
      ...space(1),

      h2("AI 設定"),
      featureTable([
        ["OpenAI API Key", "輸入您的 API Key 後點「儲存」，App 才能進行 AI 運勢解讀"],
        ["顯示 / 隱藏", "可切換 Key 的顯示狀態，方便確認內容"],
        ["清除", "清空已儲存的 API Key"],
        ["申請教學", "點選「如何申請 OpenAI API Key？」進入詳細教學頁"],
      ]),
      ...space(1),

      h2("文字顯示"),
      featureTable([
        ["字體大小", "可選「小 / 標準 / 大」，依個人視力調整"],
        ["色彩模式", "可選「淺色 / 深色 / 跟隨系統」"],
      ]),
      ...space(1),

      h2("進階設定"),
      featureTable([
        ["顯示完整命盤頁", "開啟後，首頁會新增「查看完整命盤」按鈕，可查看十二宮的詳細星曜"],
        ["晚子時歸屬", "23:00–24:00 出生者的命盤計算方式，可選「歸明日（現代派）」或「歸今日（傳統派）」"],
      ]),
      ...space(1),

      noteBox("💡 晚子時是什麼？", [
        "晚子時指晚上 23:00 至凌晨 00:00 這個時段。",
        "傳統紫微斗數中，對於此時段出生的人，各流派對「算哪一天」有不同看法。",
        "如果不確定，建議使用預設的「現代派」設定。",
      ], C_BG_LIGHT),
      ...space(1),

      new Paragraph({ children: [new TextRun("")], pageBreakBefore: true }),

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      // 第七章  常見問題
      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      h1("七、常見問題 Q&A", "ch7"),

      h3("Q1：為什麼點「開始今日詢問」沒有反應或出現錯誤？"),
      bullet("請確認設定頁的「OpenAI API Key」已填寫並儲存。"),
      bullet("請確認手機有連上網路（Wi-Fi 或行動數據）。"),
      bullet("如果 API Key 顯示過期或餘額不足，請登入 OpenAI 官網儲值或更換新的 Key。"),
      ...space(1),

      h3("Q2：運勢解讀每天都可以重新查詢嗎？"),
      body("可以。每個主題每天只計算一次，當天查詢完成後會儲存在記錄中。隔天可重新查詢新的一天運勢。"),
      ...space(1),

      h3("Q3：命盤資料會上傳到網路嗎？"),
      body("命盤計算在手機本機進行，不會上傳。但是當您進行 AI 運勢查詢時，App 會將該位家人的姓名、出生資料與命盤摘要透過您的 API Key 傳送給 OpenAI，由 AI 撰寫解讀後回傳。"),
      body("資料不會傳給 App 開發者或任何其他第三方。"),
      ...space(1),

      h3("Q4：可以為幾位家人建立命盤？"),
      body("沒有數量限制，您可以依需求新增多位家人。"),
      ...space(1),

      h3("Q5：查詢費用怎麼算？"),
      body("費用由 OpenAI 依實際使用量計算，約每次查詢美金 0.001 元以下。每天問 10 個主題約只花 0.01 美元（約臺幣 0.3 元）。費用直接從您 OpenAI 帳號的儲值餘額扣除。"),
      ...space(1),

      h3("Q6：如何刪除家人資料？"),
      body("目前版本請在「家人管理」頁面點選家人後，找到刪除按鈕操作。"),
      ...space(1),

      h3("Q7：App 支援哪些 Android 版本？"),
      body("今日紫微需要 Android 8.0（Oreo）以上版本，建議使用 Android 10 以上以獲得最佳體驗。"),
      ...space(1),

      new Paragraph({ children: [new TextRun("")], pageBreakBefore: true }),

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      // 結語
      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      h1("八、結語", "ch8"),
      body("感謝您使用今日紫微。"),
      body("紫微斗數是中華文化數千年的智慧結晶，今日紫微希望透過現代科技，讓這份傳統文化更親近您的日常生活。"),
      body("每天只需幾分鐘，為自己和家人了解今日的運勢，做好一天的心理準備。"),
      ...space(2),
      new Paragraph({
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text: "✦  祝您每天運勢順遂  ✦", bold: true, size: 28, font: "微軟正黑體", color: C_PRIMARY })],
      }),
      ...space(1),
      new Paragraph({
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text: "今日紫微 v1.1.0", size: 20, font: "微軟正黑體", color: C_GRAY })],
      }),
    ]
  }]
});

Packer.toBuffer(doc).then(buf => {
  fs.writeFileSync("E:/今日紫微/今日紫微_使用說明.docx", buf);
  console.log("Done! 今日紫微_使用說明.docx");
});
