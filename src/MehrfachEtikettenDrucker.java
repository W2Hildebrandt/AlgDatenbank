import java.awt.*;
import java.awt.print.*;
import java.util.List;

public class MehrfachEtikettenDrucker implements Printable {

    private List<Mitglied> mitgliederListe;
    private int startPosition; // 1 bis 24 (wo auf der ALLERERSTEN Seite begonnen wird)

    // Maße für Avery L7161 (3x7 oder 3x8 Bogen)
    private final int ETIKETT_BREITE = 198;
    private final int ETIKETT_HOEHE = 105;
    private final int RAND_LINKS = 20;
    private final int RAND_OBEN = 30;
    private final int SPALTEN = 3;
    private final int ZEILEN_PRO_SEITE = 7; // Bei L7161 oft 7 Zeilen (21 Etiketten), bei Bedarf auf 8 ändern
    private final int ETIKETTEN_PRO_SEITE = SPALTEN * ZEILEN_PRO_SEITE;

    public MehrfachEtikettenDrucker(List<Mitglied> mitgliederListe, int startPosition) {
        this.mitgliederListe = mitgliederListe;
        this.startPosition = startPosition;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {

        // Berechnen, welche Etiketten auf die aktuelle Seite gehören
        // Auf Seite 0 starten wir bei (startPosition - 1), auf Folgeseiten immer ganz oben (0)
        int startIndexFuerDieseSeite = (pageIndex == 0) ? (startPosition - 1) : 0;

        // Wie viele Etikettenplätze wurden auf den vorherigen Seiten bereits "verbraucht"?
        int bereitsGedruckteMitglieder = 0;
        if (pageIndex > 0) {
            // Erste Seite hatte weniger Platz wegen der Startposition:
            int plaetzeErsteSeite = ETIKETTEN_PRO_SEITE - (startPosition - 1);
            bereitsGedruckteMitglieder = plaetzeErsteSeite + (pageIndex - 1) * ETIKETTEN_PRO_SEITE;
        }

        // Wenn wir schon alle Mitglieder gedruckt haben, ist der Druckjob fertig
        if (bereitsGedruckteMitglieder >= mitgliederListe.size()) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));

        // Wir laufen durch alle verfügbaren Plätze auf der AKTUELLEN Seite
        for (int slot = startIndexFuerDieseSeite; slot < ETIKETTEN_PRO_SEITE; slot++) {

            // Welches Mitglied aus der Liste ist jetzt dran?
            int mitgliedListenIndex = bereitsGedruckteMitglieder;
            if (pageIndex == 0) {
                mitgliedListenIndex = slot - (startPosition - 1);
            }

            // Wenn keine Mitglieder mehr in der Auswahlliste sind, brechen wir die Schleife ab
            if (mitgliedListenIndex >= mitgliederListe.size() || mitgliedListenIndex < 0) {
                break;
            }

            Mitglied mitglied = mitgliederListe.get(mitgliedListenIndex);

            // Spalte und Zeile NUR für die optische Positionierung auf dem aktuellen Blatt berechnen
            int spalte = slot % SPALTEN;
            int zeile = slot / SPALTEN;

            int posX = RAND_LINKS + (spalte * ETIKETT_BREITE);
            int posY = RAND_OBEN + (zeile * ETIKETT_HOEHE);

            int textX = posX + 15;
            int textY = posY + 25;

            // Adresse zeichnen (inklusive deiner neuen Leerzeile!)
            String nameZeile = (mitglied.getTitel().isEmpty() ? "" : mitglied.getTitel() + " ")
                    + mitglied.getVorname() + " " + mitglied.getNachname();

            g2d.drawString(mitglied.getAnrede(), textX, textY);
            textY += 14;
            g2d.drawString(nameZeile, textX, textY);
            textY += 14;
            g2d.drawString(mitglied.getStrasse(), textX, textY);
            textY += 28; // Deine gewünschte Leerzeile
            g2d.drawString(mitglied.getPostleitzahl() + " " + mitglied.getOrt(), textX, textY);

            // Zähler für die nächste Iteration / nächste Seite hochzählen
            bereitsGedruckteMitglieder++;
        }

        return PAGE_EXISTS;
    }
}