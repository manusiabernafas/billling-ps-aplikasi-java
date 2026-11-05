package id.papicode.billingps.model;

public record Paket(long id, String nama, long konsolId, String konsol, int menit, long harga, boolean aktif) {
    @Override public String toString() { return nama; }
}
