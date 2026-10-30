package com.iki.ps3player.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import java.util.Locale

fun formatBytes(b: Long): String = String.format(Locale.US, "%.2f GB", b / 1073741824.0)

object DeviceDiagnostics {
    private fun vk(v: Int) = "${v shr 22}.${(v shr 12) and 0x3ff}.${v and 0xfff}"

    fun collect(ctx: Context): List<Pair<String, String>> {
        val pm = ctx.packageManager
        val soc = "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}".trim()
        val vkVer = pm.systemAvailableFeatures.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }?.version
        val vkLvl = pm.systemAvailableFeatures.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL }?.version
        val mem = ActivityManager.MemoryInfo().also { (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it) }
        val thermal = when ((ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "Normal"
            PowerManager.THERMAL_STATUS_LIGHT -> "Ringan"
            PowerManager.THERMAL_STATUS_MODERATE -> "Sedang"
            PowerManager.THERMAL_STATUS_SEVERE -> "Berat (throttling)"
            PowerManager.THERMAL_STATUS_CRITICAL -> "Kritis"
            else -> "Darurat/shutdown"
        }
        val temp = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?.takeIf { it != Int.MIN_VALUE }?.let { "${it / 10.0} °C" } ?: "tidak tersedia"
        val isDimensity1200 = Build.SOC_MODEL.contains("MT6893", ignoreCase = true)
        return listOf(
            "Perangkat" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "SoC" to soc.ifBlank { "tidak terbaca" },
            "Cocok target Dimensity 1200 (MT6893)" to if (isDimensity1200) "Ya" else "Tidak / tidak dapat dipastikan",
            "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "ABI" to Build.SUPPORTED_ABIS.joinToString(),
            "ARM64" to if (Build.SUPPORTED_64_BIT_ABIS.contains("arm64-v8a")) "Ya" else "Tidak",
            "Vulkan" to (vkVer?.let { "Didukung, versi ${vk(it)}" } ?: "Tidak dilaporkan"),
            "Vulkan hardware level" to (vkLvl?.toString() ?: "Tidak dilaporkan"),
            "GPU" to "Nama GPU tidak dibaca (butuh konteks GL/Vulkan, belum diimplementasikan)",
            "RAM total / tersedia" to "${formatBytes(mem.totalMem)} / ${formatBytes(mem.availMem)}",
            "Penyimpanan bebas" to formatBytes(StatFs(ctx.filesDir.path).availableBytes),
            "Status termal" to thermal,
            "Suhu baterai" to temp,
        )
    }
}
