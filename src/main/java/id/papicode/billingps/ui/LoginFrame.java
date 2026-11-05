package id.papicode.billingps.ui;

import id.papicode.billingps.Konteks;
import id.papicode.billingps.db.DataContoh;
import id.papicode.billingps.model.Pengguna;
import id.papicode.billingps.service.Gagal;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Layar masuk: stik piksel bergaya arcade di kiri, form login di kanan. */
public class LoginFrame extends JFrame {

    private final Konteks k;
    private final JTextField user = Ui.field("Username");
    private final JPasswordField pass = new JPasswordField();
    private final JLabel galat = Ui.teks(" ", 13f, Tema.MAGENTA);

    public LoginFrame(Konteks k) {
        super("Billing PS - Masuk");
        this.k = k;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1060, 650);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.add(new Layar(k.pengaturan.get("nama_rental")), BorderLayout.CENTER);
        root.add(form(), BorderLayout.EAST);
        setContentPane(root);
    }

    private JPanel form() {
        JPanel luar = new JPanel(new GridBagLayout());
        luar.setBackground(Tema.KARTU);
        luar.setPreferredSize(new Dimension(430, 0));
        JPanel f = Ui.transparan(null);
        f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
        JLabel an = Ui.label("Player login");
        an.setForeground(Tema.MAGENTA);
        JLabel judul = Ui.judul("Masuk operator", 30f);
        JLabel sub = Ui.teks("Timer tetap jalan walau ganti shift.", 14f, Tema.TEKS_2);
        user.putClientProperty("JTextField.leadingIcon", Ikon.of("user", 16, Tema.TEKS_2));
        pass.putClientProperty("JTextField.placeholderText", "Password");
        pass.putClientProperty("JTextField.leadingIcon", Ikon.of("lock", 16, Tema.TEKS_2));
        pass.putClientProperty("JPasswordField.showRevealButton", true);
        for (JComponent c : new JComponent[]{user, pass}) {
            c.setMaximumSize(new Dimension(320, 44));
            c.setPreferredSize(new Dimension(320, 44));
            c.setFont(Tema.reg(15f));
        }
        Ui.Tombol masuk = Ui.tombol("Press start", "play", Ui.Jenis.UTAMA);
        masuk.setFont(Tema.bold(15f));
        masuk.setMaximumSize(new Dimension(320, 48));
        masuk.setPreferredSize(new Dimension(320, 48));
        masuk.addActionListener(e -> masuk());
        getRootPane().setDefaultButton(masuk);
        JLabel info = Ui.teks("<html>Akun bawaan <b>admin / admin123</b>.<br>Ganti segera di menu Pengaturan.</html>", 12.5f, Tema.TEKS_2);
        for (JComponent c : new JComponent[]{an, judul, sub, user, pass, galat, masuk, info}) c.setAlignmentX(0f);
        f.add(an);
        f.add(Ui.jarak(0, 6));
        f.add(judul);
        f.add(Ui.jarak(0, 4));
        f.add(sub);
        f.add(Ui.jarak(0, 28));
        f.add(user);
        f.add(Ui.jarak(0, 12));
        f.add(pass);
        f.add(Ui.jarak(0, 8));
        f.add(galat);
        f.add(Ui.jarak(0, 10));
        f.add(masuk);
        f.add(Ui.jarak(0, 22));
        f.add(info);
        f.setPreferredSize(new Dimension(320, 400));
        luar.add(f);
        return luar;
    }

    private void masuk() {
        try {
            Pengguna p = k.auth.login(user.getText(), new String(pass.getPassword()));
            pass.setText("");
            if (p.admin() && DataContoh.kosong(k.db)) {
                if (Kotak.konfirmasi(this, "Database masih kosong",
                        "Isi data contoh (10 unit PS3/PS4/PS5/VIP, paket, menu jajan, transaksi 30 hari, dan beberapa sesi yang sedang main)?",
                        "Isi data contoh", false)) {
                    DataContoh.isi(k.db, k.jam);
                }
            }
            new MainFrame(k, p).setVisible(true);
            dispose();
        } catch (Gagal g) {
            galat.setText(g.getMessage());
            pass.requestFocusInWindow();
        } catch (RuntimeException e) {
            Kotak.pesan(this, "Terjadi kesalahan", String.valueOf(e.getMessage()), true);
        }
    }

    /** Stik game dari kotak-kotak piksel. */
    static class Layar extends JPanel {
        private static final String[] STIK = {
            "....XXXXXX......XXXXXX....",
            "..XXXXXXXXXXXXXXXXXXXXXX..",
            ".XXXXXXXXXXXXXXXXXXXXXXXX.",
            "XXXXGGXXXXXXXXXXXXXXCXXXXX",
            "XXXGGGGXXXXXXXXXXXXCXMXXXX",
            "XXXXGGXXXXXX..XXXXXXCXXXXX",
            "XXXXXXXXXXX....XXXXXXXXXXX",
            "XXXXXXXXXX......XXXXXXXXXX",
            "XXXXXXXXX........XXXXXXXXX",
            ".XXXXXXX..........XXXXXXX.",
            "..XXXXX............XXXXX..",
        };
        private final String nama;

        Layar(String nama) {
            this.nama = nama;
            setBackground(Tema.LATAR);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            // titik-titik latar seperti layar CRT (tanpa efek cahaya)
            g2.setColor(new Color(0x1A1428));
            for (int y = 0; y < h; y += 16) for (int x = (y / 16) % 2 * 8; x < w; x += 16) g2.fillRect(x, y, 3, 3);
            int px = Math.max(10, Math.min(20, (w - 120) / STIK[0].length()));
            int lebar = STIK[0].length() * px;
            int x0 = (w - lebar) / 2, y0 = h / 2 - STIK.length * px / 2 - 30;
            for (int r = 0; r < STIK.length; r++) {
                for (int c = 0; c < STIK[r].length(); c++) {
                    char ch = STIK[r].charAt(c);
                    if (ch == '.') continue;
                    g2.setColor(switch (ch) { case 'G' -> Tema.LIME; case 'C' -> Tema.CYAN; case 'M' -> Tema.MAGENTA; default -> Tema.UNGU; });
                    g2.fillRect(x0 + c * px, y0 + r * px, px - 2, px - 2);
                }
            }
            g2.setFont(Tema.piksel(44f));
            g2.setColor(Tema.LIME);
            String t = "PRESS START";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(t, (w - fm.stringWidth(t)) / 2, y0 + STIK.length * px + 70);
            g2.setFont(Tema.bold(16f));
            g2.setColor(Tema.TEKS);
            fm = g2.getFontMetrics();
            String n = nama.toUpperCase();
            g2.drawString(n, (w - fm.stringWidth(n)) / 2, y0 + STIK.length * px + 100);
            g2.setFont(Tema.semi(12f));
            g2.setColor(Tema.TEKS_2);
            String s = "BILLING  ·  TIMER  ·  JAJAN  ·  LAPORAN";
            fm = g2.getFontMetrics();
            g2.drawString(s, (w - fm.stringWidth(s)) / 2, y0 + STIK.length * px + 124);
            g2.dispose();
        }
    }
}
