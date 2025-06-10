# HabitTracker

**HabitTracker** adalah aplikasi Android untuk membantu Anda membangun dan memantau kebiasaan baik secara harian, mingguan, maupun bulanan. Dengan fitur pelacakan, penyelesaian, skip, statistik, dan tema terang/gelap, aplikasi ini cocok untuk semua pengguna yang ingin hidup lebih produktif dan disiplin.

---

## Fitur Utama

- **Tambah, Edit, Hapus Habit**  
  Buat kebiasaan baru, ubah, atau hapus sesuai kebutuhan Anda.
- **Kategori dan Target**  
  Setiap habit dapat memiliki kategori, target jumlah hari, dan deskripsi.
- **Pelacakan Harian, Mingguan, Bulanan**  
  Pilih frekuensi habit sesuai kebutuhan (daily/weekly/monthly).
- **Tandai Selesai/Skip**  
  Tandai habit sebagai selesai atau skip (tanpa memutus streak).
- **Undo**  
  Batalkan penyelesaian habit pada hari berjalan.
- **Streak dan Progress**  
  Lihat perkembangan streak dan persentase pencapaian habit.
- **Sorting & Section**  
  Habit dikelompokkan secara otomatis ke:  
  - Active (belum selesai hari ini)
  - Completed (sudah selesai hari ini)
  - Inactive (habit yang dinonaktifkan)
- **Mode Terang & Gelap**  
  Dukungan tema light/dark dan system default.
- **Inspirasi Harian**  
  Dapatkan kutipan motivasi setiap hari.
- **UI Modern**  
  Menggunakan Material Design.

---

## Prasyarat

- Android Studio (rekomendasi versi terbaru)
- Android SDK minimum API 21 (Lollipop)
- Gradle 7+
- Java 8+

---

## Cara Instalasi

### 1. Clone Repository

```sh
git clone https://github.com/username/habittracker.git
cd habittracker
```

### 2. Buka di Android Studio

- Buka Android Studio
- Pilih **Open an existing project**
- Pilih folder hasil clone tadi (`habittracker`)

### 3. Sinkronisasi Gradle

- Tunggu hingga Gradle selesai sinkronisasi
- Jika ada error dependency, klik "Try Again" atau cek koneksi internet

### 4. Jalankan Aplikasi

- Sambungkan perangkat Android/aktifkan emulator
- Klik tombol **Run** (ikon play) di Android Studio

---

## Cara Penggunaan

### 1. Menambah Habit Baru

- Tekan tombol `+` (Add Habit) pada halaman utama
- Isi nama habit, deskripsi, kategori, target streak, dan frekuensi (harian, mingguan, bulanan)
- Simpan habit

### 2. Menandai Habit Selesai

- Pada daftar habit aktif, tekan tombol **Complete**
- Jika ingin membatalkan, tekan tombol **Undo Complete** (selama masih di hari yang sama)

### 3. Skip Habit

- Tekan tombol **Skip** pada habit yang ingin dilewati tanpa memutus streak
- Akan muncul dialog konfirmasi sebelum habit di-skip

### 4. Edit atau Hapus Habit

- Tekan ikon **edit** pada habit yang ingin diubah
- Edit informasi lalu simpan, atau pilih hapus untuk menghilangkan habit

### 5. Mengatur Tema

- Masuk ke menu **Settings**
- Pilih tema: Light, Dark, atau System Default

### 6. Melihat Statistik

- Progress bar pada setiap habit menunjukkan pencapaian
- Tag/status habit otomatis: "Active", "Completed", "Inactive"
- Habit dikelompokkan berdasarkan status dalam section berbeda di satu daftar

---

## Struktur Section Habit

- **Active**: Habit aktif dan belum selesai hari ini
- **Completed Today**: Habit aktif dan sudah selesai hari ini (bisa di-undo)
- **Inactive**: Habit yang dinonaktifkan

---

## FAQ

**Q: Apakah data saya aman?**  
A: Data habit disimpan di database lokal perangkat. Tidak ada transfer data ke server manapun.

**Q: Bagaimana jika saya uninstall aplikasi?**  
A: Semua data akan hilang. Backup data secara manual jika diperlukan.

**Q: Apakah aplikasi ini open source?**  
A: Ya! Silakan modifikasi sesuai kebutuhan.

---

## Kontribusi

Pull request, issue, dan saran sangat diterima!  
Lihat [CONTRIBUTING.md](CONTRIBUTING.md) untuk panduan kontribusi.

---

## Lisensi

Aplikasi ini menggunakan lisensi MIT.  
Lihat file [LICENSE](LICENSE) untuk detailnya.

---

## Kontak & Bantuan

- Email: muhammadraihanaan123@email.com
- GitHub Issues: Silakan ajukan issue pada repo ini.

---

Terima kasih telah menggunakan **HabitTracker**!  
Semoga produktivitas dan kebiasaan baik Anda semakin meningkat 🎉
