package com.iki.ps3player.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Mengunduh core RPCSX-Clanker (GPLv2) dari rilis resmi GitHub-nya ke penyimpanan app. */
object CoreManager {
    private const val RELEASES = "https://api.github.com/repos/Ouroboros420/rpcsx/releases?per_page=10"

    fun coreFile(ctx: Context) = File(ctx.filesDir, "core/librpcsx-android.so")

    /** Varian aman berdasarkan /proc/cpuinfo (tidak pernah memilih armv9/SVE). Urut dari terbaik. */
    fun archOrder(): List<String> {
        val feats = runCatching {
            File("/proc/cpuinfo").readLines().filter { it.trimStart().startsWith("Features") }.joinToString(" ")
        }.getOrDefault("")
        fun has(f: String) = Regex("(^|\\s)$f(\\s|$)").containsMatchIn(feats)
        return if (has("asimddp") && has("atomics")) listOf("armv8.2-a", "armv8-a") else listOf("armv8-a")
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000; readTimeout = 30000; instanceFollowRedirects = true
            setRequestProperty("User-Agent", "PS3MobilePlayer"); setRequestProperty("Accept", "application/vnd.github+json")
        }

    /** onProgress(terunduh, total) dipanggil saat persen berubah. Mengembalikan "tag (arch)". */
    suspend fun download(ctx: Context, onProgress: (Long, Long) -> Unit): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val releases = JSONArray(open(RELEASES).inputStream.bufferedReader().use { it.readText() })
            var url: String? = null; var label = ""
            outer@ for (i in 0 until releases.length()) {
                val r = releases.getJSONObject(i)
                if (r.optBoolean("draft")) continue
                val assets = r.getJSONArray("assets")
                for (arch in archOrder()) {
                    for (j in 0 until assets.length()) {
                        val a = assets.getJSONObject(j)
                        if (a.getString("name") == "librpcsx-android-arm64-v8a-$arch.so") {
                            url = a.getString("browser_download_url"); label = "${r.optString("tag_name")} ($arch)"; break@outer
                        }
                    }
                }
            }
            val dl = url ?: error("Asset core untuk perangkat ini tidak ditemukan di rilis")
            val dest = coreFile(ctx); dest.parentFile!!.mkdirs()
            val tmp = File(dest.parentFile, "core.tmp")
            val conn = open(dl); val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(1 shl 16); var done = 0L; var lastPct = -1
                    while (true) {
                        val n = input.read(buf); if (n < 0) break
                        out.write(buf, 0, n); done += n
                        val pct = if (total > 0) (done * 100 / total).toInt() else 0
                        if (pct != lastPct) { lastPct = pct; onProgress(done, total) }
                    }
                }
            }
            check(tmp.length() > 1_000_000) { "File core tidak lengkap" }
            if (dest.exists()) dest.delete()
            check(tmp.renameTo(dest)) { "Gagal menyimpan core" }
            label
        }
    }
}
