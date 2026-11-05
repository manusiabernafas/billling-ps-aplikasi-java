package id.papicode.billingps;

import id.papicode.billingps.ui.LoginFrame;
import id.papicode.billingps.ui.Tema;

import javax.swing.*;
import java.io.File;
import java.time.Clock;

/**
 * Titik masuk aplikasi Billing Rental PS.
 * Lokasi database bisa diganti: java -Dbilling.db=D:/data/rental.db -jar billing-ps.jar
 */
public class App {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        File fileDb = new File(System.getProperty("billing.db", "data/billing-ps.db"));
        SwingUtilities.invokeLater(() -> {
            Tema.pasang();
            Konteks k;
            try {
                k = new Konteks(fileDb, Clock.systemDefaultZone());
                k.auth.pastikanAdminAwal();
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(null, "Database tidak bisa dibuka:\n" + e.getMessage(), "Billing PS", JOptionPane.ERROR_MESSAGE);
                return;
            }
            new LoginFrame(k).setVisible(true);
        });
    }
}
