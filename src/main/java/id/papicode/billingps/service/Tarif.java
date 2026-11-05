package id.papicode.billingps.service;

/**
 * Aturan hitung biaya sewa. Murni (tanpa database) supaya mudah diuji.
 * - Open billing: menit dibulatkan ke atas per blok (misal 15 menit), minimal sekian menit, biaya dibulatkan ke atas per Rp500.
 * - Paket: harga paket; bila main melewati waktu paket lebih dari toleransi, kelebihannya ditagih seperti open billing.
 */
public final class Tarif {
    private Tarif() { }

    public record Aturan(int blokMenit, int minimalMenit, int toleransiMenit) {
        public static final Aturan BAWAAN = new Aturan(15, 30, 3);
    }

    public record Hasil(long biaya, int menitTagih, long lebihMenit, String rincian) { }

    public static int menitMain(long durasiMs) {
        return (int) ((durasiMs + 59_999) / 60_000); // menit berjalan dihitung penuh
    }

    public static long bulat500(double rupiah) {
        return (long) Math.ceil(rupiah / 500.0 - 1e-9) * 500;
    }

    public static int bulatBlok(int menit, int blok) {
        if (menit <= 0) return 0;
        return ((menit + blok - 1) / blok) * blok;
    }

    public static Hasil open(long durasiMs, long tarifJam, Aturan a) {
        int menit = menitMain(durasiMs);
        int tagih = Math.max(a.minimalMenit(), bulatBlok(menit, a.blokMenit()));
        long biaya = bulat500(tagih * tarifJam / 60.0);
        return new Hasil(biaya, tagih, 0, "Open " + tagih + " menit x Rp" + tarifJam + "/jam");
    }

    public static Hasil paket(long durasiMs, int menitPaket, long hargaPaket, long tarifJam, Aturan a) {
        int menit = menitMain(durasiMs);
        int lebih = menit - menitPaket;
        if (lebih <= a.toleransiMenit()) return new Hasil(hargaPaket, menitPaket, 0, "Paket " + menitPaket + " menit");
        int tagihLebih = bulatBlok(lebih, a.blokMenit());
        long biayaLebih = bulat500(tagihLebih * tarifJam / 60.0);
        return new Hasil(hargaPaket + biayaLebih, menitPaket + tagihLebih, tagihLebih,
                "Paket " + menitPaket + " menit + lebih " + tagihLebih + " menit");
    }
}
