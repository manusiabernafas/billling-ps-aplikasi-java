package id.papicode.billingps.ui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.geom.Path2D;
import java.io.InputStream;

/**
 * Tema "Arcade": latar ungu kehitaman, panel bersudut potong (chamfer) seperti kabinet arcade,
 * hijau limau untuk aksi utama, magenta untuk waktu habis, cyan untuk jeda. Semua warna solid, tanpa glow.
 * Font: Chakra Petch (teks) + VT323 (angka timer gaya layar piksel). Keduanya SIL OFL.
 */
public final class Tema {

    public static final Color LATAR = new Color(0x120E1C);
    public static final Color KARTU = new Color(0x1C1630);
    public static final Color KARTU_2 = new Color(0x231B3B);
    public static final Color GARIS = new Color(0x3A2E5C);
    public static final Color TEKS = new Color(0xF1ECFF);
    public static final Color TEKS_2 = new Color(0xA99CC9);
    public static final Color LIME = new Color(0xB6F23A);
    public static final Color LIME_HOVER = new Color(0xCBFA6A);
    public static final Color GELAP = new Color(0x0F0B18);
    public static final Color MAGENTA = new Color(0xFF4FA3);
    public static final Color CYAN = new Color(0x3DE0E8);
    public static final Color KUNING = new Color(0xFFD447);
    public static final Color UNGU = new Color(0x8B5CF6);
    public static final Color SIDEBAR = new Color(0x0D0A15);
    public static final Color PILIH = new Color(0x2E2450);

    public static Font F_REG, F_SEMI, F_BOLD, F_PIKSEL;

    private Tema() { }

    public static synchronized void muatFont() {
        if (F_REG != null) return;
        F_REG = muat("ChakraPetch-Regular.ttf", Font.SANS_SERIF);
        F_SEMI = muat("ChakraPetch-SemiBold.ttf", Font.SANS_SERIF);
        F_BOLD = muat("ChakraPetch-Bold.ttf", Font.SANS_SERIF);
        F_PIKSEL = muat("VT323-Regular.ttf", Font.MONOSPACED);
    }

    public static void pasang() {
        muatFont();
        FlatDarkLaf.setup();
        UIManager.put("defaultFont", new FontUIResource(F_REG.deriveFont(14f)));
        UIManager.put("Component.arc", 0);
        UIManager.put("Button.arc", 0);
        UIManager.put("TextComponent.arc", 0);
        UIManager.put("CheckBox.arc", 0);
        UIManager.put("Component.focusWidth", 0);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("Component.focusedBorderColor", LIME);
        UIManager.put("Component.borderColor", GARIS);
        UIManager.put("Component.accentColor", LIME);
        UIManager.put("Panel.background", LATAR);
        UIManager.put("RootPane.background", LATAR);
        for (String k : new String[]{"TextField", "PasswordField", "FormattedTextField", "ComboBox", "Spinner", "TextArea"}) {
            UIManager.put(k + ".background", new Color(0x17122A));
            UIManager.put(k + ".foreground", TEKS);
            UIManager.put(k + ".disabledBackground", KARTU);
            UIManager.put(k + ".inactiveBackground", KARTU);
        }
        UIManager.put("ComboBox.buttonBackground", new Color(0x17122A));
        UIManager.put("ComboBox.buttonArrowColor", LIME);
        UIManager.put("ComboBox.popupBackground", KARTU);
        UIManager.put("ComboBox.selectionBackground", PILIH);
        UIManager.put("List.background", KARTU);
        UIManager.put("List.selectionBackground", PILIH);
        UIManager.put("TextField.placeholderForeground", new Color(0x6E6290));
        UIManager.put("TextField.margin", new Insets(6, 10, 6, 10));
        UIManager.put("PasswordField.margin", new Insets(6, 10, 6, 10));
        UIManager.put("Table.background", KARTU);
        UIManager.put("Table.foreground", TEKS);
        UIManager.put("Table.alternateRowColor", KARTU_2);
        UIManager.put("Table.selectionBackground", PILIH);
        UIManager.put("Table.selectionForeground", TEKS);
        UIManager.put("Table.selectionInactiveBackground", PILIH);
        UIManager.put("Table.selectionInactiveForeground", TEKS);
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.cellMargins", new Insets(4, 12, 4, 12));
        UIManager.put("TableHeader.background", KARTU);
        UIManager.put("TableHeader.foreground", LIME);
        UIManager.put("TableHeader.separatorColor", KARTU);
        UIManager.put("TableHeader.bottomSeparatorColor", GARIS);
        UIManager.put("TableHeader.height", 36);
        UIManager.put("TableHeader.font", F_BOLD.deriveFont(12f));
        UIManager.put("TableHeader.cellMargins", new Insets(4, 12, 4, 12));
        UIManager.put("ScrollBar.thumbArc", 0);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.track", KARTU);
        UIManager.put("ScrollBar.thumb", GARIS);
        UIManager.put("ScrollBar.hoverThumbColor", UNGU);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollPane.background", KARTU);
        UIManager.put("Viewport.background", KARTU);
        UIManager.put("CheckBox.icon.checkmarkColor", GELAP);
        UIManager.put("CheckBox.icon.selectedBackground", LIME);
        UIManager.put("CheckBox.icon.selectedBorderColor", LIME);
        UIManager.put("CheckBox.icon.borderColor", TEKS_2);
        UIManager.put("CheckBox.icon.background", KARTU);
        UIManager.put("ToolTip.background", LIME);
        UIManager.put("ToolTip.foreground", GELAP);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(5, 9, 5, 9));
        UIManager.put("Spinner.buttonStyle", "none");
        UIManager.put("PopupMenu.background", KARTU);
        UIManager.put("Label.foreground", TEKS);
        UIManager.put("CheckBox.foreground", TEKS);
        UIManager.put("Button.background", KARTU);
        UIManager.put("Button.foreground", TEKS);
        UIManager.put("TabbedPane.background", LATAR);
        UIManager.put("TabbedPane.hoverColor", KARTU_2);
        UIManager.put("TabbedPane.focusColor", KARTU);
        UIManager.put("TabbedPane.contentAreaColor", GARIS);
        UIManager.put("TabbedPane.selectedForeground", LIME);
        UIManager.put("TabbedPane.foreground", TEKS_2);
        UIManager.put("TabbedPane.selectedBackground", KARTU);
        UIManager.put("TabbedPane.underlineColor", LIME);
        UIManager.put("TabbedPane.inactiveUnderlineColor", LIME);
        UIManager.put("TabbedPane.tabHeight", 40);
        UIManager.put("TabbedPane.font", F_BOLD.deriveFont(13.5f));
    }

    private static Font muat(String file, String cadangan) {
        try (InputStream in = Tema.class.getResourceAsStream("/fonts/" + file)) {
            Font f = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            return f.deriveFont(14f);
        } catch (Exception e) {
            return new Font(cadangan, Font.PLAIN, 14);
        }
    }

    public static Font reg(float s) { muatFont(); return F_REG.deriveFont(s); }
    public static Font semi(float s) { muatFont(); return F_SEMI.deriveFont(s); }
    public static Font bold(float s) { muatFont(); return F_BOLD.deriveFont(s); }
    public static Font piksel(float s) { muatFont(); return F_PIKSEL.deriveFont(s); }

    /** Bentuk persegi dengan sudut kanan-atas dan kiri-bawah terpotong (ciri khas tema arcade). */
    public static Shape chamfer(double x, double y, double w, double h, double c) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(x + w - c, y);
        p.lineTo(x + w, y + c);
        p.lineTo(x + w, y + h);
        p.lineTo(x + c, y + h);
        p.lineTo(x, y + h - c);
        p.closePath();
        return p;
    }
}
