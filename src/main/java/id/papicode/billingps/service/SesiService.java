package id.papicode.billingps.service;

import id.papicode.billingps.db.Database;
import id.papicode.billingps.model.*;
import id.papicode.billingps.util.Rupiah;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Inti billing: mulai sesi, jeda/lanjut, tambah waktu, pindah unit, jajan, hitung tagihan, bayar, batal.
 * Semua waktu memakai Clock yang disuntikkan sehingga timer bisa diuji dan tetap jalan walau aplikasi ditutup.
 */
public class SesiService {

    public static final List<String> METODE = List.of("TUNAI", "QRIS");

    private static final String SELECT = """
            SELECT s.*, u.kode AS unit_kode, u.nama AS unit_nama, k.nama AS konsol, p.nama AS operator
            FROM sesi s LEFT JOIN unit u ON u.id=s.unit_id LEFT JOIN konsol k ON k.id=u.konsol_id
            JOIN pengguna p ON p.id=s.operator_id""";

    private final Database db;
    private final PengaturanService set;
    private final Clock jam;

    public SesiService(Database db, PengaturanService set, Clock jam) {
        this.db = db;
        this.set = set;
        this.jam = jam;
    }

    public long sekarang() { return jam.millis(); }

    public List<Sesi> berjalan() { return db.daftar(SELECT + " WHERE s.status='MAIN' ORDER BY s.mulai", SesiService::map); }

    public Sesi byId(long id) { return db.satu(SELECT + " WHERE s.id=?", SesiService::map, id); }

    /** Mulai sesi di unit kosong. paketId null = open billing. */
    public Sesi mulai(Pengguna aktor, long unitId, String pelanggan, Long paketId) {
        wajibLogin(aktor);
        String nama = pelanggan == null || pelanggan.isBlank() ? "Tamu" : pelanggan.trim();
        if (nama.length() > 40) throw new Gagal("Nama pelanggan maksimal 40 karakter.");
        long now = jam.millis();
        return db.tx(c -> {
            Object[] u = Database.satu(c, "SELECT u.aktif, u.servis, u.konsol_id, k.tarif_jam, u.kode FROM unit u JOIN konsol k ON k.id=u.konsol_id WHERE u.id=?",
                    rs -> new Object[]{rs.getInt(1), rs.getInt(2), rs.getLong(3), rs.getLong(4), rs.getString(5)}, unitId);
            if (u == null || (int) u[0] != 1) throw new Gagal("Unit tidak ditemukan.");
            if ((int) u[1] == 1) throw new Gagal("Unit " + u[4] + " sedang servis.");
            if (Database.angka(c, "SELECT COUNT(*) FROM sesi WHERE unit_id=? AND status='MAIN'", unitId) > 0) {
                throw new Gagal("Unit " + u[4] + " sedang dipakai.");
            }
            String mode = Sesi.OPEN, paketNama = null;
            int menit = 0;
            long harga = 0;
            if (paketId != null) {
                Object[] p = Database.satu(c, "SELECT nama, konsol_id, menit, harga, aktif FROM paket WHERE id=?",
                        rs -> new Object[]{rs.getString(1), rs.getLong(2), rs.getInt(3), rs.getLong(4), rs.getInt(5)}, paketId);
                if (p == null || (int) p[4] != 1) throw new Gagal("Paket tidak ditemukan.");
                if ((long) p[1] != (long) u[2]) throw new Gagal("Paket ini bukan untuk jenis konsol unit " + u[4] + ".");
                mode = Sesi.PAKET;
                paketNama = (String) p[0];
                menit = (int) p[2];
                harga = (long) p[3];
            }
            String nomor = nomor(c, "PS", now);
            long id = Database.sisip(c, """
                    INSERT INTO sesi(nomor,unit_id,pelanggan,mode,paket_nama,menit_paket,harga_paket,tarif_jam,mulai,status,operator_id)
                    VALUES(?,?,?,?,?,?,?,?,?,'MAIN',?)""",
                    nomor, unitId, nama, mode, paketNama, menit, harga, u[3], now, aktor.id());
            log(c, id, now, "MULAI", mode.equals(Sesi.PAKET) ? paketNama : "Open billing", aktor);
            return Database.satu(c, SELECT + " WHERE s.id=?", SesiService::map, id);
        });
    }

    /** Tambah waktu dengan paket tambahan (hanya sesi paket). */
    public Sesi tambahWaktu(Pengguna aktor, long sesiId, long paketId) {
        wajibLogin(aktor);
        return db.tx(c -> {
            Sesi s = sesiMain(c, sesiId);
            if (!s.mode().equals(Sesi.PAKET)) throw new Gagal("Sesi open billing tidak perlu tambah waktu, tarif dihitung otomatis.");
            Object[] p = Database.satu(c, "SELECT p.nama, p.konsol_id, p.menit, p.harga FROM paket p WHERE p.id=? AND p.aktif=1",
                    rs -> new Object[]{rs.getString(1), rs.getLong(2), rs.getInt(3), rs.getLong(4)}, paketId);
            long konsolUnit = Database.angka(c, "SELECT konsol_id FROM unit WHERE id=?", s.unitId());
            if (p == null || (long) p[1] != konsolUnit) throw new Gagal("Paket tidak cocok dengan konsol unit ini.");
            Database.ubah(c, "UPDATE sesi SET menit_paket = menit_paket + ?, harga_paket = harga_paket + ? WHERE id=?", p[2], p[3], sesiId);
            log(c, sesiId, jam.millis(), "TAMBAH", "+" + p[0] + " (" + Rupiah.format((long) p[3]) + ")", aktor);
            return Database.satu(c, SELECT + " WHERE s.id=?", SesiService::map, sesiId);
        });
    }

    /** Jeda (misal pelanggan salat/makan). Waktu jeda tidak dihitung. */
    public Sesi jeda(Pengguna aktor, long sesiId) {
        wajibLogin(aktor);
        return db.tx(c -> {
            Sesi s = sesiMain(c, sesiId);
            if (s.dijeda()) throw new Gagal("Sesi sudah dalam jeda.");
            long now = jam.millis();
            Database.ubah(c, "UPDATE sesi SET jeda_sejak=? WHERE id=?", now, sesiId);
            log(c, sesiId, now, "JEDA", null, aktor);
            return Database.satu(c, SELECT + " WHERE s.id=?", SesiService::map, sesiId);
        });
    }

    public Sesi lanjut(Pengguna aktor, long sesiId) {
        wajibLogin(aktor);
        return db.tx(c -> {
            Sesi s = sesiMain(c, sesiId);
            if (!s.dijeda()) throw new Gagal("Sesi tidak sedang dijeda.");
            long now = jam.millis();
            Database.ubah(c, "UPDATE sesi SET jeda_ms = jeda_ms + ?, jeda_sejak=0 WHERE id=?", now - s.jedaSejak(), sesiId);
            log(c, sesiId, now, "LANJUT", Math.round((now - s.jedaSejak()) / 60000.0) + " menit jeda", aktor);
            return Database.satu(c, SELECT + " WHERE s.id=?", SesiService::map, sesiId);
        });
    }

    /** Pindah ke unit lain berjenis konsol sama (misal stik rusak). Waktu tetap berjalan. */
    public Sesi pindah(Pengguna aktor, long sesiId, long unitBaru) {
        wajibLogin(aktor);
        return db.tx(c -> {
            Sesi s = sesiMain(c, sesiId);
            Object[] u = Database.satu(c, "SELECT konsol_id, servis, aktif, kode FROM unit WHERE id=?",
                    rs -> new Object[]{rs.getLong(1), rs.getInt(2), rs.getInt(3), rs.getString(4)}, unitBaru);
            long konsolLama = Database.angka(c, "SELECT konsol_id FROM unit WHERE id=?", s.unitId());
            if (u == null || (int) u[2] != 1 || (int) u[1] == 1) throw new Gagal("Unit tujuan tidak bisa dipakai.");
            if ((long) u[0] != konsolLama) throw new Gagal("Hanya bisa pindah ke unit dengan jenis konsol yang sama.");
            if (Database.angka(c, "SELECT COUNT(*) FROM sesi WHERE unit_id=? AND status='MAIN'", unitBaru) > 0) throw new Gagal("Unit " + u[3] + " sedang dipakai.");
            Database.ubah(c, "UPDATE sesi SET unit_id=? WHERE id=?", unitBaru, sesiId);
            log(c, sesiId, jam.millis(), "PINDAH", s.unitKode() + " -> " + u[3], aktor);
            return Database.satu(c, SELECT + " WHERE s.id=?", SesiService::map, sesiId);
        });
    }

    /** Tambah jajan ke tagihan sesi. Stok dikurangi atomik. */
    public void tambahJajan(Pengguna aktor, long sesiId, long jajanId, int qty) {
        wajibLogin(aktor);
        if (qty <= 0 || qty > 100) throw new Gagal("Jumlah jajan 1-100.");
        db.tx(c -> {
            sesiMain(c, sesiId);
            masukkanJajan(c, sesiId, jajanId, qty);
            return null;
        });
    }

    /** Hapus satu baris jajan dari sesi yang masih berjalan (salah input). Stok dikembalikan. */
    public void hapusJajan(Pengguna aktor, long sesiId, long jajanId) {
        wajibLogin(aktor);
        db.tx(c -> {
            sesiMain(c, sesiId);
            Long id = Database.satu(c, "SELECT id FROM sesi_jajan WHERE sesi_id=? AND jajan_id=? ORDER BY id DESC LIMIT 1", rs -> rs.getLong(1), sesiId, jajanId);
            if (id == null) throw new Gagal("Jajan tidak ada di tagihan.");
            int qty = (int) Database.angka(c, "SELECT qty FROM sesi_jajan WHERE id=?", id);
            Database.ubah(c, "DELETE FROM sesi_jajan WHERE id=?", id);
            Database.ubah(c, "UPDATE jajan SET stok = stok + ? WHERE id=?", qty, jajanId);
            return null;
        });
    }

    private void masukkanJajan(Connection c, long sesiId, long jajanId, int qty) throws SQLException {
        Object[] j = Database.satu(c, "SELECT nama, harga, stok, aktif FROM jajan WHERE id=?",
                rs -> new Object[]{rs.getString(1), rs.getLong(2), rs.getInt(3), rs.getInt(4)}, jajanId);
        if (j == null || (int) j[3] != 1) throw new Gagal("Menu jajan tidak tersedia.");
        if (Database.ubah(c, "UPDATE jajan SET stok = stok - ? WHERE id=? AND stok >= ?", qty, jajanId, qty) == 0) {
            throw new Gagal("Stok " + j[0] + " tinggal " + j[2] + ".");
        }
        Database.ubah(c, "INSERT INTO sesi_jajan(sesi_id,jajan_id,nama,harga,qty,waktu) VALUES(?,?,?,?,?,?)", sesiId, jajanId, j[0], j[1], qty, jam.millis());
    }

    public List<ItemJajan> jajan(long sesiId) {
        return db.daftar("SELECT jajan_id, nama, harga, SUM(qty) FROM sesi_jajan WHERE sesi_id=? GROUP BY jajan_id, harga ORDER BY MIN(id)",
                rs -> new ItemJajan(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getInt(4)), sesiId);
    }

    public record Tagihan(long durasiMs, int menitTagih, String rincianSewa, long biayaSewa, List<ItemJajan> jajan, long biayaJajan) {
        public long total() { return biayaSewa + biayaJajan; }
    }

    /** Tagihan berjalan (untuk ditampilkan di kartu unit dan dialog bayar). */
    public Tagihan tagihan(long sesiId) {
        Sesi s = byId(sesiId);
        if (s == null) throw new Gagal("Sesi tidak ditemukan.");
        return tagihan(s, s.selesai() != null ? s.selesai() : jam.millis());
    }

    public Tagihan tagihan(Sesi s, long waktu) {
        List<ItemJajan> j = jajan(s.id());
        long bj = j.stream().mapToLong(ItemJajan::subtotal).sum();
        if (s.mode().equals(Sesi.JAJAN)) return new Tagihan(0, 0, "Jajan saja", 0, j, bj);
        long durasi = s.durasiMs(waktu);
        Tarif.Hasil h = s.mode().equals(Sesi.PAKET)
                ? Tarif.paket(durasi, s.menitPaket(), s.hargaPaket(), s.tarifJam(), set.aturan())
                : Tarif.open(durasi, s.tarifJam(), set.aturan());
        return new Tagihan(durasi, h.menitTagih(), h.rincian(), h.biaya(), j, bj);
    }

    /** Selesai & bayar. Biaya dihitung ulang dari database dan jam saat ini, bukan dari angka di layar. */
    public Sesi bayar(Pengguna aktor, long sesiId, long diskon, String metode, long uang) {
        wajibLogin(aktor);
        if (!METODE.contains(metode)) throw new Gagal("Metode pembayaran tidak dikenal.");
        if (diskon < 0) throw new Gagal("Diskon tidak boleh negatif.");
        return db.tx(c -> {
            Sesi s = sesiMain(c, sesiId);
            long now = jam.millis();
            long jedaTambahan = s.dijeda() ? now - s.jedaSejak() : 0;
            Sesi akhir = new Sesi(s.id(), s.nomor(), s.unitId(), s.unitKode(), s.unitNama(), s.konsol(), s.pelanggan(), s.mode(), s.paketNama(),
                    s.menitPaket(), s.hargaPaket(), s.tarifJam(), s.mulai(), s.jedaMs() + jedaTambahan, 0, now, "MAIN",
                    0, 0, 0, 0, null, 0, 0, s.operator(), null);
            Tagihan t = tagihan(akhir, now);
            if (diskon > t.total()) throw new Gagal("Diskon melebihi total tagihan.");
            long total = t.total() - diskon;
            long bayar = metode.equals("TUNAI") ? uang : total;
            if (bayar < total) throw new Gagal("Uang kurang " + Rupiah.format(total - bayar) + ".");
            Database.ubah(c, """
                    UPDATE sesi SET status='SELESAI', selesai=?, jeda_ms=?, jeda_sejak=0, tanggal=?, biaya_sewa=?, biaya_jajan=?,
                      diskon=?, total=?, metode=?, bayar=?, kembali=? WHERE id=?""",
                    now, akhir.jedaMs(), tanggal(now), t.biayaSewa(), t.biayaJajan(), diskon, total, metode, bayar, bayar - total, sesiId);
            log(c, sesiId, now, "BAYAR", metode + " " + Rupiah.format(total), aktor);
            return Database.satu(c, SELECT + " WHERE s.id=?", SesiService::map, sesiId);
        });
    }

    /** Jual jajan tanpa main (pelanggan yang hanya beli minum/mie). */
    public Sesi jualJajan(Pengguna aktor, String pelanggan, Map<Long, Integer> isi, long diskon, String metode, long uang) {
        wajibLogin(aktor);
        if (isi == null || isi.isEmpty()) throw new Gagal("Pilih minimal satu menu.");
        if (!METODE.contains(metode)) throw new Gagal("Metode pembayaran tidak dikenal.");
        if (diskon < 0) throw new Gagal("Diskon tidak boleh negatif.");
        for (int q : isi.values()) if (q <= 0 || q > 100) throw new Gagal("Jumlah per menu 1-100.");
        return db.tx(c -> {
            long now = jam.millis();
            String nomor = nomor(c, "JJ", now);
            long id = Database.sisip(c, "INSERT INTO sesi(nomor,pelanggan,mode,mulai,selesai,tanggal,status,operator_id) VALUES(?,?,'JAJAN',?,?,?,'MAIN',?)",
                    nomor, pelanggan == null || pelanggan.isBlank() ? "Tamu" : pelanggan.trim(), now, now, tanggal(now), aktor.id());
            for (Map.Entry<Long, Integer> e : isi.entrySet()) masukkanJajan(c, id, e.getKey(), e.getValue());
            long bj = Database.angka(c, "SELECT COALESCE(SUM(harga*qty),0) FROM sesi_jajan WHERE sesi_id=?", id);
            if (diskon > bj) throw new Gagal("Diskon melebihi total.");
            long total = bj - diskon;
            long bayar = metode.equals("TUNAI") ? uang : total;
            if (bayar < total) throw new Gagal("Uang kurang " + Rupiah.format(total - bayar) + ".");
            Database.ubah(c, "UPDATE sesi SET status='SELESAI', biaya_jajan=?, diskon=?, total=?, metode=?, bayar=?, kembali=? WHERE id=?",
                    bj, diskon, total, metode, bayar, bayar - total, id);
            log(c, id, now, "BAYAR", "Jajan " + metode + " " + Rupiah.format(total), aktor);
            return Database.satu(c, SELECT + " WHERE s.id=?", SesiService::map, id);
        });
    }

    /** Batalkan sesi (salah unit / transaksi salah). Admin saja. Stok jajan dikembalikan. */
    public void batal(Pengguna aktor, long sesiId, String alasan) {
        AuthService.wajibAdmin(aktor);
        if (alasan == null || alasan.trim().length() < 3) throw new Gagal("Tulis alasan pembatalan.");
        db.tx(c -> {
            Sesi s = Database.satu(c, SELECT + " WHERE s.id=?", SesiService::map, sesiId);
            if (s == null) throw new Gagal("Transaksi tidak ditemukan.");
            if (s.batal()) throw new Gagal("Transaksi ini sudah dibatalkan.");
            long now = jam.millis();
            Database.ubah(c, "UPDATE sesi SET status='BATAL', alasan_batal=?, selesai=COALESCE(selesai, ?), tanggal=COALESCE(tanggal, ?) WHERE id=?",
                    alasan.trim(), now, tanggal(now), sesiId);
            for (ItemJajan it : Database.daftar(c, "SELECT jajan_id, nama, harga, qty FROM sesi_jajan WHERE sesi_id=?",
                    rs -> new ItemJajan(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getInt(4)), sesiId)) {
                Database.ubah(c, "UPDATE jajan SET stok = stok + ? WHERE id=?", it.qty(), it.jajanId());
            }
            log(c, sesiId, now, "BATAL", alasan.trim(), aktor);
            return null;
        });
    }

    public List<Sesi> riwayat(LocalDate dari, LocalDate sampai, String cari) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE s.status<>'MAIN' AND s.tanggal >= ? AND s.tanggal <= ?");
        List<Object> p = new ArrayList<>(List.of(dari.toString(), sampai.toString()));
        if (cari != null && !cari.isBlank()) {
            sql.append(" AND (s.nomor LIKE ? ESCAPE '\\' OR s.pelanggan LIKE ? ESCAPE '\\' OR u.kode LIKE ? ESCAPE '\\')");
            String k = "%" + cari.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            p.add(k); p.add(k); p.add(k);
        }
        sql.append(" ORDER BY s.selesai DESC LIMIT 3000");
        return db.daftar(sql.toString(), SesiService::map, p.toArray());
    }

    public record Log(long waktu, String aksi, String keterangan, String operator) { }

    public List<Log> log(long sesiId) {
        return db.daftar("SELECT l.waktu, l.aksi, l.keterangan, p.nama FROM log_sesi l LEFT JOIN pengguna p ON p.id=l.operator_id WHERE l.sesi_id=? ORDER BY l.id",
                rs -> new Log(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)), sesiId);
    }

    // ---------------- bantuan ----------------

    private Sesi sesiMain(Connection c, long id) throws SQLException {
        Sesi s = Database.satu(c, SELECT + " WHERE s.id=?", SesiService::map, id);
        if (s == null) throw new Gagal("Sesi tidak ditemukan.");
        if (!s.main()) throw new Gagal("Sesi ini sudah selesai.");
        return s;
    }

    private String tanggal(long ms) { return Instant.ofEpochMilli(ms).atZone(jam.getZone()).toLocalDate().toString(); }

    /** Nomor harian PS-yyMMdd-001 / JJ-yyMMdd-001, dibuat di dalam transaksi sehingga tidak dobel. */
    private String nomor(Connection c, String kode, long ms) throws SQLException {
        String prefix = kode + "-" + Instant.ofEpochMilli(ms).atZone(jam.getZone()).format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        String last = Database.satu(c, "SELECT nomor FROM sesi WHERE nomor LIKE ? ORDER BY nomor DESC LIMIT 1", rs -> rs.getString(1), prefix + "%");
        int urut = last == null ? 1 : Integer.parseInt(last.substring(prefix.length())) + 1;
        return prefix + String.format("%03d", urut);
    }

    private static void log(Connection c, long sesiId, long waktu, String aksi, String ket, Pengguna aktor) throws SQLException {
        Database.ubah(c, "INSERT INTO log_sesi(sesi_id,waktu,aksi,keterangan,operator_id) VALUES(?,?,?,?,?)", sesiId, waktu, aksi, ket, aktor.id());
    }

    private static void wajibLogin(Pengguna p) { if (p == null) throw new Gagal("Silakan login dulu."); }

    static Sesi map(ResultSet rs) throws SQLException {
        long u = rs.getLong("unit_id");
        Long unitId = rs.wasNull() ? null : u;
        long sl = rs.getLong("selesai");
        Long selesai = rs.wasNull() ? null : sl;
        return new Sesi(rs.getLong("id"), rs.getString("nomor"), unitId, rs.getString("unit_kode"), rs.getString("unit_nama"),
                rs.getString("konsol"), rs.getString("pelanggan"), rs.getString("mode"), rs.getString("paket_nama"),
                rs.getInt("menit_paket"), rs.getLong("harga_paket"), rs.getLong("tarif_jam"), rs.getLong("mulai"),
                rs.getLong("jeda_ms"), rs.getLong("jeda_sejak"), selesai, rs.getString("status"),
                rs.getLong("biaya_sewa"), rs.getLong("biaya_jajan"), rs.getLong("diskon"), rs.getLong("total"),
                rs.getString("metode"), rs.getLong("bayar"), rs.getLong("kembali"), rs.getString("operator"), rs.getString("alasan_batal"));
    }
}
