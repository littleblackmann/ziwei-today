package com.zwds.today.data.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class ReleaseInfoTest {
    private fun release(tag: String = "v1.10.0", prerelease: Boolean = false,
                        page: String = "https://github.com/littleblackmann/ziwei-today/releases/tag/$tag",
                        download: String = "https://github.com/littleblackmann/ziwei-today/releases/download/$tag/app.apk") =
        Json.parseToJsonElement("""{"draft":false,"prerelease":$prerelease,"tag_name":"$tag",
          "html_url":"$page","body":"更新內容","assets":[{"name":"app.apk","size":123,
          "browser_download_url":"$download"}]}""").jsonObject
    @Test fun semanticVersionsCompareNumbersNotStrings() {
        assertTrue(AppVersion.parse("v1.10.0")!! > AppVersion.parse("1.9.9")!!)
        assertNull(AppVersion.parse("v1.2.0-beta")); assertNull(AppVersion.parse("1.2"))
        assertNotNull(ReleaseInfo.parse(release(), "1.2.0"))
        assertNull(ReleaseInfo.parse(release("v1.2.0"), "1.2.0"))
    }
    @Test fun rejectsPrereleaseForeignLinksAndNonInstallableReleases() {
        assertNull(ReleaseInfo.parse(release(prerelease = true), "1.2.0"))
        assertNull(ReleaseInfo.parse(release(page = "https://evil.example/app"), "1.2.0"))
        assertNull(ReleaseInfo.parse(release(download = "https://github.com/other/repo/app.apk"), "1.2.0"))
        val noApk = Json.parseToJsonElement(release().toString().replace("app.apk", "source.zip")).jsonObject
        assertNull(ReleaseInfo.parse(noApk, "1.2.0"))
    }
}
