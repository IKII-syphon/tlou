package com.iki.ps3player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen(
    backendName: String, glueOk: Boolean, coreText: String, fwText: String,
    working: String?, message: String?,
    onDownloadCore: () -> Unit, onPickFirmware: () -> Unit, onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Setup emulator", style = MaterialTheme.typography.headlineSmall)
        Text("Backend aktif: $backendName")
        Text("1. Native glue: " + if (glueOk) "ter-build" else "TIDAK ada (APK ini tanpa glue; backend nyata tidak bisa aktif)")
        Text("2. Core: $coreText")
        Button(onDownloadCore, enabled = glueOk && working == null) { Text("Unduh / perbarui core") }
        Text("3. Firmware PS3: $fwText")
        Button(onPickFirmware, enabled = glueOk && working == null && !coreText.startsWith("Belum")) { Text("Pilih PS3UPDAT.PUP milik Anda") }
        Text("Firmware dan game harus dari konsol PS3 Anda sendiri; aplikasi tidak menyertakan keduanya.", style = MaterialTheme.typography.bodySmall)
        if (working != null) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(working) }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        TextButton(onBack) { Text("Kembali") }
    }
}
