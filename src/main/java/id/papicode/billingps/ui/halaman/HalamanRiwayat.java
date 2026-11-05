package id.papicode.billingps.ui.halaman;

import id.papicode.billingps.Konteks;
import id.papicode.billingps.model.Sesi;
import id.papicode.billingps.service.SesiService;
import id.papicode.billingps.service.Struk;
import id.papicode.billingps.ui.*;
import id.papicode.billingps.util.Rupiah;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Riwayat transaksi: sesi main dan penjualan jajan, struk, log aktivitas, dan pembatalan (admin). */
public class HalamanRiwayat extends JPanel implements MainFrame.Halaman {

    private static final DateTimeFormatter WKT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private final Konteks k;
    private final MainFrame frame;
    private final PilihPeriode periode;
    private final JTextField cari = Ui.cari("Cari nomor, pelanggan, unit");
    private final List<Sesi> data = new ArrayList<>();
    private final AbstractTableModel model;
    private final JTable tabel;
    private final JLabel ringkas = Ui.label(" ");
    private final JPanel detail = Ui.transparan(new BorderLayout(0, 10));

    public HalamanRiwayat(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        MainFrame.padding(this);
        periode = new PilihPeriode(k.jam, 0);
        periode.onChange(this::muat);
        add(MainFrame.kepala("Riwayat transaksi", "Sesi main dan penjualan jajan. Klik baris untuk melihat struk dan log."), BorderLayout.NORTH);

        model = new AbstractTableModel() {
            final String[] kol = {"Nomor", "Selesai", "Unit", "Pelanggan", "Jenis", "Lama", "Total", "Status"};
            public int getRowCount() { return data.size(); }
            public int getColumnCount() { return kol.length; }
            public String getColumnName(int c) { return kol[c]; }
            public Object getValueAt(int r, int c) {
                Sesi s = data.get(r);
                return switch (c) {
                    case 0 -> s.nomor();
                    case 1 -> Instant.ofEpochMilli(s.selesai()).atZone(k.jam.getZone()).format(WKT);
                    case 2 -> s.unitKode() == null ? "-" : s.unitKode();
                    case 3 -> s.pelanggan();
                    case 4 -> s.mode().equals(Sesi.JAJAN) ? "Jajan" : s.mode().equals(Sesi.PAKET) ? s.paketNama() : "Open";
                    case 5 -> s.mode().equals(Sesi.JAJAN) ? "-" : Struk.durasi(s.durasiMs(s.selesai()));
                    case 6 -> s.total();
                    default -> s.batal() ? "Batal" : "Lunas";
                };
            }
        };
        tabel = new JTable(model);
        Ui.rapikanTabel(tabel);
        Ui.lebarKolom(tabel, 120, 95, 70, 150, 150, 100, 90, 80);
        Ui.kolomAngka(tabel, 6);
        tabel.getColumnModel().getColumn(7).setCellRenderer(new Ui.Pil(Map.of("Lunas", Tema.LIME, "Batal", Tema.MAGENTA)));
        tabel.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) tampilDetail(); });

        JPanel kiri = Ui.transparan(new BorderLayout(0, 10));
        JPanel f = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 8, 0));
        f.add(periode);
        cari.setPreferredSize(new Dimension(240, 38));
        cari.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { muat(); }
            public void removeUpdate(DocumentEvent e) { muat(); }
            public void changedUpdate(DocumentEvent e) { muat(); }
        });
        f.add(cari);
        kiri.add(f, BorderLayout.NORTH);
        Ui.Kartu w = new Ui.Kartu(new BorderLayout());
        w.setBorder(new EmptyBorder(6, 6, 10, 6));
        w.add(Ui.gulir(tabel));
        kiri.add(w, BorderLayout.CENTER);
        kiri.add(ringkas, BorderLayout.SOUTH);
        detail.setPreferredSize(new Dimension(390, 0));
        JPanel tengah = Ui.transparan(new BorderLayout(16, 0));
        tengah.add(kiri, BorderLayout.CENTER);
        tengah.add(detail, BorderLayout.EAST);
        add(tengah, BorderLayout.CENTER);
    }

    @Override
    public void muat() {
        data.clear();
        data.addAll(k.sesi.riwayat(periode.dari(), periode.sampai(), cari.getText()));
        model.fireTableDataChanged();
        long total = data.stream().filter(s -> !s.batal()).mapToLong(Sesi::total).sum();
        long batal = data.stream().filter(Sesi::batal).count();
        ringkas.setText(data.size() + " transaksi" + (batal > 0 ? " (" + batal + " batal)" : "") + "  ·  " + Rupiah.format(total));
        tampilDetail();
    }

    private void tampilDetail() {
        detail.removeAll();
        int r = tabel.getSelectedRow();
        if (r < 0) {
            Ui.Kartu kosong = new Ui.Kartu(new GridBagLayout());
            kosong.add(Ui.teks("Pilih transaksi untuk melihat struk.", 13.5f, Tema.TEKS_2));
            detail.add(kosong);
        } else {
            Sesi s = data.get(r);
            SesiService.Tagihan t = k.sesi.tagihan(s.id());
            List<String> baris = Struk.susun(s, t, k.pengaturan, k.jam.getZone());
            JTabbedPane tab = new JTabbedPane();
            DialogStruk.Kertas kertas = new DialogStruk.Kertas(baris);
            JPanel p = Ui.transparan(new FlowLayout(FlowLayout.CENTER, 0, 10));
            p.add(kertas);
            JScrollPane sp = new JScrollPane(p);
            sp.setBorder(null);
            sp.getViewport().setBackground(Tema.LATAR);
            tab.addTab("Struk", sp);
            JPanel log = Ui.transparan(null);
            log.setLayout(new BoxLayout(log, BoxLayout.Y_AXIS));
            log.setBorder(new EmptyBorder(10, 10, 10, 10));
            DateTimeFormatter j = DateTimeFormatter.ofPattern("dd/MM HH:mm");
            for (SesiService.Log l : k.sesi.log(s.id())) {
                JLabel x = Ui.teks("<html><b>" + Instant.ofEpochMilli(l.waktu()).atZone(k.jam.getZone()).format(j) + "  " + l.aksi() + "</b>"
                        + (l.keterangan() == null ? "" : "  " + l.keterangan()) + "<br><font color='#A99CC9'>" + l.operator() + "</font></html>", 13f, Tema.TEKS);
                x.setBorder(new EmptyBorder(4, 0, 6, 0));
                log.add(x);
            }
            JPanel lw = Ui.transparan(new BorderLayout());
            lw.add(log, BorderLayout.NORTH);
            tab.addTab("Log", Ui.gulir(lw));
            detail.add(tab, BorderLayout.CENTER);
            JPanel tombol = Ui.transparan(new GridLayout(1, 0, 8, 0));
            Ui.Tombol cetak = Ui.tombol("Cetak ulang", "printer", Ui.Jenis.GARIS);
            cetak.addActionListener(e -> DialogStruk.cetak(this, baris));
            tombol.add(cetak);
            if (frame.pengguna().admin() && !s.batal()) {
                Ui.Tombol batal = Ui.tombol("Batalkan", "xcircle", Ui.Jenis.BAHAYA);
                batal.addActionListener(e -> {
                    String alasan = Kotak.input(this, "Batalkan " + s.nomor(), "Alasan (stok jajan dikembalikan)", "");
                    if (alasan != null && Kotak.aman(this, () -> k.sesi.batal(frame.aktor(), s.id(), alasan))) {
                        frame.kabar(s.nomor() + " dibatalkan");
                        muat();
                    }
                });
                tombol.add(batal);
            }
            detail.add(tombol, BorderLayout.SOUTH);
        }
        detail.revalidate();
        detail.repaint();
    }
}
