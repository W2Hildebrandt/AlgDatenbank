import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test-Programm zur Überprüfung des Druck-Outputs
 */
public class TestDruckOutput {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Druck-Output Test ===\n");

        // Lade das Test-HTML-Template
        String htmlTemplate = Files.readString(Path.of("src/main/resources/templates/algorithmus.html"));
        System.out.println("Template geladen: " + htmlTemplate.length() + " Zeichen\n");

        // Erstelle einen HtmlDrucker
        HtmlDrucker drucker = new HtmlDrucker(htmlTemplate);

        // Simuliere Seitengröße (A4: 210mm x 297mm, bei 72 DPI = ca 595x842 px)
        java.awt.print.PageFormat pageFormat = new java.awt.print.PageFormat();
        pageFormat.setOrientation(java.awt.print.PageFormat.PORTRAIT);
        pageFormat.setPaper(new java.awt.print.Paper());

        System.out.println("Test: Druck-Rendering wird eingeleitet...");
        System.out.println("Dies kann einige Sekunden dauern (JavaFX-Initialization)...\n");

        // Der Druck wird durchgeführt, was die Rendering-Logik auslöst
        java.awt.print.PrinterJob printerJob = java.awt.print.PrinterJob.getPrinterJob();
        printerJob.setPrintable(drucker, pageFormat);

        // Nur einfach das print() aufrufen, um die Debug-Ausgaben zu sehen
        java.awt.Graphics dummyGraphics = new java.awt.image.BufferedImage(595, 842, java.awt.image.BufferedImage.TYPE_INT_RGB).getGraphics();
        try {
            int result = drucker.print(dummyGraphics, pageFormat, 0);
            System.out.println("Druck-Ergebnis: " + (result == java.awt.print.Printable.PAGE_EXISTS ? "PAGE_EXISTS" : "NO_SUCH_PAGE"));
        } catch (Exception ex) {
            System.err.println("Fehler beim Drucken: " + ex.getMessage());
            ex.printStackTrace();
        }

        // Prüfe die Vorschau-Datei
        Path preview = Path.of("/tmp/alg_brief_preview.png");
        if (Files.exists(preview)) {
            long size = Files.size(preview);
            System.out.println("\nVorschau-Datei erstellt: " + preview + " (" + size + " bytes)");
        } else {
            System.out.println("\nVorschau-Datei nicht erstellt!");
        }

        System.out.println("\nTest abgeschlossen. Überprüfe die Console-Ausgaben oben für DEBUG-Informationen.");
    }
}

