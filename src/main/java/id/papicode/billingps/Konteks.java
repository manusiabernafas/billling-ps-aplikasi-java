package id.papicode.billingps;

import id.papicode.billingps.db.Database;
import id.papicode.billingps.service.*;

import java.io.File;
import java.time.Clock;

/** Wadah semua service supaya mudah dioper ke layar dan ke tes. */
public class Konteks {
    public final Database db;
    public final Clock jam;
    public final AuthService auth;
    public final PengaturanService pengaturan;
    public final MasterService master;
    public final SesiService sesi;
    public final LaporanService laporan;

    public Konteks(File fileDb, Clock jam) {
        this.db = new Database(fileDb);
        this.jam = jam;
        this.auth = new AuthService(db, jam);
        this.pengaturan = new PengaturanService(db);
        this.master = new MasterService(db);
        this.sesi = new SesiService(db, pengaturan, jam);
        this.laporan = new LaporanService(db, pengaturan);
    }
}
