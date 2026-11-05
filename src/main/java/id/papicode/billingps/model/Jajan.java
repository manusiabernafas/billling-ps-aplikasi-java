package id.papicode.billingps.model;

public record Jajan(long id, String nama, String kategori, long harga, int stok, boolean aktif) {
    @Override public String toString() { return nama; }
}
