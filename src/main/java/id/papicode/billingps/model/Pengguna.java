package id.papicode.billingps.model;

public record Pengguna(long id, String username, String nama, String peran, boolean aktif) {
    public static final String ADMIN = "ADMIN";
    public static final String OPERATOR = "OPERATOR";

    public boolean admin() { return ADMIN.equals(peran); }
}
