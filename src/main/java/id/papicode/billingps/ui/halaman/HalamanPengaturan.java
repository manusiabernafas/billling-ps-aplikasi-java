package id.papicode.billingps.ui.halaman;

import id.papicode.billingps.Konteks;
import id.papicode.billingps.service.AuthService;
import id.papicode.billingps.service.Gagal;
import id.papicode.billingps.ui.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Identitas toko & struk (admin), ganti password (semua), cadangkan database (admin). */
public class HalamanPengaturan extends JPanel implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final JTextField nama = Ui.field("Nama rental");
    private final JTextField alamat = Ui.field("Alamat singkat");
    private final JTextField telp = Ui.field("Nomor telepon/WA");
    private final JTextField catatan = Ui.field("Pesan di bawah struk");
    private final JTextField buka = Ui.field("10"), tutup = Ui.field("24"), blok = Ui.field("15"), minimal = Ui.field("30"),
            toleransi = Ui.field("3"), peringatan = Ui.field("5");

    public HalamanPengaturan(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        MainFrame.padding(this);
        add(MainFrame.kepala("Pengaturan", "Identitas rental, aturan tarif, keamanan akun, dan cadangan data."), BorderLayout.NORTH);

        JPanel isi = Ui.transparan(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH; c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0; c.gridy = 0; c.weightx = 0.55; c.gridheight = 2; c.insets = new Insets(0, 0, 0, 18);
        isi.add(kartuToko(), c);
        c.gridx = 1; c.gridheight = 1; c.weightx = 0.45; c.insets = new Insets(0, 0, 18, 0);
        isi.add(kartuPassword(), c);
        c.gridy = 1; c.insets = new Insets(0, 0, 0, 0);
        isi.add(kartuData(), c);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weighty = 1;
        isi.add(Box.createGlue(), c);
        add(isi, BorderLayout.CENTER);
    }

    private JPanel kartu(String judul, String ikon) {
        Ui.Kartu k1 = new Ui.Kartu(null);
        k1.setLayout(new BoxLayout(k1, BoxLayout.Y_AXIS));
        k1.setBorder(new EmptyBorder(20, 22, 22, 22));
        JLabel j = Ui.judul(judul, 17f);
        j.setIcon(Ikon.of(ikon, 20, Tema.LIME));
        j.setIconTextGap(10);
        j.setAlignmentX(0f);
        k1.add(j);
        k1.add(Ui.jarak(0, 16));
        return k1;
    }

    private void tambahBaris(JPanel p, String label, JComponent c) {
        JPanel b = Ui.baris(label, c);
        b.setAlignmentX(0f);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        p.add(b);
        p.add(Ui.jarak(0, 12));
    }

    private JPanel kartuToko() {
        JPanel p = kartu("Identitas & aturan tarif", "gamepad");
        tambahBaris(p, "Nama rental", nama);
        tambahBaris(p, "Alamat", alamat);
        tambahBaris(p, "Telepon", telp);
        tambahBaris(p, "Catatan kaki struk", catatan);
        JPanel jam = Ui.transparan(new GridLayout(1, 2, 10, 0));
        jam.add(Ui.baris("Jam buka", buka));
        jam.add(Ui.baris("Jam tutup (24 = tengah malam)", tutup));
        jam.setAlignmentX(0f);
        jam.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        p.add(jam);
        p.add(Ui.jarak(0, 12));
        JPanel aturan = Ui.transparan(new GridLayout(1, 4, 10, 0));
        aturan.add(Ui.baris("Blok (menit)", blok));
        aturan.add(Ui.baris("Minimal main", minimal));
        aturan.add(Ui.baris("Toleransi lebih", toleransi));
        aturan.add(Ui.baris("Peringatan", peringatan));
        aturan.setAlignmentX(0f);
        aturan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        p.add(aturan);
        p.add(Ui.jarak(0, 8));
        JLabel ket = Ui.teks("<html>Open billing dibulatkan ke atas per blok menit, minimal sekian menit. Paket yang lewat waktu lebih dari toleransi ditagih kelebihannya dengan tarif open.</html>", 12.5f, Tema.TEKS_2);
        ket.setAlignmentX(0f);
        p.add(ket);
        p.add(Ui.jarak(0, 12));
        boolean admin = frame.pengguna().admin();
        for (JComponent f : new JComponent[]{nama, alamat, telp, catatan, buka, tutup, blok, minimal, toleransi, peringatan}) f.setEnabled(admin);
        Ui.Tombol simpan = Ui.tombol("Simpan", "check", Ui.Jenis.UTAMA);
        simpan.setEnabled(admin);
        simpan.addActionListener(e -> Kotak.aman(this, () -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("nama_rental", nama.getText());
            m.put("alamat", alamat.getText());
            m.put("telepon", telp.getText());
            m.put("catatan_struk", catatan.getText());
            m.put("jam_buka", buka.getText());
            m.put("jam_tutup", tutup.getText());
            m.put("blok_menit", blok.getText());
            m.put("minimal_menit", minimal.getText());
            m.put("toleransi_menit", toleransi.getText());
            m.put("peringatan_menit", peringatan.getText());
            k.pengaturan.simpan(frame.aktor(), m);
            frame.kabar("Pengaturan disimpan");
        }));
        JPanel t = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 0, 4));
        t.add(simpan);
        if (!admin) t.add(Ui.teks("   Hanya admin yang bisa mengubah.", 13f, Tema.TEKS_2));
        t.setAlignmentX(0f);
        p.add(t);
        return p;
    }

    private JPanel kartuPassword() {
        JPanel p = kartu("Ganti password saya", "lock");
        JPasswordField lama = new JPasswordField(), baru = new JPasswordField(), ulang = new JPasswordField();
        for (JPasswordField f : new JPasswordField[]{lama, baru, ulang}) f.putClientProperty("JPasswordField.showRevealButton", true);
        tambahBaris(p, "Password lama", lama);
        tambahBaris(p, "Password baru (min. 6 karakter)", baru);
        tambahBaris(p, "Ulangi password baru", ulang);
        Ui.Tombol simpan = Ui.tombol("Ganti password", "check", Ui.Jenis.UTAMA);
        simpan.addActionListener(e -> Kotak.aman(this, () -> {
            String b = new String(baru.getPassword());
            if (!b.equals(new String(ulang.getPassword()))) throw new Gagal("Ulangi password baru belum sama.");
            k.auth.gantiPassword(frame.aktor(), new String(lama.getPassword()), b);
            lama.setText(""); baru.setText(""); ulang.setText("");
            frame.kabar("Password berhasil diganti");
        }));
        JPanel t = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 0, 4));
        t.add(simpan);
        t.setAlignmentX(0f);
        p.add(t);
        return p;
    }

    private JPanel kartuData() {
        JPanel p = kartu("Cadangan data", "database");
        JLabel info = Ui.teks("<html>Semua data tersimpan di satu file SQLite. Cadangkan rutin ke flashdisk atau Google Drive.</html>", 13.5f, Tema.TEKS_2);
        info.setAlignmentX(0f);
        p.add(info);
        p.add(Ui.jarak(0, 14));
        Ui.Tombol cadang = Ui.tombol("Cadangkan sekarang", "download", Ui.Jenis.INFO);
        cadang.setEnabled(frame.pengguna().admin());
        cadang.addActionListener(e -> cadangkan());
        JPanel t = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 0, 0));
        t.add(cadang);
        t.setAlignmentX(0f);
        p.add(t);
        return p;
    }

    private void cadangkan() {
        JFileChooser fc = new JFileChooser();
        String stamp = LocalDateTime.now(k.jam).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
        fc.setSelectedFile(new File("cadangan-rental-ps-" + stamp + ".db"));
        fc.setFileFilter(new FileNameExtensionFilter("Database SQLite", "db"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        Kotak.aman(this, () -> {
            AuthService.wajibAdmin(frame.aktor());
            if (f.exists()) throw new Gagal("File sudah ada, pilih nama lain.");
            k.db.cadangkan(f);
            frame.kabar("Cadangan tersimpan: " + f.getName());
        });
    }

    @Override
    public void muat() {
        nama.setText(k.pengaturan.get("nama_rental"));
        alamat.setText(k.pengaturan.get("alamat"));
        telp.setText(k.pengaturan.get("telepon"));
        catatan.setText(k.pengaturan.get("catatan_struk"));
        buka.setText(k.pengaturan.get("jam_buka"));
        tutup.setText(k.pengaturan.get("jam_tutup"));
        blok.setText(k.pengaturan.get("blok_menit"));
        minimal.setText(k.pengaturan.get("minimal_menit"));
        toleransi.setText(k.pengaturan.get("toleransi_menit"));
        peringatan.setText(k.pengaturan.get("peringatan_menit"));
    }
}
