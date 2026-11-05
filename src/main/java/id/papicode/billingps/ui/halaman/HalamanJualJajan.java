package id.papicode.billingps.ui.halaman;

import id.papicode.billingps.Konteks;
import id.papicode.billingps.model.Jajan;
import id.papicode.billingps.model.Sesi;
import id.papicode.billingps.service.Gagal;
import id.papicode.billingps.ui.*;
import id.papicode.billingps.util.Rupiah;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Penjualan jajan langsung (tanpa main), misal pelanggan yang hanya menunggu teman atau beli minum. */
public class HalamanJualJajan extends JPanel implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final GridAlir menu = new GridAlir(190, 70, 10);
    private final Map<Long, Integer> keranjang = new LinkedHashMap<>();
    private final Map<Long, Jajan> data = new LinkedHashMap<>();
    private final JPanel daftar = Ui.transparan(null);
    private final JTextField pelanggan = Ui.field("Nama pelanggan (opsional)");
    private final JTextField diskon = Ui.field("0");
    private final JTextField uang = Ui.field("Uang diterima");
    private final JRadioButton tunai = new JRadioButton("Tunai", true), qris = new JRadioButton("QRIS");
    private final JLabel total = Ui.angka("Rp0", 48f, Tema.LIME);
    private final JLabel kembali = Ui.teks(" ", 14f, Tema.TEKS_2);

    public HalamanJualJajan(Konteks k, MainFrame frame) {
        super(new BorderLayout(20, 0));
        this.k = k;
        this.frame = frame;
        MainFrame.padding(this);

        JPanel kiri = Ui.transparan(new BorderLayout());
        kiri.add(MainFrame.kepala("Jual jajan", "Untuk pelanggan yang tidak main. Jajan pemain ditambahkan dari kartu unit."), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(menu);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        kiri.add(sp, BorderLayout.CENTER);
        add(kiri, BorderLayout.CENTER);

        Ui.Kartu kanan = new Ui.Kartu(new BorderLayout(0, 12));
        kanan.setBorder(new EmptyBorder(18, 18, 18, 18));
        kanan.setPreferredSize(new Dimension(380, 0));
        JPanel atas = Ui.transparan(new BorderLayout(0, 10));
        atas.add(Ui.judul("Pesanan", 18f), BorderLayout.NORTH);
        atas.add(pelanggan, BorderLayout.CENTER);
        kanan.add(atas, BorderLayout.NORTH);
        daftar.setLayout(new BoxLayout(daftar, BoxLayout.Y_AXIS));
        JPanel bungkus = Ui.transparan(new BorderLayout());
        bungkus.add(daftar, BorderLayout.NORTH);
        JScrollPane sd = Ui.gulir(bungkus);
        kanan.add(sd, BorderLayout.CENTER);

        JPanel bawah = Ui.transparan(null);
        bawah.setLayout(new BoxLayout(bawah, BoxLayout.Y_AXIS));
        JPanel dsk = Ui.transparan(new BorderLayout(10, 0));
        dsk.add(Ui.label("Diskon"), BorderLayout.WEST);
        diskon.setColumns(8);
        dsk.add(diskon, BorderLayout.EAST);
        uang.setPreferredSize(new Dimension(100, 50));
        bawah.add(dsk);
        JPanel tt = Ui.transparan(new BorderLayout());
        tt.add(Ui.label("Total"), BorderLayout.WEST);
        tt.add(total, BorderLayout.EAST);
        bawah.add(tt);
        ButtonGroup g = new ButtonGroup();
        g.add(tunai);
        g.add(qris);
        JPanel met = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 12, 0));
        for (JRadioButton r : new JRadioButton[]{tunai, qris}) { r.setOpaque(false); r.setFont(Tema.semi(14f)); met.add(r); r.addActionListener(e -> hitung()); }
        bawah.add(met);
        uang.setFont(Tema.piksel(26f));
        bawah.add(uang);
        bawah.add(kembali);
        Ui.Tombol bayar = Ui.tombol("Bayar", "check", Ui.Jenis.UTAMA);
        bayar.setFont(Tema.bold(16f));
        bayar.addActionListener(e -> bayar());
        bawah.add(bayar);
        for (Component c : bawah.getComponents()) {
            ((JComponent) c).setAlignmentX(0f);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        }
        bayar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        java.util.List<Component> isi = new java.util.ArrayList<>(java.util.List.of(bawah.getComponents()));
        bawah.removeAll();
        for (Component c : isi) { bawah.add(c); bawah.add(Box.createVerticalStrut(10)); }
        kanan.add(bawah, BorderLayout.SOUTH);
        add(kanan, BorderLayout.EAST);

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { hitung(); }
            public void removeUpdate(DocumentEvent e) { hitung(); }
            public void changedUpdate(DocumentEvent e) { hitung(); }
        };
        diskon.getDocument().addDocumentListener(dl);
        uang.getDocument().addDocumentListener(dl);
    }

    @Override
    public void muat() {
        data.clear();
        menu.removeAll();
        for (Jajan j : k.master.jajan(false)) {
            data.put(j.id(), j);
            JButton b = DialogSesi.tombolMenu(j);
            b.addActionListener(e -> {
                int ada = keranjang.getOrDefault(j.id(), 0);
                if (ada + 1 > j.stok()) { frame.kabar("Stok " + j.nama() + " tinggal " + j.stok()); return; }
                keranjang.put(j.id(), ada + 1);
                gambar();
            });
            menu.add(b);
        }
        keranjang.keySet().removeIf(id -> !data.containsKey(id));
        menu.revalidate();
        menu.repaint();
        gambar();
    }

    private void gambar() {
        daftar.removeAll();
        if (keranjang.isEmpty()) daftar.add(Ui.teks("Klik menu di kiri untuk menambah.", 13f, Tema.TEKS_2));
        for (Map.Entry<Long, Integer> e : keranjang.entrySet()) {
            Jajan j = data.get(e.getKey());
            JPanel r = Ui.transparan(new BorderLayout(8, 0));
            r.setBorder(new EmptyBorder(5, 0, 5, 0));
            r.add(Ui.teks(e.getValue() + "x  " + j.nama(), 13.5f, Tema.TEKS), BorderLayout.CENTER);
            JPanel ka = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            ka.add(Ui.teks(Rupiah.format(j.harga() * e.getValue()), 13.5f, Tema.TEKS_2));
            Ui.Tombol min = Ui.ikonSaja("minus", "Kurangi", Ui.Jenis.HANTU);
            min.addActionListener(ev -> {
                int q = keranjang.get(j.id()) - 1;
                if (q <= 0) keranjang.remove(j.id()); else keranjang.put(j.id(), q);
                gambar();
            });
            ka.add(min);
            r.add(ka, BorderLayout.EAST);
            r.setAlignmentX(0f);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            daftar.add(r);
        }
        daftar.revalidate();
        daftar.repaint();
        hitung();
    }

    private long subtotal() {
        long s = 0;
        for (Map.Entry<Long, Integer> e : keranjang.entrySet()) s += data.get(e.getKey()).harga() * e.getValue();
        return s;
    }

    private void hitung() {
        long d;
        try { d = Rupiah.parse(diskon.getText()); } catch (RuntimeException e) { d = 0; }
        long t = Math.max(0, subtotal() - d);
        total.setText(Rupiah.format(t));
        uang.setEnabled(tunai.isSelected());
        if (!tunai.isSelected()) { kembali.setText("Bayar pas via QRIS"); kembali.setForeground(Tema.CYAN); return; }
        long u;
        try { u = Rupiah.parse(uang.getText()); } catch (RuntimeException e) { u = 0; }
        if (uang.getText().isBlank()) { kembali.setText(" "); return; }
        if (u < t) { kembali.setText("Kurang " + Rupiah.format(t - u)); kembali.setForeground(Tema.MAGENTA); }
        else { kembali.setText("Kembalian " + Rupiah.format(u - t)); kembali.setForeground(Tema.LIME); }
    }

    private void bayar() {
        Kotak.aman(this, () -> {
            long d, u = 0;
            try { d = Rupiah.parse(diskon.getText()); } catch (RuntimeException e) { throw new Gagal("Diskon harus angka."); }
            if (tunai.isSelected()) {
                try { u = Rupiah.parse(uang.getText()); } catch (RuntimeException e) { throw new Gagal("Uang diterima harus angka."); }
            }
            Sesi s = k.sesi.jualJajan(frame.aktor(), pelanggan.getText(), keranjang, d, tunai.isSelected() ? "TUNAI" : "QRIS", u);
            keranjang.clear();
            pelanggan.setText("");
            diskon.setText("");
            uang.setText("");
            muat();
            DialogStruk.tampil(this, k, s);
        });
    }
}
