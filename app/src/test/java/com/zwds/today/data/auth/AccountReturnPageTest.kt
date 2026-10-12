package com.zwds.today.data.auth

import org.junit.Assert.*
import org.junit.Test

class AccountReturnPageTest {
    @Test fun appReturnIsAnExactNavigationLinkWithoutAuthorizationData() {
        assertTrue(AccountReturnPage.isAppReturn("ziweitoday://login-complete"))
        listOf(null, "https://auth.openai.com", "chefai://login-complete",
            "ziweitoday://login-complete?code=forged", "ziweitoday://login-complete?key=secret",
            "ziweitoday://login-complete/other").forEach { assertFalse(AccountReturnPage.isAppReturn(it)) }
    }
    @Test fun callbackPageDoesNotClaimAccountVerificationHasCompleted() {
        val accepted = AccountReturnPage.html(true)
        assertTrue(accepted.contains("正在驗證帳號"))
        assertTrue(accepted.contains("href=\"${AccountReturnPage.APP_URI}\""))
        assertFalse(accepted.contains("登入完成"))
        val rejected = AccountReturnPage.html(false)
        assertTrue(rejected.contains("尚未收到有效授權"))
        assertFalse(rejected.contains("正在驗證帳號"))
        assertFalse(accepted.contains("javascript:"))
    }
}
