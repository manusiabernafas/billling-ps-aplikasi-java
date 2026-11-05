package id.papicode.billingps.ui;

import id.papicode.billingps.Konteks;
import id.papicode.billingps.model.Pengguna;
import id.papicode.billingps.ui.halaman.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Jendela utama: sidebar gelap dengan jam digital + area halaman. */
public class MainFrame extends JFrame {

    public interface Halaman {
        void muat();
        default JComponent fokusAwal() { return null; }
    }

    private final Konteks k;
    private Pengguna pengguna;
    private final CardLayout kartu = new CardLayout();
    private final JPanel isi = new JPanel(kartu);
    private final Map<String, Halaman> halaman = new LinkedHashMap<>();
    private final Map<String, ItemNav> nav = new LinkedHashMap<>();
    private final JPanel menu = Ui.transparan(null);
    private final Toast toast = new Toast();
    private final JLabel jamBesar = Ui.angka("00:00", 46f, Tema.LIME);
    private final JLabel tanggal = Ui.label(" ");
    private final Timer detak;

    public MainFrame(Konteks k, Pengguna p) {
        super("Billing PS - " + k.pengaturan.get("nama_rental"));
        this.k = k;
        this.pengguna = p;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1366, 800);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);
        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        isi.setBackground(Tema.LATAR);

        tambah("unit", "Layar Unit", "gamepad", new HalamanUnit(k, this), false);
        tambah("jajan", "Jual Jajan", "cup", new HalamanJualJajan(k, this), false);
        tambah("riwayat", "Riwayat", "receipt", new HalamanRiwayat(k, this), false);
        tambah("laporan", "Laporan", "chart", new HalamanLaporan(k, this), true);
        tambah("master", "Unit & Tarif", "tv", new HalamanMaster(k, this), false);
        tambah("stok", "Menu & Stok Jajan", "package", new HalamanMenuJajan(k, this), false);
        tambah("pengguna", "Pengguna", "users", new HalamanPengguna(k, this), true);
        tambah("pengaturan", "Pengaturan", "sliders", new HalamanPengaturan(k, this), false);

        JPanel root = new JPanel(new BorderLayout());
        root.add(sidebar(), BorderLayout.WEST);
        root.add(isi, BorderLayout.CENTER);
        setContentPane(root);
        getLayeredPane().add(toast, JLayeredPane.POPUP_LAYER);
        detak = new Timer(1000, e -> perbaruiJam());
        detak.start();
        perbaruiJam();
        buka("unit");
    }

    @Override public void dispose() { detak.stop(); super.dispose(); }

    public Pengguna pengguna() { return pengguna; }

    public Pengguna aktor() {
        pengguna = k.auth.segarkan(pengguna);
        return pengguna;
    }

    @SuppressWarnings("unchecked")
    public <T> T halaman(String id) { return (T) halaman.get(id); }

    private void tambah(String id, String label, String ikon, JComponent h, boolean adminSaja) {
        if (adminSaja && !pengguna.admin()) return;
        halaman.put(id, (Halaman) h);
        isi.add(h, id);
        ItemNav n = new ItemNav(label, ikon, () -> buka(id));
        nav.put(id, n);
        menu.add(n);
        menu.add(Box.createVerticalStrut(4));
    }

    public void buka(String id) {
        Halaman h = halaman.get(id);
        if (h == null) return;
        nav.forEach((kunci, n) -> n.setAktif(kunci.equals(id)));
        kartu.show(isi, id);
        Kotak.aman(this, h::muat);
        JComponent f = h.fokusAwal();
        if (f != null) SwingUtilities.invokeLater(f::requestFocusInWindow);
    }

    public void kabar(String pesan) { toast.tampil(pesan); }

    private void perbaruiJam() {
        LocalDateTime t = LocalDateTime.now(k.jam);
        jamBesar.setText(t.format(DateTimeFormatter.ofPattern("HH:mm")));
        tanggal.setText(t.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.forLanguageTag("id-ID"))));
    }

    private JPanel sidebar() {
        JPanel s = new JPanel(new BorderLayout());
        s.setBackground(Tema.SIDEBAR);
        s.setPreferredSize(new Dimension(232, 0));
        s.setBorder(new EmptyBorder(20, 14, 16, 14));

        JPanel merek = Ui.transparan(new BorderLayout(12, 0));
        JLabel logo = new JLabel(Ikon.of("gamepad", 24, Tema.GELAP)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Tema.LIME);
                g2.fill(Tema.chamfer(0, 0, getWidth(), getHeight(), 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logo.setPreferredSize(new Dimension(44, 44));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        merek.add(logo, BorderLayout.WEST);
        JPanel nama = Ui.transparan(new GridLayout(2, 1));
        JLabel n1 = new JLabel(k.pengaturan.get("nama_rental").toUpperCase());
        n1.setFont(Tema.bold(12.5f));
        n1.setForeground(Tema.TEKS);
        JLabel n2 = new JLabel("BILLING RENTAL PS");
        n2.setFont(Tema.semi(10.5f));
        n2.setForeground(Tema.MAGENTA);
        nama.add(n1);
        nama.add(n2);
        merek.add(nama, BorderLayout.CENTER);
        merek.setBorder(new EmptyBorder(0, 4, 24, 0));
        s.add(merek, BorderLayout.NORTH);
        s.add(menu, BorderLayout.CENTER);

        JPanel bawah = Ui.transparan(new BorderLayout(0, 12));
        JPanel jamPanel = Ui.transparan(new BorderLayout());
        jamPanel.add(jamBesar, BorderLayout.CENTER);
        jamPanel.add(tanggal, BorderLayout.SOUTH);
        jamPanel.setBorder(new EmptyBorder(0, 6, 0, 0));
        bawah.add(jamPanel, BorderLayout.NORTH);

        Ui.Kartu akun = new Ui.Kartu(new BorderLayout(10, 0), Tema.KARTU, Tema.GARIS, 10);
        akun.setBorder(new EmptyBorder(9, 10, 9, 6));
        JLabel ini = new JLabel(inisial(pengguna.nama()), SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Tema.UNGU);
                g2.fill(Tema.chamfer(0, 0, getWidth(), getHeight(), 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        ini.setFont(Tema.bold(13f));
        ini.setForeground(Tema.TEKS);
        ini.setPreferredSize(new Dimension(34, 34));
        akun.add(ini, BorderLayout.WEST);
        JPanel teks = Ui.transparan(new GridLayout(2, 1));
        JLabel nm = new JLabel(pengguna.nama());
        nm.setFont(Tema.semi(13f));
        nm.setForeground(Tema.TEKS);
        JLabel pr = new JLabel(pengguna.admin() ? "ADMIN" : "OPERATOR");
        pr.setFont(Tema.semi(10.5f));
        pr.setForeground(Tema.TEKS_2);
        teks.add(nm);
        teks.add(pr);
        akun.add(teks, BorderLayout.CENTER);
        JButton keluar = new JButton(Ikon.of("logout", 18, Tema.TEKS_2));
        keluar.setToolTipText("Keluar / ganti shift");
        keluar.setContentAreaFilled(false);
        keluar.setBorderPainted(false);
        keluar.setFocusable(false);
        keluar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        keluar.addActionListener(e -> {
            if (!Kotak.konfirmasi(this, "Keluar / ganti shift?", "Sesi yang sedang main tetap berjalan dan tersimpan. Timer lanjut saat operator berikutnya login.", "Keluar", false)) return;
            dispose();
            new LoginFrame(k).setVisible(true);
        });
        akun.add(keluar, BorderLayout.EAST);
        bawah.add(akun, BorderLayout.SOUTH);
        s.add(bawah, BorderLayout.SOUTH);
        return s;
    }

    static String inisial(String nama) {
        String[] p = nama.trim().split("\\s+");
        return (p[0].substring(0, 1) + (p.length > 1 ? p[1].substring(0, 1) : "")).toUpperCase();
    }

    static class ItemNav extends JPanel {
        private boolean aktif, hover;
        private final JLabel label;
        private final String ikon;

        ItemNav(String teks, String ikon, Runnable klik) {
            super(new BorderLayout());
            this.ikon = ikon;
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setPreferredSize(new Dimension(200, 40));
            setAlignmentX(0f);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label = new JLabel(teks);
            label.setIconTextGap(12);
            label.setBorder(new EmptyBorder(0, 12, 0, 0));
            add(label);
            perbarui();
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { klik.run(); }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setAktif(boolean a) { aktif = a; perbarui(); repaint(); }

        private void perbarui() {
            Color c = aktif ? Tema.GELAP : Tema.TEKS_2;
            label.setForeground(aktif ? Tema.GELAP : Tema.TEKS);
            label.setFont(aktif ? Tema.bold(14f) : Tema.semi(14f));
            label.setIcon(Ikon.of(ikon, 18, c));
        }

        @Override protected void paintComponent(Graphics g) {
            if (aktif || hover) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(aktif ? Tema.LIME : Tema.KARTU_2);
                g2.fill(Tema.chamfer(0, 0, getWidth(), getHeight(), 10));
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    class Toast extends JLabel {
        private final Timer timer = new Timer(2800, e -> setVisible(false));

        Toast() {
            setVisible(false);
            setFont(Tema.bold(13.5f));
            setForeground(Tema.GELAP);
            setIcon(Ikon.of("check", 16, Tema.GELAP));
            setIconTextGap(10);
            setBorder(new EmptyBorder(12, 16, 12, 18));
            timer.setRepeats(false);
        }

        void tampil(String pesan) {
            setText(pesan);
            Dimension d = getPreferredSize();
            JLayeredPane lp = getLayeredPane();
            setBounds(lp.getWidth() - d.width - 26, lp.getHeight() - d.height - 26, d.width, d.height);
            setVisible(true);
            timer.restart();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.LIME);
            g2.fill(Tema.chamfer(0, 0, getWidth(), getHeight(), 10));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JPanel kepala(String judul, String sub, JComponent... aksi) {
        JPanel p = Ui.transparan(new BorderLayout(16, 0));
        JPanel kiri = Ui.transparan(new BorderLayout(0, 2));
        kiri.add(Ui.judul(judul, 27f), BorderLayout.NORTH);
        if (sub != null) kiri.add(Ui.teks(sub, 14f, Tema.TEKS_2), BorderLayout.CENTER);
        p.add(kiri, BorderLayout.CENTER);
        JPanel kanan = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        for (JComponent a : aksi) kanan.add(a);
        p.add(kanan, BorderLayout.EAST);
        p.setBorder(new EmptyBorder(0, 0, 18, 0));
        return p;
    }

    public static void padding(JPanel h) {
        h.setBackground(Tema.LATAR);
        h.setBorder(new EmptyBorder(24, 28, 24, 28));
    }
}
