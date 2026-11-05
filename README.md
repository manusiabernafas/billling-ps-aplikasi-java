# Billing PS — Aplikasi Billing Rental PlayStation Desktop (Java Swing + SQLite)

Aplikasi billing untuk rental PS (PS3, PS4, PS5, room VIP), rental game, atau tempat main konsol lainnya. Operator melihat semua unit dalam satu layar dengan timer yang berjalan, menambah waktu, mencatat jajan, lalu menagih dengan struk. Jalan offline di Windows, Linux, dan macOS; data di satu file SQLite.

## Menu
| Menu | Isi |
|---|---|
| Layar Unit | Grid semua unit: timer sisa waktu (paket) atau lama main (open billing), biaya berjalan, status Kosong/Main/Hampir habis/Habis/Jeda/Servis. Aksi: mulai, tambah waktu, jajan, jeda/lanjut, pindah unit, stop & bayar, tandai servis. Ringkasan unit main, kosong, waktu habis, dan pendapatan hari ini. |
| Jual Jajan | Penjualan makanan/minuman untuk pelanggan yang tidak main. |
| Riwayat | Semua transaksi per periode, struk, log aktivitas (mulai, tambah, jeda, pindah, bayar), cetak ulang, pembatalan (admin). |
| Laporan | Total pendapatan, sewa vs jajan, jam main, grafik harian, jam ramai, okupansi per unit, paket terlaris, jajan terlaris, ekspor CSV. |
| Unit & Tarif | Jenis konsol & tarif per jam, unit/meja (kode, TV, stik, status servis), paket waktu (termasuk paket malam). |
| Menu & Stok Jajan | Menu, harga, stok, stok masuk. |
| Pengguna | Admin dan operator (penjaga rental). |
| Pengaturan | Nama rental, alamat, struk, jam buka-tutup, aturan pembulatan, ganti password, cadangan database. |

## Cara kerja tarif
- **Open billing**: lama main dibulatkan ke atas per blok (default 15 menit), minimal 30 menit, biaya dibulatkan ke atas per Rp500. Contoh PS4 Rp7.000/jam, main 61 menit → ditagih 75 menit = Rp8.750 → Rp9.000.
- **Paket**: harga tetap. Bila pemain lewat waktu lebih dari toleransi (default 3 menit), kelebihannya ditagih seperti open billing.
- **Jeda**: waktu jeda (salat, makan, mati listrik) tidak dihitung.
- Semua angka di atas bisa diubah di Pengaturan oleh admin.

## Fitur yang memudahkan penjaga
- Kartu unit berubah kuning saat sisa waktu ≤ 5 menit (bisa diatur), magenta berkedip saat habis, disertai bunyi dan notifikasi di pojok layar.
- Timer dihitung dari waktu mulai yang tersimpan di database: aplikasi boleh ditutup, laptop mati, atau ganti shift; saat dibuka lagi timer tetap akurat.
- Pindah unit (stik rusak) membawa sisa waktu dan jajan.
- Struk thermal 58 mm dengan rincian sewa, jajan, diskon, dan kembalian.

## Daftar file
```
billing-ps-java/
├── pom.xml, jalankan.bat, jalankan.sh
├── src/main/java/id/papicode/billingps/
│   ├── App.java, Konteks.java
│   ├── db/            Database, Skema, DataContoh
│   ├── model/         Sesi, Unit, Konsol, Paket, Jajan, ItemJajan, Pengguna
│   ├── service/       Tarif (aturan hitung), SesiService, MasterService, LaporanService, PengaturanService, AuthService, Struk
│   ├── util/          Rupiah, Waktu, Csv, Password
│   └── ui/            Tema arcade, Ikon, komponen, LoginFrame, MainFrame, halaman/
├── src/main/resources/fonts/   Chakra Petch + VT323 (SIL Open Font License)
└── src/test/java/              15 tes JUnit
```

## Menjalankan
- Butuh JDK 17+ dan Maven (atau NetBeans / IntelliJ IDEA).
- Windows: klik dua kali `jalankan.bat`. Linux/macOS: `./jalankan.sh`. Manual: `mvn package` lalu `java -jar target/billing-ps.jar`.
- Login **admin / admin123**. Saat database kosong, pilih *Isi data contoh* untuk mencoba (10 unit, paket, 12 menu jajan, transaksi 30 hari, 6 sesi yang sedang main).
- Atur tarif di **Unit & Tarif**, identitas & aturan di **Pengaturan**, lalu buat akun operator. **Ganti password admin.**

Database default `data/billing-ps.db`; bisa diganti dengan `java -Dbilling.db=D:/Rental/billing.db -jar target/billing-ps.jar`. Dua komputer kasir bisa memakai file database di folder bersama; layar unit memuat ulang otomatis tiap 30 detik.

Installer .exe (opsional): `jpackage --input target --main-jar billing-ps.jar --name "Billing PS" --type exe --win-shortcut --win-menu`.

## Tes
```
mvn test
```
15 skenario: aturan tarif open & paket (blok, minimal, toleransi, pembulatan Rp500), alur paket + tambah waktu + jajan + diskon + bayar, jeda tidak dihitung (termasuk bayar saat masih jeda), satu unit satu sesi (dijaga juga oleh unique index), paket harus sesuai konsol, pindah unit, stok jajan tidak minus & kembali saat batal, hak akses operator, 8 operator rebutan unit yang sama, validasi master & pengaturan, data contoh, struk & CSV, kunci login.

## Catatan keamanan
- Password PBKDF2-HMAC-SHA256 bersalt; akun terkunci 5 menit setelah 5 kali salah; akun nonaktif langsung ditolak saat bertransaksi.
- Biaya selalu dihitung ulang di service dari data sesi di database dan jam saat bayar, bukan dari angka di layar.
- Semua perubahan memakai transaksi `BEGIN IMMEDIATE`; index unik `ux_unit_main` memastikan satu unit tidak pernah punya dua sesi berjalan; stok jajan dikurangi dengan `UPDATE ... WHERE stok >= qty`.
- Transaksi tidak dihapus: pembatalan oleh admin dengan alasan, tercatat di log, dan tidak dihitung di laporan.
- Query memakai `PreparedStatement`; ekspor CSV aman dari formula injection Excel.

## Lisensi pihak ketiga
FlatLaf (Apache 2.0), sqlite-jdbc (Apache 2.0), SLF4J (MIT), font Chakra Petch & VT323 (SIL OFL 1.1), bentuk ikon mengikuti Lucide (ISC).
