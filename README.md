# SmartPond 🐟🌱
### Kolam Ikan Pintar, Masa Depan Lebih Baik

SmartPond adalah sistem pemantauan dan pengendalian kolam ikan berbasis IoT yang dirancang untuk membantu menjaga kondisi kolam, mengelola penggunaan energi, dan mendukung keberlangsungan hidup ikan. Sistem ini mengintegrasikan aplikasi Android, ESP32, sensor kualitas air, pompa DC, turbin angin, baterai cadangan, serta pemberi pakan otomatis.

## 📱 Fitur Aplikasi Android

### 1. Dashboard Monitoring dan Kontrol
- Memantau kondisi kolam melalui dashboard secara real-time ketika perangkat terhubung.
- Menampilkan suhu air, tingkat kekeruhan, dan level air.
- Mengontrol pompa DC dan perangkat pendukung secara manual.
- Menampilkan status perangkat dan kondisi sistem.

### 2. Monitoring Energi
- Memantau produksi energi dari turbin angin.
- Menampilkan status pengisian baterai dan kapasitas energi yang tersedia, sesuai kemampuan sensor yang digunakan.
- Memantau konsumsi energi perangkat.
- Menampilkan informasi sumber daya dan status daya cadangan.

### 3. Fish Feeder Otomatis
- Memantau status pemberian pakan ikan.
- Mengatur jadwal pemberian pakan melalui aplikasi.
- Mendukung pengaturan jadwal lokal agar pemberian pakan tetap berjalan ketika koneksi internet terputus, selama pengendali dan perangkat masih mendapat daya.

### 4. Notifikasi dan Riwayat Data
- Mengirimkan notifikasi jarak jauh untuk kondisi kolam atau perangkat yang memerlukan perhatian.
- Menyimpan riwayat pembacaan sensor.
- Mencatat perubahan status perangkat dan aktivitas kontrol.
- Menyediakan data historis untuk membantu evaluasi kondisi kolam dan penggunaan energi.

### 5. Penjadwalan dan Operasi Offline
- Mengatur jadwal operasional perangkat melalui aplikasi.
- Mendukung penjadwalan lokal pada ESP32 agar fungsi penting tetap berjalan tanpa internet.
- Menyimpan konfigurasi jadwal pada perangkat agar tidak bergantung sepenuhnya pada aplikasi.
- Menyinkronkan riwayat data kembali ketika koneksi tersedia, jika mekanisme penyimpanan dan sinkronisasi telah diterapkan.

## ⚙️ Komponen Sistem

| Komponen | Fungsi |
|---|---|
| ESP32 | Pengendali utama dan komunikasi perangkat IoT |
| Turbin angin | Sumber energi alternatif |
| Baterai dan rangkaian pengisian | Menyimpan energi untuk kebutuhan daya cadangan |
| Pompa DC | Mendukung sirkulasi atau pengelolaan air kolam |
| Sensor suhu | Memantau suhu air |
| Sensor kekeruhan | Memantau tingkat kekeruhan air |
| Sensor level air | Memantau ketinggian air kolam |
| Fish feeder otomatis | Mengatur pemberian pakan ikan |
| Aplikasi Android | Monitoring, kontrol, konfigurasi, dan visualisasi data |

## 🎯 Tujuan Proyek

- Membantu pemilik kolam memantau kondisi air dengan lebih mudah.
- Mendukung operasional kolam ketika terjadi gangguan pasokan listrik melalui sistem daya cadangan.
- Mengoptimalkan pengelolaan energi dari sumber energi alternatif.
- Mengurangi ketergantungan pada pemantauan dan pemberian pakan secara manual.
- Mendukung pemeliharaan ikan yang lebih efisien melalui teknologi IoT.

## 🛠️ Teknologi

- **Mobile Application:** Android
- **Programming Language:** Kotlin
- **IoT Controller:** ESP32
- **Communication:** Wi-Fi dan protokol komunikasi yang didukung sistem
- **Sensors:** Suhu, kekeruhan, dan level air
- **Power System:** Turbin angin, aki, dan rangkaian pengisian daya

## 🚧 Status Pengembangan

SmartPond dikembangkan sebagai prototipe IoT untuk monitoring dan kontrol kolam ikan. Ketersediaan fitur bergantung pada implementasi perangkat keras, firmware ESP32, komunikasi data, dan aplikasi Android.

---

**SmartPond — Kolam Ikan Pintar, Masa Depan Lebih Baik.**
