package com.iki.ps3player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iki.ps3player.diagnostics.formatBytes
import com.iki.ps3player.emulator.BackendCapabilities
import com.iki.ps3player.game.GameEntry

@Composable
fun HomeScreen(
    games: List<GameEntry>, error: String?, busy: String?, backendName: String,
    onAdd: () -> Unit, onOpen: (GameEntry) -> Unit, onDiag: () -> Unit, onSetup: () -> Unit,
) {
    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(0.4f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PS3 Mobile Player", style = MaterialTheme.typography.headlineSmall)
            Text("Backend: $backendName", style = MaterialTheme.typography.bodySmall)
            Button(onAdd, enabled = busy == null) { Text("Add Game") }
            OutlinedButton(onSetup) { Text("Setup emulator") }
            OutlinedButton(onDiag) { Text("Diagnostik Perangkat") }
            if (busy != null) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(busy, style = MaterialTheme.typography.bodySmall) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        LazyColumn(Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (games.isEmpty()) item { Text("Belum ada game. Tekan Add Game dan pilih file .iso / .pkg milik Anda.") }
            items(games, key = { it.id }) { g ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(g) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(g.name, style = MaterialTheme.typography.titleMedium)
                        Text("${formatBytes(g.sizeBytes)} • ${g.status.label}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(
    game: GameEntry, caps: BackendCapabilities, logLines: List<String>,
    onPlay: () -> Unit, onControls: () -> Unit, onRemove: () -> Unit, onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(game.name, style = MaterialTheme.typography.headlineSmall)
        Text("Ukuran: ${formatBytes(game.sizeBytes)}")
        Text("Lokasi: ${game.uri}", style = MaterialTheme.typography.bodySmall)
        Text("Kompatibilitas: ${game.status.label}")
        Text("Backend: ${caps.name}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onPlay) { Text("Play") }
            OutlinedButton(onControls) { Text("Configure Controls") }
            OutlinedButton(onRemove) { Text("Hapus") }
            TextButton(onBack) { Text("Kembali") }
        }
        Text("Graphics Settings", style = MaterialTheme.typography.titleMedium)
        val opts = buildList {
            if (caps.supportsResolutionScale) add("Skala resolusi (belum diimplementasikan di UI)")
            if (caps.supportsFrameLimiter) add("Frame limiter (belum diimplementasikan di UI)")
            if (caps.supportsVulkan) add("Renderer Vulkan (belum diimplementasikan di UI)")
        }
        if (opts.isEmpty()) Text("Backend ini tidak menyediakan opsi grafis.", style = MaterialTheme.typography.bodySmall)
        else opts.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        Text("Log backend terakhir", style = MaterialTheme.typography.titleMedium)
        Text(if (logLines.isEmpty()) "(kosong)" else logLines.joinToString("\n"), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun DiagnosticsScreen(rows: List<Pair<String, String>>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Diagnostik Perangkat", style = MaterialTheme.typography.headlineSmall)
        rows.forEach { (k, v) -> Text("$k: $v", style = MaterialTheme.typography.bodyMedium) }
        TextButton(onBack) { Text("Kembali") }
    }
}
