package id.papicode.billingps.db;

/** Skema tabel. Dijalankan setiap aplikasi start (CREATE IF NOT EXISTS, aman diulang). */
final class Skema {
    private Skema() { }

    static final String[] SQL = {
        "CREATE TABLE IF NOT EXISTS pengaturan (kunci TEXT PRIMARY KEY, nilai TEXT NOT NULL)",
        """
        CREATE TABLE IF NOT EXISTS pengguna (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          username TEXT NOT NULL UNIQUE COLLATE NOCASE,
          nama TEXT NOT NULL,
          peran TEXT NOT NULL CHECK (peran IN ('ADMIN','OPERATOR')),
          hash TEXT NOT NULL,
          aktif INTEGER NOT NULL DEFAULT 1,
          gagal INTEGER NOT NULL DEFAULT 0,
          kunci_sampai INTEGER NOT NULL DEFAULT 0,
          dibuat TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS konsol (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          nama TEXT NOT NULL UNIQUE COLLATE NOCASE,
          tarif_jam INTEGER NOT NULL CHECK (tarif_jam > 0)
        )""",
        """
        CREATE TABLE IF NOT EXISTS unit (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          kode TEXT NOT NULL UNIQUE COLLATE NOCASE,
          nama TEXT NOT NULL,
          konsol_id INTEGER NOT NULL REFERENCES konsol(id),
          keterangan TEXT,
          servis INTEGER NOT NULL DEFAULT 0,
          aktif INTEGER NOT NULL DEFAULT 1,
          urut INTEGER NOT NULL DEFAULT 0
        )""",
        """
        CREATE TABLE IF NOT EXISTS paket (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          nama TEXT NOT NULL,
          konsol_id INTEGER NOT NULL REFERENCES konsol(id),
          menit INTEGER NOT NULL CHECK (menit > 0),
          harga INTEGER NOT NULL CHECK (harga > 0),
          aktif INTEGER NOT NULL DEFAULT 1
        )""",
        """
        CREATE TABLE IF NOT EXISTS jajan (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          nama TEXT NOT NULL UNIQUE COLLATE NOCASE,
          kategori TEXT NOT NULL DEFAULT 'Minuman',
          harga INTEGER NOT NULL CHECK (harga > 0),
          stok INTEGER NOT NULL DEFAULT 0 CHECK (stok >= 0),
          aktif INTEGER NOT NULL DEFAULT 1
        )""",
        """
        CREATE TABLE IF NOT EXISTS sesi (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          nomor TEXT NOT NULL UNIQUE,
          unit_id INTEGER REFERENCES unit(id),
          pelanggan TEXT NOT NULL,
          mode TEXT NOT NULL CHECK (mode IN ('PAKET','OPEN','JAJAN')),
          paket_nama TEXT,
          menit_paket INTEGER NOT NULL DEFAULT 0,
          harga_paket INTEGER NOT NULL DEFAULT 0,
          tarif_jam INTEGER NOT NULL DEFAULT 0,
          mulai INTEGER NOT NULL,
          jeda_ms INTEGER NOT NULL DEFAULT 0,
          jeda_sejak INTEGER NOT NULL DEFAULT 0,
          selesai INTEGER,
          tanggal TEXT,
          status TEXT NOT NULL CHECK (status IN ('MAIN','SELESAI','BATAL')),
          biaya_sewa INTEGER NOT NULL DEFAULT 0,
          biaya_jajan INTEGER NOT NULL DEFAULT 0,
          diskon INTEGER NOT NULL DEFAULT 0,
          total INTEGER NOT NULL DEFAULT 0,
          metode TEXT,
          bayar INTEGER NOT NULL DEFAULT 0,
          kembali INTEGER NOT NULL DEFAULT 0,
          operator_id INTEGER NOT NULL REFERENCES pengguna(id),
          alasan_batal TEXT
        )""",
        // satu unit hanya boleh punya satu sesi berjalan: dijaga langsung oleh database
        "CREATE UNIQUE INDEX IF NOT EXISTS ux_unit_main ON sesi(unit_id) WHERE status='MAIN'",
        "CREATE INDEX IF NOT EXISTS ix_sesi_tgl ON sesi(tanggal)",
        """
        CREATE TABLE IF NOT EXISTS sesi_jajan (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          sesi_id INTEGER NOT NULL REFERENCES sesi(id) ON DELETE CASCADE,
          jajan_id INTEGER NOT NULL REFERENCES jajan(id),
          nama TEXT NOT NULL,
          harga INTEGER NOT NULL,
          qty INTEGER NOT NULL CHECK (qty > 0),
          waktu INTEGER NOT NULL
        )""",
        "CREATE INDEX IF NOT EXISTS ix_sj_sesi ON sesi_jajan(sesi_id)",
        """
        CREATE TABLE IF NOT EXISTS log_sesi (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          sesi_id INTEGER NOT NULL REFERENCES sesi(id) ON DELETE CASCADE,
          waktu INTEGER NOT NULL,
          aksi TEXT NOT NULL,
          keterangan TEXT,
          operator_id INTEGER REFERENCES pengguna(id)
        )"""
    };
}
