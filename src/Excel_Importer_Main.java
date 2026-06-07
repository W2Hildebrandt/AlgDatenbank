public class Excel_Importer_Main {
    public static void main(String[] args) {
        // 1. Datenbank-Datei erzeugen und Tabelle anlegen
        DatabaseManager.initializeDatabase();

        // 2. Pfad zur Excel-Datei angeben
        // Liegt die Datei im Hauptverzeichnis deines Projekts, reicht der Dateiname
        String excelPfad = "/home/wolfram/JB-Java_Projekte/Databanken/ALGData_Mitglieder.xlsx";

        System.out.println("Starte Datenimport aus " + excelPfad + "...");

        // 3. Import ausführen
        ExcelImporter.importMitglieder(excelPfad);
    }
}