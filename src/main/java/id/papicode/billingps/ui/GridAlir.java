package id.papicode.billingps.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Panel grid yang jumlah kolomnya menyesuaikan lebar (seperti CSS grid auto-fill).
 * Implementasi Scrollable supaya lebar mengikuti viewport dan tinggi bertambah ke bawah.
 */
public class GridAlir extends JPanel implements Scrollable {

    private final int minLebar, tinggi, celah;

    public GridAlir(int minLebar, int tinggi, int celah) {
        this.minLebar = minLebar;
        this.tinggi = tinggi;
        this.celah = celah;
        setOpaque(false);
        setLayout(new LayoutManager() {
            @Override public void addLayoutComponent(String n, Component c) { }
            @Override public void removeLayoutComponent(Component c) { }
            @Override public Dimension preferredLayoutSize(Container p) { return ukuran(p.getWidth()); }
            @Override public Dimension minimumLayoutSize(Container p) { return new Dimension(minLebar, tinggi); }
            @Override public void layoutContainer(Container p) {
                Insets in = p.getInsets();
                int w = p.getWidth() - in.left - in.right;
                int kol = kolom(w);
                int lebar = (w - celah * (kol - 1)) / kol;
                int n = p.getComponentCount();
                for (int i = 0; i < n; i++) {
                    int r = i / kol, c = i % kol;
                    p.getComponent(i).setBounds(in.left + c * (lebar + celah), in.top + r * (tinggi + celah), lebar, tinggi);
                }
            }
        });
    }

    private int kolom(int w) { return Math.max(1, (w + celah) / (minLebar + celah)); }

    private Dimension ukuran(int w) {
        Insets in = getInsets();
        int lebar = Math.max(w, minLebar) - in.left - in.right;
        int kol = kolom(lebar);
        int baris = (getComponentCount() + kol - 1) / kol;
        return new Dimension(lebar, in.top + in.bottom + Math.max(0, baris * tinggi + (baris - 1) * celah));
    }

    @Override public Dimension getPreferredSize() {
        int w = getParent() instanceof JViewport vp ? vp.getWidth() : getWidth();
        return ukuran(w);
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 40; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
