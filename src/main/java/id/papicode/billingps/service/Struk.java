package id.papicode.billingps.service;

import id.papicode.billingps.model.ItemJajan;
import id.papicode.billingps.model.Sesi;
import id.papicode.billingps.util.Rupiah;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Struk 32 kolom (printer thermal 58 mm). */
public final class Struk {
    private Struk() { }

    private static final DateTimeFormatter J = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
    public static final int LEBAR = 32;

    public static List<String> susun(Sesi s, SesiService.Tagihan t, PengaturanService set, ZoneId zona) {
        List<String> b = new ArrayList<>();
        tengah(b, set.get("nama_rental").toUpperCase());
        tengah(b, set.get("alamat"));
        tengah(b, set.get("telepon"));
        b.add("=".repeat(LEBAR));
        b.add(kk(s.nomor(), Instant.ofEpochMilli(s.selesai() == null ? s.mulai() : s.selesai()).atZone(zona).format(J)));
        b.add(kk("Pelanggan", s.pelanggan()));
        if (!s.mode().equals(Sesi.JAJAN)) {
            b.add(kk("Unit", s.unitKode() + " " + s.konsol()));
            b.add(kk("Mulai", Instant.ofEpochMilli(s.mulai()).atZone(zona).format(J)));
            b.add(kk("Lama main", durasi(t.durasiMs())));
            b.add("-".repeat(LEBAR));
            String sewa = s.mode().equals(Sesi.PAKET) ? s.paketNama() : "Open " + t.menitTagih() + " mnt";
            b.add(kk(potong(sewa, 20), Rupiah.ribuan(t.biayaSewa())));
            if (s.mode().equals(Sesi.PAKET) && t.biayaSewa() > s.hargaPaket()) b.add("  (termasuk lebih waktu)");
        } else {
            b.add("-".repeat(LEBAR));
        }
        for (ItemJajan j : t.jajan()) {
            b.add(potong(j.nama(), LEBAR));
            b.add(kk("  " + j.qty() + " x " + Rupiah.ribuan(j.harga()), Rupiah.ribuan(j.subtotal())));
        }
        b.add("-".repeat(LEBAR));
        if (s.diskon() > 0) b.add(kk("Diskon", "-" + Rupiah.ribuan(s.diskon())));
        b.add(kk("TOTAL", Rupiah.format(s.total())));
        if (s.metode() != null) b.add(kk(s.metode(), Rupiah.ribuan(s.bayar())));
        if ("TUNAI".equals(s.metode())) b.add(kk("Kembali", Rupiah.ribuan(s.kembali())));
        b.add("=".repeat(LEBAR));
        if (s.batal()) tengah(b, "*** DIBATALKAN ***");
        tengah(b, set.get("catatan_struk"));
        return b;
    }

    public static String durasi(long ms) {
        long m = ms / 60_000;
        return (m / 60) + " jam " + (m % 60) + " mnt";
    }

    static String potong(String s, int n) { return s == null ? "" : s.length() <= n ? s : s.substring(0, n); }

    static void tengah(List<String> b, String s) {
        if (s == null || s.isBlank()) return;
        s = potong(s, LEBAR);
        b.add(" ".repeat((LEBAR - s.length()) / 2) + s);
    }

    static String kk(String kiri, String kanan) {
        kanan = potong(kanan, LEBAR - 2);
        kiri = potong(kiri, LEBAR - kanan.length() - 1);
        return kiri + " ".repeat(Math.max(1, LEBAR - kiri.length() - kanan.length())) + kanan;
    }
}
