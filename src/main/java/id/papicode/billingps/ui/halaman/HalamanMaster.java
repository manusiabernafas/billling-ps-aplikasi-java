package id.papicode.billingps.ui.halaman;

import id.papicode.billingps.Konteks;
import id.papicode.billingps.model.Konsol;
import id.papicode.billingps.model.Paket;
import id.papicode.billingps.model.Unit;
import id.papicode.billingps.service.Gagal;
import id.papicode.billingps.ui.*;
import id.papicode.billingps.util.Rupiah;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pengaturan unit: jenis konsol & tarif per jam, daftar unit/meja, dan paket waktu. */
public class HalamanMaster extends JPanel implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final List<Konsol> konsol = new ArrayList<>();
    private final List<Unit> unit = new ArrayList<>();
    private final List<Paket> paket = new ArrayList<>();
    private final DefaultTableModel mKonsol = model("Konsol", "Tarif per jam", "Jumlah unit", "Jumlah paket");
    private final DefaultTableModel mUnit = model("Kode", "Nama", "Konsol", "Keterangan", "Status");
    private final DefaultTableModel mPaket = model("Paket", "Konsol", "Durasi", "Harga", "Hemat vs open", "Status");
    private final JTable tKonsol, tUnit, tPaket;

    public HalamanMaster(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        MainFrame.padding(this);
        boolean admin = frame.pengguna().admin();
        add(MainFrame.kepala("Unit & tarif", admin ? "Atur jenis konsol, tarif per jam, unit/meja, dan paket waktu." : "Daftar tarif dan paket. Perubahan hanya oleh admin."), BorderLayout.NORTH);

        tKonsol = tabel(mKonsol, new int[]{200, 140, 120, 120}, 1, 2, 3);
        tUnit = tabel(mUnit, new int[]{90, 140, 140, 260, 100});
        tUnit.getColumnModel().getColumn(4).setCellRenderer(new Ui.Pil(Map.of("Aktif", Tema.LIME, "Servis", Tema.KUNING, "Nonaktif", Tema.GARIS)));
        tPaket = tabel(mPaket, new int[]{220, 140, 100, 110, 130, 100}, 3);
        tPaket.getColumnModel().getColumn(5).setCellRenderer(new Ui.Pil(Map.of("Aktif", Tema.LIME, "Nonaktif", Tema.GARIS)));

        JTabbedPane tab = new JTabbedPane();
        tab.addTab("Konsol & tarif", bagian(tKonsol, admin, () -> formKonsol(null), () -> { int r = tKonsol.getSelectedRow(); if (r >= 0) formKonsol(konsol.get(r)); }));
        tab.addTab("Unit / meja", bagian(tUnit, admin, () -> formUnit(null), () -> { int r = tUnit.getSelectedRow(); if (r >= 0) formUnit(unit.get(r)); }));
        tab.addTab("Paket waktu", bagian(tPaket, admin, () -> formPaket(null), () -> { int r = tPaket.getSelectedRow(); if (r >= 0) formPaket(paket.get(r)); }));
        add(tab, BorderLayout.CENTER);
    }

    private static DefaultTableModel model(String... kol) {
        return new DefaultTableModel(kol, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    }

    private static JTable tabel(DefaultTableModel m, int[] lebar, int... angka) {
        JTable t = new JTable(m);
        Ui.rapikanTabel(t);
        Ui.lebarKolom(t, lebar);
        Ui.kolomAngka(t, angka);
        return t;
    }

    private JPanel bagian(JTable t, boolean admin, Runnable tambah, Runnable ubah) {
        JPanel p = Ui.transparan(new BorderLayout(0, 10));
        p.setBorder(new EmptyBorder(14, 0, 0, 0));
        JPanel aksi = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 8, 0));
        Ui.Tombol bt = Ui.tombol("Tambah", "plus", Ui.Jenis.UTAMA);
        bt.setEnabled(admin);
        bt.addActionListener(e -> tambah.run());
        Ui.Tombol bu = Ui.tombol("Ubah terpilih", "pencil", Ui.Jenis.GARIS);
        bu.setEnabled(admin);
        bu.addActionListener(e -> { if (t.getSelectedRow() < 0) frame.kabar("Pilih baris dulu"); else ubah.run(); });
        aksi.add(bt);
        aksi.add(bu);
        p.add(aksi, BorderLayout.NORTH);
        Ui.Kartu w = new Ui.Kartu(new BorderLayout());
        w.setBorder(new EmptyBorder(6, 6, 10, 6));
        w.add(Ui.gulir(t));
        p.add(w, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void muat() {
        konsol.clear(); konsol.addAll(k.master.konsol());
        unit.clear(); unit.addAll(k.master.unit(true));
        paket.clear(); paket.addAll(k.master.paket(null, true));
        mKonsol.setRowCount(0);
        for (Konsol c : konsol) {
            long nu = unit.stream().filter(u -> u.konsolId() == c.id()).count();
            long np = paket.stream().filter(p -> p.konsolId() == c.id()).count();
            mKonsol.addRow(new Object[]{c.nama(), c.tarifJam(), nu, np});
        }
        mUnit.setRowCount(0);
        for (Unit u : unit) mUnit.addRow(new Object[]{u.kode(), u.nama(), u.konsol(), u.keterangan(), !u.aktif() ? "Nonaktif" : u.servis() ? "Servis" : "Aktif"});
        mPaket.setRowCount(0);
        for (Paket p : paket) {
            long tarif = konsol.stream().filter(c -> c.id() == p.konsolId()).mapToLong(Konsol::tarifJam).findFirst().orElse(0);
            long normal = tarif * p.menit() / 60;
            String hemat = normal > p.harga() ? "hemat " + Rupiah.format(normal - p.harga()) : "-";
            mPaket.addRow(new Object[]{p.nama(), p.konsol(), DialogSesi.menit(p.menit()), p.harga(), hemat, p.aktif() ? "Aktif" : "Nonaktif"});
        }
    }

    private static long angka(String s, String nama) {
        try { return Rupiah.parse(s); } catch (IllegalArgumentException e) { throw new Gagal(nama + " harus angka."); }
    }

    private void formKonsol(Konsol c) {
        JTextField nama = Ui.field("Misal PS5 atau Nintendo Switch");
        JTextField tarif = Ui.field("Tarif per jam");
        if (c != null) { nama.setText(c.nama()); tarif.setText(Rupiah.ribuan(c.tarifJam())); }
        JPanel f = Ui.transparan(new GridLayout(2, 1, 0, 12));
        f.add(Ui.baris("Nama konsol", nama));
        f.add(Ui.baris("Tarif open billing per jam (Rp)", tarif));
        f.setPreferredSize(new Dimension(400, f.getPreferredSize().height));
        Kotak.form(this, c == null ? "Konsol baru" : "Ubah konsol", f, "Simpan", () -> {
            k.master.simpanKonsol(frame.aktor(), new Konsol(c == null ? 0 : c.id(), nama.getText(), angka(tarif.getText(), "Tarif")));
            muat();
            frame.kabar("Konsol disimpan. Sesi yang sedang main tetap memakai tarif lama.");
            return true;
        });
    }

    private void formUnit(Unit u) {
        JTextField kode = Ui.field("PS-11");
        JTextField nama = Ui.field("Meja 11 / Room 2");
        JComboBox<Konsol> ks = new JComboBox<>(konsol.toArray(new Konsol[0]));
        JTextField ket = Ui.field("TV 43\", 2 stik");
        JCheckBox servis = new JCheckBox("Sedang servis");
        JCheckBox aktif = new JCheckBox("Aktif", true);
        servis.setOpaque(false);
        aktif.setOpaque(false);
        if (u != null) {
            kode.setText(u.kode()); nama.setText(u.nama()); ket.setText(u.keterangan());
            servis.setSelected(u.servis()); aktif.setSelected(u.aktif());
            for (int i = 0; i < ks.getItemCount(); i++) if (ks.getItemAt(i).id() == u.konsolId()) ks.setSelectedIndex(i);
        }
        JPanel cek = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cek.add(aktif);
        cek.add(Box.createHorizontalStrut(16));
        cek.add(servis);
        JPanel f = Ui.transparan(new GridLayout(5, 1, 0, 10));
        f.add(Ui.baris("Kode unit", kode));
        f.add(Ui.baris("Nama", nama));
        f.add(Ui.baris("Jenis konsol", ks));
        f.add(Ui.baris("Keterangan (TV, stik, fasilitas)", ket));
        f.add(cek);
        f.setPreferredSize(new Dimension(420, f.getPreferredSize().height));
        Kotak.form(this, u == null ? "Unit baru" : "Ubah unit", f, "Simpan", () -> {
            Konsol c = (Konsol) ks.getSelectedItem();
            if (c == null) throw new Gagal("Tambahkan jenis konsol dulu.");
            k.master.simpanUnit(frame.aktor(), new Unit(u == null ? 0 : u.id(), kode.getText(), nama.getText(), c.id(), null, 0,
                    ket.getText(), servis.isSelected(), aktif.isSelected(), 0));
            muat();
            frame.kabar("Unit disimpan");
            return true;
        });
    }

    private void formPaket(Paket p) {
        JTextField nama = Ui.field("Misal PS5 Paket Malam 5 Jam");
        JComboBox<Konsol> ks = new JComboBox<>(konsol.toArray(new Konsol[0]));
        JTextField menit = Ui.field("Durasi dalam menit");
        JTextField harga = Ui.field("Harga");
        JCheckBox aktif = new JCheckBox("Aktif", true);
        aktif.setOpaque(false);
        if (p != null) {
            nama.setText(p.nama()); menit.setText(String.valueOf(p.menit())); harga.setText(Rupiah.ribuan(p.harga())); aktif.setSelected(p.aktif());
            for (int i = 0; i < ks.getItemCount(); i++) if (ks.getItemAt(i).id() == p.konsolId()) ks.setSelectedIndex(i);
        }
        JPanel f = Ui.transparan(new GridLayout(5, 1, 0, 10));
        f.add(Ui.baris("Nama paket", nama));
        f.add(Ui.baris("Untuk konsol", ks));
        f.add(Ui.baris("Durasi (menit)", menit));
        f.add(Ui.baris("Harga (Rp)", harga));
        f.add(aktif);
        f.setPreferredSize(new Dimension(420, f.getPreferredSize().height));
        Kotak.form(this, p == null ? "Paket baru" : "Ubah paket", f, "Simpan", () -> {
            Konsol c = (Konsol) ks.getSelectedItem();
            if (c == null) throw new Gagal("Tambahkan jenis konsol dulu.");
            k.master.simpanPaket(frame.aktor(), new Paket(p == null ? 0 : p.id(), nama.getText(), c.id(), null,
                    (int) angka(menit.getText(), "Durasi"), angka(harga.getText(), "Harga"), aktif.isSelected()));
            muat();
            frame.kabar("Paket disimpan");
            return true;
        });
    }
}
