package id.papicode.billingps.ui.halaman;

import id.papicode.billingps.Konteks;
import id.papicode.billingps.model.*;
import id.papicode.billingps.service.LaporanService;
import id.papicode.billingps.service.Tarif;
import id.papicode.billingps.ui.*;
import id.papicode.billingps.util.Rupiah;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Layar utama operator: semua unit dalam grid. Timer di kartu dihitung ulang setiap detik dari waktu mulai
 * yang tersimpan di database, jadi tetap akurat walau aplikasi sempat ditutup.
 */
public class HalamanUnit extends JPanel implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final GridAlir grid = new GridAlir(262, 262, 14);
    private final Map<Long, KartuUnit> kartu = new LinkedHashMap<>();
    private final Map<Long, Long> jajanPerSesi = new HashMap<>();
    private final Set<Long> sudahDiingatkan = new HashSet<>(), sudahHabis = new HashSet<>();
    private final JLabel sMain = Ui.angka("0", 34f, Tema.LIME), sKosong = Ui.angka("0", 34f, Tema.TEKS),
            sHabis = Ui.angka("0", 34f, Tema.MAGENTA), sHari = Ui.angka("Rp0", 34f, Tema.CYAN);
    private Tarif.Aturan aturan = Tarif.Aturan.BAWAAN;
    private int peringatanMenit = 5;
    private int detikSejakMuat;

    public HalamanUnit(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        MainFrame.padding(this);

        Ui.Tombol jajan = Ui.tombol("Jual jajan", "cup", Ui.Jenis.GARIS);
        jajan.addActionListener(e -> frame.buka("jajan"));
        Ui.Tombol segar = Ui.ikonSaja("refresh", "Muat ulang", Ui.Jenis.GARIS);
        segar.addActionListener(e -> muat());
        JPanel atas = Ui.transparan(new BorderLayout(0, 14));
        atas.add(MainFrame.kepala("Layar unit", "Klik unit kosong untuk mulai main. Timer berjalan otomatis.", segar, jajan), BorderLayout.NORTH);
        JPanel stat = Ui.transparan(new GridLayout(1, 4, 14, 0));
        stat.add(stat("Sedang main", sMain));
        stat.add(stat("Unit kosong", sKosong));
        stat.add(stat("Waktu habis", sHabis));
        stat.add(stat("Pendapatan hari ini", sHari));
        atas.add(stat, BorderLayout.CENTER);
        atas.setBorder(new EmptyBorder(0, 0, 16, 0));
        add(atas, BorderLayout.NORTH);

        grid.setBorder(new EmptyBorder(2, 0, 12, 4));
        JScrollPane sp = new JScrollPane(grid);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        add(sp, BorderLayout.CENTER);

        new javax.swing.Timer(1000, e -> detik()).start();
    }

    private static JPanel stat(String label, JLabel nilai) {
        Ui.Kartu c = new Ui.Kartu(new BorderLayout(0, 0));
        c.setBorder(new EmptyBorder(10, 18, 8, 18));
        c.add(Ui.label(label), BorderLayout.NORTH);
        c.add(nilai, BorderLayout.CENTER);
        return c;
    }

    @Override
    public void muat() {
        aturan = k.pengaturan.aturan();
        peringatanMenit = k.pengaturan.angka("peringatan_menit");
        Map<Long, Sesi> aktif = new HashMap<>();
        jajanPerSesi.clear();
        for (Sesi s : k.sesi.berjalan()) {
            if (s.unitId() == null) continue;
            aktif.put(s.unitId(), s);
            jajanPerSesi.put(s.id(), k.sesi.jajan(s.id()).stream().mapToLong(ItemJajan::subtotal).sum());
        }
        grid.removeAll();
        kartu.clear();
        for (Unit u : k.master.unit(false)) {
            KartuUnit c = new KartuUnit(u, aktif.get(u.id()));
            kartu.put(u.id(), c);
            grid.add(c);
        }
        grid.revalidate();
        grid.repaint();
        LocalDate h = LocalDate.now(k.jam);
        LaporanService.Ringkasan r = k.laporan.ringkasan(h, h);
        sHari.setText(Rupiah.format(r.total()));
        detikSejakMuat = 0;
        detik();
    }

    /** Dipanggil tiap detik: perbarui timer, hitung status, dan beri peringatan. */
    private void detik() {
        if (!isShowing() && detikSejakMuat++ < 30) return;
        long now = k.sesi.sekarang();
        int main = 0, kosong = 0, habis = 0;
        for (KartuUnit c : kartu.values()) {
            c.perbarui(now);
            if (c.sesi == null) { if (!c.unit.servis()) kosong++; continue; }
            main++;
            long sisa = c.sesi.sisaMs(now);
            if (c.sesi.mode().equals(Sesi.PAKET) && !c.sesi.dijeda()) {
                if (sisa <= 0) {
                    habis++;
                    if (sudahHabis.add(c.sesi.id())) {
                        Toolkit.getDefaultToolkit().beep();
                        frame.kabar(c.unit.kode() + " waktu habis! (" + c.sesi.pelanggan() + ")");
                    }
                } else if (sisa <= peringatanMenit * 60_000L && sudahDiingatkan.add(c.sesi.id())) {
                    frame.kabar(c.unit.kode() + " sisa " + ((sisa + 59_999) / 60_000) + " menit");
                }
            }
        }
        sMain.setText(main + " / " + kartu.size());
        sKosong.setText(String.valueOf(kosong));
        sHabis.setText(String.valueOf(habis));
        // muat ulang berkala agar perubahan dari komputer operator lain ikut tampil
        if (++detikSejakMuat >= 30 && isShowing()) muat();
    }

    static String jamMenitDetik(long ms) {
        boolean minus = ms < 0;
        long d = Math.abs(ms) / 1000;
        return (minus ? "-" : "") + String.format("%02d:%02d:%02d", d / 3600, (d / 60) % 60, d % 60);
    }

    /** Kartu satu unit. */
    class KartuUnit extends Ui.Kartu {
        final Unit unit;
        final Sesi sesi;
        private final JLabel status = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isiStatus);
                g2.fill(Tema.chamfer(0, 0, getWidth(), getHeight(), 6));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        private Color isiStatus = Tema.GARIS;
        private final JLabel timer = Ui.angka("--:--:--", 50f, Tema.TEKS);
        private final JLabel bawahTimer = Ui.label(" ");
        private final JLabel biaya = Ui.angka(" ", 26f, Tema.TEKS);
        private final JComponent bar;
        private double progres;
        private Color warna = Tema.GARIS;

        KartuUnit(Unit unit, Sesi sesi) {
            super(new BorderLayout(0, 8), Tema.KARTU, Tema.GARIS, 18);
            this.unit = unit;
            this.sesi = sesi;
            setBorder(new EmptyBorder(14, 16, 14, 16));

            JPanel atas = Ui.transparan(new BorderLayout(10, 0));
            JLabel kode = Ui.angka(unit.kode(), 34f, Tema.TEKS);
            atas.add(kode, BorderLayout.WEST);
            JPanel info = Ui.transparan(new GridLayout(2, 1));
            JLabel konsol = new JLabel(unit.konsol().toUpperCase());
            konsol.setFont(Tema.bold(12f));
            konsol.setForeground(unit.konsol().contains("PS5") ? Tema.CYAN : unit.konsol().contains("PS4") ? Tema.UNGU.brighter() : Tema.TEKS_2);
            info.add(konsol);
            JLabel ket = Ui.teks(unit.nama() + " · " + (unit.keterangan() == null ? "" : unit.keterangan()), 11.5f, Tema.TEKS_2);
            info.add(ket);
            atas.add(info, BorderLayout.CENTER);
            status.setFont(Tema.bold(11f));
            status.setForeground(Tema.GELAP);
            status.setBorder(new EmptyBorder(3, 8, 3, 8));
            JPanel st = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 0, 6));
            st.add(status);
            atas.add(st, BorderLayout.EAST);
            add(atas, BorderLayout.NORTH);

            JPanel tengah = Ui.transparan(new BorderLayout(0, 2));
            timer.setHorizontalAlignment(SwingConstants.LEFT);
            tengah.add(timer, BorderLayout.NORTH);
            bar = new JComponent() {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(Tema.KARTU_2);
                    g.fillRect(0, 0, getWidth(), 6);
                    g.setColor(warna);
                    g.fillRect(0, 0, (int) (getWidth() * Math.max(0, Math.min(1, progres))), 6);
                }
            };
            bar.setPreferredSize(new Dimension(10, 6));
            tengah.add(bar, BorderLayout.CENTER);
            tengah.add(bawahTimer, BorderLayout.SOUTH);
            add(tengah, BorderLayout.CENTER);

            JPanel bawah = Ui.transparan(new BorderLayout(0, 8));
            JPanel b1 = Ui.transparan(new BorderLayout());
            String pelanggan = sesi == null ? (unit.servis() ? "Sedang diperbaiki" : "Siap dipakai") : sesi.pelanggan();
            JLabel pl = Ui.teks(pelanggan, 13.5f, sesi == null ? Tema.TEKS_2 : Tema.TEKS);
            pl.setFont(Tema.semi(13.5f));
            b1.add(pl, BorderLayout.WEST);
            b1.add(biaya, BorderLayout.EAST);
            bawah.add(b1, BorderLayout.NORTH);
            bawah.add(tombol(), BorderLayout.SOUTH);
            add(bawah, BorderLayout.SOUTH);

            if (sesi == null && !unit.servis()) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { mulai(); }
                    @Override public void mouseEntered(MouseEvent e) { setGaris(Tema.LIME); }
                    @Override public void mouseExited(MouseEvent e) { setGaris(warnaGaris()); }
                });
            }
        }

        private JPanel tombol() {
            JPanel p = Ui.transparan(new GridLayout(1, 0, 6, 0));
            if (sesi == null) {
                if (unit.servis()) {
                    Ui.Tombol selesai = Ui.tombol("Selesai servis", "check", Ui.Jenis.GARIS);
                    selesai.addActionListener(e -> servis(false));
                    p.add(selesai);
                } else {
                    Ui.Tombol mulai = Ui.tombol("Mulai main", "play", Ui.Jenis.UTAMA);
                    mulai.addActionListener(e -> mulai());
                    Ui.Tombol servis = Ui.ikonSaja("wrench", "Tandai sedang servis", Ui.Jenis.GARIS);
                    servis.addActionListener(e -> servis(true));
                    JPanel w = Ui.transparan(new BorderLayout(6, 0));
                    w.add(mulai);
                    w.add(servis, BorderLayout.EAST);
                    p.add(w);
                }
                return p;
            }
            if (sesi.mode().equals(Sesi.PAKET)) {
                Ui.Tombol tambah = Ui.ikonSaja("pluscircle", "Tambah waktu", Ui.Jenis.GARIS);
                tambah.addActionListener(e -> DialogSesi.tambahWaktu(this, k, frame, sesi, HalamanUnit.this::muat));
                p.add(tambah);
            }
            Ui.Tombol jajan = Ui.ikonSaja("cup", "Tambah jajan", Ui.Jenis.GARIS);
            jajan.addActionListener(e -> DialogSesi.jajan(this, k, frame, sesi, HalamanUnit.this::muat));
            Ui.Tombol jeda = Ui.ikonSaja(sesi.dijeda() ? "play" : "pause", sesi.dijeda() ? "Lanjutkan" : "Jeda (salat/makan)", Ui.Jenis.GARIS);
            jeda.addActionListener(e -> {
                if (Kotak.aman(this, () -> { if (sesi.dijeda()) k.sesi.lanjut(frame.aktor(), sesi.id()); else k.sesi.jeda(frame.aktor(), sesi.id()); })) muat();
            });
            Ui.Tombol pindah = Ui.ikonSaja("swap", "Pindah unit", Ui.Jenis.GARIS);
            pindah.addActionListener(e -> DialogSesi.pindah(this, k, frame, sesi, HalamanUnit.this::muat));
            Ui.Tombol stop = Ui.tombol("Stop", "stop", Ui.Jenis.BAHAYA);
            stop.setToolTipText("Stop & bayar");
            stop.addActionListener(e -> DialogSesi.bayar(this, k, frame, sesi, HalamanUnit.this::muat));
            p.add(jajan);
            p.add(jeda);
            p.add(pindah);
            JPanel baris = Ui.transparan(new BorderLayout(6, 0));
            baris.add(p, BorderLayout.CENTER);
            baris.add(stop, BorderLayout.EAST);
            return baris;
        }

        private Color warnaGaris() { return sesi == null ? Tema.GARIS : warna; }

        void perbarui(long now) {
            if (sesi == null) {
                timer.setText(unit.servis() ? "SERVIS" : "--:--:--");
                timer.setForeground(unit.servis() ? Tema.TEKS_2 : Tema.GARIS);
                pil(unit.servis() ? "Servis" : "Kosong", unit.servis() ? Tema.TEKS_2 : Tema.GARIS, unit.servis() ? Tema.GELAP : Tema.TEKS);
                bawahTimer.setText(unit.servis() ? "Unit tidak bisa dipakai" : "Tarif " + Rupiah.format(unit.tarifJam()) + " / jam");
                biaya.setText(" ");
                progres = 0;
                return;
            }
            long durasi = sesi.durasiMs(now);
            long jj = jajanPerSesi.getOrDefault(sesi.id(), 0L);
            long sewa;
            if (sesi.mode().equals(Sesi.PAKET)) {
                long sisa = sesi.sisaMs(now);
                timer.setText(jamMenitDetik(sisa));
                progres = durasi / (double) (sesi.menitPaket() * 60_000L);
                sewa = Tarif.paket(durasi, sesi.menitPaket(), sesi.hargaPaket(), sesi.tarifJam(), aturan).biaya();
                bawahTimer.setText(sesi.paketNama() + " · main " + jamMenitDetik(durasi).substring(0, 5));
                if (sesi.dijeda()) warna = Tema.CYAN;
                else if (sisa <= 0) warna = Tema.MAGENTA;
                else if (sisa <= peringatanMenit * 60_000L) warna = Tema.KUNING;
                else warna = Tema.LIME;
            } else {
                timer.setText(jamMenitDetik(durasi));
                progres = (durasi % 3_600_000L) / 3_600_000.0;
                sewa = Tarif.open(durasi, sesi.tarifJam(), aturan).biaya();
                bawahTimer.setText("Open billing · " + Rupiah.format(sesi.tarifJam()) + "/jam");
                warna = sesi.dijeda() ? Tema.CYAN : Tema.UNGU.brighter();
            }
            timer.setForeground(warna);
            String teks = sesi.dijeda() ? "Jeda" : warna == Tema.MAGENTA ? "Habis" : warna == Tema.KUNING ? "Hampir" : "Main";
            // waktu habis berkedip (ganti warna isi, bukan efek cahaya)
            boolean kedip = warna == Tema.MAGENTA && (now / 1000) % 2 == 0;
            pil(teks, kedip ? Tema.TEKS : warna, Tema.GELAP);
            setGaris(warna);
            biaya.setText(Rupiah.format(sewa + jj));
            biaya.setForeground(Tema.TEKS);
            bar.repaint();
        }

        private void pil(String teks, Color isi, Color fg) {
            status.setText(teks.toUpperCase());
            status.setForeground(fg);
            isiStatus = isi;
            status.repaint();
        }

        private void mulai() { DialogSesi.mulai(this, k, frame, unit, HalamanUnit.this::muat); }

        private void servis(boolean ya) {
            if (ya && !Kotak.konfirmasi(this, "Tandai " + unit.kode() + " servis?", "Unit tidak bisa dipakai sampai ditandai selesai servis.", "Tandai servis", true)) return;
            if (Kotak.aman(this, () -> k.master.setServis(frame.aktor(), unit.id(), ya))) muat();
        }
    }
}
