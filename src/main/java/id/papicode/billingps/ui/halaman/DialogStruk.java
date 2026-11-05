package id.papicode.billingps.ui.halaman;

import id.papicode.billingps.Konteks;
import id.papicode.billingps.model.Sesi;
import id.papicode.billingps.service.Struk;
import id.papicode.billingps.ui.Kotak;
import id.papicode.billingps.ui.Tema;
import id.papicode.billingps.ui.Ui;
import id.papicode.billingps.util.Rupiah;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;

/** Pratinjau struk (kertas thermal) dan cetak ke printer. */
public final class DialogStruk {
    private DialogStruk() { }

    public static void tampil(Component induk, Konteks k, Sesi t) {
        List<String> baris = Struk.susun(t, k.sesi.tagihan(t.id()), k.pengaturan, k.jam.getZone());
        Window w = SwingUtilities.getWindowAncestor(induk);
        JDialog d = new JDialog(w, "Struk " + t.nomor(), java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Tema.KARTU);
        root.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, Tema.LIME), new EmptyBorder(22, 24, 20, 24)));

        JPanel atas = Ui.transparan(new BorderLayout(0, 4));
        atas.add(Ui.judul(t.batal() ? "Struk (dibatalkan)" : "Transaksi berhasil", 20f), BorderLayout.NORTH);
        String sub = "TUNAI".equals(t.metode()) ? "Kembalian " + Rupiah.format(t.kembali()) : "Dibayar via " + t.metode();
        JLabel s = Ui.teks(sub, 15f, Tema.LIME);
        s.setFont(Tema.bold(15f));
        atas.add(s, BorderLayout.CENTER);
        root.add(atas, BorderLayout.NORTH);

        Kertas kertas = new Kertas(baris);
        JPanel tengah = Ui.transparan(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tengah.add(kertas);
        JScrollPane sp = new JScrollPane(tengah);
        sp.setBorder(null);
        sp.getViewport().setBackground(Tema.KARTU);
        sp.setPreferredSize(new Dimension(kertas.getPreferredSize().width + 40, Math.min(460, kertas.getPreferredSize().height + 10)));
        root.add(sp, BorderLayout.CENTER);

        Ui.Tombol cetak = Ui.tombol("Cetak struk", "printer", Ui.Jenis.GARIS);
        Ui.Tombol tutup = Ui.tombol("Selesai", "check", Ui.Jenis.UTAMA);
        cetak.addActionListener(e -> cetak(d, baris));
        tutup.addActionListener(e -> d.dispose());
        JPanel bawah = Ui.transparan(new GridLayout(1, 2, 8, 0));
        bawah.add(cetak);
        bawah.add(tutup);
        root.add(bawah, BorderLayout.SOUTH);

        d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.getRootPane().registerKeyboardAction(e -> cetak(d, baris), KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.getRootPane().setDefaultButton(tutup);
        d.setContentPane(root);
        d.pack();
        d.setResizable(false);
        d.setLocationRelativeTo(w);
        d.setVisible(true);
    }

    static void cetak(Component induk, List<String> baris) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Struk");
        int kolom = baris.stream().mapToInt(String::length).max().orElse(32);
        double lebarMm = kolom > 32 ? 80 : 58;
        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        double lebar = lebarMm / 25.4 * 72;
        double tinggi = (baris.size() + 4) * 11.0 + 36;
        paper.setSize(lebar, tinggi);
        paper.setImageableArea(6, 10, lebar - 12, tinggi - 20);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);
        job.setPrintable(new CetakStruk(baris, kolom), pf);
        if (!job.printDialog()) return;
        try {
            job.print();
        } catch (PrinterException e) {
            Kotak.pesan(induk, "Gagal mencetak", e.getMessage(), true);
        }
    }

    /** Printable: ukuran font dihitung agar satu baris pas selebar kertas. */
    record CetakStruk(List<String> baris, int kolom) implements Printable {
        @Override
        public int print(Graphics g, PageFormat pf, int halaman) {
            if (halaman > 0) return NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            // ukur dengan ukuran acuan 10pt, lalu skalakan agar satu baris pas selebar kertas
            Font f = Tema.piksel(10f);
            FontMetrics fm = g2.getFontMetrics(f);
            double lebarBaris = fm.charWidth('M') * (double) kolom;
            float size = (float) Math.min(16, 10f * pf.getImageableWidth() / lebarBaris);
            f = Tema.piksel(size);
            g2.setFont(f);
            g2.setColor(Color.BLACK);
            fm = g2.getFontMetrics();
            int y = fm.getAscent();
            for (String b : baris) {
                g2.drawString(b, 0, y);
                y += fm.getHeight();
            }
            return PAGE_EXISTS;
        }
    }

    /** Tampilan kertas thermal dengan tepi bergerigi. */
    static class Kertas extends JComponent {
        private final List<String> baris;
        private final Font font = Tema.piksel(19f);

        Kertas(List<String> baris) {
            this.baris = baris;
            FontMetrics fm = getFontMetrics(font);
            int kol = baris.stream().mapToInt(String::length).max().orElse(32);
            setPreferredSize(new Dimension(fm.charWidth('M') * kol + 48, baris.size() * (fm.getHeight() + 1) + 64));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), gigi = 8;
            Path2D.Float p = new Path2D.Float();
            p.moveTo(0, gigi);
            for (int x = 0; x < w; x += gigi * 2) { p.lineTo(x + gigi, 0); p.lineTo(Math.min(w, x + gigi * 2), gigi); }
            p.lineTo(w, h - gigi);
            for (int x = w; x > 0; x -= gigi * 2) { p.lineTo(x - gigi, h); p.lineTo(Math.max(0, x - gigi * 2), h - gigi); }
            p.closePath();
            g2.setColor(Color.WHITE);
            g2.fill(p);
            g2.setColor(Tema.GARIS);
            g2.draw(p);
            g2.setFont(font);
            g2.setColor(new Color(0x2B2B2B));
            FontMetrics fm = g2.getFontMetrics();
            int y = 32 + fm.getAscent();
            for (String b : baris) {
                g2.drawString(b, 24, y);
                y += fm.getHeight() + 1;
            }
            g2.dispose();
        }
    }
}
