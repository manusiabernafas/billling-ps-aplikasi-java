package id.papicode.billingps.ui.halaman;

import id.papicode.billingps.Konteks;
import id.papicode.billingps.model.*;
import id.papicode.billingps.service.Gagal;
import id.papicode.billingps.service.SesiService;
import id.papicode.billingps.service.Struk;
import id.papicode.billingps.ui.*;
import id.papicode.billingps.util.Rupiah;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Dialog-dialog untuk sesi main: mulai, tambah waktu, jajan, pindah unit, bayar. */
final class DialogSesi {
    private DialogSesi() { }

    // ---------------- mulai ----------------

    static void mulai(Component induk, Konteks k, MainFrame frame, Unit unit, Runnable selesai) {
        JTextField nama = Ui.field("Nama pelanggan (boleh kosong)");
        nama.setFont(Tema.reg(15f));
        List<Paket> paket = k.master.paket(unit.konsolId(), false);
        Long[] pilih = {null};
        boolean[] open = {false};
        JPanel daftar = Ui.transparan(new GridLayout(0, 2, 8, 8));
        ButtonGroup g = new ButtonGroup();
        JToggleButton openBtn = pilihan("Open billing", Rupiah.format(unit.tarifJam()) + " / jam, bayar sesuai lama main");
        openBtn.addActionListener(e -> { pilih[0] = null; open[0] = true; });
        g.add(openBtn);
        for (Paket p : paket) {
            JToggleButton b = pilihan(p.nama(), Rupiah.format(p.harga()) + "  ·  " + menit(p.menit()));
            b.addActionListener(e -> { pilih[0] = p.id(); open[0] = false; });
            g.add(b);
            daftar.add(b);
        }
        daftar.add(openBtn);
        if (!paket.isEmpty()) { ((JToggleButton) daftar.getComponent(0)).setSelected(true); pilih[0] = paket.get(0).id(); }
        else { openBtn.setSelected(true); open[0] = true; }

        JPanel f = Ui.transparan(new BorderLayout(0, 14));
        f.add(Ui.baris("Pelanggan", nama), BorderLayout.NORTH);
        f.add(Ui.baris("Pilih paket", daftar), BorderLayout.CENTER);
        f.add(Ui.teks("<html>" + unit.kode() + " · " + unit.konsol() + " · " + unit.keterangan() + "</html>", 12.5f, Tema.TEKS_2), BorderLayout.SOUTH);
        f.setPreferredSize(new Dimension(560, f.getPreferredSize().height));
        SwingUtilities.invokeLater(nama::requestFocusInWindow);
        Kotak.form(induk, "Mulai main di " + unit.kode(), f, "Mulai", () -> {
            if (pilih[0] == null && !open[0]) throw new Gagal("Pilih paket atau open billing.");
            Sesi s = k.sesi.mulai(frame.aktor(), unit.id(), nama.getText(), open[0] ? null : pilih[0]);
            frame.kabar(unit.kode() + " mulai: " + s.pelanggan() + (s.paketNama() == null ? " (open)" : " · " + s.paketNama()));
            selesai.run();
            return true;
        });
    }

    static String menit(int m) {
        return m % 60 == 0 ? (m / 60) + " jam" : m >= 60 ? (m / 60) + " jam " + (m % 60) + " mnt" : m + " menit";
    }

    private static JToggleButton pilihan(String judul, String sub) {
        JToggleButton b = new JToggleButton("<html><b>" + judul + "</b><br><span style='font-size:9px'>" + sub + "</span></html>") {
            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g2 = (Graphics2D) gr.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Shape s = Tema.chamfer(0.5, 0.5, getWidth() - 1.5, getHeight() - 1.5, 10);
                g2.setColor(isSelected() ? Tema.LIME : Tema.KARTU_2);
                g2.fill(s);
                g2.setColor(isSelected() ? Tema.LIME : Tema.GARIS);
                g2.draw(s);
                g2.dispose();
                setForeground(isSelected() ? Tema.GELAP : Tema.TEKS);
                super.paintComponent(gr);
            }
        };
        b.setFont(Tema.semi(13.5f));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------------- tambah waktu ----------------

    static void tambahWaktu(Component induk, Konteks k, MainFrame frame, Sesi s, Runnable selesai) {
        long konsolId = k.master.unitById(s.unitId()).konsolId();
        List<Paket> paket = k.master.paket(konsolId, false);
        if (paket.isEmpty()) { frame.kabar("Belum ada paket untuk konsol ini"); return; }
        JPanel daftar = Ui.transparan(new GridLayout(0, 2, 8, 8));
        ButtonGroup g = new ButtonGroup();
        Long[] pilih = {paket.get(0).id()};
        for (Paket p : paket) {
            JToggleButton b = pilihan("+ " + menit(p.menit()), p.nama() + " · " + Rupiah.format(p.harga()));
            b.addActionListener(e -> pilih[0] = p.id());
            g.add(b);
            daftar.add(b);
        }
        ((JToggleButton) daftar.getComponent(0)).setSelected(true);
        JPanel f = Ui.transparan(new BorderLayout(0, 12));
        f.add(Ui.teks(s.unitKode() + " · " + s.pelanggan() + " · sisa " + HalamanUnit.jamMenitDetik(s.sisaMs(k.sesi.sekarang())), 13.5f, Tema.TEKS_2), BorderLayout.NORTH);
        f.add(daftar, BorderLayout.CENTER);
        f.setPreferredSize(new Dimension(520, f.getPreferredSize().height));
        Kotak.form(induk, "Tambah waktu", f, "Tambah", () -> {
            k.sesi.tambahWaktu(frame.aktor(), s.id(), pilih[0]);
            frame.kabar("Waktu " + s.unitKode() + " ditambah");
            selesai.run();
            return true;
        });
    }

    // ---------------- jajan ----------------

    static void jajan(Component induk, Konteks k, MainFrame frame, Sesi s, Runnable selesai) {
        JPanel daftarTagihan = Ui.transparan(null);
        daftarTagihan.setLayout(new BoxLayout(daftarTagihan, BoxLayout.Y_AXIS));
        JLabel total = Ui.angka("Rp0", 28f, Tema.LIME);
        Runnable[] segar = new Runnable[1];
        segar[0] = () -> {
            daftarTagihan.removeAll();
            List<ItemJajan> l = k.sesi.jajan(s.id());
            if (l.isEmpty()) daftarTagihan.add(Ui.teks("Belum ada jajan.", 13f, Tema.TEKS_2));
            for (ItemJajan it : l) {
                JPanel r = Ui.transparan(new BorderLayout(8, 0));
                r.setBorder(new EmptyBorder(4, 0, 4, 0));
                r.add(Ui.teks(it.qty() + "x  " + it.nama(), 13.5f, Tema.TEKS), BorderLayout.CENTER);
                JPanel kanan = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 6, 0));
                kanan.add(Ui.teks(Rupiah.format(it.subtotal()), 13.5f, Tema.TEKS_2));
                Ui.Tombol hapus = Ui.ikonSaja("x", "Hapus satu baris terakhir menu ini", Ui.Jenis.HANTU);
                hapus.addActionListener(e -> { if (Kotak.aman(r, () -> k.sesi.hapusJajan(frame.aktor(), s.id(), it.jajanId()))) segar[0].run(); });
                kanan.add(hapus);
                r.add(kanan, BorderLayout.EAST);
                r.setAlignmentX(0f);
                r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                daftarTagihan.add(r);
            }
            total.setText(Rupiah.format(l.stream().mapToLong(ItemJajan::subtotal).sum()));
            daftarTagihan.revalidate();
            daftarTagihan.repaint();
        };
        JPanel menu = Ui.transparan(new GridLayout(0, 3, 8, 8));
        for (Jajan j : k.master.jajan(false)) {
            JButton b = tombolMenu(j);
            b.addActionListener(e -> {
                if (Kotak.aman(b, () -> k.sesi.tambahJajan(frame.aktor(), s.id(), j.id(), 1))) segar[0].run();
            });
            menu.add(b);
        }
        segar[0].run();
        JPanel kanan = Ui.transparan(new BorderLayout(0, 10));
        kanan.add(Ui.label("Tagihan jajan " + s.unitKode()), BorderLayout.NORTH);
        JScrollPane sp = Ui.gulir(daftarTagihan);
        sp.getViewport().setBackground(Tema.KARTU);
        kanan.add(sp, BorderLayout.CENTER);
        kanan.add(total, BorderLayout.SOUTH);
        kanan.setPreferredSize(new Dimension(280, 360));
        JPanel f = Ui.transparan(new BorderLayout(18, 0));
        f.add(menu, BorderLayout.CENTER);
        f.add(kanan, BorderLayout.EAST);
        Kotak.form(induk, "Jajan untuk " + s.unitKode() + " · " + s.pelanggan(), f, "Selesai", () -> true);
        selesai.run();
    }

    static JButton tombolMenu(Jajan j) {
        JButton b = new JButton("<html><b>" + j.nama() + "</b><br>" + Rupiah.format(j.harga()) + "  <span style='color:#A99CC9'>stok " + j.stok() + "</span></html>") {
            boolean hover;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g2 = (Graphics2D) gr.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Shape s = Tema.chamfer(0.5, 0.5, getWidth() - 1.5, getHeight() - 1.5, 10);
                g2.setColor(hover ? Tema.PILIH : Tema.KARTU_2);
                g2.fill(s);
                g2.setColor(hover ? Tema.LIME : Tema.GARIS);
                g2.draw(s);
                g2.dispose();
                super.paintComponent(gr);
            }
        };
        b.setFont(Tema.reg(13f));
        b.setForeground(j.stok() <= 0 ? Tema.TEKS_2 : Tema.TEKS);
        b.setEnabled(j.stok() > 0);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 12, 10, 12));
        b.setPreferredSize(new Dimension(170, 62));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------------- pindah ----------------

    static void pindah(Component induk, Konteks k, MainFrame frame, Sesi s, Runnable selesai) {
        long konsolId = k.master.unitById(s.unitId()).konsolId();
        Set<Long> dipakai = new LinkedHashSet<>();
        for (Sesi x : k.sesi.berjalan()) if (x.unitId() != null) dipakai.add(x.unitId());
        JComboBox<Unit> tujuan = new JComboBox<>();
        for (Unit u : k.master.unit(false)) if (u.konsolId() == konsolId && !u.servis() && !dipakai.contains(u.id())) tujuan.addItem(u);
        if (tujuan.getItemCount() == 0) { Kotak.pesan(induk, "Tidak ada unit kosong", "Belum ada unit " + s.konsol() + " yang kosong untuk pindah.", true); return; }
        JPanel f = Ui.transparan(new BorderLayout(0, 10));
        f.add(Ui.teks("Waktu main dan jajan ikut pindah. " + s.unitKode() + " akan kosong.", 13.5f, Tema.TEKS_2), BorderLayout.NORTH);
        f.add(Ui.baris("Pindah ke", tujuan), BorderLayout.CENTER);
        Kotak.form(induk, "Pindah unit " + s.unitKode(), f, "Pindahkan", () -> {
            Unit u = (Unit) tujuan.getSelectedItem();
            k.sesi.pindah(frame.aktor(), s.id(), u.id());
            frame.kabar(s.pelanggan() + " pindah ke " + u.kode());
            selesai.run();
            return true;
        });
    }

    // ---------------- bayar ----------------

    private static final DateTimeFormatter JAM = DateTimeFormatter.ofPattern("HH:mm");

    static void bayar(Component induk, Konteks k, MainFrame frame, Sesi s, Runnable selesai) {
        SesiService.Tagihan t = k.sesi.tagihan(s.id());
        Window w = SwingUtilities.getWindowAncestor(induk);
        JDialog d = new JDialog(w, "Stop & bayar " + s.unitKode(), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Tema.KARTU);
        root.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, Tema.MAGENTA), new EmptyBorder(20, 24, 20, 24)));

        JPanel atas = Ui.transparan(new BorderLayout(0, 4));
        atas.add(Ui.label("Stop & bayar · " + s.nomor()), BorderLayout.NORTH);
        atas.add(Ui.judul(s.unitKode() + " · " + s.pelanggan(), 21f), BorderLayout.CENTER);
        String mulai = Instant.ofEpochMilli(s.mulai()).atZone(k.jam.getZone()).format(JAM);
        atas.add(Ui.teks("Mulai " + mulai + " · lama main " + Struk.durasi(t.durasiMs()) + (s.jedaMs() > 0 || s.dijeda() ? " (jeda tidak dihitung)" : ""), 13.5f, Tema.TEKS_2), BorderLayout.SOUTH);
        root.add(atas, BorderLayout.NORTH);

        JPanel rincian = Ui.transparan(null);
        rincian.setLayout(new BoxLayout(rincian, BoxLayout.Y_AXIS));
        String ketSewa = s.mode().equals(Sesi.PAKET) ? t.rincianSewa().replaceFirst("Paket \\d+ menit", java.util.regex.Matcher.quoteReplacement(s.paketNama())) : t.rincianSewa();
        rincian.add(baris(ketSewa, Rupiah.format(t.biayaSewa()), Tema.TEKS));
        for (ItemJajan j : t.jajan()) rincian.add(baris(j.qty() + "x " + j.nama(), Rupiah.format(j.subtotal()), Tema.TEKS_2));
        JTextField diskon = Ui.field("0");
        diskon.setColumns(8);
        JPanel dsk = Ui.transparan(new BorderLayout(10, 0));
        dsk.add(Ui.teks("Diskon", 14f, Tema.TEKS_2), BorderLayout.WEST);
        JPanel dskKanan = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        dskKanan.add(diskon);
        dsk.add(dskKanan, BorderLayout.EAST);
        dsk.setAlignmentX(0f);
        dsk.setBorder(new EmptyBorder(8, 0, 0, 0));
        rincian.add(dsk);
        JLabel total = Ui.angka(Rupiah.format(t.total()), 54f, Tema.LIME);
        JPanel tot = Ui.transparan(new BorderLayout());
        tot.add(Ui.label("Total"), BorderLayout.WEST);
        tot.add(total, BorderLayout.EAST);
        tot.setAlignmentX(0f);
        tot.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.GARIS), new EmptyBorder(8, 0, 0, 0)));
        rincian.add(Box.createVerticalStrut(10));
        rincian.add(tot);

        // metode & uang
        JToggleButton tunai = pilihan("Tunai", "hitung kembalian"), qris = pilihan("QRIS", "pastikan dana masuk");
        ButtonGroup g = new ButtonGroup();
        g.add(tunai);
        g.add(qris);
        tunai.setSelected(true);
        JPanel metode = Ui.transparan(new GridLayout(1, 2, 8, 0));
        metode.add(tunai);
        metode.add(qris);
        metode.setAlignmentX(0f);
        JTextField uang = Ui.field("Uang diterima");
        uang.setFont(Tema.piksel(30f));
        JLabel kembali = Ui.teks(" ", 14f, Tema.TEKS_2);
        kembali.setFont(Tema.bold(15f));
        JPanel cepat = Ui.transparan(new GridLayout(1, 0, 6, 0));
        long[] totalSaatIni = {t.total()};
        Runnable hitung = () -> {
            long dk;
            try { dk = Rupiah.parse(diskon.getText()); } catch (RuntimeException e) { dk = -1; }
            long tt = dk < 0 ? t.total() : Math.max(0, t.total() - dk);
            totalSaatIni[0] = tt;
            total.setText(Rupiah.format(tt));
            uang.setEnabled(tunai.isSelected());
            if (!tunai.isSelected()) { kembali.setText("Bayar pas via QRIS"); kembali.setForeground(Tema.CYAN); return; }
            long u;
            try { u = Rupiah.parse(uang.getText()); } catch (RuntimeException e) { u = 0; }
            if (u < tt) { kembali.setText("Kurang " + Rupiah.format(tt - u)); kembali.setForeground(Tema.MAGENTA); }
            else { kembali.setText("Kembalian " + Rupiah.format(u - tt)); kembali.setForeground(Tema.LIME); }
        };
        Set<Long> saran = new LinkedHashSet<>();
        saran.add(t.total());
        for (long p : new long[]{10_000, 20_000, 50_000, 100_000}) { long b = ((t.total() + p - 1) / p) * p; if (b > t.total()) saran.add(b); if (saran.size() >= 4) break; }
        for (long n : saran) {
            Ui.Tombol b = Ui.tombol(n == t.total() ? "Uang pas" : Rupiah.ribuan(n), null, Ui.Jenis.GARIS);
            b.addActionListener(e -> uang.setText(Rupiah.ribuan(n == t.total() ? totalSaatIni[0] : n)));
            cepat.add(b);
        }
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { hitung.run(); }
            public void removeUpdate(DocumentEvent e) { hitung.run(); }
            public void changedUpdate(DocumentEvent e) { hitung.run(); }
        };
        uang.getDocument().addDocumentListener(dl);
        diskon.getDocument().addDocumentListener(dl);
        tunai.addActionListener(e -> hitung.run());
        qris.addActionListener(e -> hitung.run());
        JPanel bayarPanel = Ui.transparan(new GridLayout(0, 1, 0, 8));
        bayarPanel.add(metode);
        bayarPanel.add(uang);
        bayarPanel.add(cepat);
        bayarPanel.add(kembali);

        JPanel tengah = Ui.transparan(new GridLayout(1, 2, 24, 0));
        tengah.add(rincian);
        tengah.add(bayarPanel);
        root.add(tengah, BorderLayout.CENTER);

        Ui.Tombol ok = Ui.tombol("Selesai & cetak struk", "check", Ui.Jenis.UTAMA);
        Ui.Tombol batal = Ui.tombol("Kembali", null, Ui.Jenis.GARIS);
        ok.addActionListener(e -> Kotak.aman(d, () -> {
            long dk;
            try { dk = Rupiah.parse(diskon.getText()); } catch (RuntimeException ex) { throw new Gagal("Diskon harus angka."); }
            long u = 0;
            if (tunai.isSelected()) {
                try { u = Rupiah.parse(uang.getText()); } catch (RuntimeException ex) { throw new Gagal("Uang diterima harus angka."); }
            }
            Sesi hasil = k.sesi.bayar(frame.aktor(), s.id(), dk, tunai.isSelected() ? "TUNAI" : "QRIS", u);
            d.dispose();
            frame.kabar(s.unitKode() + " selesai · " + Rupiah.format(hasil.total()));
            selesai.run();
            DialogStruk.tampil(induk, k, hasil);
        }));
        batal.addActionListener(e -> d.dispose());
        JPanel bawah = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bawah.add(batal);
        bawah.add(ok);
        root.add(bawah, BorderLayout.SOUTH);
        hitung.run();
        d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.getRootPane().setDefaultButton(ok);
        d.setContentPane(root);
        d.pack();
        d.setSize(Math.max(780, d.getWidth()), d.getHeight());
        d.setResizable(false);
        d.setLocationRelativeTo(w);
        SwingUtilities.invokeLater(uang::requestFocusInWindow);
        d.setVisible(true);
    }

    private static JComponent baris(String kiri, String kanan, Color c) {
        JPanel p = Ui.transparan(new BorderLayout(10, 0));
        p.add(Ui.teks(kiri, 14f, c), BorderLayout.CENTER);
        JLabel k = Ui.teks(kanan, 14f, c);
        k.setFont(Tema.semi(14f));
        p.add(k, BorderLayout.EAST);
        p.setAlignmentX(0f);
        p.setBorder(new EmptyBorder(4, 0, 4, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return p;
    }
}
