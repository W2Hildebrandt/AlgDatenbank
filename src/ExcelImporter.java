import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ExcelImporter {

    public static void importMitglieder(String excelPfad) {
        MitgliedDAO mitgliedDAO = new MitgliedDAO();

        try (FileInputStream fis = new FileInputStream(new File(excelPfad));
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Das erste Tabellenblatt auswählen
            Sheet sheet = workbook.getSheetAt(0);

            // Schleife über alle Zeilen, Übersprung der ersten Zeile (Index 0 = Header)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue; // Leere Zeilen überspringen

                try {
                    // Spalten exakt nach deiner Reihenfolge auslesen
                    int mNr          = (int) getNumericCellValue(row.getCell(0));
                    String eintritt  = getCellValueAsString(row.getCell(1));
                    String austritt  = getCellValueAsString(row.getCell(2));
                    String anrede    = getCellValueAsString(row.getCell(3));
                    String titel     = getCellValueAsString(row.getCell(4));
                    String nachname  = getCellValueAsString(row.getCell(5));
                    String vorname   = getCellValueAsString(row.getCell(6));
                    String strasse   = getCellValueAsString(row.getCell(7));
                    String plz       = getCellValueAsString(row.getCell(8));
                    String ort       = getCellValueAsString(row.getCell(9));
                    String telefon   = getCellValueAsString(row.getCell(10));
                    String email     = getCellValueAsString(row.getCell(11));
                    String hinweise  = getCellValueAsString(row.getCell(12));

                    // Wenn der Nachname leer ist, überspringen wir den Datensatz
                    if (nachname.isEmpty()) continue;

                    // Mitglied-Objekt erstellen
                    Mitglied mitglied = new Mitglied(mNr, eintritt, austritt, anrede, titel,
                            nachname, vorname, strasse, plz,
                            ort, telefon, email, hinweise);

                    // In die SQLite-Datenbank schreiben
                    mitgliedDAO.save(mitglied);

                } catch (Exception e) {
                    System.err.println("Fehler beim Einlesen der Zeile " + (i + 1) + ": " + e.getMessage());
                }
            }

            System.out.println("Import der Excel-Daten erfolgreich abgeschlossen!");

        } catch (IOException e) {
            System.err.println("Excel-Datei konnte nicht geöffnet werden: " + e.getMessage());
        }
    }

    // Hilfsmethode: Liefert den Inhalt einer Zelle immer verlässlich als String
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Verhindert, dass Postleitzahlen wie 30419 als "30419.0" eingelesen werden
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Einfache Konvertierung für Datumsfelder (z.B. YYYY-MM-DD)
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                long numericValue = (long) cell.getNumericCellValue();
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    // Hilfsmethode: Speziell für die Mitgliedsnummer
    private static double getNumericCellValue(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}