package id.papicode.billingps.db;

import id.papicode.billingps.model.Pengguna;
import id.papicode.billingps.service.SesiService;
import id.papicode.billingps.service.Tarif;
import id.papicode.billingps.util.Password;
import id.papicode.billingps.util.Waktu;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Data contoh rental PS: 4 jenis konsol, 10 unit, paket, 12 menu jajan, 3 pengguna, transaksi 30 hari,
 * dan beberapa sesi yang sedang berjalan saat ini (termasuk yang hampir habis, lewat waktu, dan dijeda).
 */
public final class DataContoh {
    private DataContoh() { }

    private static final String[] PELANGGAN = {"Raka", "Bima", "Fajar", "Dio", "Kevin", "Ilham", "Reza", "Yoga", "Andre", "Rizal",
            "Galih", "Tegar", "Bang Ucup", "Arya", "Naufal", "Daffa", "Farhan", "Rombongan SMA 5", "Mas Joko", "Hendra", "Putra", "Zaki"};

    public static boolean kosong(Database db) {
        return db.angka("SELECT COUNT(*) FROM unit") == 0 && db.angka("SELECT COUNT(*) FROM sesi") == 0;
    }

    public static void isi(Database db, Clock jam) {
        if (!kosong(db)) throw new IllegalStateException("Data contoh hanya bisa diisi pada database kosong.");
        Random r = new Random(1945_0817L);
        ZoneId zona = jam.getZone();
        long now = jam.millis();
        LocalDate hariIni = LocalDate.now(jam);
        Tarif.Aturan aturan = Tarif.Aturan.BAWAAN;

        db.tx(c -> {
            String dibuat = Waktu.db(LocalDateTime.now(jam).minusDays(40));
            Long admin = Database.satu(c, "SELECT id FROM pengguna WHERE username='admin'", rs -> rs.getLong(1));
            if (admin == null) {
                admin = Database.sisip(c, "INSERT INTO pengguna(username,nama,peran,hash,dibuat) VALUES(?,?,?,?,?)",
                        "admin", "Dimas Prasetyo", Pengguna.ADMIN, Password.hash("admin123"), dibuat);
            } else {
                Database.ubah(c, "UPDATE pengguna SET nama='Dimas Prasetyo' WHERE id=? AND nama='Pemilik Rental'", admin);
            }
            long[] op = {
                operator(c, "agus", "Agus Setiawan", dibuat),
                operator(c, "rina", "Rina Marlina", dibuat)
            };
            // konsol: nama, tarif per jam
            Object[][] konsol = {{"PS3", 4000L}, {"PS4", 7000L}, {"PS5", 12000L}, {"PS5 VIP Room", 20000L}};
            long[] kid = new long[konsol.length];
            for (int i = 0; i < konsol.length; i++) kid[i] = Database.sisip(c, "INSERT INTO konsol(nama,tarif_jam) VALUES(?,?)", konsol[i][0], konsol[i][1]);
            // unit: kode, nama, indeks konsol, keterangan
            Object[][] unit = {
                {"PS-01", "Meja 1", 1, "TV 32\", 2 stik"}, {"PS-02", "Meja 2", 1, "TV 32\", 2 stik"}, {"PS-03", "Meja 3", 1, "TV 32\", 4 stik"},
                {"PS-04", "Meja 4", 1, "TV 32\", 2 stik"}, {"PS-05", "Meja 5", 0, "TV 29\" tabung"}, {"PS-06", "Meja 6", 0, "TV 29\" tabung"},
                {"PS-07", "Meja 7", 2, "TV 43\", 2 stik DualSense"}, {"PS-08", "Meja 8", 2, "TV 43\", 2 stik DualSense"},
                {"PS-09", "Meja 9", 2, "TV 43\", 4 stik DualSense"}, {"VIP-01", "Room VIP", 3, "TV 55\", sofa, AC"}
            };
            long[] uid = new long[unit.length];
            for (int i = 0; i < unit.length; i++) {
                uid[i] = Database.sisip(c, "INSERT INTO unit(kode,nama,konsol_id,keterangan,urut) VALUES(?,?,?,?,?)",
                        unit[i][0], unit[i][1], kid[(int) unit[i][2]], unit[i][3], i + 1);
            }
            // paket: konsol, nama, menit, harga
            Object[][] paket = {
                {0, "PS3 1 Jam", 60, 4000L}, {0, "PS3 3 Jam", 180, 11000L},
                {1, "PS4 1 Jam", 60, 7000L}, {1, "PS4 2 Jam", 120, 13000L}, {1, "PS4 3 Jam", 180, 19000L}, {1, "PS4 Paket Malam 5 Jam", 300, 28000L},
                {2, "PS5 1 Jam", 60, 12000L}, {2, "PS5 2 Jam", 120, 22000L}, {2, "PS5 3 Jam", 180, 32000L}, {2, "PS5 Paket Malam 5 Jam", 300, 50000L},
                {3, "VIP 2 Jam", 120, 38000L}, {3, "VIP 3 Jam", 180, 55000L}
            };
            for (Object[] p : paket) Database.sisip(c, "INSERT INTO paket(nama,konsol_id,menit,harga) VALUES(?,?,?,?)", p[1], kid[(int) p[0]], p[2], p[3]);
            Object[][] jajan = {
                {"Indomie Goreng Telur", "Makanan", 10000L}, {"Indomie Rebus", "Makanan", 8000L}, {"Pop Mie", "Makanan", 7000L},
                {"Roti Bakar Coklat Keju", "Makanan", 12000L}, {"Es Teh Manis", "Minuman", 4000L}, {"Kopi Susu Sachet", "Minuman", 5000L},
                {"Air Mineral 600 ml", "Minuman", 4000L}, {"Teh Pucuk 350 ml", "Minuman", 5000L}, {"Coca-Cola 390 ml", "Minuman", 7000L},
                {"Good Day Freeze", "Minuman", 6000L}, {"Chitato 68 g", "Snack", 10000L}, {"Kacang Garuda", "Snack", 6000L}
            };
            for (Object[] j : jajan) Database.sisip(c, "INSERT INTO jajan(nama,kategori,harga,stok) VALUES(?,?,?,?)", j[0], j[1], j[2], 400);

            List<long[]> pk = Database.daftar(c, "SELECT id, konsol_id, menit, harga FROM paket", rs -> new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)});
            List<Object[]> menu = Database.daftar(c, "SELECT id, nama, harga FROM jajan", rs -> new Object[]{rs.getLong(1), rs.getString(2), rs.getLong(3)});
            int[] stokPakai = new int[menu.size() + 1];

            // ---- riwayat 30 hari ----
            long batasHariIni = now - 4 * 3_600_000L;
            for (int hari = 30; hari >= 0; hari--) {
                LocalDate tgl = hariIni.minusDays(hari);
                boolean ramai = tgl.getDayOfWeek().getValue() >= 5; // Jumat-Minggu
                List<Object[]> sesiHari = new ArrayList<>();
                for (int u = 0; u < unit.length; u++) {
                    long tarif = (long) konsol[(int) unit[u][2]][1];
                    long konsolId = kid[(int) unit[u][2]];
                    long t = tgl.atTime(10, 0).atZone(zona).toInstant().toEpochMilli() + r.nextInt(90) * 60_000L;
                    long tutup = tgl.atTime(23, 50).atZone(zona).toInstant().toEpochMilli();
                    double peluangKosong = u >= 4 && u <= 5 ? 0.55 : ramai ? 0.12 : 0.3;
                    while (t < tutup) {
                        if (r.nextDouble() < peluangKosong) { t += (20 + r.nextInt(70)) * 60_000L; continue; }
                        boolean pakaiPaket = r.nextDouble() < 0.62;
                        long[] p = null;
                        if (pakaiPaket) {
                            boolean malam = t > tutup - 6 * 3_600_000L;
                            List<long[]> cocok = pk.stream().filter(x -> x[1] == konsolId && (x[2] < 300 || malam)).toList();
                            p = cocok.get(r.nextInt(cocok.size()));
                        }
                        long durasi = p != null ? (p[2] + (r.nextDouble() < 0.12 ? 5 + r.nextInt(25) : -r.nextInt(3))) * 60_000L
                                                : (30 + r.nextInt(150)) * 60_000L;
                        long jeda = r.nextDouble() < 0.1 ? (5 + r.nextInt(20)) * 60_000L : 0;
                        long akhir = t + durasi + jeda;
                        if (akhir > tutup + 3_600_000L || (hari == 0 && akhir > batasHariIni)) break;
                        Tarif.Hasil h = p != null ? Tarif.paket(durasi, (int) p[2], p[3], tarif, aturan) : Tarif.open(durasi, tarif, aturan);
                        sesiHari.add(new Object[]{t, akhir, jeda, uid[u], p, tarif, h, op[LocalTime.ofInstant(Instant.ofEpochMilli(t), zona).getHour() < 17 ? 0 : 1]});
                        t = akhir + (5 + r.nextInt(40)) * 60_000L;
                    }
                }
                // penjualan jajan saja
                int nJajan = 3 + r.nextInt(5);
                for (int i = 0; i < nJajan; i++) {
                    long t = tgl.atTime(11 + r.nextInt(11), r.nextInt(60)).atZone(zona).toInstant().toEpochMilli();
                    if (hari == 0 && t > batasHariIni) continue;
                    sesiHari.add(new Object[]{t, t, 0L, null, null, 0L, null, op[r.nextInt(2)]});
                }
                sesiHari.sort(Comparator.comparingLong(x -> (long) x[0]));
                DateTimeFormatter f = DateTimeFormatter.ofPattern("yyMMdd");
                int nPs = 0, nJj = 0;
                for (Object[] s : sesiHari) {
                    long mulai = (long) s[0], selesai = (long) s[1], jeda = (long) s[2];
                    String kode = s[3] == null ? "JJ" : "PS";
                    String nomor = kode + "-" + Instant.ofEpochMilli(mulai).atZone(zona).format(f) + "-" + String.format("%03d", kode.equals("PS") ? ++nPs : ++nJj);
                    String tanggal = Instant.ofEpochMilli(selesai).atZone(zona).toLocalDate().toString();
                    String pelanggan = PELANGGAN[r.nextInt(PELANGGAN.length)];
                    long[] p = (long[]) s[4];
                    Tarif.Hasil h = (Tarif.Hasil) s[6];
                    String mode = s[3] == null ? "JAJAN" : p != null ? "PAKET" : "OPEN";
                    String paketNama = p == null ? null : Database.satu(c, "SELECT nama FROM paket WHERE id=?", rs -> rs.getString(1), p[0]);
                    long sid = Database.sisip(c, """
                            INSERT INTO sesi(nomor,unit_id,pelanggan,mode,paket_nama,menit_paket,harga_paket,tarif_jam,mulai,jeda_ms,selesai,tanggal,status,operator_id)
                            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,'SELESAI',?)""",
                            nomor, s[3], pelanggan, mode, paketNama, p == null ? 0 : p[2], p == null ? 0 : p[3], s[5], mulai, jeda, selesai, tanggal, s[7]);
                    long bj = 0;
                    int nItem = mode.equals("JAJAN") ? 1 + r.nextInt(3) : (r.nextDouble() < 0.4 ? 1 + r.nextInt(2) : 0);
                    for (int i = 0; i < nItem; i++) {
                        Object[] m = menu.get(r.nextInt(menu.size()));
                        int q = 1 + (r.nextDouble() < 0.25 ? 1 : 0);
                        stokPakai[(int) (long) (Long) m[0]] += q;
                        bj += (long) m[2] * q;
                        Database.ubah(c, "INSERT INTO sesi_jajan(sesi_id,jajan_id,nama,harga,qty,waktu) VALUES(?,?,?,?,?,?)", sid, m[0], m[1], m[2], q, mulai + 600_000L);
                    }
                    long sewa = h == null ? 0 : h.biaya();
                    long diskon = sewa >= 30000 && r.nextDouble() < 0.15 ? 2000 : 0;
                    long total = sewa + bj - diskon;
                    String metode = r.nextDouble() < 0.7 ? "TUNAI" : "QRIS";
                    long bayar = metode.equals("TUNAI") ? pecahan(total, r) : total;
                    Database.ubah(c, "UPDATE sesi SET biaya_sewa=?, biaya_jajan=?, diskon=?, total=?, metode=?, bayar=?, kembali=? WHERE id=?",
                            sewa, bj, diskon, total, metode, bayar, bayar - total, sid);
                    Database.ubah(c, "INSERT INTO log_sesi(sesi_id,waktu,aksi,keterangan,operator_id) VALUES(?,?,?,?,?)", sid, selesai, "BAYAR", metode, s[7]);
                }
            }
            for (int i = 1; i < stokPakai.length; i++) Database.ubah(c, "UPDATE jajan SET stok = MAX(8, stok - ?) WHERE id=?", stokPakai[i], i);
            Database.ubah(c, "UPDATE jajan SET stok=3 WHERE nama='Roti Bakar Coklat Keju'");
            // satu transaksi kemarin dibatalkan
            Long batal = Database.satu(c, "SELECT id FROM sesi WHERE tanggal=? AND mode<>'JAJAN' ORDER BY id LIMIT 1", rs -> rs.getLong(1), hariIni.minusDays(1).toString());
            if (batal != null) Database.ubah(c, "UPDATE sesi SET status='BATAL', alasan_batal='Salah input unit, pelanggan pindah meja' WHERE id=?", batal);
            // unit servis
            Database.ubah(c, "UPDATE unit SET servis=1 WHERE kode='PS-06'");
            return op;
        });

        // ---- sesi yang sedang berjalan sekarang (lewat service agar sesuai aturan) ----
        long opAgus = db.angka("SELECT id FROM pengguna WHERE username='agus'");
        Pengguna agus = new Pengguna(opAgus, "agus", "Agus Setiawan", Pengguna.OPERATOR, true);
        mulaiLalu(db, jam, agus, "PS-01", "Raka & Bima", "PS4 2 Jam", 85, 0, "Indomie Goreng Telur", "Es Teh Manis");
        mulaiLalu(db, jam, agus, "PS-03", "Rombongan SMA 5", null, 52, 0, "Kopi Susu Sachet", "Chitato 68 g");
        mulaiLalu(db, jam, agus, "PS-07", "Kevin", "PS5 1 Jam", 57, 0, "Coca-Cola 390 ml");
        mulaiLalu(db, jam, agus, "PS-08", "Fajar", "PS5 1 Jam", 62, 0);
        mulaiLalu(db, jam, agus, "VIP-01", "Mas Joko + 3 teman", "VIP 3 Jam", 70, 12, "Roti Bakar Coklat Keju", "Teh Pucuk 350 ml");
        mulaiLalu(db, jam, agus, "PS-09", "Tegar", "PS5 2 Jam", 18, 0);
    }

    /** Buat sesi yang dimulai 'menitLalu' menit yang lalu; bila jedaMenit > 0, sesi sedang dijeda sejak sekian menit. */
    private static void mulaiLalu(Database db, Clock jam, Pengguna op, String unit, String pelanggan, String paket, int menitLalu, int jedaMenit, String... jajan) {
        long now = jam.millis();
        Clock lalu = Clock.fixed(Instant.ofEpochMilli(now - menitLalu * 60_000L), jam.getZone());
        SesiService s0 = new SesiService(db, new id.papicode.billingps.service.PengaturanService(db), lalu);
        long uid = db.angka("SELECT id FROM unit WHERE kode=?", unit);
        Long pid = paket == null ? null : db.satu("SELECT id FROM paket WHERE nama=?", rs -> rs.getLong(1), paket);
        long sid = s0.mulai(op, uid, pelanggan, pid).id();
        SesiService s1 = new SesiService(db, new id.papicode.billingps.service.PengaturanService(db),
                Clock.fixed(Instant.ofEpochMilli(now - menitLalu * 30_000L), jam.getZone()));
        for (String j : jajan) s1.tambahJajan(op, sid, db.angka("SELECT id FROM jajan WHERE nama=?", j), 1);
        if (jedaMenit > 0) {
            new SesiService(db, new id.papicode.billingps.service.PengaturanService(db),
                    Clock.fixed(Instant.ofEpochMilli(now - jedaMenit * 60_000L), jam.getZone())).jeda(op, sid);
        }
    }

    private static long operator(java.sql.Connection c, String u, String nama, String dibuat) throws java.sql.SQLException {
        Long ada = Database.satu(c, "SELECT id FROM pengguna WHERE username=?", rs -> rs.getLong(1), u);
        if (ada != null) return ada;
        return Database.sisip(c, "INSERT INTO pengguna(username,nama,peran,hash,dibuat) VALUES(?,?,?,?,?)", u, nama, Pengguna.OPERATOR, Password.hash("operator123"), dibuat);
    }

    private static long pecahan(long total, Random r) {
        if (r.nextDouble() < 0.3) return total;
        for (long p : new long[]{5000, 10000, 20000, 50000, 100000}) if (p >= total) return p;
        return ((total + 49_999) / 50_000) * 50_000;
    }
}
