package com.zwds.today.data.auth

/** This link only opens the account screen. Authorization stays in the bound loopback exchange. */
object AccountReturnPage {
    const val APP_URI = "ziweitoday://login-complete"
    fun isAppReturn(uri: String?): Boolean = uri == APP_URI

    fun html(accepted: Boolean): String = """
        <!doctype html><html lang="zh-Hant"><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1"><title>今日紫微</title>
        <style>body{font:18px sans-serif;margin:0;padding:24px;background:#f7f5ef;color:#203747}
        main{max-width:480px;margin:48px auto;padding:28px;background:white;border-radius:24px}
        p{line-height:1.7}a{display:block;margin-top:24px;padding:16px;background:#253f50;color:white;
        border-radius:14px;text-align:center;text-decoration:none;font-weight:bold}</style></head><body><main>
        <h1>今日紫微</h1><p>${if (accepted) "已收到授權回應，正在驗證帳號。請返回 APP 查看登入結果。"
            else "尚未收到有效授權。請返回 APP 重新登入。"}</p>
        <a href="$APP_URI">返回 APP</a></main></body></html>
    """.trimIndent()
}
