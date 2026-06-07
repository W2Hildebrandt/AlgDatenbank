import java.awt.*;
import java.awt.print.*;

public class BriefDrucker implements Printable {

    private Mitglied mitglied;
    private String briefText;

    public BriefDrucker(Mitglied mitglied, String roherBriefText) {
        this.mitglied = mitglied;
        // Platzhalter im Freitext automatisch durch echte Daten ersetzen!
        this.briefText = roherBriefText
                .replace("[MNr]", String.valueOf(mitglied.getMNr()))
                .replace("[Eintritt]", mitglied.getEintritt())
                .replace("[Austritt]", mitglied.getAustritt() != null ? mitglied.getAustritt() : "");
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) return NO_SUCH_PAGE;

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        Font standardFont = new Font("Arial", Font.PLAIN, 11);
        g2d.setFont(standardFont);

        // 1. Adresse im Sichtfenster (wie gehabt)
        int x = 72;
        int y = 120;
        String nameZeile = (mitglied.getTitel().isEmpty() ? "" : mitglied.getTitel() + " ") + mitglied.getVorname() + " " + mitglied.getNachname();
        g2d.drawString(mitglied.getAnrede(), x, y); y += 15;
        g2d.drawString(nameZeile, x, y); y += 15;
        g2d.drawString(mitglied.getStrasse(), x, y); y += 15;
        g2d.drawString(mitglied.getPostleitzahl() + " " + mitglied.getOrt(), x, y);

        // 2. Datum
        g2d.drawString("Hannover, den " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")), 400, 220);

        // 3. Dynamischer Brieftext (Zeilenweise gezeichnet)
        y = 300; // Starthöhe des Brieftexts
        String begruessung = (mitglied.getAnrede().equals("Frau") ? "Sehr geehrte Frau " : "Sehr geehrter Herr ") + mitglied.getNachname() + ",\n\n";

        // Wir fügen die Begrüßung vor den eingegebenen Text
        String gesamterText = begruessung + briefText + "\n\nMit freundlichen Grüßen,\nDer Vorstand";

        // Den Text an den Zeilenumbrüchen (\n) spalten, damit Java ihn zeilenweise druckt
        for (String zeile : gesamterText.split("\n")) {
            g2d.drawString(zeile, x, y);
            y += 15; // 15 Pixel Abstand zur nächsten Zeile
        }

        return PAGE_EXISTS;
    }
}