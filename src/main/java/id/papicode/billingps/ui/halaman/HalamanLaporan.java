package id.papicode.billingps.ui.halaman;

import id.papicode.billingps.Konteks;
import id.papicode.billingps.service.Gagal;
import id.papicode.billingps.service.LaporanService;
import id.papicode.billingps.ui.*;
import id.papicode.billingps.util.Csv;
import id.papicode.billingps.util.Rupiah;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Laporan pemilik: pendapatan sewa & jajan, okupansi unit, jam ramai, paket & jajan terlaris. */
public class HalamanLaporan extends JPanel implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final PilihPeriode periode;
    private final JLabel vTotal = Ui.angka("Rp0", 36f, Tema.LIME), kTotal = Ui.teks(" ", 12f, Tema.TEKS_2);
    private final JLabel vSewa = Ui.angka("Rp0", 36f, Tema.TEKS), kSewa = Ui.teks(" ", 12f, Tema.TEKS_2);
    private final JLabel vJajan = Ui.angka("Rp0", 36f, Tema.MAGENTA), kJajan = Ui.teks(" ", 12f, Tema.TEKS_2);
    private final JLabel vJam = Ui.angka("0", 36f, Tema.CYAN), kJam = Ui.teks(" ", 12f, Tema.TEKS_2);
    private final GrafikBatang harian = new GrafikBatang(new String[]{"Sewa", "Jajan"}, Tema.LIME, Tema.MAGENTA);
    private final GrafikBatang jam = new GrafikBatang(new String[]{"Sesi dimulai"}, Tema.CYAN);
    private final DefaultTableModel mUnit = model("Unit", "Jam", "Sewa", "Okupansi");
    private final DefaultTableModel mPaket = model("Paket", "Sesi", "Sewa");
    private final DefaultTableModel mJajan = model("Menu", "Qty", "Nilai");

    public HalamanLaporan(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        MainFrame.padding(this);
        periode = new PilihPeriode(k.jam, 3);
        periode.onChange(this::muat);
        Ui.Tombol csv = Ui.tombol("Ekspor CSV", "download", Ui.Jenis.GARIS);
        csv.addActionListener(e -> ekspor());
        JPanel atas = Ui.transparan(new BorderLayout());
        atas.add(MainFrame.kepala("Laporan", "Pendapatan, okupansi unit, dan menu terlaris.", csv), BorderLayout.NORTH);
        JPanel p = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(periode);
        p.setBorder(new EmptyBorder(0, 0, 14, 0));
        atas.add(p, BorderLayout.SOUTH);
        add(atas, BorderLayout.NORTH);

        JPanel isi = Ui.transparan(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        JPanel stat = Ui.transparan(new GridLayout(1, 4, 14, 0));
        stat.add(stat("Total pendapatan", vTotal, kTotal));
        stat.add(stat("Sewa unit", vSewa, kSewa));
        stat.add(stat("Jajan", vJajan, kJajan));
        stat.add(stat("Jam main", vJam, kJam));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 3; c.weightx = 1; c.insets = new Insets(0, 0, 14, 0);
        isi.add(stat, c);
        c.gridy = 1; c.gridwidth = 2; c.weightx = 0.66; c.weighty = 0.5; c.insets = new Insets(0, 0, 14, 14);
        isi.add(panel("Pendapatan harian", harian, harian.legenda()), c);
        c.gridx = 2; c.gridwidth = 1; c.weightx = 0.34; c.insets = new Insets(0, 0, 14, 0);
        isi.add(panel("Jam ramai (sesi dimulai)", jam, null), c);
        c.gridx = 0; c.gridy = 2; c.weightx = 0.36; c.weighty = 0.5; c.insets = new Insets(0, 0, 0, 14);
        JTable tu = tabel(mUnit, new int[]{60, 50, 95, 150}, 1, 2);
        tu.getColumnModel().getColumn(3).setCellRenderer(new RendererOkupansi());
        isi.add(panel("Okupansi per unit", Ui.gulir(tu), null), c);
        c.gridx = 1; c.weightx = 0.32;
        isi.add(panel("Paket terlaris", Ui.gulir(tabel(mPaket, new int[]{150, 50, 110}, 1, 2)), null), c);
        c.gridx = 2; c.weightx = 0.32; c.insets = new Insets(0, 0, 0, 0);
        isi.add(panel("Jajan terlaris", Ui.gulir(tabel(mJajan, new int[]{170, 60, 100}, 1, 2)), null), c);
        add(isi, BorderLayout.CENTER);
    }

    private static DefaultTableModel model(String... kol) {
        return new DefaultTableModel(kol, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    }

    private static JTable tabel(DefaultTableModel m, int[] lebar, int... angka) {
        JTable t = new JTable(m);
        Ui.rapikanTabel(t);
        t.setRowHeight(32);
        Ui.lebarKolom(t, lebar);
        Ui.kolomAngka(t, angka);
        return t;
    }

    private static JPanel stat(String label, JLabel v, JLabel ket) {
        Ui.Kartu c = new Ui.Kartu(new BorderLayout());
        c.setBorder(new EmptyBorder(12, 18, 12, 18));
        c.add(Ui.label(label), BorderLayout.NORTH);
        c.add(v, BorderLayout.CENTER);
        c.add(ket, BorderLayout.SOUTH);
        return c;
    }

    private static JPanel panel(String judul, JComponent isi, JComponent kanan) {
        Ui.Kartu c = new Ui.Kartu(new BorderLayout(0, 10));
        c.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel a = Ui.transparan(new BorderLayout());
        JLabel j = new JLabel(judul.toUpperCase());
        j.setFont(Tema.bold(13f));
        j.setForeground(Tema.TEKS);
        a.add(j, BorderLayout.WEST);
        if (kanan != null) a.add(kanan, BorderLayout.EAST);
        c.add(a, BorderLayout.NORTH);
        c.add(isi, BorderLayout.CENTER);
        c.setPreferredSize(new Dimension(100, 220));
        return c;
    }

    @Override
    public void muat() {
        LocalDate a = periode.dari(), b = periode.sampai();
        LaporanService.Ringkasan r = k.laporan.ringkasan(a, b);
        long hari = java.time.temporal.ChronoUnit.DAYS.between(a, b) + 1;
        vTotal.setText(Rupiah.format(r.total()));
        kTotal.setText("rata-rata " + Rupiah.format(r.total() / hari) + " / hari · diskon " + Rupiah.format(r.diskon()));
        vSewa.setText(Rupiah.format(r.sewa()));
        kSewa.setText(r.sesi() + " sesi · rata-rata " + r.rataMenit() + " menit");
        vJajan.setText(Rupiah.format(r.jajan()));
        kJajan.setText(r.transaksiJajan() + " transaksi jajan saja");
        vJam.setText(Rupiah.ribuan(r.menitMain() / 60) + " jam");
        kJam.setText("total waktu main semua unit");

        Map<LocalDate, long[]> h = k.laporan.harian(a, b);
        List<String> lab = new ArrayList<>();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("d/M");
        for (LocalDate d : h.keySet()) lab.add(d.format(f));
        harian.isi(lab, new ArrayList<>(h.values()), h.size() > 16 ? (h.size() + 15) / 16 : 1);

        long[] j = k.laporan.jamMulai(a, b);
        List<String> lj = new ArrayList<>();
        List<long[]> vj = new ArrayList<>();
        int buka = k.pengaturan.angka("jam_buka"), tutup = Math.min(24, k.pengaturan.angka("jam_tutup"));
        for (int i = buka; i < tutup; i++) { lj.add(String.valueOf(i)); vj.add(new long[]{j[i]}); }
        jam.isi(lj, vj, 2);

        mUnit.setRowCount(0);
        for (LaporanService.PerUnit u : k.laporan.perUnit(a, b)) {
            mUnit.addRow(new Object[]{u.kode(), u.menit() / 60, u.pendapatan(), u.okupansi()});
        }
        mPaket.setRowCount(0);
        for (LaporanService.Peringkat p : k.laporan.paketPopuler(a, b)) mPaket.addRow(new Object[]{p.nama(), p.jumlah(), p.nilai()});
        mJajan.setRowCount(0);
        for (LaporanService.Peringkat p : k.laporan.jajanTerlaris(a, b)) mJajan.addRow(new Object[]{p.nama(), p.jumlah(), p.nilai()});
        repaint();
    }

    /** Batang okupansi: hijau >= 60%, kuning 35-60%, magenta < 35%. */
    static class RendererOkupansi extends DefaultTableCellRenderer {
        private double v;

        @Override public Component getTableCellRendererComponent(JTable t, Object val, boolean s, boolean f, int r, int c) {
            v = val instanceof Double d ? d : 0;
            super.getTableCellRendererComponent(t, Math.round(v * 100) + "%", s, false, r, c);
            setFont(Tema.semi(12.5f));
            setHorizontalAlignment(RIGHT);
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            int w = getWidth() - 56, y = getHeight() / 2 - 4;
            g.setColor(Tema.KARTU_2);
            g.fillRect(8, y, w, 8);
            g.setColor(v >= 0.6 ? Tema.LIME : v >= 0.35 ? Tema.KUNING : Tema.MAGENTA);
            g.fillRect(8, y, (int) (w * v), 8);
            setOpaque(false);
            super.paintComponent(g);
            setOpaque(true);
        }
    }

    private void ekspor() {
        LocalDate a = periode.dari(), b = periode.sampai();
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("transaksi-rental_" + a + "_" + b + ".csv"));
        fc.setFileFilter(new FileNameExtensionFilter("CSV (Excel)", "csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile().getName().toLowerCase().endsWith(".csv") ? fc.getSelectedFile() : new File(fc.getSelectedFile() + ".csv");
        Kotak.aman(this, () -> {
            List<List<Object>> rows = k.laporan.detail(a, b);
            try {
                Csv.tulis(file.toPath(), List.of("Nomor", "Tanggal", "Unit", "Pelanggan", "Jenis", "Paket", "Menit main", "Sewa", "Jajan", "Diskon", "Total", "Metode", "Operator"), rows);
            } catch (java.io.IOException e) { throw new Gagal("File tidak bisa disimpan: " + e.getMessage()); }
            frame.kabar(rows.size() + " transaksi diekspor ke " + file.getName());
        });
    }
}
