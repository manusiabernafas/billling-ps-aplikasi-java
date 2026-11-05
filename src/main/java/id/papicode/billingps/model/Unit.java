package id.papicode.billingps.model;

public record Unit(long id, String kode, String nama, long konsolId, String konsol, long tarifJam,
                   String keterangan, boolean servis, boolean aktif, int urut) {
    @Override public String toString() { return kode + " · " + nama + " (" + konsol + ")"; }
}
