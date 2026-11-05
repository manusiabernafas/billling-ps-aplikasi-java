package id.papicode.billingps.model;

/**
 * Satu sesi main (atau penjualan jajan langsung bila mode JAJAN).
 * Waktu disimpan dalam epoch milidetik agar timer bisa dilanjutkan walau aplikasi ditutup.
 */
public record Sesi(long id, String nomor, Long unitId, String unitKode, String unitNama, String konsol, String pelanggan,
                   String mode, String paketNama, int menitPaket, long hargaPaket, long tarifJam,
                   long mulai, long jedaMs, long jedaSejak, Long selesai, String status,
                   long biayaSewa, long biayaJajan, long diskon, long total, String metode, long bayar, long kembali,
                   String operator, String alasanBatal) {

    public static final String PAKET = "PAKET", OPEN = "OPEN", JAJAN = "JAJAN";

    public boolean main() { return "MAIN".equals(status); }
    public boolean dijeda() { return main() && jedaSejak > 0; }
    public boolean batal() { return "BATAL".equals(status); }

    /** Lama bermain bersih (tanpa jeda) sampai waktu 'sekarang' atau waktu selesai. */
    public long durasiMs(long sekarang) {
        long akhir = selesai != null ? selesai : sekarang;
        long jeda = jedaMs + (jedaSejak > 0 && selesai == null ? sekarang - jedaSejak : 0);
        return Math.max(0, akhir - mulai - jeda);
    }

    /** Sisa waktu paket (negatif bila lewat). Untuk mode OPEN selalu 0. */
    public long sisaMs(long sekarang) {
        return PAKET.equals(mode) ? menitPaket * 60_000L - durasiMs(sekarang) : 0;
    }
}
