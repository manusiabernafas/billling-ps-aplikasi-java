package id.papicode.billingps.model;

public record Konsol(long id, String nama, long tarifJam) {
    @Override public String toString() { return nama; }
}
