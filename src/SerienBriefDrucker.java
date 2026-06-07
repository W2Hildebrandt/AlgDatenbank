import java.awt.*;
import java.awt.print.*;
import java.util.List;

public class SerienBriefDrucker implements Printable {

    private List<Mitglied> mitglieder;
    private String roherText;

    public SerienBriefDrucker(List<Mitglied> mitglieder, String roherText) {
        this.mitglieder = mitglieder;
        this.roherText = roherText;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        // Wenn der pageIndex größer ist als die Anzahl der ausgewählten Mitglieder, ist der Druck fertig
        if (pageIndex >= mitglieder.size()) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2d.setColor(Color.BLACK);

        // Das aktuelle Mitglied für DIESE Seite holen
        Mitglied m = mitglieder.get(pageIndex);

        // Platzhalter im Text für dieses spezifische Mitglied ersetzen
        String individualisierterText = ersetzePlatzhalter(roherText, m);

        // --- HIER FOLGT DEIN DIN 5008 LAYOUT ---
        // (Hinweis: Passe die Koordinaten und Felder an deine bestehende BriefDrucker-Logik an)

        // 1. Anschriftenfeld (DIN 5008 Zone)
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        int y = 120; // Start-Höhe für die Adresse
        g2d.drawString(m.getAnrede() + " " + (m.getTitel() != null ? m.getTitel() : ""), 70, y);
        y += 15;
        g2d.drawString(m.getVorname() + " " + m.getNachname(), 70, y);
        y += 15;
        g2d.drawString(m.getStrasse(), 70, y);
        y += 15;
        g2d.drawString(m.getPostleitzahl() + " " + m.getOrt(), 70, y);

        // 2. Betreffzeile & Datum
        y = 220;
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Mitglieder-Schreiben", 70, y);

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("Datum: 05.06.2026", 450, y); // Oder dynamisches Datum

        // 3. Der Brieftext (Zeilenweise zeichnen)
        y = 280;
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));

        // Text an den Zeilenumbrüchen spalten
        String[] zeilen = individualisierterText.split("\n");
        for (String zeile : zeilen) {
            // Optionale Zeilenumbruch-Logik für sehr lange Zeilen (Line-Wrap) hier einfügen,
            // oder darauf vertrauen, dass im GUI-Feld bereits sinnvoll umgebrochen wurde.
            g2d.drawString(zeile, 70, y);
            y += 15; // Zeilenabstand

            // Falls der Text zu lang für eine Seite wird:
            if (y > pageFormat.getImageableHeight() - 50) {
                break;
            }
        }

        return PAGE_EXISTS;
    }

    // Hilfsmethode, um die Platzhalter im Text durch die echten Mitgliedsdaten zu ersetzen
    private String ersetzePlatzhalter(String text, Mitglied m) {
        if (text == null) return "";

        return text
                .replace("[MNr]", String.valueOf(m.getMNr()))
                .replace("[Eintritt]", m.getEintritt() != null ? m.getEintritt() : "")
                .replace("[Austritt]", m.getAustritt() != null ? m.getAustritt() : "")
                .replace("[Anrede]", m.getAnrede() != null ? m.getAnrede() : "")
                .replace("[Titel]", m.getTitel() != null ? m.getTitel() : "")
                .replace("[Nachname]", m.getNachname() != null ? m.getNachname() : "")
                .replace("[Vorname]", m.getVorname() != null ? m.getVorname() : "")
                .replace("[Strasse]", m.getStrasse() != null ? m.getStrasse() : "")
                .replace("[PLZ]", m.getPostleitzahl() != null ? m.getPostleitzahl() : "")
                .replace("[Ort]", m.getOrt() != null ? m.getOrt() : "");
    }
}