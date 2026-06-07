// ==========================================
// 1. JAVA STANDARD BIBLIOTHEKEN
// ==========================================
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

// ==========================================
// 2. MICROSOFT WORD (.docx) - APACHE POI
// ==========================================
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

// ==========================================
// 3. LIBREOFFICE (.odt) - ODF TOOLKIT (v0.12.0)
// ==========================================
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.incubator.search.TextNavigation;
import org.odftoolkit.odfdom.incubator.search.TextSelection;

public class LetterGenerator {

    /**
     * MODUS 1: Generiert einen reinen Text-Brief (wie gehabt)
     */
    public static String generateTextLetter(Address address, String date, String subject, String bodyTemplate) {
        String addressBlock = address.getFirstName() + " " + address.getLastName() + "\n"
                + address.getStreet() + "\n"
                + address.getZipCode() + " " + address.getCity();

        String template = "An:\n" + addressBlock + "\n\n"
                + "Datum: " + date + "\n\n"
                + "Betreff: " + subject + "\n\n"
                + "Sehr geehrte(r) Herr/Frau " + address.getLastName() + ",\n\n"
                + bodyTemplate;

        // Platzhalter ersetzen
        String finalLetter = template;
        finalLetter = finalLetter.replace("{VORNAME}", address.getFirstName());
        finalLetter = finalLetter.replace("{NACHNAME}", address.getLastName());
        finalLetter = finalLetter.replace("{STADT}", address.getCity());

        return finalLetter;
    }

    /**
     * MODUS 2: Liest eine Word-Vorlage, ersetzt Platzhalter und speichert sie neu ab
     */
    public static void generateWordLetter(String templatePath, String outputPath, Map<String, String> replacements) throws IOException {
        try (FileInputStream fis = new FileInputStream(templatePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            // Absätze durchsuchen
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                replaceInParagraph(paragraph, replacements);
            }

            // Tabellen durchsuchen
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            replaceInParagraph(paragraph, replacements);
                        }
                    }
                }
            }

            // Ausgabe schreiben
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                document.write(fos);
            }
        }
    }

    private static void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> replacements) {
        if (paragraph.getRuns() != null) {
            for (XWPFRun run : paragraph.getRuns()) {
                String text = run.getText(0);
                if (text != null) {
                    for (Map.Entry<String, String> entry : replacements.entrySet()) {
                        if (text.contains(entry.getKey())) {
                            text = text.replace(entry.getKey(), entry.getValue());
                            run.setText(text, 0);
                        }
                    }
                }
            }
        }
    }

    /**
     * MODUS 3: Liest eine LibreOffice-Vorlage (.odt), ersetzt Platzhalter und speichert sie neu ab.
     */
    public static void generateOdtLetter(String templatePath, String outputPath, Map<String, String> replacements) throws Exception {
        // 1. ODT-Dokument laden
        OdfTextDocument document = OdfTextDocument.loadDocument(templatePath);

        // 2. Jedes Element aus der Ersetzungs-Map durchgehen
        for (Map.Entry<String, String> entry : replacements.entrySet()) {

            // TextNavigation initialisieren
            TextNavigation search = new TextNavigation(entry.getKey(), document);

            // Da 'search' ein Iterator ist, nutzen wir hasNext() und next()
            while (search.hasNext()) {
                TextSelection match = search.next(); // <-- HIER: Einfach nur .next() nutzen!
                match.replaceWith(entry.getValue());
            }
        }

        // 3. Das geänderte Dokument am Zielort speichern
        document.save(new File(outputPath));
        document.close();
    }

}