package id.papicode.billingps;

import id.papicode.billingps.db.DataContoh;
import id.papicode.billingps.model.*;
import id.papicode.billingps.service.Gagal;
import id.papicode.billingps.service.LaporanService;
import id.papicode.billingps.service.SesiService;
import id.papicode.billingps.service.Struk;
import id.papicode.billingps.service.Tarif;
import id.papicode.billingps.ui.Ikon;
import id.papicode.billingps.util.Csv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BillingPsTest {

    static class JamUji extends Clock {
        Instant t = LocalDateTime.of(2026, 7, 4, 14, 0).atZone(ZoneId.of("Asia/Jakarta")).toInstant();
        @Override public ZoneId getZone() { return ZoneId.of("Asia/Jakarta"); }
        @Override public Clock withZone(ZoneId z) { return this; }
        @Override public Instant instant() { return t; }
        void maju(int menit) { t = t.plusSeconds(menit * 60L); }
    }

    @TempDir Path tmp;
    JamUji jam;
    Konteks k;
    Pengguna admin;
    Konsol ps4;
    Unit u1, u2;
    Paket p1, p2;
    Jajan mie;

    @BeforeEach
    void siap() {
        jam = new JamUji();
        k = new Konteks(tmp.resolve("uji.db").toFile(), jam);
        k.auth.pastikanAdminAwal();
        admin = k.auth.login("admin", "admin123");
        ps4 = k.master.simpanKonsol(admin, new Konsol(0, "PS4", 7000));
        u1 = k.master.simpanUnit(admin, new Unit(0, "PS-01", "Meja 1", ps4.id(), null, 0, "TV 32\"", false, true, 0));
        u2 = k.master.simpanUnit(admin, new Unit(0, "PS-02", "Meja 2", ps4.id(), null, 0, "TV 32\"", false, true, 0));
        p1 = k.master.simpanPaket(admin, new Paket(0, "PS4 1 Jam", ps4.id(), null, 60, 7000, true));
        p2 = k.master.simpanPaket(admin, new Paket(0, "PS4 2 Jam", ps4.id(), null, 120, 13000, true));
        mie = k.master.simpanJajan(admin, new Jajan(0, "Indomie Goreng Telur", "Makanan", 10000, 0, true), 5);
    }

    // ---------------- aturan tarif ----------------

    @Test
    void tarifOpenDibulatkanPerBlokDanMinimal() {
        Tarif.Aturan a = new Tarif.Aturan(15, 30, 3);
        assertEquals(3500, Tarif.open(ms(10), 7000, a).biaya(), "minimal 30 menit");
        assertEquals(30, Tarif.open(ms(10), 7000, a).menitTagih());
        assertEquals(7000, Tarif.open(ms(46), 7000, a).biaya(), "46 menit dibulatkan ke 60 menit");
        assertEquals(60, Tarif.open(ms(46), 7000, a).menitTagih());
        assertEquals(7000, Tarif.open(ms(60), 7000, a).biaya());
        assertEquals(9000, Tarif.open(ms(61), 7000, a).biaya(), "61 -> 75 menit = Rp8.750, dibulatkan ke atas per Rp500");
    }

    @Test
    void tarifPaketDenganToleransiDanLebihWaktu() {
        Tarif.Aturan a = new Tarif.Aturan(15, 30, 3);
        assertEquals(13000, Tarif.paket(ms(118), 120, 13000, 7000, a).biaya(), "belum habis");
        assertEquals(13000, Tarif.paket(ms(123), 120, 13000, 7000, a).biaya(), "lebih 3 menit masih toleransi");
        Tarif.Hasil h = Tarif.paket(ms(124), 120, 13000, 7000, a);
        assertEquals(15, h.lebihMenit(), "lebih 4 menit dibulatkan ke blok 15");
        assertEquals(13000 + 2000, h.biaya(), "15 menit x 7000/60 = 1750 -> Rp2.000");
        assertEquals(1, Tarif.menitMain(1), "detik pertama sudah dihitung satu menit");
        assertEquals(500, Tarif.bulat500(1));
        assertEquals(1500, Tarif.bulat500(1500));
    }

    // ---------------- alur sesi ----------------

    @Test
    void alurPaketTambahWaktuJajanDanBayar() {
        Sesi s = k.sesi.mulai(admin, u1.id(), "Raka", p1.id());
        assertEquals("PS-260704-001", s.nomor());
        assertEquals(60, s.menitPaket());
        jam.maju(50);
        k.sesi.tambahWaktu(admin, s.id(), p1.id());
        k.sesi.tambahJajan(admin, s.id(), mie.id(), 2);
        assertEquals(3, k.db.angka("SELECT stok FROM jajan WHERE id=?", mie.id()));
        jam.maju(65); // total 115 menit dari paket 120
        SesiService.Tagihan t = k.sesi.tagihan(s.id());
        assertEquals(14000, t.biayaSewa());
        assertEquals(20000, t.biayaJajan());
        assertEquals(34000, t.total());
        assertThrows(Gagal.class, () -> k.sesi.bayar(admin, s.id(), 0, "TUNAI", 30000), "uang kurang");
        Sesi b = k.sesi.bayar(admin, s.id(), 4000, "TUNAI", 50000);
        assertEquals("SELESAI", b.status());
        assertEquals(30000, b.total());
        assertEquals(20000, b.kembali());
        assertEquals("2026-07-04", k.db.satu("SELECT tanggal FROM sesi WHERE id=?", rs -> rs.getString(1), s.id()));
        assertThrows(Gagal.class, () -> k.sesi.tambahJajan(admin, s.id(), mie.id(), 1), "sesi sudah selesai");
        assertTrue(k.sesi.berjalan().isEmpty());
        List<SesiService.Log> log = k.sesi.log(s.id());
        assertEquals(List.of("MULAI", "TAMBAH", "BAYAR"), log.stream().map(SesiService.Log::aksi).toList());
    }

    @Test
    void jedaTidakDihitung() {
        Sesi s = k.sesi.mulai(admin, u1.id(), "Bima", null);
        jam.maju(40);
        k.sesi.jeda(admin, s.id());
        assertThrows(Gagal.class, () -> k.sesi.jeda(admin, s.id()), "sudah dijeda");
        jam.maju(30); // salat & makan
        assertEquals(40 * 60_000L, k.sesi.byId(s.id()).durasiMs(k.sesi.sekarang()));
        k.sesi.lanjut(admin, s.id());
        jam.maju(20);
        Sesi b = k.sesi.bayar(admin, s.id(), 0, "QRIS", 0);
        assertEquals(60 * 60_000L, b.durasiMs(b.selesai()));
        assertEquals(7000, b.biayaSewa(), "60 menit open billing");
        assertEquals(b.total(), b.bayar(), "QRIS selalu pas");
    }

    @Test
    void bayarSaatMasihJedaJugaBenar() {
        Sesi s = k.sesi.mulai(admin, u1.id(), "Dio", null);
        jam.maju(30);
        k.sesi.jeda(admin, s.id());
        jam.maju(45);
        Sesi b = k.sesi.bayar(admin, s.id(), 0, "TUNAI", 10000);
        assertEquals(30 * 60_000L, b.durasiMs(b.selesai()));
        assertEquals(3500, b.biayaSewa());
    }

    @Test
    void satuUnitHanyaSatuSesiDanUnitServisDitolak() {
        k.sesi.mulai(admin, u1.id(), "A", null);
        assertThrows(Gagal.class, () -> k.sesi.mulai(admin, u1.id(), "B", null));
        k.master.setServis(admin, u2.id(), true);
        assertThrows(Gagal.class, () -> k.sesi.mulai(admin, u2.id(), "C", null));
        // index unik di database tetap menjaga walau pengecekan di kode dilewati
        assertThrows(RuntimeException.class, () -> k.db.ubah(
                "INSERT INTO sesi(nomor,unit_id,pelanggan,mode,mulai,status,operator_id) VALUES('X',?, 'Z','OPEN',0,'MAIN',?)", u1.id(), admin.id()));
    }

    @Test
    void paketHarusSesuaiKonsol() {
        Konsol ps5 = k.master.simpanKonsol(admin, new Konsol(0, "PS5", 12000));
        Paket p5 = k.master.simpanPaket(admin, new Paket(0, "PS5 1 Jam", ps5.id(), null, 60, 12000, true));
        assertThrows(Gagal.class, () -> k.sesi.mulai(admin, u1.id(), "Kevin", p5.id()));
        Sesi s = k.sesi.mulai(admin, u1.id(), "Kevin", p1.id());
        assertThrows(Gagal.class, () -> k.sesi.tambahWaktu(admin, s.id(), p5.id()));
        Sesi open = k.sesi.mulai(admin, u2.id(), "Open", null);
        assertThrows(Gagal.class, () -> k.sesi.tambahWaktu(admin, open.id(), p1.id()), "open billing tidak perlu tambah waktu");
    }

    @Test
    void pindahUnitMembawaWaktu() {
        Sesi s = k.sesi.mulai(admin, u1.id(), "Fajar", p2.id());
        jam.maju(30);
        Sesi p = k.sesi.pindah(admin, s.id(), u2.id());
        assertEquals("PS-02", p.unitKode());
        assertEquals(90 * 60_000L, p.sisaMs(k.sesi.sekarang()));
        assertNotNull(k.sesi.mulai(admin, u1.id(), "Baru", null), "unit lama kosong lagi");
        Unit u3 = k.master.simpanUnit(admin, new Unit(0, "PS-03", "Meja 3", ps4.id(), null, 0, "", false, true, 0));
        Sesi lain = k.sesi.mulai(admin, u3.id(), "X", null);
        assertThrows(Gagal.class, () -> k.sesi.pindah(admin, lain.id(), u2.id()), "unit tujuan dipakai");
    }

    @Test
    void stokJajanTidakBolehMinusDanDikembalikanSaatBatal() {
        Sesi s = k.sesi.mulai(admin, u1.id(), "Reza", null);
        k.sesi.tambahJajan(admin, s.id(), mie.id(), 4);
        assertThrows(Gagal.class, () -> k.sesi.tambahJajan(admin, s.id(), mie.id(), 2));
        k.sesi.hapusJajan(admin, s.id(), mie.id());
        assertEquals(5, k.db.angka("SELECT stok FROM jajan WHERE id=?", mie.id()));
        Sesi j = k.sesi.jualJajan(admin, "", Map.of(mie.id(), 3), 0, "TUNAI", 50000);
        assertEquals("JJ-260704-001", j.nomor());
        assertEquals(30000, j.total());
        assertEquals(2, k.db.angka("SELECT stok FROM jajan WHERE id=?", mie.id()));
        k.sesi.batal(admin, j.id(), "salah input");
        assertEquals(5, k.db.angka("SELECT stok FROM jajan WHERE id=?", mie.id()));
        assertThrows(Gagal.class, () -> k.sesi.batal(admin, j.id(), "lagi"));
        assertEquals(0, k.laporan.ringkasan(LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 4)).total(), "batal tidak dihitung");
    }

    @Test
    void operatorTidakBolehAksiAdmin() {
        k.auth.tambah(admin, "agus", "Agus Setiawan", Pengguna.OPERATOR, "rahasia1");
        Pengguna agus = k.auth.login("agus", "rahasia1");
        Sesi s = k.sesi.mulai(agus, u1.id(), "Tegar", p1.id());
        jam.maju(60);
        Sesi b = k.sesi.bayar(agus, s.id(), 0, "QRIS", 0);
        assertThrows(Gagal.class, () -> k.sesi.batal(agus, b.id(), "iseng"));
        assertThrows(Gagal.class, () -> k.master.simpanKonsol(agus, new Konsol(ps4.id(), "PS4", 1000)), "operator tidak bisa ubah tarif");
        assertThrows(Gagal.class, () -> k.pengaturan.simpan(agus, Map.of("blok_menit", "60")));
        assertEquals(10, k.master.tambahStokJajan(agus, mie.id(), 5), "operator boleh stok masuk");
        k.auth.setAktif(admin, agus.id(), false);
        assertThrows(Gagal.class, () -> k.auth.segarkan(agus));
    }

    @Test
    void duaOperatorRebutanUnitYangSama() throws Exception {
        int n = 8;
        ExecutorService ex = Executors.newFixedThreadPool(n);
        CountDownLatch mulai = new CountDownLatch(1);
        AtomicInteger sukses = new AtomicInteger();
        File file = tmp.resolve("uji.db").toFile();
        List<Future<?>> f = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            f.add(ex.submit(() -> {
                Konteks lain = new Konteks(file, jam);
                mulai.await();
                try { lain.sesi.mulai(admin, u1.id(), "Rebutan", null); sukses.incrementAndGet(); } catch (Gagal g) { /* sudah dipakai */ }
                return null;
            }));
        }
        mulai.countDown();
        for (Future<?> x : f) x.get(30, TimeUnit.SECONDS);
        ex.shutdown();
        assertEquals(1, sukses.get());
        assertEquals(1, k.sesi.berjalan().size());
    }

    @Test
    void validasiMasterDanPengaturan() {
        assertThrows(Gagal.class, () -> k.master.simpanKonsol(admin, new Konsol(0, "PS4", 9000)), "nama dobel");
        assertThrows(Gagal.class, () -> k.master.simpanKonsol(admin, new Konsol(0, "PS2", 0)), "tarif 0");
        assertThrows(Gagal.class, () -> k.master.simpanUnit(admin, new Unit(0, "ps 1", "X", ps4.id(), null, 0, "", false, true, 0)), "kode berspasi");
        assertThrows(Gagal.class, () -> k.master.simpanPaket(admin, new Paket(0, "Kilat", ps4.id(), null, 5, 1000, true)), "durasi < 15 menit");
        k.sesi.mulai(admin, u1.id(), "A", null);
        assertThrows(Gagal.class, () -> k.master.setServis(admin, u1.id(), true), "unit dipakai tidak bisa diservis");
        assertThrows(Gagal.class, () -> k.pengaturan.simpan(admin, Map.of("blok_menit", "0")));
        assertThrows(Gagal.class, () -> k.pengaturan.simpan(admin, Map.of("jam_tutup", "25")));
        k.pengaturan.simpan(admin, Map.of("blok_menit", "30", "minimal_menit", "60"));
        assertEquals(new Tarif.Aturan(30, 60, 3), k.pengaturan.aturan());
    }

    // ---------------- data contoh, laporan, struk ----------------

    @Test
    void dataContohLengkapDenganSesiBerjalan() {
        Konteks c = new Konteks(tmp.resolve("contoh.db").toFile(), jam);
        c.auth.pastikanAdminAwal();
        DataContoh.isi(c.db, jam);
        assertEquals(10, c.db.angka("SELECT COUNT(*) FROM unit"));
        assertEquals(6, c.sesi.berjalan().size());
        assertTrue(c.db.angka("SELECT COUNT(*) FROM sesi WHERE status='SELESAI'") > 1000);
        assertEquals(1, c.db.angka("SELECT COUNT(*) FROM sesi WHERE status='BATAL'"));
        assertEquals(0, c.db.angka("SELECT COUNT(*) FROM jajan WHERE stok < 0"));
        // total = sewa + jajan - diskon untuk semua transaksi selesai
        assertEquals(0, c.db.angka("SELECT COUNT(*) FROM sesi WHERE status='SELESAI' AND total <> biaya_sewa + biaya_jajan - diskon"));
        assertEquals(0, c.db.angka("SELECT COUNT(*) FROM sesi s WHERE status='SELESAI' AND biaya_jajan <> (SELECT COALESCE(SUM(harga*qty),0) FROM sesi_jajan j WHERE j.sesi_id=s.id)"));
        long now = c.sesi.sekarang();
        assertTrue(c.sesi.berjalan().stream().anyMatch(s -> s.sisaMs(now) < 0), "ada yang lewat waktu");
        assertTrue(c.sesi.berjalan().stream().anyMatch(Sesi::dijeda), "ada yang dijeda");
        LocalDate h = LocalDate.now(jam);
        LaporanService.Ringkasan r = c.laporan.ringkasan(h.minusDays(29), h);
        assertEquals(r.total(), c.laporan.harian(h.minusDays(29), h).values().stream().mapToLong(v -> v[0] + v[1]).sum());
        assertTrue(c.laporan.perUnit(h.minusDays(29), h).stream().allMatch(u -> u.okupansi() > 0 && u.okupansi() <= 1));
        assertNotNull(c.auth.login("agus", "operator123"));
        assertThrows(IllegalStateException.class, () -> DataContoh.isi(c.db, jam));
    }

    @Test
    void strukPasLebarDanCsvAman() {
        Sesi s = k.sesi.mulai(admin, u1.id(), "Rombongan Kelas XII IPA 2 SMAN 5 Bekasi", p1.id());
        k.sesi.tambahJajan(admin, s.id(), mie.id(), 1);
        jam.maju(61);
        Sesi b = k.sesi.bayar(admin, s.id(), 0, "TUNAI", 20000);
        List<String> st = Struk.susun(b, k.sesi.tagihan(b.id()), k.pengaturan, jam.getZone());
        assertTrue(st.stream().allMatch(x -> x.length() <= Struk.LEBAR), String.join("\n", st));
        assertTrue(st.stream().anyMatch(x -> x.startsWith("TOTAL") && x.endsWith("Rp17.000")));
        assertEquals("'=SUM(A1)", Csv.sel("=SUM(A1)"));
        assertEquals("'+62", Csv.sel("+62"));
        assertEquals(1, k.laporan.detail(LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 4)).size());
    }

    @Test
    void loginTerkunciDanIkonValid() throws Exception {
        for (int i = 0; i < 5; i++) assertThrows(Gagal.class, () -> k.auth.login("admin", "salah"));
        assertThrows(Gagal.class, () -> k.auth.login("admin", "admin123"));
        jam.maju(6);
        assertNotNull(k.auth.login("admin", "admin123"));
        java.lang.reflect.Field f = Ikon.class.getDeclaredField("SVG");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> svg = (Map<String, String>) f.get(null);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(24, 24, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (String n : svg.keySet()) Ikon.of(n, 24, java.awt.Color.BLACK).paintIcon(null, img.createGraphics(), 0, 0);
    }

    static long ms(int menit) { return menit * 60_000L; }
}
