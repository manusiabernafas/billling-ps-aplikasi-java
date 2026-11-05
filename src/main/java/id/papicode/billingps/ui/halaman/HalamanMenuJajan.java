package id.papicode.billingps.ui.halaman;

import id.papicode.billingps.Konteks;
import id.papicode.billingps.model.Jajan;
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

/** Menu jajan (makanan, minuman, snack), harga, dan stok. */
public class HalamanMenuJajan extends JPanel implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final List<Jajan> data = new ArrayList<>();
    private final DefaultTableModel m = new DefaultTableModel(new String[]{"Menu", "Kategori", "Harga", "Stok", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable t = new JTable(m);

    public HalamanMenuJajan(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        MainFrame.padding(this);
        boolean admin = frame.pengguna().admin();
        Ui.Tombol tambah = Ui.tombol("Menu baru", "plus", Ui.Jenis.UTAMA);
        tambah.setEnabled(admin);
        tambah.addActionListener(e -> form(null));
        Ui.Tombol ubah = Ui.tombol("Ubah", "pencil", Ui.Jenis.GARIS);
        ubah.setEnabled(admin);
        ubah.addActionListener(e -> { int r = t.getSelectedRow(); if (r < 0) frame.kabar("Pilih menu dulu"); else form(data.get(r)); });
        Ui.Tombol stok = Ui.tombol("Stok masuk", "download", Ui.Jenis.INFO);
        stok.addActionListener(e -> stokMasuk());
        add(MainFrame.kepala("Menu & stok jajan", "Stok berkurang otomatis setiap jajan ditambahkan ke tagihan.", stok, ubah, tambah), BorderLayout.NORTH);
        Ui.rapikanTabel(t);
        Ui.lebarKolom(t, 300, 150, 120, 100, 120);
        Ui.kolomAngka(t, 2, 3);
        t.getColumnModel().getColumn(4).setCellRenderer(new Ui.Pil(Map.of("Aman", Tema.LIME, "Menipis", Tema.KUNING, "Habis", Tema.MAGENTA, "Nonaktif", Tema.GARIS)));
        Ui.Kartu w = new Ui.Kartu(new BorderLayout());
        w.setBorder(new EmptyBorder(6, 6, 10, 6));
        w.add(Ui.gulir(t));
        add(w, BorderLayout.CENTER);
    }

    @Override
    public void muat() {
        data.clear();
        data.addAll(k.master.jajan(true));
        m.setRowCount(0);
        for (Jajan j : data) {
            String st = !j.aktif() ? "Nonaktif" : j.stok() == 0 ? "Habis" : j.stok() <= 10 ? "Menipis" : "Aman";
            m.addRow(new Object[]{j.nama(), j.kategori(), j.harga(), j.stok(), st});
        }
    }

    private void form(Jajan j) {
        JTextField nama = Ui.field("Nama menu");
        JComboBox<String> kat = new JComboBox<>(new String[]{"Makanan", "Minuman", "Snack", "Lainnya"});
        kat.setEditable(true);
        JTextField harga = Ui.field("Harga jual");
        JTextField stok = Ui.field("0");
        JCheckBox aktif = new JCheckBox("Dijual", true);
        aktif.setOpaque(false);
        if (j != null) { nama.setText(j.nama()); kat.setSelectedItem(j.kategori()); harga.setText(Rupiah.ribuan(j.harga())); aktif.setSelected(j.aktif()); }
        JPanel f = Ui.transparan(new GridLayout(0, 1, 0, 10));
        f.add(Ui.baris("Nama menu", nama));
        f.add(Ui.baris("Kategori", kat));
        f.add(Ui.baris("Harga (Rp)", harga));
        if (j == null) f.add(Ui.baris("Stok awal", stok));
        f.add(aktif);
        f.setPreferredSize(new Dimension(400, f.getPreferredSize().height));
        Kotak.form(this, j == null ? "Menu baru" : "Ubah menu", f, "Simpan", () -> {
            long h;
            int s;
            try { h = Rupiah.parse(harga.getText()); s = (int) Rupiah.parse(stok.getText()); }
            catch (IllegalArgumentException e) { throw new Gagal("Harga dan stok harus angka."); }
            k.master.simpanJajan(frame.aktor(), new Jajan(j == null ? 0 : j.id(), nama.getText(), String.valueOf(kat.getSelectedItem()), h, 0, aktif.isSelected()), j == null ? s : 0);
            muat();
            frame.kabar("Menu disimpan");
            return true;
        });
    }

    private void stokMasuk() {
        JComboBox<Jajan> pilih = new JComboBox<>(k.master.jajan(false).toArray(new Jajan[0]));
        int r = t.getSelectedRow();
        if (r >= 0) for (int i = 0; i < pilih.getItemCount(); i++) if (pilih.getItemAt(i).id() == data.get(r).id()) pilih.setSelectedIndex(i);
        JTextField qty = Ui.field("Jumlah");
        JPanel f = Ui.transparan(new GridLayout(2, 1, 0, 10));
        f.add(Ui.baris("Menu", pilih));
        f.add(Ui.baris("Jumlah masuk", qty));
        f.setPreferredSize(new Dimension(380, f.getPreferredSize().height));
        Kotak.form(this, "Stok masuk", f, "Tambah stok", () -> {
            Jajan j = (Jajan) pilih.getSelectedItem();
            if (j == null) throw new Gagal("Pilih menu.");
            int q;
            try { q = (int) Rupiah.parse(qty.getText()); } catch (IllegalArgumentException e) { throw new Gagal("Jumlah harus angka."); }
            int akhir = k.master.tambahStokJajan(frame.aktor(), j.id(), q);
            muat();
            frame.kabar("Stok " + j.nama() + " sekarang " + akhir);
            return true;
        });
    }
}
