package id.papicode.billingps.service;

import id.papicode.billingps.db.Database;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Laporan pendapatan, okupansi unit, jam ramai, paket & jajan terlaris. Hanya transaksi SELESAI. */
public class LaporanService {

    private final Database db;
    private final PengaturanService set;

    public LaporanService(Database db, PengaturanService set) {
        this.db = db;
        this.set = set;
    }

    public record Ringkasan(long sewa, long jajan, long diskon, long total, long sesi, long menitMain, long transaksiJajan) {
        public long rataMenit() { return sesi == 0 ? 0 : menitMain / sesi; }
        public long rataBelanja() { return sesi + transaksiJajan == 0 ? 0 : total / (sesi + transaksiJajan); }
    }

    public Ringkasan ringkasan(LocalDate dari, LocalDate sampai) {
        return db.satu("""
                SELECT COALESCE(SUM(biaya_sewa),0), COALESCE(SUM(biaya_jajan),0), COALESCE(SUM(diskon),0), COALESCE(SUM(total),0),
                  SUM(CASE WHEN mode<>'JAJAN' THEN 1 ELSE 0 END),
                  COALESCE(SUM(CASE WHEN mode<>'JAJAN' THEN (selesai - mulai - jeda_ms) / 60000 END),0),
                  SUM(CASE WHEN mode='JAJAN' THEN 1 ELSE 0 END)
                FROM sesi WHERE status='SELESAI' AND tanggal >= ? AND tanggal <= ?""",
                rs -> new Ringkasan(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getLong(6), rs.getLong(7)),
                dari.toString(), sampai.toString());
    }

    /** Pendapatan per hari: [0] = sewa (setelah diskon dibebankan ke sewa), [1] = jajan. */
    public Map<LocalDate, long[]> harian(LocalDate dari, LocalDate sampai) {
        Map<LocalDate, long[]> out = new LinkedHashMap<>();
        for (LocalDate d = dari; !d.isAfter(sampai); d = d.plusDays(1)) out.put(d, new long[2]);
        db.daftar("SELECT tanggal, SUM(biaya_sewa - diskon), SUM(biaya_jajan) FROM sesi WHERE status='SELESAI' AND tanggal >= ? AND tanggal <= ? GROUP BY tanggal",
                rs -> {
                    long[] v = out.get(LocalDate.parse(rs.getString(1)));
                    if (v != null) { v[0] = rs.getLong(2); v[1] = rs.getLong(3); }
                    return null;
                }, dari.toString(), sampai.toString());
        return out;
    }

    public record PerUnit(String kode, String nama, String konsol, long sesi, long menit, long pendapatan, double okupansi) { }

    /** Okupansi = menit main / (jumlah hari x jam operasional). */
    public List<PerUnit> perUnit(LocalDate dari, LocalDate sampai) {
        long hari = ChronoUnit.DAYS.between(dari, sampai) + 1;
        long menitBuka = Math.max(1, (long) (set.angka("jam_tutup") - set.angka("jam_buka")) * 60 * hari);
        return db.daftar("""
                SELECT u.kode, u.nama, k.nama, COUNT(s.id), COALESCE(SUM((s.selesai - s.mulai - s.jeda_ms) / 60000),0), COALESCE(SUM(s.biaya_sewa),0)
                FROM unit u JOIN konsol k ON k.id=u.konsol_id
                LEFT JOIN sesi s ON s.unit_id=u.id AND s.status='SELESAI' AND s.tanggal >= ? AND s.tanggal <= ?
                WHERE u.aktif=1 GROUP BY u.id ORDER BY u.urut, u.kode""",
                rs -> new PerUnit(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5), rs.getLong(6),
                        Math.min(1.0, rs.getLong(5) / (double) menitBuka)),
                dari.toString(), sampai.toString());
    }

    /** Jumlah sesi yang dimulai per jam (0-23). */
    public long[] jamMulai(LocalDate dari, LocalDate sampai) {
        long[] j = new long[24];
        db.daftar("SELECT CAST(strftime('%H', mulai/1000, 'unixepoch', 'localtime') AS INTEGER), COUNT(*) FROM sesi "
                        + "WHERE status='SELESAI' AND mode<>'JAJAN' AND tanggal >= ? AND tanggal <= ? GROUP BY 1",
                rs -> { j[rs.getInt(1)] = rs.getLong(2); return null; }, dari.toString(), sampai.toString());
        return j;
    }

    public record Peringkat(String nama, long jumlah, long nilai) { }

    public List<Peringkat> paketPopuler(LocalDate dari, LocalDate sampai) {
        return db.daftar("""
                SELECT COALESCE(paket_nama, 'Open billing'), COUNT(*), SUM(biaya_sewa) FROM sesi
                WHERE status='SELESAI' AND mode<>'JAJAN' AND tanggal >= ? AND tanggal <= ? GROUP BY 1 ORDER BY 2 DESC LIMIT 10""",
                rs -> new Peringkat(rs.getString(1), rs.getLong(2), rs.getLong(3)), dari.toString(), sampai.toString());
    }

    public List<Peringkat> jajanTerlaris(LocalDate dari, LocalDate sampai) {
        return db.daftar("""
                SELECT j.nama, SUM(j.qty), SUM(j.qty*j.harga) FROM sesi_jajan j JOIN sesi s ON s.id=j.sesi_id
                WHERE s.status='SELESAI' AND s.tanggal >= ? AND s.tanggal <= ? GROUP BY j.jajan_id ORDER BY 2 DESC LIMIT 10""",
                rs -> new Peringkat(rs.getString(1), rs.getLong(2), rs.getLong(3)), dari.toString(), sampai.toString());
    }

    /** Baris transaksi untuk ekspor CSV. */
    public List<List<Object>> detail(LocalDate dari, LocalDate sampai) {
        List<List<Object>> out = new ArrayList<>();
        db.daftar("""
                SELECT s.nomor, s.tanggal, COALESCE(u.kode,'-'), s.pelanggan, s.mode, COALESCE(s.paket_nama,''),
                  CASE WHEN s.mode='JAJAN' THEN 0 ELSE (s.selesai - s.mulai - s.jeda_ms)/60000 END,
                  s.biaya_sewa, s.biaya_jajan, s.diskon, s.total, s.metode, p.nama
                FROM sesi s LEFT JOIN unit u ON u.id=s.unit_id JOIN pengguna p ON p.id=s.operator_id
                WHERE s.status='SELESAI' AND s.tanggal >= ? AND s.tanggal <= ? ORDER BY s.selesai""",
                rs -> out.add(List.of(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getLong(7), rs.getLong(8), rs.getLong(9), rs.getLong(10), rs.getLong(11), rs.getString(12), rs.getString(13))),
                dari.toString(), sampai.toString());
        return out;
    }
}
