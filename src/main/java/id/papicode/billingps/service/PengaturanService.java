package id.papicode.billingps.service;

import id.papicode.billingps.db.Database;
import id.papicode.billingps.model.Pengguna;

import java.util.LinkedHashMap;
import java.util.Map;

/** Identitas rental, jam buka, dan aturan pembulatan biaya. */
public class PengaturanService {

    public static final Map<String, String> BAWAAN = new LinkedHashMap<>();

    static {
        BAWAAN.put("nama_rental", "GameOn PS Corner");
        BAWAAN.put("alamat", "Jl. Kartini No. 27, Bekasi Timur");
        BAWAAN.put("telepon", "0812-9100-2277");
        BAWAAN.put("catatan_struk", "Terima kasih, sampai main lagi!");
        BAWAAN.put("jam_buka", "10");
        BAWAAN.put("jam_tutup", "24");
        BAWAAN.put("blok_menit", "15");
        BAWAAN.put("minimal_menit", "30");
        BAWAAN.put("toleransi_menit", "3");
        BAWAAN.put("peringatan_menit", "5");
    }

    private final Database db;

    public PengaturanService(Database db) { this.db = db; }

    public String get(String kunci) {
        String v = db.satu("SELECT nilai FROM pengaturan WHERE kunci=?", rs -> rs.getString(1), kunci);
        return v != null ? v : BAWAAN.getOrDefault(kunci, "");
    }

    public int angka(String kunci) {
        try { return Integer.parseInt(get(kunci)); } catch (NumberFormatException e) { return Integer.parseInt(BAWAAN.get(kunci)); }
    }

    public Tarif.Aturan aturan() {
        return new Tarif.Aturan(angka("blok_menit"), angka("minimal_menit"), angka("toleransi_menit"));
    }

    public void simpan(Pengguna aktor, Map<String, String> nilai) {
        AuthService.wajibAdmin(aktor);
        for (Map.Entry<String, String> e : nilai.entrySet()) {
            String k = e.getKey();
            String v = e.getValue() == null ? "" : e.getValue().trim();
            if (!BAWAAN.containsKey(k)) throw new Gagal("Pengaturan tidak dikenal: " + k);
            if (v.length() > 120) throw new Gagal("Isian terlalu panjang (maks 120 karakter).");
            if (k.equals("nama_rental") && v.isEmpty()) throw new Gagal("Nama rental wajib diisi.");
            switch (k) {
                case "jam_buka" -> rentang(v, 0, 23, "Jam buka");
                case "jam_tutup" -> rentang(v, 1, 24, "Jam tutup");
                case "blok_menit" -> rentang(v, 1, 60, "Blok pembulatan");
                case "minimal_menit" -> rentang(v, 0, 180, "Minimal main");
                case "toleransi_menit" -> rentang(v, 0, 30, "Toleransi");
                case "peringatan_menit" -> rentang(v, 1, 30, "Peringatan");
                default -> { }
            }
        }
        db.tx(c -> {
            for (Map.Entry<String, String> e : nilai.entrySet()) {
                Database.ubah(c, "INSERT INTO pengaturan(kunci,nilai) VALUES(?,?) ON CONFLICT(kunci) DO UPDATE SET nilai=excluded.nilai",
                        e.getKey(), e.getValue() == null ? "" : e.getValue().trim());
            }
            return null;
        });
    }

    private static void rentang(String v, int min, int max, String nama) {
        int n;
        try { n = Integer.parseInt(v); } catch (NumberFormatException e) { throw new Gagal(nama + " harus angka."); }
        if (n < min || n > max) throw new Gagal(nama + " harus " + min + "-" + max + ".");
    }
}
