# 1.4.1 星軌漸層版

[開啟畫面總覽](index.html)。截圖來自專用 Android 35 AOSP 裝置，包含手機、寬畫面、淺深色與特大字。

- [深色首頁](home-dark.png)、[淺色首頁](home-light.png)
- [八主題卡片](topics-dark.png)
- [帳號](account-dark.png)、[授權說明](consent-dark.png)
- [家人](family-dark.png)、[編輯資料與滾輪](family-edit-dark.png)
- [歷史](history-dark.png)、[閱讀範例](reading-fixture-dark.png)
- [設定](settings-dark.png)
- [完整命盤](chart-light.png)、[左右捲動](chart-light-pan.png)
- [實際轉場影片](motion.mp4)、[關閉系統動畫的測試紀錄](reduced-motion.json)
- [特大字首頁](home-dark-extra.png)、[特大字帳號](account-dark-extra.png)、[特大字說明](consent-dark-extra.png)
- [寬畫面帳號](account-wide-dark-extra.png)

這次調整介面、動畫與圖示。沿用 1.4.0 的 ChatGPT 授權及生成程式，沒有真人登入、額度消耗或模型請求。歷史範例只存在專用測試裝置，明確標示為版面驗收 fixture，不會包進 APK。

build-test.log、lint.log、verification.json 與簽名文字檔記錄實際檢查。家人／歷史的 data-before.txt、data-after.txt 用於 1.4.0 → 1.4.1 覆蓋升級比對。沒有提交、推送或發布 GitHub Release。
