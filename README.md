# File Manager + (Android)

A modern, secure Android File Manager built with Kotlin and Jetpack Compose.

## 🚀 Build APK di GitHub (GitHub Actions)

Repositori ini sudah dilengkapi dengan **GitHub Actions Workflow** otomatis (`.github/workflows/build-apk.yml`) untuk membangun file APK secara otomatis di GitHub.

### Cara Mendapatkan APK dari GitHub:

1. **Otomatis saat Push:**
   - Setiap kali Anda melakukan `git push` ke repositori GitHub, GitHub Actions akan langsung memulai proses build APK.

2. **Manual (Workflow Dispatch):**
   - Masuk ke tab **Actions** di repositori GitHub Anda.
   - Pilih alur kerja **Build Android APK** di panel sebelah kiri.
   - Klik tombol **Run workflow** -> **Run workflow**.

3. **Cara Mengunduh File APK:**
   - Buka tab **Actions** di repositori GitHub.
   - Klik proses build terbaru yang selesai (bertanda centang hijau).
   - Scroll ke bagian bawah ke bagian **Artifacts**.
   - Klik **File-Manager-Plus-Debug-APK** untuk mengunduh file `.zip` yang berisi `app-debug.apk`.
   - Ekstrak dan pasang (`install`) APK tersebut ke perangkat Android Anda.

---

## 🛠️ Build APK Secara Lokal (Local Build)

Jika ingin build langsung di komputer:

```bash
# Pastikan menggunakan JDK 21
./gradlew assembleDebug
```

File APK akan tersedia di:
`app/build/outputs/apk/debug/app-debug.apk`
