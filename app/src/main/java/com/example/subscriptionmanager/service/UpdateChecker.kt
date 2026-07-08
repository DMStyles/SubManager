package com.example.subscriptionmanager.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val available: Boolean,
    val latestVersion: String,
    val downloadUrl: String
)

object UpdateChecker {
    private const val GITHUB_API_URL =
        "https://api.github.com/repos/DMStyles/SubManager/releases/latest"
    private const val WEB_PORTAL_URL =
        "https://dmstyles.github.io/SubManager/web/"

    suspend fun check(currentVersion: String): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (connection.responseCode != 200) {
                return@withContext UpdateInfo(false, currentVersion, WEB_PORTAL_URL)
            }

            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val json = JSONObject(response)
            val latestTag = json.getString("tag_name").removePrefix("v").trim()
            val current = currentVersion.removePrefix("v").trim()

            val isNewer = isVersionNewer(latestTag, current)
            UpdateInfo(isNewer, latestTag, WEB_PORTAL_URL)
        } catch (e: Exception) {
            // Silently fail — don't bother user if network is unavailable
            UpdateInfo(false, currentVersion, WEB_PORTAL_URL)
        }
    }

    /**
     * Compares two semantic version strings (e.g. "2.0.2" vs "2.0.1").
     * Returns true if [latest] is strictly newer than [current].
     */
    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false // same version
    }
}
