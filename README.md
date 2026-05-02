# 今日紫微

> 專為長者設計的紫微斗數每日運勢 App
> 底層做嚴謹的紫微斗數排盤運算，上層只給長者看得懂的白話文生活建議。

![Platform](https://img.shields.io/badge/platform-Android-green)
![Language](https://img.shields.io/badge/language-Kotlin-blueviolet)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![MinSDK](https://img.shields.io/badge/minSDK-26-orange)
![Status](https://img.shields.io/badge/status-v1.0.0%20待驗收-orange)

---

## 這是什麼

**「奶奶看得懂、爸媽願意每天開、底下一樣是真的紫微斗數演算法。」**

市面上的紫微 App 大多直接把傳統十二宮星盤丟給使用者，對不懂命理的長者形同天書。本專案反向操作：

- **底層**：完整實作紫微斗數演算法（14 主星 + 輔星 + 煞星 + 四化 + 三層流運）
- **上層**：白話文卡片，每天早上 7 點推播一次「今日適合做什麼」
- **無障礙**：大字、高對比、TalkBack 友善

---

## 特色

- 🌙 **完全離線** — 排盤、農曆、字典全部本地，零網路、零後端、零 API 費用
- 👵 **長者優先** — 24sp 起跳的字、單頁卡片、零學習曲線
- 🧮 **演算法不偷工** — 純 Kotlin 從零實作，不依賴任何外部命理套件
- 👨‍👩‍👧‍👦 **多家人管理** — 一台手機管全家人的命盤
- 🔐 **隱私 100%** — 所有資料只在手機上，可匯出 JSON 自行備份

---

## 技術規格

| 項目 | 內容 |
|------|------|
| 語言 | Kotlin 2.1.0 |
| UI | Jetpack Compose (BOM 2024.12.01) + Material 3 |
| 最低 SDK | API 26 (Android 8.0) |
| 目標 SDK | API 35 (Android 15) |
| 資料庫 | Room 2.6.1 (KSP) |
| 偏好 | DataStore Preferences |
| 排程 | WorkManager 2.9.1（OneTime 鏈式排程確保準時）|
| 導覽 | Navigation Compose |
| JVM Target | 17 |

---

## 專案結構

```
今日紫微/
├── app/                           Android 專案
│   ├── src/main/java/com/zwds/today/
│   │   ├── astro/                 演算法核心（純 Kotlin、可 JVM 測試）
│   │   ├── data/                  Room、DataStore、Repository
│   │   ├── domain/                UseCase、Model
│   │   ├── ui/                    Compose 畫面
│   │   └── worker/                推播排程
│   └── src/main/assets/database/  預打包的字典 SQLite
├── 工作日誌/                       每日工作日誌
├── 今日紫微_專案報告.md             完整專案報告（接手者必讀）
├── 開發計畫書.md                   階段任務清單
└── dist/                          打包好的 APK
```

---

## 給接手者

第一次來的話請依序看：

1. [今日紫微_專案報告.md](今日紫微_專案報告.md) — 5 分鐘掌握全貌
2. [開發計畫書.md](開發計畫書.md) — 看現在做到哪、下一步是什麼
3. [工作日誌/](工作日誌/) — 看最近三篇了解最新脈絡

---

## 開發指令

```bash
# 一鍵編譯 + 安裝到模擬器（需先開 MSI App Player）
build_and_install.bat

# 或手動
gradlew assembleDebug
gradlew assembleRelease
```

---

## 下載與安裝

**v1.0.0 已產出**：[`dist/今日紫微-v1.0.0.apk`](dist/今日紫微-v1.0.0.apk)（18.0 MB，debug 簽名）

### 安裝到 MSI App Player

```
build_and_install.bat
```

腳本會自動：編譯 → adb connect 127.0.0.1:21503 → install → 啟動 App。

### 安裝到實體 Android 手機

1. 把 `dist/今日紫微-v1.0.0.apk` 傳到手機
2. 開啟「未知來源安裝」
3. 點擊 APK 檔安裝
4. 第一次啟動會看到免責聲明 → 同意後新增第一位家人 → 進入首頁

> **注意**：v1.0.0 是 debug 簽名，未來換成 release keystore 重新發版時，使用者必須先解除安裝才能裝新版（家人資料會清空）。建議目前只發給願意配合測試的試用者。

---

## 開發守則（從踩過的坑學來的）

1. 改 `family_members` schema 必寫 migration（長者不會重輸資料）
2. 演算法層保持純淨（`astro/` 不可 import Android Framework）
3. 字體一律用 `sp` 不用 `dp`
4. 觸控區一律 ≥ 48dp
5. 推播用 `OneTimeWorkRequest` 鏈式排程（PeriodicWork 不準時）
6. 離線優先（不引入網路依賴）
7. 不擴大 scope（順手做的衝動先記到日誌的「先不做」清單）

---

## License

私人專案，未開源。
