package id.papicode.billingps.model;

public record ItemJajan(long jajanId, String nama, long harga, int qty) {
    public long subtotal() { return harga * qty; }
}
