import java.awt.*;
import java.awt.print.*;

public class UmschlagDrucker implements Printable {

    private Mitglied m;

    public UmschlagDrucker(Mitglied m) {
        this.m = m;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        // Verschiebung zum bedruckbaren Bereich
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2d.setColor(Color.BLACK);

        // 1. ABSENDER (Optional - links oben, klein)
        g2d.setFont(new Font("Arial", Font.PLAIN, 8));
        g2d.drawString("Mein Verein e.V. - Musterstraße 1 - 12345 Musterstadt", 10, 20);

        // Dezente Trennlinie unter dem Absender
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawLine(10, 25, 250, 25);
        g2d.setColor(Color.BLACK);

        // 2. EMPFÄNGER (Rechts eingerückt und mittig platziert)
        // Bei LANDSCAPE-Ausrichtung verschieben wir die X-Koordinate nach rechts
        int empfaengerX = 280;
        int y = 90;

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));

        // Anrede & Titel
        String anredeTitel = m.getAnrede() + (m.getTitel() != null && !m.getTitel().isEmpty() ? " " + m.getTitel() : "");
        g2d.drawString(anredeTitel, empfaengerX, y);
        y += 18;

        // Vorname & Nachname
        g2d.drawString(m.getVorname() + " " + m.getNachname(), empfaengerX, y);
        y += 18;

        // Straße
        g2d.drawString(m.getStrasse(), empfaengerX, y);
        y += 18;

        // PLZ & Ort
        g2d.setFont(new Font("Arial", Font.BOLD, 12)); // PLZ/Ort fett für bessere Lesbarkeit der Post-Scanner
        g2d.drawString(m.getPostleitzahl() + " " + m.getOrt(), empfaengerX, y);

        return PAGE_EXISTS;
    }
}