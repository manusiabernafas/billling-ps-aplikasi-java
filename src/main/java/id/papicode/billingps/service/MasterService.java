package id.papicode.billingps.service;

import id.papicode.billingps.db.Database;
import id.papicode.billingps.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Data master: jenis konsol & tarif, unit/meja, paket, dan menu jajan. */
public class MasterService {

    private static final String SEL_UNIT = "SELECT u.*, k.nama AS konsol, k.tarif_jam FROM unit u JOIN konsol k ON k.id=u.konsol_id";
    private static final String SEL_PAKET = "SELECT p.*, k.nama AS konsol FROM paket p JOIN konsol k ON k.id=p.konsol_id";

    private final Database db;

    public MasterService(Database db) { this.db = db; }

    // ---------------- konsol ----------------

    public List<Konsol> konsol() {
        return db.daftar("SELECT * FROM konsol ORDER BY tarif_jam", rs -> new Konsol(rs.getLong("id"), rs.getString("nama"), rs.getLong("tarif_jam")));
    }

    public Konsol simpanKonsol(Pengguna aktor, Konsol k) {
        AuthService.wajibAdmin(aktor);
        String nama = k.nama() == null ? "" : k.nama().trim();
        if (nama.isEmpty() || nama.length() > 30) throw new Gagal("Nama konsol 1-30 karakter.");
        if (k.tarifJam() < 500 || k.tarifJam() > 1_000_000) throw new Gagal("Tarif per jam harus Rp500 - Rp1.000.000.");
        return db.tx(c -> {
            if (Database.angka(c, "SELECT COUNT(*) FROM konsol WHERE nama=? AND id<>?", nama, k.id()) > 0) throw new Gagal("Konsol " + nama + " sudah ada.");
            long id = k.id();
            if (id == 0) id = Database.sisip(c, "INSERT INTO konsol(nama,tarif_jam) VALUES(?,?)", nama, k.tarifJam());
            else Database.ubah(c, "UPDATE konsol SET nama=?, tarif_jam=? WHERE id=?", nama, k.tarifJam(), id);
            return new Konsol(id, nama, k.tarifJam());
        });
    }

    // ---------------- unit ----------------

    public List<Unit> unit(boolean termasukNonaktif) {
        return db.daftar(SEL_UNIT + (termasukNonaktif ? "" : " WHERE u.aktif=1") + " ORDER BY u.urut, u.kode", MasterService::mapUnit);
    }

    public Unit unitById(long id) { return db.satu(SEL_UNIT + " WHERE u.id=?", MasterService::mapUnit, id); }

    public Unit simpanUnit(Pengguna aktor, Unit u) {
        AuthService.wajibAdmin(aktor);
        String kode = u.kode() == null ? "" : u.kode().trim().toUpperCase();
        String nama = u.nama() == null ? "" : u.nama().trim();
        if (!kode.matches("[A-Z0-9\\-]{2,10}")) throw new Gagal("Kode unit 2-10 karakter (huruf, angka, -), misal PS-01.");
        if (nama.isEmpty() || nama.length() > 40) throw new Gagal("Nama unit 1-40 karakter.");
        return db.tx(c -> {
            if (Database.angka(c, "SELECT COUNT(*) FROM konsol WHERE id=?", u.konsolId()) == 0) throw new Gagal("Pilih jenis konsol.");
            if (Database.angka(c, "SELECT COUNT(*) FROM unit WHERE kode=? AND id<>?", kode, u.id()) > 0) throw new Gagal("Kode " + kode + " sudah dipakai.");
            long id = u.id();
            if (id == 0) {
                int urut = (int) Database.angka(c, "SELECT COALESCE(MAX(urut),0)+1 FROM unit");
                id = Database.sisip(c, "INSERT INTO unit(kode,nama,konsol_id,keterangan,servis,aktif,urut) VALUES(?,?,?,?,?,?,?)",
                        kode, nama, u.konsolId(), u.keterangan(), u.servis() ? 1 : 0, u.aktif() ? 1 : 0, urut);
            } else {
                boolean dipakai = Database.angka(c, "SELECT COUNT(*) FROM sesi WHERE unit_id=? AND status='MAIN'", id) > 0;
                Unit lama = Database.satu(c, SEL_UNIT + " WHERE u.id=?", MasterService::mapUnit, id);
                if (dipakai && (lama.konsolId() != u.konsolId() || !u.aktif() || u.servis())) {
                    throw new Gagal("Unit sedang dipakai. Selesaikan sesinya dulu sebelum mengubah konsol, status servis, atau menonaktifkan.");
                }
                Database.ubah(c, "UPDATE unit SET kode=?, nama=?, konsol_id=?, keterangan=?, servis=?, aktif=? WHERE id=?",
                        kode, nama, u.konsolId(), u.keterangan(), u.servis() ? 1 : 0, u.aktif() ? 1 : 0, id);
            }
            return Database.satu(c, SEL_UNIT + " WHERE u.id=?", MasterService::mapUnit, id);
        });
    }

    /** Tandai unit sedang diperbaiki (stik rusak, TV mati) agar tidak bisa dipakai sesi baru. Operator boleh. */
    public void setServis(Pengguna aktor, long unitId, boolean servis) {
        if (aktor == null) throw new Gagal("Silakan login dulu.");
        db.tx(c -> {
            if (servis && Database.angka(c, "SELECT COUNT(*) FROM sesi WHERE unit_id=? AND status='MAIN'", unitId) > 0) {
                throw new Gagal("Unit sedang dipakai, selesaikan sesinya dulu.");
            }
            Database.ubah(c, "UPDATE unit SET servis=? WHERE id=?", servis ? 1 : 0, unitId);
            return null;
        });
    }

    // ---------------- paket ----------------

    public List<Paket> paket(Long konsolId, boolean termasukNonaktif) {
        return db.daftar(SEL_PAKET + " WHERE (? IS NULL OR p.konsol_id=?) AND (? OR p.aktif=1) ORDER BY k.tarif_jam, p.menit",
                MasterService::mapPaket, konsolId, konsolId, termasukNonaktif ? 1 : 0);
    }

    public Paket paketById(long id) { return db.satu(SEL_PAKET + " WHERE p.id=?", MasterService::mapPaket, id); }

    public Paket simpanPaket(Pengguna aktor, Paket p) {
        AuthService.wajibAdmin(aktor);
        String nama = p.nama() == null ? "" : p.nama().trim();
        if (nama.isEmpty() || nama.length() > 40) throw new Gagal("Nama paket 1-40 karakter.");
        if (p.menit() < 15 || p.menit() > 24 * 60) throw new Gagal("Durasi paket 15 menit - 24 jam.");
        if (p.harga() < 500) throw new Gagal("Harga paket minimal Rp500.");
        return db.tx(c -> {
            if (Database.angka(c, "SELECT COUNT(*) FROM konsol WHERE id=?", p.konsolId()) == 0) throw new Gagal("Pilih jenis konsol.");
            long id = p.id();
            if (id == 0) id = Database.sisip(c, "INSERT INTO paket(nama,konsol_id,menit,harga,aktif) VALUES(?,?,?,?,?)", nama, p.konsolId(), p.menit(), p.harga(), p.aktif() ? 1 : 0);
            else Database.ubah(c, "UPDATE paket SET nama=?, konsol_id=?, menit=?, harga=?, aktif=? WHERE id=?", nama, p.konsolId(), p.menit(), p.harga(), p.aktif() ? 1 : 0, id);
            return Database.satu(c, SEL_PAKET + " WHERE p.id=?", MasterService::mapPaket, id);
        });
    }

    // ---------------- jajan ----------------

    public List<Jajan> jajan(boolean termasukNonaktif) {
        return db.daftar("SELECT * FROM jajan" + (termasukNonaktif ? "" : " WHERE aktif=1") + " ORDER BY kategori, nama", MasterService::mapJajan);
    }

    public Jajan simpanJajan(Pengguna aktor, Jajan j, int stokAwal) {
        AuthService.wajibAdmin(aktor);
        String nama = j.nama() == null ? "" : j.nama().trim();
        if (nama.isEmpty() || nama.length() > 50) throw new Gagal("Nama menu 1-50 karakter.");
        if (j.harga() < 500) throw new Gagal("Harga minimal Rp500.");
        if (stokAwal < 0) throw new Gagal("Stok tidak boleh negatif.");
        String kat = j.kategori() == null || j.kategori().isBlank() ? "Minuman" : j.kategori().trim();
        return db.tx(c -> {
            if (Database.angka(c, "SELECT COUNT(*) FROM jajan WHERE nama=? AND id<>?", nama, j.id()) > 0) throw new Gagal("Menu " + nama + " sudah ada.");
            long id = j.id();
            if (id == 0) id = Database.sisip(c, "INSERT INTO jajan(nama,kategori,harga,stok,aktif) VALUES(?,?,?,?,?)", nama, kat, j.harga(), stokAwal, j.aktif() ? 1 : 0);
            else Database.ubah(c, "UPDATE jajan SET nama=?, kategori=?, harga=?, aktif=? WHERE id=?", nama, kat, j.harga(), j.aktif() ? 1 : 0, id);
            return Database.satu(c, "SELECT * FROM jajan WHERE id=?", MasterService::mapJajan, id);
        });
    }

    /** Tambah stok jajan (belanja ke warung/grosir). Operator boleh. */
    public int tambahStokJajan(Pengguna aktor, long jajanId, int qty) {
        if (aktor == null) throw new Gagal("Silakan login dulu.");
        if (qty <= 0 || qty > 10_000) throw new Gagal("Jumlah stok masuk 1 - 10.000.");
        return db.tx(c -> {
            if (Database.ubah(c, "UPDATE jajan SET stok = stok + ? WHERE id=?", qty, jajanId) == 0) throw new Gagal("Menu tidak ditemukan.");
            return (int) Database.angka(c, "SELECT stok FROM jajan WHERE id=?", jajanId);
        });
    }

    static Unit mapUnit(ResultSet rs) throws SQLException {
        return new Unit(rs.getLong("id"), rs.getString("kode"), rs.getString("nama"), rs.getLong("konsol_id"), rs.getString("konsol"),
                rs.getLong("tarif_jam"), rs.getString("keterangan"), rs.getInt("servis") == 1, rs.getInt("aktif") == 1, rs.getInt("urut"));
    }

    static Paket mapPaket(ResultSet rs) throws SQLException {
        return new Paket(rs.getLong("id"), rs.getString("nama"), rs.getLong("konsol_id"), rs.getString("konsol"),
                rs.getInt("menit"), rs.getLong("harga"), rs.getInt("aktif") == 1);
    }

    static Jajan mapJajan(ResultSet rs) throws SQLException {
        return new Jajan(rs.getLong("id"), rs.getString("nama"), rs.getString("kategori"), rs.getLong("harga"), rs.getInt("stok"), rs.getInt("aktif") == 1);
    }
}
