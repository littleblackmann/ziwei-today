# 今日紫微

Android 紫微斗數生活提醒 APP。命盤在裝置計算，新解讀透過使用者授權的 ChatGPT 會員額度生成。

## 1.4.2 解讀串流修正候選版

- 收集逐段文字及各層完成快照；最後完成事件未重複附上文字時，可使用已收齊的內容。完整終態快照仍優先，避免重複拼接。
- 仍要求 response.completed、completed 狀態與有效 JSON。中斷、額度失敗、拒絕或不完整回覆不保存；拒絕會顯示明確提示。
- 解讀失敗可展開並複製錯誤資訊，包含版本、HTTP、串流事件數、文字字元數、已知錯誤碼與清理後的 request ID；不包含命盤內容、原始回覆、token 或 email。

- 移除 OpenRouter 登入、模型路由、免費次數查詢及舊連線程式。升級時只清除該連線的本機憑證，保留家人與歷史資料。
- 只使用 OpenAI 官方 Sign in with ChatGPT 與會員額度授權；沒有 API Key 輸入或開發者付費 Key 備援。
- 符合資格的 Plus／Pro 帳號可依官方授權結果使用會員額度。Free、會員資格、APP 註冊是否被接受及真實生成必須分別驗證。
- 登入透過系統瀏覽器、PKCE、state、nonce、RS256 ID Token 驗證與 loopback callback。返回連結只導航，不傳遞授權碼或憑證。
- 憑證以 Android Keystore AES/GCM 保存，更新的 refresh token 原子寫入。登出會清除本機連線並嘗試撤銷遠端授權。
- 解讀使用當前帳號回報的可用模型，保留官方目錄順序；日常介面不展示模型名稱。使用 Responses 串流，store=false；只有完整成功事件與有效格式才保存。切換帳號、登出或取消後不保存舊連線的結果。
- 首頁、帳號、家人、主題、解讀、歷史、設定與說明統一為星空深藍、藍紫青漸層、星軌與細緻邊框；淺色版搭配冰藍／淡紫背景，保留三級大字。加入短暫分階段淡入與頁面轉場，依系統動畫縮放設定；沒有常駐動畫迴圈。完整命盤可左右滑動，宮位內可上下捲動。
- 保留 GitHub Release 更新提醒、家人資料、歷史快取、農曆輸入與晚子時選項。

v1.4.2 提供 GitHub 發行下載；編譯與合成協定測試不代表真人登入、會員額度或推論已成功。真人帳號可用性仍依授權與實際生成結果確認。

## 安裝與驗收

[下載最新發行版](https://github.com/littleblackmann/ziwei-today/releases/latest) · [v1.4.2 更新說明](docs/RELEASE-v1.4.2.md)

主要 APK：dist/今日紫微-v1.4.2.apk。沿用原發行包的憑證；直接覆蓋相容簽名的舊版，不需要解除安裝或清除資料。

若遇「簽名不符」，請先確認原安裝來源。舊 local-development APK 使用另一份簽名，對應檔案為 dist/今日紫微-v1.4.2-local-development.apk；兩份新版 APK 的簽名均已與同類舊版比對。不要為了安裝新版而刪除有家人資料的 APP。

流程：我的帳號 → Continue with ChatGPT → 閱讀授權說明 → OpenAI 官方頁面登入與授權 → 返回 APP 確認權限 → 開始今日解讀。

登入失敗可複製安全的階段、HTTP 狀態與 request ID；解讀失敗請按「檢視錯誤資訊」→「複製錯誤資訊」。請勿分享含授權碼的瀏覽器網址。

完整驗收與實際證據：[docs/TESTING.md](docs/TESTING.md)、[docs/preview/v1.4.2](docs/preview/v1.4.2)。1.4.1 外觀驗收仍保留在 docs/preview/v1.4.1。

## 開發

Kotlin 2.1、Compose Material 3、Room、DataStore、OkHttp、WorkManager；minSdk 26、targetSdk 35。

```powershell
.\build.ps1 -Test
.\build.ps1 -Debug
.\build.ps1 -Install -Device emulator-5582
```

SDK、Gradle 與 keystore 使用專案忽略的本機目錄。腳本不會自動提交、推送或發布。

## 發布與更新

儲存庫：[littleblackmann/ziwei-today](https://github.com/littleblackmann/ziwei-today)。

提高版本、編譯，再依需求使用 tools/Publish-Release.ps1 建立正式 Release，附相容簽名 APK。APP 啟動／回到前景與背景約六小時檢查固定 repo 的正式 Release。背景檢查受 Android 調度與通知權限影響；更新由 Android 安裝畫面確認。

會員額度整合目前依 OpenAI 的預覽資格與分發條件提供。官方文件區分開源／本機應用與付費／遠端代管應用；公開分發前需確認適用資格及授權，不能把本機候選版當成獲准正式上架。

## 官方整合文件

- [Sign in with ChatGPT](https://developers.openai.com/siwc/quickstart)
- [ChatGPT plan usage](https://developers.openai.com/siwc/token-sharing-open-source)
- [Registration and sign-in](https://developers.openai.com/siwc/token-sharing-open-source/sign-in)
- [Models and inference](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference)
- [Preview limitations](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations)

舊報告與工作日誌只記載各次歷史狀態，不代表現行整合或實測結果。
