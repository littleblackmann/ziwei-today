# 1.4.0 畫面與驗收紀錄

本資料夾是專用 Android 35 模擬器的實際截圖與 UI XML；沒有使用真人帳號、沒有執行會員 OAuth 或線上生成。

- [首頁（淺色）](home-final-light.png)
- [我的帳號（淺色）](account-light.png)
- [授權說明（淺色）](consent-light.png)
- [首頁（深色特大字）](home-dark-extra.png)
- [帳號（深色特大字）](account-final-dark-extra.png)
- [授權說明（深色特大字）](consent-dark-extra.png)
- [帳號（寬畫面）](account-wide-dark-extra.png)
- [我的家人](family-light.png)
- [家人資料與日期滾輪](family-edit-light.png)
- [主題選擇](topics-light.png)
- [解讀紀錄](history-light.png)
- [歷史範例閱讀](reading-fixture-light.png)
- [完整命盤](chart-light.png)
- [命盤左右滑動](chart-light-pan.png)
- [設定與更新檢查](settings-update-check.png)

歷史閱讀範例以專用測試裝置的 SQLite 預先放入，文字明確標示「版面驗收用的範例，並非會員帳號實際產生的解讀」。此資料不會包進 APK。

升級測試：先安裝 1.3.1，在 UI 接受說明、建立 QA 家人、設定深色與大字，再加入上述歷史 fixture 與無效的舊連線 fixture。直接覆蓋 1.4.0 後，家人／歷史列與設定保留，舊連線儲存清空。data-before.txt 與 data-after.txt 記錄比對值。

failed-api37 保留不穩定測試環境的錯誤畫面；它不是新版畫面驗收結果。最後改用獨立 Android 35 環境完成驗收，未操作其他專案的模擬器。

本機建置與測試結果：build-test.log、lint.log、verification.json。正常 APK 與 local-development 相容包分別驗證簽名與 SHA-256。沒有推送或發布 GitHub Release。