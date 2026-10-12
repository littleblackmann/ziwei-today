# 2026-10-02 · v1.2.0 帳號登入與介面候選版

## 範圍與狀態

使用者要求取消 API Key，先製作可用自己的 ChatGPT 帳號登入的版本，由使用者再測試。同步處理原先要求的外觀及更新提示。

原始 `master`／`origin/master` 為 `70a74ce47d705b56cdcebc783558d5a1a5aba01c`。修改位於本機 `codex/chatgpt-login-modernize`，尚未 commit、push 或發布 Release；原始 Git 狀態及舊 dist APK 保留。

GitHub 已有公開 repo：`littleblackmann/ziwei-today`，本輪讀取時沒有正式 Release。

## 實作

- 刪除金鑰輸入與教學路由；原教學檔送資源回收筒。啟動時只清除舊 DataStore `openai_api_key`，不清除資料庫。
- 官方動態註冊、Authorization Code + PKCE、state、nonce、RS256／JWKS 驗證。loopback 只綁定手機 `127.0.0.1`，最長等待五分鐘。
- Android Keystore AES/GCM 加密保存憑證；序列化登入／refresh／登出，保留穩定 host ID 與已驗證的 client registration。新帳號完成驗證才替換原帳號。
- 依帳號取得模型；Responses 請求使用 `store:false`、`stream:true`、array input。只有完整 `response.completed` 才保存，失敗與中斷不保存片段；額度錯誤提供管理入口。
- 首次登入說明方案額度、官方登入按鈕與官方 SVG 轉 Android vector logo。可更新模型、切換帳號、撤銷／登出。
- 靛藍淺／深色主題、首頁帳號卡片、漸層問候、卡片式設定、可換行字體選項、自適應主題網格。狀態列圖示同步 APP 強制深／淺色偏好，避免深色背景仍使用黑圖示。
- 修正歷史開啟日期、重複查詢／儲存點擊、快取標示、刪除家人後孤立的解讀。
- GitHub 最新穩定 Release 檢查、前景提示、WorkManager 約六小時背景檢查、Android 通知權限。只接受固定 repo 的正式新版及非空 APK；開啟 Release 頁由使用者安裝。
- `build.ps1` 預設輸出 R8 縮小的 release APK，`-Debug` 可用開發包；JVM 測試直接由 Java 執行，處理中文 Windows Gradle test worker classpath 問題。

## 本輪驗證

- `build.ps1 -Test`：release 編譯及 lintVital 通過；23 項 JUnit 測試通過（原演算法 13 項、新 OAuth／SSE／Release 10 項）。未使用真人帳號或付費生成。
- 曾發現測試 classpath 的 runtime classes.jar 未更新，已補上 task 依賴並重跑通過；不可把先前失敗那輪算成成功。
- 最終主要 APK：`dist/今日紫微-v1.2.0.apk`，2,638,216 bytes，SHA-256 `aaa45faeb0103b3363a49316af3fab0e8f45cdaa09c4ede9c3a2f07549e37e63`。
- APK 簽章驗證通過。發行相容憑證 SHA-256 `bf954c35579f6a0c6aa66216273e6dba27b0e3e0d8a3d70e507bcef686740331`，與原 dist APK 相符。本機發行簽名副本在忽略的 `.android-home/published-debug.keystore`，未修改全域 keystore，未將任何 keystore 提交。
- 原本另有開發憑證 `256e5390154e8598f3d9f5c27536491edec6630215c37cbf04be2eb73b727497`；同一最終 release APK 另簽為 `dist/今日紫微-v1.2.0-local-development.apk`，保留兩條升級路徑。大小／hash 在 `dist/v1.2.0-manifest.json`。
- `emulator-5554` 安裝舊 dist APK，透過 UI 建立 `UpgradeTest` 家人，再 `install -r` 覆蓋新版 debug 及最終 release，均成功；家人與聲明接受狀態保留。沒有 uninstall 或 clear data。
- 最終 release `am start -W` 回報 `Status: ok`，兩輪冷啟動約 10／33 秒（共享模擬器負載高）。debug 新類別首次驗證曾明顯卡頓；交付為縮小 release。
- 首頁及設定實際截圖保存在 `docs/preview`；最後 APK 的 `home-dark.png` 確認狀態列白色圖示及家人資料保留。點登入按鈕確實開啟 Chrome，但模擬器的 Chrome 尚處於初次初始化，本輪未完成 OpenAI 登入頁與回傳驗證。
- `git diff --check` 通過；API Key 控制已移除。WorkManager 在無 Release 情況完成，不阻擋首頁。設定手動檢查實際顯示「目前沒有可安裝的新版本」，淺／深色切換可用。

## 尚待使用者驗收

實際登入、手機瀏覽器回傳、refresh／遠端撤銷、帳號模型可用性及 AI 生成，需使用者登入測試。官方文件目前將 ChatGPT plan usage 列為符合資格的 Plus／Pro 功能，Free 帳號不能承諾取得推論額度。

更新是開啟 APP 與背景定期檢查，並非即時推播；公開通知的實際到達仍待正式發布較高版號及 APK 後驗證。其他人工驗收項目見 `docs/TESTING.md`，不能以單元測試代替全功能手機驗收。

沒有改系統設定、開機項、全域工具或其他 APP 資料，沒有結束共享的 emulator-5554。編譯後只停止本專案已閒置的 Gradle daemon。

官方參考：

- https://developers.openai.com/siwc/token-sharing-open-source
- https://developers.openai.com/siwc/token-sharing-open-source/sign-in
- https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference
- https://learn.chatgpt.com/docs/sign-in-with-chatgpt
- Logo：https://developers.openai.com/assets/siwc/sign-in-buttons/chatgpt-logo-white.svg

# 2026-10-03 · v1.2.1 登入診斷候選版

## 使用者回報與判斷

使用者在 MSI App Player 以 ChatGPT Free 帳號授權。截圖顯示 loopback 已接到可驗證的回傳，但 APP 顯示 HTTP 400。舊版沒有保留服務錯誤代碼，因此尚無法確定失敗是在 token 交換、公鑰讀取或模型清單，更不能把 Free 資格限制直接視為本次 400 的已證實原因。沒有取用或重送截圖中的真人授權碼。

重新讀取官方 sign-in／errors-and-recovery 文件，現有 token POST 欄位符合文件。以完全虛構 client／code／verifier 做一次合成請求，得到 OAuth `invalid_grant`；這只證明端點可達及錯誤解析形式，並未重現使用者的真實失敗。官方仍只列符合資格的 Plus／Pro 可分享方案額度；身分登入與可用推論額度要分開判斷。

## 修改

- `OAuthServiceError` 保留失敗階段、HTTP 狀態、已知機器代碼、回應格式與安全 request ID。原始 body、error_description、登入網址、code、token 不出現在錯誤 UI 或 clipboard。
- 設定增加「複製登入錯誤資訊」，包含 APP 版本及上述安全診斷。
- 初次動態註冊取得 client ID 後立即保存為 pending registration。交換失敗後下次登入使用這個 ID 並取得新的授權碼；不重送已失敗的碼。Pending ID 不代表已驗證帳號或登入，仍要完成 token、JWKS、audience 與 nonce 驗證。
- 登入 session、已驗證 client／subject 與 pending 清除改成一次 atomic preference commit；保留原已登入帳號，驗證新帳號成功後才切換。
- 更新憑證、驗證公鑰、讀取模型使用不同診斷階段；解讀頁也能顯示安全的憑證更新失敗訊息。
- `versionCode=13`／`versionName=1.2.1`；README、手動驗收表同步更新。

## 已驗證與交付

- `build.ps1 -Test`：release build、lintVital 與 28 項 JUnit 測試通過。新增測試覆蓋實際 token form builder 的 client／redirect／resource／PKCE／編碼，以及 OAuth 錯誤分類、未知回應與憑證文字不洩漏。
- 主要 APK：`dist/今日紫微-v1.2.1.apk`，2,638,220 bytes，SHA-256 `f47f45dbf462a0391f35068d72e52eb74468b00963a8bfae6d3e17b2935011ce`；原發行相容簽章驗證通過。
- 開發簽名 APK：`dist/今日紫微-v1.2.1-local-development.apk`，2,647,207 bytes，SHA-256 `d63ec42ea822dd17e7f0fb87c60e678d34bbd3fa9b51310313e714b2cbd7f336`；開發簽章驗證通過。兩者 manifest 在 `dist/v1.2.1-manifest.json`，舊 APK 保留。
- MSI App Player 本機設定顯示 ADB 未開啟，未更改其系統設定。真實登入需使用者覆蓋新版後操作，失敗時提供安全診斷文字。
- 本輪對 Android Studio `emulator-5554` 的 `install -r` 與啟動未取得結果，連 `pm list packages` 也在 15 秒後逾時；不可視為覆蓋安裝驗證成功。停止本輪兩個逾時的 ADB client，保留 ADB server、共享模擬器及其資料，沒有卸載或清除資料。
- 未 commit／push／建立 Release，未使用 API Key 或付費生成。只停止本專案已確認 IDLE 的 Gradle daemon。

HTTP 400 根因與實際帳號登入尚未驗證；此版為可重試、可取得原因的修正候選版，不能宣稱登入已恢復。

官方參考：

- https://developers.openai.com/siwc/token-sharing-open-source/sign-in
- https://developers.openai.com/siwc/token-sharing-open-source/errors-and-recovery
- https://learn.chatgpt.com/docs/sign-in-with-chatgpt

# 2026-10-03 · v1.3.0 OpenRouter 帳號與免費解讀候選版

## 授權範圍與介面決定

使用者同意試用 OpenRouter 的 NVIDIA 免費模型，並要求日常畫面不要展示 AI 品牌或技術提醒，讓每個人登入自己的帳號。改為「我的帳號」「登入帳號」「重新解讀」；授權前、帳號管理及資料說明仍保留 OpenRouter 身分與資料流向。這是 OpenRouter 帳號授權，不是 ChatGPT 帳號授權；沒有手動 API Key 欄位，官方授權在內部產生的使用者憑證由 Android Keystore AES/GCM 加密保存。

## 修改

- 新增 OpenRouterProtocol、AccountCredentialStore、OpenRouterHttpClient、AccountRepository：S256 PKCE、每次隨機 callback 路徑、IPv4 loopback、單次 code 交換、五分鐘等待與安全錯誤診斷。新憑證通過額度端點驗證及保存才替換原帳號；可取消、重新登入與切換。
- ServiceLocator／ViewModelFactory 改接新帳號與 ReadingClient；原 ChatGPT 協定檔保留歷史回歸，沒有接到 1.3.0 帳號或生成流程，原憑證不轉作新服務憑證。
- 固定 `nvidia/nemotron-3-super-120b-a12b:free`，每次查詢確認目錄價格為零及 structured output 能力，請求再設 provider.require_parameters 與 prompt/completion 零價格上限。沒有付費備援、模型清單或自動重送生成。
- Chat Completions JSON schema 保留解讀格式；串流只取內容，忽略 reasoning，要求 stop 與 DONE，拒絕截斷、錯誤、拒絕和不符合 JSON 格式的結果，不保存半段解讀。成功結果保存前確認仍是同一連線憑證。
- GET /key 讀取官方回報的免費請求餘額；401 清除失效的本機憑證，額度或服務錯誤提供中性訊息，不展示原始回應或秘密。管理頁 URL 使用憑證的 SHA-256，沒有原始憑證。
- 登出只移除此裝置憑證，官方遠端刪除需要額外管理權限，本 APP 不索取；使用者可經管理頁自行撤銷。
- 新授權說明可捲動；首頁、設定、解讀及歷史用 APP 日常語言。移除 ChatGPT 標誌、模型選單與啟動方案提醒。
- 生成 prompt 不傳家人姓名、完整出生年月日時或本機 ID；仍有生年干支等命盤摘要，不能宣稱完全匿名。家人資料與歷史保持原資料庫。
- versionCode=14／versionName=1.3.0。README、docs/TESTING.md 同步目前服務及驗收範圍。原更新檢查機制保持啟動／前景／約六小時背景檢查，不是即時推播。

## 驗證與交付

- `build.ps1 -Test` 最後一輪 Release build、lintVital 與 43 項 JUnit 通過。新增 15 項覆蓋 PKCE、callback 綁定、憑證保密、免費模型策略、串流完整性與真實 loopback HTTP fixture；保留原 28 項。
- 前一輪編譯通過但一項 HTTP fixture 測試逾時，因測試伺服器以字元數讀取 UTF-8 request body。改為位元組數後完整重跑 43 項通過，不把失敗那輪算成功。
- 主要 APK `dist/今日紫微-v1.3.0.apk`：2,603,200 bytes，SHA-256 `18b1cbae53a57ae4b47bc9ea8b9479a2e11021f02345f49a17e25b7608a2ffee`。原發行相容簽章驗證通過，憑證 SHA-256 `bf954c35579f6a0c6aa66216273e6dba27b0e3e0d8a3d70e507bcef686740331`。
- 開發簽名 APK `dist/今日紫微-v1.3.0-local-development.apk`：2,610,287 bytes，SHA-256 `6c05673d09ec7ba4947f90c4821bd6a1c6ef0ee4dc120869527f2cad0d3830d6`。開發簽章驗證通過，憑證 SHA-256 `256e5390154e8598f3d9f5c27536491edec6630215c37cbf04be2eb73b727497`。manifest 在 dist/v1.3.0-manifest.json；舊 APK 保留。
- aapt 無法處理本機中文 APK 路徑，改讀取 SHA-256 相同的 ASCII 暫存副本，確認 package=com.zwds.today、versionCode=14、versionName=1.3.0。沒有以檔名代替 manifest 驗證。
- Android Studio emulator-5554 的 package/activity 服務查詢均逾時；停止本專案 IDLE Gradle daemon 後最後一次 pm 查詢仍在八秒逾時。本輪未安裝、啟動或拍攝 1.3.0。只停止自己的查詢 client，保留 ADB server、共享模擬器與資料；未碰另一個 emulator-5580。
- MSI App Player ADB 保持未開啟，未更改系統設定。沒有使用真人 OpenRouter 憑證、產生真實模型請求或消耗帳號額度，繁體中文解讀品質、帳號授權、切換／撤銷及裝置覆蓋升級仍需實測。
- git diff --check 通過。仍在本機 codex/chatgpt-login-modernize，HEAD=70a74ce；未 commit、push 或發布 Release，未碰 ChefAIToolbox 專案。未修改全域工具、全域 keystore、排程或啟動項。

官方依據（本輪重新查詢）：

- https://openrouter.ai/docs/guides/overview/auth/oauth
- https://openrouter.ai/api/v1/models
- https://openrouter.ai/docs/guides/features/structured-outputs
- https://openrouter.ai/docs/guides/routing/provider-selection
- https://openrouter.ai/docs/api-reference/chat-completion
- https://openrouter.ai/docs/api_reference/limits
- https://openrouter.ai/docs/api/api-reference/api-keys/delete-keys
# 2026-10-03 · v1.3.1 參考 AI 廚房的獨立帳號流程

使用者要求查看 AI 廚房目前的 OpenRouter 改版，表示它的登入方式更接近需求。本輪唯讀檢查 E:\AI廚房\ChefAIToolbox 的 OpenRouterSetupScreen、OpenRouterAuth、manifest、最新畫面與該專案工作紀錄，沒有寫入或要求另一個任務修改其專案。兩者底層原本就同為官方 PKCE；參考的是獨立登入頁、分階段進度及瀏覽器的返回 APP 入口，保留使用者先前要求的日常介面不展示品牌／模型。

## 變更

- 新增 AccountScreen，「我的帳號」獨立於一般設定。首頁帳號卡片、設定入口與解讀錯誤都進入同一頁；登入、狀態更新、重新登入、切換、管理與本機登出集中在這裡。
- LoginStep 明確顯示瀏覽器授權、驗證、準備解讀；保留一個 application-scope 登入作業，repository 可取消目前作業，避免離開頁面再返回時無法取消。
- 帳號連線與免費服務目錄確認分開，只有驗證目錄後才開啟「開始今日解讀」；目前免費次數為零時不提供此捷徑。
- 新增靜態 AccountReturnPage，提供 ziweitoday://login-complete 的「返回 APP」。該 URI 僅導航，沒有 code/key/token，不能標記帳號已授權；只接受完全相符的 URI。真正的 code 仍由綁定本次 PKCE 的 loopback 交換，callback 頁寫明正在驗證，不假稱已登入完成。
- MainActivity singleTask／onNewIntent 接回原 APP，accepted disclaimer 後才導到帳號頁；返回不重建第二個登入作業。loopback HTML 使用 no-store 與限制資源的 CSP。
- versionCode=15／versionName=1.3.1；免費生成策略與資料庫 schema 保持 1.3.0 設計。README、docs/TESTING.md 同步。

## 驗證

- build.ps1 -Test：Release build、lintVital 及 45 項 JUnit 全通過。新增兩項測試涵蓋返回 URI 不能含授權資料／偽造 query、頁面區分 callback 與驗證完成。其餘 43 項保留。
- 主要 APK：dist/今日紫微-v1.3.1.apk，2,603,380 bytes，SHA-256 0899d1d7bc8f65584c1e3b561cef44d9eb193e92e5f2090f20e8a7d07bca6b6b；原發行相容簽章通過，憑證 SHA-256 bf954c35579f6a0c6aa66216273e6dba27b0e3e0d8a3d70e507bcef686740331。
- 開發簽名 APK：dist/今日紫微-v1.3.1-local-development.apk，2,610,287 bytes，SHA-256 515567bec702f23f8c90acdeae8fc48ec10ecaaff3cd378d9a1fad5c69950c72；開發簽章通過，憑證 SHA-256 256e5390154e8598f3d9f5c27536491edec6630215c37cbf04be2eb73b727497。兩者記錄於 dist/v1.3.1-manifest.json。
- aapt 以 ASCII 暫存副本確認 package=com.zwds.today、code15、1.3.1；直接讀最終 APK DEX，確認新增帳號頁標題、驗證進度與返回 URI 確實打包。
- 本輪另建專用 Ziwei_QA131／emulator-5582，AVD 設定來源只唯讀參考另一專案的 config，寫入自己的新 TEMP 目錄，沒有共用其可寫磁碟。先安裝 1.3.0，接受說明，在 UI 建立測試家人；ADB 輸入 LoginFlowQA 實際只形成「Log」，以實際已保存的「Log」作為升級驗證值，沒有假稱完整名稱已輸入。
- install -r 覆蓋 1.3.1 成功，am start -W Status ok／COLD 約3.2秒，已安裝版本15／1.3.1；家人「Log」及聲明接受狀態保留。首頁點帳號卡片確實進新 AccountScreen。新版本實際 home.png、account.png 和 XML 在 docs/preview/v1.3.1。
- 後續 UiAutomation.connect 多次逾時／dump 工具程序崩潰；截圖 client 與 am start 返回連結測試也逾時，根因未確定。不能把登入說明、進度或瀏覽器返回列為裝置實測成功。consent.png 拉取到的影像仍為先前帳號頁，不是授權對話框證據。
- 沒有輸入真人 OpenRouter 帳密、交換真人憑證、呼叫模型或消耗生成額度。本人帳號登入、實際解讀、瀏覽器返回、切換／撤銷、特大字／深色仍需使用者裝置驗收。
- 本輪停止自己的逾時截圖／探測 client，最後對本輪 emulator-5582 發出 emu kill，取得 OK；自己的閒置 Gradle daemon 亦已停止。未操作／結束共享5554、另一專案5580，未開啟 MSI ADB，未改系統／啟動／全域設定。TEMP QA 資料保留。
- 未 commit、push 或發布；舊 APK 及原未提交修改保留。新增 source 的 Kotlin 編譯與 lint 通過；git diff --check 通過。

Android 返回與 task 行為參考：
- https://developer.android.com/guide/components/activities/tasks-and-back-stack
- https://developer.android.com/guide/navigation
# 2026-10-12 · v1.4.0 ChatGPT 會員整合與全介面改版候選版

使用者明確要求移除 OpenRouter，只保留 OpenAI 自己的會員，並重新調整整體美觀。本輪沿用原工作目錄，保留先前未提交修改；開始前另將 app/src、app/build.gradle.kts、README 與 WORKLOG 複製至 C:\Users\kchbk\AppData\Local\Temp\ziwei-before-1.4-20261012-152056。沒有修改 AI 廚房專案。

## 實作

- 移除 OpenRouter 登入、HTTP client、免費模型／次數邏輯及其測試檔；舊檔經資源回收筒處理。僅保留 LegacyCredentialCleanup 的舊 shared preferences 名稱及 Keystore alias，用於清除舊連線，不觸及家人或歷史。舊手動 API Key 欄位於啟動清除。
- ServiceLocator、帳號與解讀 ViewModel 改接 ChatGptAuthRepository 和 ReadingClient；使用官方 Sign in with ChatGPT 與會員額度授權，不提供手動 API Key 或其他供應商備援。
- PKCE、state、nonce、RS256/JWKS 與 issuer/audience/subject/time claims 驗證，保留本次 loopback URI 與已發出的 client ID。註冊使用穩定 host ID；憑證以 AES/GCM 原子保存。登入程序可取消，離開帳號頁仍維持同一 application-scope 作業。
- 帳號身分、會員權限、可用模型及生成就緒狀態分開。模型依帳號官方目錄順序選取，不再寫死模型或展示日常模型選單。帳號頁與授權說明保留 Plus／Pro 資格、額度共用、資料傳送及撤銷資訊。
- Responses 使用 store=false、stream=true、JSON schema；只接受完整 completed 終態與有效解讀格式，不保存截斷、拒絕或半段內容。取消、登出或切換帳號會使舊 generation receipt 失效，歷史保存前再次確認。
- 登出先清除本機連線並嘗試官方撤銷；撤銷未確認會明確顯示。安全診斷只顯示階段、HTTP 與清理後的 request ID，不展示授權碼、憑證或原始服務錯誤。
- 首頁、帳號、家人、新增／編輯、主題、解讀、歷史、設定、完整命盤與首次說明統一改為米白、墨藍、淡金。更新明暗色 surface tokens、按鈕、字體階層、星曜視覺與寬螢幕置中；保留三級字體、深色及觸控區域。
- 家人日期滾輪改用公開 API，移除私有反射；出生年支援 1920 至目前年。完整命盤保留可讀宮位尺寸、水平滑動及宮位內捲動。保留更新檢查、農曆、晚子時、家人與歷史快取。
- versionCode=16、versionName=1.4.0；README、docs/TESTING.md 與畫面索引同步。

## 建置與實際裝置驗收

- 最終 build.ps1 -Test：Release、R8/minify、lintVital、36 項 JVM 測試全通過；另跑 lintDebug 通過，0 errors / 31 warnings。移除已不用的 OpenRouter 測試後加入會員模型目錄、generation guard、Responses payload 等回歸。git diff --check 通過。
- 最終主要 APK：dist/今日紫微-v1.4.0.apk，2,638,388 bytes，SHA-256 9df80614ef51bacd5660cfee5b39f05d48cdaa821e65755c29f7d0bf184be953；簽章驗證通過，憑證 SHA-256 bf954c35579f6a0c6aa66216273e6dba27b0e3e0d8a3d70e507bcef686740331，與同類 1.3.1 相符。
- local-development 相容包：dist/今日紫微-v1.4.0-local-development.apk，2,647,207 bytes，SHA-256 bd05ad366336e040b0ae845444b56a2e530a8f0b087d9abdd7ea738b8bde90e8；簽章驗證通過，憑證 SHA-256 256e5390154e8598f3d9f5c27536491edec6630215c37cbf04be2eb73b727497，與同類舊包相符。
- 以 SHA-256 相同的 ASCII 暫存副本執行 aapt，及實際安裝的 dumpsys package，確認 com.zwds.today / code16 / 1.4.0 / min26 / target35。
- 自建 TEMP Android 35 AOSP AVD，專用 emulator-5582、ADB server port5054。先裝 1.3.1，在 UI 接受說明、建立 QA（1955-06-15，6時）家人、選深色大字。專用 DB 加入一筆明確標示版面驗收的歷史 fixture；專用 shared preferences 加入無效舊憑證 fixture，沒有真人資料或憑證。
- install -r 覆蓋 1.4.0 成功；data-before / data-after / data-final 正規化比對一致，家人、歷史、首次說明與深色／大字設定保留。舊 OpenRouter shared preferences 清為空 map。Fixture 只在專用裝置，不包含於 APK，也不是線上 AI 生成。
- 實際檢查淺色、深色、特大字、900x1950 手機與 2400x1350 寬畫面；檢查帳號與授權對話框、取消、返回入口、家人編輯日期滾輪、八主題、未登入解讀錯誤、歷史原日期閱讀、完整命盤左右滑動與手動更新檢查。最終 PNG/XML 在 docs/preview/v1.4.0。
- ziweitoday://login-complete 只開帳號頁，未變成登入成功；含 code query 的偽造返回 URI 未被接受。實際真人瀏覽器授權與 callback code 交換沒有測試。
- Crash buffer 中保留一次 Android uiautomator 連線失敗，未發現本 APP 的 AndroidRuntime crash。先前 Android 37 專用環境出現 launcher/SystemUI/automation 不穩定，保留 failed-api37 證據，未將它算成 APP 驗收成功；最後以 Android 35 完成。

## 明確未驗證／未發布

沒有真人 ChatGPT OAuth、會員額度扣用、線上模型生成或品質測試；Free 帳號、此 APP 的 OpenAI 預覽／分發資格也未確認。UI 與合成協定測試通過不能代表這些項目成功。官方目前說明符合資格的 Plus／Pro 可授權會員使用，須依實際授權結果判斷。

沒有 commit、push 或 GitHub Release；更新通知仍依既有正式 Release 檢查機制，不會把本機候選 APK 當成已發布版本。未操作其他專案 5554／5580 模擬器、未開 MSI ADB、未修改全域 SDK 安裝或系統設定。專案本機及 TEMP QA 依賴保留。

官方依據（本輪核對）：https://developers.openai.com/siwc/quickstart 、https://developers.openai.com/siwc/token-sharing-open-source/sign-in 、https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference 、https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations 。

本輪收尾：已結束本輪 Ziwei_QA140_35 專用 emulator／qemu 與 port5054 ADB server，保留 TEMP 資料；沒有停止其他裝置。


# 2026-10-12 · v1.4.1 星軌漸層與漸進式轉場

使用者表示人在外面，要求先重新設計成現代、帶未來感與「漸進式」的介面，回家再測試。將需求落實為星空深藍、藍紫青漸層、柔和光暈、星軌、細邊框卡片，以及短暫依序淡入與頁面轉場；淺色版為冰藍／淡紫。開始前完整保存本次來源至 TEMP/ziwei-before-future-141-20261012-183709，保留舊 APK 與先前未提交狀態。

- 共用主題與背景、首頁、八主題卡片、家人、歷史、帳號、設定、閱讀與命盤皆套用新設計。主按鈕使用漸層，新增自適應／單色星軌 icon 與配套 splash。沒有使用常駐背景動畫或新增套件。
- ProgressiveReveal 使用 Animatable 與 Compose 系統動畫縮放；頁面使用短暫 fade/slide。透明 Scaffold／卡片明確指定文字色，修正實際深色截圖發現的文字繼承問題。特大字首頁標語縮短，避免尾端只剩兩個字。
- 本輪沒有修改帳號授權、資料儲存、生成、命盤演算法或更新機制；42 個 data/domain/astro 原始檔逐一 SHA-256 與開始前快照相符。OpenRouter 維持移除，不加入其他供應商或 API Key。
- 最終 versionCode=17 / versionName=1.4.1。Release/R8、36 項 JVM 測試、lintDebug 通過（0 errors / 31 warnings）；git diff --check 通過。沒有為純外觀變更新增鏡像式測試。
- 重用本專案專用 Android 35 AOSP AVD／emulator-5582，獨立 port5054 ADB。從 1.4.0 直接 install -r 升級，家人 QA 與 2026-10-11 歷史 fixture 的前後 DB 列一致。Fixture 明確標示版面驗收，並非線上 AI 生成，也不在 APK 內。
- 實際檢查淺／深色、標準／特大字、900x1950 手機、2400x1350 寬畫面，以及家人資料滾輪、八主題、帳號與取消授權、未登入提示、歷史原日期閱讀、完整命盤橫向捲動。當專用裝置 animator_duration_scale=0 時，首頁可見且可進主題、返回；測試後已恢復原 unset 值，未修改主機或使用者真機設定。
- 保存八秒 screenrecord 實際頁面轉場，退出碼0。圖片、XML、影片、建置日誌、簽名與 verification.json 在 docs/preview/v1.4.1，index.html 提供本機響應式總覽。最終裝置 crash buffer 為空。
- 主要 APK dist/今日紫微-v1.4.1.apk：2,036,744 bytes，SHA-256 729344deb66ec9e42f29d1fc272040907233eadf8f4ccbc4baa1e7ce4c15c445。簽章驗證通過，原發行相容憑證 bf954c35579f6a0c6aa66216273e6dba27b0e3e0d8a3d70e507bcef686740331。
- local-development APK：2,044,983 bytes，SHA-256 c60596719b300583c7961971bc922dfa7fdefafc96643865c563f255d053e534。簽章驗證通過，憑證 256e5390154e8598f3d9f5c27536491edec6630215c37cbf04be2eb73b727497。manifest 以 hash 相同的 ASCII 副本確認 package=com.zwds.today、1.4.1/code17/min26/target35，另以實際 dumpsys 核對。

真人 ChatGPT 登入、會員授權、額度使用、線上生成與 OpenAI 預覽／分發資格仍未驗證，不能因外觀與協定測試通過而宣稱可用。沒有 commit、push 或 GitHub Release；沒有操作其他專案裝置，沒有全域安裝或修改系統／啟動設定。

收尾已結束本輪 Ziwei_QA140_35 專用程序與 port5054 ADB server；保留 TEMP 裝置資料與驗收檔，未操作其他裝置。


# 2026-10-13 · v1.4.2 Plus 登入後解讀空白錯誤檢查

使用者回報自己的 Plus 已登入，但「整體運勢」顯示「AI 未回傳解讀內容，請重試」。此畫面可定位至 ResponsesStream 的完成事件空白檢查，但未取得真人串流、HTTP 原始資料或額度事件，不能斷言 Plus 資格或單一線上根因。開始前保存 app/src、build.gradle.kts、README 與 WORKLOG 至 C:/Users/kchbk/AppData/Local/Temp/ziwei-before-stream-142-20261013-004140，保留所有原未提交修改及舊 APK。

- 官方會員推論範例逐段處理 response.output_text.delta，並要求等到 response.completed 才算成功。舊版只讀最後 response.output，忽略前面的所有文字事件。先新增「兩段文字 + completed/output=[]」回歸：舊版確實重現相同顯示訊息，JUnit 5 項中 1 項失敗；證據在 repro-before-result.txt。此為可重現的解析缺陷，並非取得使用者真實回應。
- 新版依 output_index/content_index 收集文字，output_text.done、content_part.done、output_item.done 的完整快照覆蓋對應片段，避免重複。完整終態 output 優先；緊湊終態缺少重複的 output 時使用已收集內容。仍要求 response.completed/status=completed 及 ReadingPayload 的完整 JSON 驗證；明確空白訊息不沿用較早的半段文字。
- 拒絕、串流中後段的額度錯誤、response.incomplete、錯誤狀態、斷線、非 JSON 事件與過大串流分別處理。只讀取 assistant/output_text，不加入 reasoning 或 tool arguments。限制總串流大小，拒絕與服務錯誤不顯示原始描述。
- 解讀錯誤卡片新增預設收合的資訊與複製按鈕。資訊只含版本、HTTP、已知碼、事件／文字計數、終態／輸出形狀及經既有白名單清理的 request ID，不含憑證、email、命盤、提示詞或解讀文字。HTTP 失敗保留安全診斷；正常 JSON 格式驗證失敗不捏造原始回覆格式。
- 沒有改動會員登入與加密保存、家人、歷史資料庫、命盤演算法、更新機制或其他外觀。與開始前快照逐一比對 main/java，只有 ReadingClient、ResponsesStream、TopicResultViewModel、TopicResultScreen 四檔不同。沒有 API Key 或 OpenRouter 備援，也沒有自動重試以免額外消耗額度。
- versionName=1.4.2/versionCode=18。最終 Release/R8 與 50 項 JVM 測試通過（新增 14 項），包含多層文字快照、排序、不重複、終態優先、各層拒絕、遲來額度錯誤、空白、未完成、格式錯誤、SSE 多行／CRLF／EOF、總大小限制及串流內容進完整解讀驗證。最終 lintDebug 通過：0 errors / 31 warnings。git diff --check 通過。
- 重用本專案專用 Android 35 AVD Ziwei_QA140_35／emulator-5582、ADB port5054。1.4.1 直接 install -r 升級 1.4.2 成功，QA 家人及 2026-10-11 歷史 fixture 前後與最後 DB 列完全相同。未接觸 MSI 使用者裝置或其他 ADB server。
- 錯誤 UI 使用 TEMP/ziwei-stream-ui-142 獨立副本的 QA 啟動頁，呼叫正式 ResponsesStream 和 TopicResultErrorPanel，明確標示非真人 AI 回應；只替換專用 QA 模擬器的 APP。實測空白、拒絕及額度錯誤，展開／收合／複製與 Android 剪貼簿預覽成功；標準／特大字及淺／深色可讀、可捲動。QA 啟動頁不在正式來源或發行 APK，驗收後已安裝回最終 APK並再次核對資料。
- 啟動專用裝置時曾出現 System UI ANR 及一次 null-root UI dump；舊 helper 會讀到舊 XML。本輪另建 TEMP helper，只接受成功 dump，再保存 XML，穩定後重新擷取有效畫面，初始舊 XML 未計入成功。最終 crash buffer 為空。
- 主要 APK：dist/今日紫微-v1.4.2.apk，2,036,744 bytes，SHA-256 276f856b1128a4219c64176b8486fdf418c826aa993abe24d1a9cb5b9e0a4e28。簽章通過，憑證 bf954c35579f6a0c6aa66216273e6dba27b0e3e0d8a3d70e507bcef686740331，與同類 1.4.1 相同。
- 開發相容 APK：dist/今日紫微-v1.4.2-local-development.apk，2,044,983 bytes，SHA-256 10dbf9b52ddb81f8a7924cbb2183d7eef29734bba91e3fc4552f3ab415daceec。簽章通過，憑證 256e5390154e8598f3d9f5c27536491edec6630215c37cbf04be2eb73b727497，與同類舊包相同。以 hash 相同 ASCII 副本 aapt 及最終 dumpsys 確認版本18／1.4.2；直接檢查最終 DEX，確認新解析與錯誤資訊字串存在、QA 啟動頁不存在。

證據在 docs/preview/v1.4.2/verification.json、建置／測試／lint 日誌、PNG/XML、簽名與 dist/v1.4.2-manifest.json。README 與 docs/TESTING.md 已更新。真人 Plus 的實際串流、額度與修正後生成仍需使用者覆蓋安裝後重試；若失敗可複製錯誤資訊繼續定位。沒有 commit、push 或 GitHub Release。已結束自己的 QA emulator/qemu 與 port5054 ADB server，保留 TEMP 證據與測試資料；未改系統設定或全域安裝。

本輪核對官方依據：
- https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference
- https://developers.openai.com/api/reference/resources/responses/streaming-events
- https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations

# 2026-10-13 · v1.4.2 GitHub 發布與作品網站同步

使用者確認成果後，明確授權上傳 GitHub 並更新 E 槽作品網站。本輪發布既有驗證過的 1.4.2，不再次改動 Android 功能、會員機制或資料。

- 程式與既有驗收文件提交 a76983567a4860c5611ed1971abcb0a184095c08，推至 littleblackmann/ziwei-today 的 master；沿用原 PUBLIC 可見性。已檢查暫存項目，不包含 APK、keystore、local.properties 或 SDK；文字憑證樣式掃描未見私密 Key。歷史原始診斷日誌保留，發布的來源空白檢查排除原始 preview 證據。
- v1.4.2 正式 Release 指向上述 source commit；先建 draft，回下載兩包 APK／checksum／manifest 核對後發布。公開 releases/latest 回傳 v1.4.2、draft=false、prerelease=false、4 assets；匿名下載兩包 SHA256 與原本驗證版本相同。
- 公開安裝包使用 ASCII 名稱 ZiweiToday-v1.4.2.apk 及 ZiweiToday-v1.4.2-local-development.apk；原憑證與舊版相容性不變，保留 SHA256SUMS 與含 source commit／憑證的 manifest。Publish-Release.ps1 同步固定 ASCII 名稱並指定 source commit，避免未來網站挑錯資產。
- 以正式 ReleaseInfo.parse 讀實際公開 JSON：安裝 1.4.1 偵測 1.4.2、安裝 1.4.2 不提示。未在使用者真機等待背景排程或代按通知／安裝。
- 網站 E:/小黑的網頁/littleblackmann.github.io-main 的紫微介紹從 v1.0.0 更新為 v1.4.2，替換 Google Drive、舊 API Key 教學與舊畫面。新增 9 張示範截圖、會員資格與資料傳送說明、更新與覆蓋安裝方式；保留暖金風格及其他作品。動態主 APK 下載有嚴格來源／名稱核對與 GitHub 頁面備援，17 個情境通過。
- 同機 AI 廚房任務已先提交 4cb8ff6；本輪保留所有 chef 內容並分離提交 15 個紫微相關檔案，網站 commit 09d084f0596f833a2ba288b602035202b306ed2c 已推 main。六個其他作品的 JSON 項目逐一相同，共用快取標記為 ziwei142-chef092。
- Pages run 38284354042 success、latest build=built 且 commit 相同。正式首頁／紫微 HTML／JS／projects.json／sitemap／lightbox 與 9 張 PNG 共 15 個檔案 HTTP200、SHA256 與本機相符（文字 CRLF/LF 正規化，PNG exact）。本機及正式網頁六種寬度無溢出；放大／下一張／Escape／關閉與實際下載 href 確認。

發布驗證：docs/publish/v1.4.2/verification.json；原 APK 測試證據仍在 docs/preview/v1.4.2，不改寫當時未發布的歷史紀錄。Ignored dist/publish-audit 保存回下載、公開 metadata、頁面截圖與詳細比對。真人 Plus 修正後生成、額度／分發資格與 Free 仍未驗收，不因 GitHub 上傳或網頁上線而宣稱通過。

正式下載：https://github.com/littleblackmann/ziwei-today/releases/tag/v1.4.2
正式介紹：https://littleblackmann.github.io/projects/ziwei-today.html
