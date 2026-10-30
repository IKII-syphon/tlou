# PS3 Mobile Player (GPLv2)

Frontend Android (Kotlin + Compose) untuk core emulator RPCSX-Clanker. Menyertakan: native glue (dari rpcsx-ui-android),
kontrol virtual multi-touch, unduh core otomatis, pemasangan firmware dan game, diagnostik perangkat.
Status: **belum pernah dikompilasi atau dijalankan di perangkat**. Kompatibilitas The Last of Us di Dimensity 1200: **belum diuji**
(core dioptimalkan untuk Adreno/Turnip; dukungan Mali belum terbukti).

## Cara mendapatkan APK (tanpa Android Studio)
1. Buat repositori GitHub **publik** baru, upload seluruh isi folder ini (termasuk `.github`).
2. Tab **Actions** → workflow "Build APK" jalan otomatis (±10–15 menit; bisa dijalankan manual lewat "Run workflow").
3. Buka run yang selesai → **Artifacts** → unduh `PS3MobilePlayer-debug-apk` → ekstrak → pasang `app-debug.apk` di ponsel
   (izinkan "install dari sumber tidak dikenal").
Alternatif: Android Studio (Koala+, JDK 17) → Open folder ini → Sync → Run di perangkat ARM64.
Jika build gagal, kirim potongan log error yang merah; kode belum pernah dikompilasi.

## Cara pakai di ponsel
1. Buka **Setup emulator** → **Unduh / perbarui core** (aplikasi mengambil `.so` dari rilis resmi RPCSX-Clanker di GitHub).
2. **Pilih PS3UPDAT.PUP** (firmware dari situs resmi PlayStation) untuk dipasang.
3. Kembali ke beranda → **Add Game** → pilih `.iso` atau `.pkg` milik Anda. Core memasangnya ke penyimpanan app
   (butuh ruang kosong ≥ ukuran file + 512 MB), lalu game muncul di daftar.
4. Buka game → **Play**. Boot pertama bisa memakan waktu lama karena kompilasi.
Tanpa langkah 1–2, aplikasi berjalan dengan MockBackend (hanya menguji UI dan kontrol).

## Struktur
`emulator/` (EmulatorBackend, MockBackend, RpcsxBackend, CoreInstaller, RpcsxPadMapper) • `core/CoreManager` (unduh core) •
`controls/` • `game/` • `storage/` • `diagnostics/` • `ui/` • `net/rpcsx/` (binding JNI + callback yang dipanggil core) • `cpp/` (native glue + libadrenotools)

## Fakta teknis yang sudah diverifikasi dari source dan binary core
- Core dimuat dengan `openLibrary(path)`, lalu `initialize` → thread `startMainThreadProcessor` + `processCompilationQueue` → `boot(path)`.
- Core memanggil balik kelas Kotlin lewat nama: `GameInfo`, `GameRepository.add`, `ProgressRepository.onProgressEvent`,
  `FirmwareRepository.onFirmwareInstalled` (ada di `CoreCallbacks.kt`). Tidak ada JNI pause; pause = `openHomeMenu()`.

## Belum ada / batasan
L3/R3, controller eksternal, pilihan grafis, pemilihan folder game (non-ISO), pembaruan core terjadwal, nama GPU,
pemindaian hasil `collectGameInfo` memakai jeda 1,5 detik (belum diketahui apakah sinkron).
Aplikasi turunan GPLv2: bagikan source bersama APK jika didistribusikan. Firmware dan game wajib milik sendiri.
