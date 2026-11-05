package id.papicode.billingps.ui;

import id.papicode.billingps.util.Rupiah;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/** Komponen bertema arcade: panel & tombol bersudut potong, label kapital, pil status. */
public final class Ui {

    private Ui() { }

    public static class Kartu extends JPanel {
        private Color isi, garis;
        private final int potong;

        public Kartu(LayoutManager lm, Color isi, Color garis, int potong) {
            super(lm);
            this.isi = isi;
            this.garis = garis;
            this.potong = potong;
            setOpaque(false);
        }

        public Kartu(LayoutManager lm) { this(lm, Tema.KARTU, Tema.GARIS, 14); }

        public void setIsi(Color c) { isi = c; repaint(); }
        public void setGaris(Color c) { garis = c; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape s = Tema.chamfer(0.5, 0.5, getWidth() - 1.5, getHeight() - 1.5, potong);
            if (isi != null) { g2.setColor(isi); g2.fill(s); }
            if (garis != null) { g2.setColor(garis); g2.setStroke(new BasicStroke(1.2f)); g2.draw(s); }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static Kartu kartu(LayoutManager lm, int pad) {
        Kartu k = new Kartu(lm);
        k.setBorder(new EmptyBorder(pad, pad, pad, pad));
        return k;
    }

    public enum Jenis { UTAMA, GARIS, HANTU, BAHAYA, INFO }

    public static class Tombol extends JButton {
        private final Jenis jenis;
        private boolean hover;

        public Tombol(String teks, String ikon, Jenis jenis) {
            super(teks == null ? null : teks.toUpperCase());
            this.jenis = jenis;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Tema.bold(13f));
            setForeground(warnaTeks());
            setBorder(new EmptyBorder(9, 16, 9, 16));
            setIconTextGap(8);
            if (ikon != null) setIcon(Ikon.of(ikon, 16, warnaTeks()));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        private Color warnaTeks() {
            return switch (jenis) {
                case UTAMA, BAHAYA, INFO -> Tema.GELAP;
                default -> Tema.TEKS;
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape s = Tema.chamfer(0.5, 0.5, getWidth() - 1.5, getHeight() - 1.5, Math.min(10, getHeight() / 3.0));
            Color isi = switch (jenis) {
                case UTAMA -> hover ? Tema.LIME_HOVER : Tema.LIME;
                case BAHAYA -> hover ? new Color(0xFF7DBD) : Tema.MAGENTA;
                case INFO -> hover ? new Color(0x7FEDF2) : Tema.CYAN;
                case GARIS -> hover ? Tema.PILIH : Tema.KARTU;
                case HANTU -> hover ? Tema.PILIH : null;
            };
            if (!isEnabled()) isi = Tema.KARTU_2;
            if (isi != null) { g2.setColor(isi); g2.fill(s); }
            if (jenis == Jenis.GARIS) { g2.setColor(hover ? Tema.LIME : Tema.GARIS); g2.draw(s); }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static Tombol tombol(String teks, String ikon, Jenis jenis) { return new Tombol(teks, ikon, jenis); }

    public static Tombol ikonSaja(String ikon, String tip, Jenis jenis) {
        Tombol t = new Tombol(null, ikon, jenis);
        t.setToolTipText(tip);
        t.setBorder(new EmptyBorder(7, 8, 7, 8));
        return t;
    }

    public static JLabel judul(String s, float size) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.bold(size));
        l.setForeground(Tema.TEKS);
        return l;
    }

    public static JLabel teks(String s, float size, Color c) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.reg(size));
        l.setForeground(c);
        return l;
    }

    public static JLabel label(String s) {
        JLabel l = new JLabel(s.toUpperCase());
        l.setFont(Tema.semi(11.5f));
        l.setForeground(Tema.TEKS_2);
        return l;
    }

    public static JLabel angka(String s, float size, Color c) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.piksel(size));
        l.setForeground(c);
        return l;
    }

    /** Pil status bersudut potong dengan isi warna solid. */
    public static JLabel lencana(String s, Color isi, Color teks) {
        JLabel l = new JLabel(s.toUpperCase()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isi);
                g2.fill(Tema.chamfer(0, 0, getWidth(), getHeight(), 6));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(Tema.bold(11f));
        l.setForeground(teks);
        l.setBorder(new EmptyBorder(3, 9, 3, 9));
        return l;
    }

    public static JTextField field(String placeholder) {
        JTextField f = new JTextField();
        f.putClientProperty("JTextField.placeholderText", placeholder);
        return f;
    }

    public static JTextField cari(String placeholder) {
        JTextField f = field(placeholder);
        f.putClientProperty("JTextField.leadingIcon", Ikon.of("search", 16, Tema.TEKS_2));
        f.putClientProperty("JTextField.showClearButton", true);
        return f;
    }

    public static JPanel baris(String label, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(label(label), BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    public static JScrollPane gulir(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Tema.KARTU);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        return sp;
    }

    public static void rapikanTabel(JTable t) {
        t.setFillsViewportHeight(true);
        t.setRowHeight(36);
        t.setFont(Tema.reg(13.5f));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer h = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v == null ? "" : v.toString().toUpperCase(), false, false, r, c);
                setFont(Tema.bold(11.5f));
                setForeground(Tema.LIME);
                setBackground(Tema.KARTU);
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Tema.GARIS), new EmptyBorder(0, 12, 0, 12)));
                return this;
            }
        };
        t.getTableHeader().setDefaultRenderer(h);
    }

    public static void kolomAngka(JTable t, int... kolom) {
        for (int k : kolom) {
            t.getColumnModel().getColumn(k).setCellRenderer(new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r1, int c1) {
                    Object tampil = v instanceof Long || v instanceof Integer ? Rupiah.ribuan(((Number) v).longValue()) : v;
                    super.getTableCellRendererComponent(tb, tampil, s, false, r1, c1);
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    setFont(Tema.semi(13.5f));
                    return this;
                }
            });
        }
    }

    public static void lebarKolom(JTable t, int... lebar) {
        for (int i = 0; i < lebar.length && i < t.getColumnCount(); i++) {
            if (lebar[i] > 0) t.getColumnModel().getColumn(i).setPreferredWidth(lebar[i]);
        }
    }

    /** Renderer pil: teks sel dipetakan ke warna isi. */
    public static class Pil extends DefaultTableCellRenderer {
        private final Map<String, Color> warna;
        private Color c = Tema.GARIS;

        public Pil(Map<String, Color> warna) { this.warna = warna; }

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int col) {
            super.getTableCellRendererComponent(t, v == null ? "" : v.toString().toUpperCase(), s, false, r, col);
            c = warna.getOrDefault(String.valueOf(v), Tema.GARIS);
            setForeground(Tema.GELAP);
            setFont(Tema.bold(11f));
            setHorizontalAlignment(CENTER);
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = Math.min(getWidth() - 14, 90), h = 22;
            g2.setColor(c);
            g2.fill(Tema.chamfer((getWidth() - w) / 2.0, (getHeight() - h) / 2.0, w, h, 6));
            g2.dispose();
            setOpaque(false);
            super.paintComponent(g);
            setOpaque(true);
        }
    }

    public static JPanel transparan(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    public static Component jarak(int w, int h) { return Box.createRigidArea(new Dimension(w, h)); }
}
