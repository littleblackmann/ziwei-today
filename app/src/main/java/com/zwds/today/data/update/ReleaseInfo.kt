package com.zwds.today.data.update

import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class AppVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion) = compareValuesBy(this, other,
        AppVersion::major, AppVersion::minor, AppVersion::patch)
    companion object {
        fun parse(value: String): AppVersion? {
            val match = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)$").matchEntire(value) ?: return null
            val numbers = match.groupValues.drop(1).map { it.toIntOrNull() ?: return null }
            return AppVersion(numbers[0], numbers[1], numbers[2])
        }
    }
}
@kotlinx.serialization.Serializable
data class ReleaseInfo(val tag: String, val notes: String, val pageUrl: String) {
    companion object {
        const val REPO = "littleblackmann/ziwei-today"
        const val RELEASES_URL = "https://github.com/$REPO/releases"
        fun parse(root: JsonObject, installedVersion: String): ReleaseInfo? {
            if (root["draft"]?.jsonPrimitive?.booleanOrNull != false ||
                root["prerelease"]?.jsonPrimitive?.booleanOrNull != false) return null
            val tag = root["tag_name"]?.jsonPrimitive?.content ?: return null
            val current = AppVersion.parse(installedVersion) ?: return null
            if ((AppVersion.parse(tag) ?: return null) <= current) return null
            val page = root["html_url"]?.jsonPrimitive?.content ?: return null
            val pageUrl = page.toHttpUrlOrNull() ?: return null
            if (pageUrl.scheme != "https" || pageUrl.host != "github.com" ||
                pageUrl.encodedPath != "/$REPO/releases/tag/$tag" || pageUrl.username.isNotEmpty() ||
                pageUrl.password.isNotEmpty() || pageUrl.port != 443) return null
            val hasApk = root["assets"]?.jsonArray.orEmpty().any {
                val asset = it.jsonObject
                val name = asset["name"]?.jsonPrimitive?.content.orEmpty()
                val url = asset["browser_download_url"]?.jsonPrimitive?.content?.toHttpUrlOrNull()
                name.endsWith(".apk", true) && (asset["size"]?.jsonPrimitive?.longOrNull ?: 0) > 0 &&
                    url?.scheme == "https" && url.host == "github.com" &&
                    url.encodedPath.startsWith("/$REPO/releases/download/$tag/")
            }
            if (!hasApk) return null
            return ReleaseInfo(tag, root["body"]?.jsonPrimitive?.content.orEmpty().take(2000), page)
        }
    }
}
