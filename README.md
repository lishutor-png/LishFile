# LishFile 📁

Aplikasi Manajer Berkas Modern, Cepat, Offline & Aman untuk Android.

## ✨ Fitur Utama
- **Browser Direktori Lengkap**: Navigasi penyimpanan internal, SD Card, dan subdirektori dengan breadcrumbs.
- **Operasi File Lengkap**: Salin, Pindahkan, Hapus, Ubah Nama, Buat Folder, Buat File Teks, Kompres/Ekstrak ZIP.
- **Brankas Aman (Safe Vault)**: Lindungi file sensitif dengan enkripsi AES-256 tingkat militer serta otentikasi PIN dan Biometrik/Sidik Jari.
- **Transfer P2P Lokal**: Kirim dan terima file antar perangkat dalam satu jaringan Wi-Fi lokal langsung tanpa server pihak ketiga.
- **Kategori & Scanner Duplikat**: Ringkasan penyimpanan visual (Gambar, Audio, Video, Dokumen, APK, Arsip) dan pemindai file duplikat.
- **Editor Gambar Terintegrasi**: Crop, filter warna, rotasi, dan penyesuaian gambar langsung di dalam aplikasi.
- **Mode Tema**: Dukungan Tema Terang, Gelap, dan Sistem (Material 3).

---

## 🚀 Cara Build APK di GitHub (GitHub Actions)

Repository ini telah dilengkapi dengan workflow GitHub Actions otomatis di `.github/workflows/build-apk.yml`.

### Langkah-langkah:
1. **Push kode ke GitHub**:
   ```bash
   git add .
   git commit -m "Initial commit of LishFile"
   git branch -M main
   git remote add origin https://github.com/USERNAME/REPO_NAME.git
   git push -u origin main
   ```
2. **Build Otomatis Berjalan**:
   - Buka tab **Actions** di repositori GitHub Anda.
   - Pilih workflow **Build LishFile APK**.
   - GitHub Actions akan otomatis mengompilasi APK debug.
3. **Download APK**:
   - Setelah workflow selesai (tanda centang hijau ✅), klik run tersebut.
   - Scroll ke bagian paling bawah ke bagian **Artifacts**.
   - Klik **LishFile-Debug-APK** untuk mengunduh berkas ZIP yang berisi file `app-debug.apk`.
   - Install APK pada perangkat Android Anda.
