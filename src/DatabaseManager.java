import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // Name der Datenbankdatei im Projektverzeichnis
    private static final String DB_URL = "jdbc:sqlite:/home/wolfram/JB-Java_Projekte/Databanken/alg_databank.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS mitglieder ("
                + "  MNr INTEGER PRIMARY KEY,"
                + "  Eintritt TEXT,"
                + "  Austritt TEXT,"
                + "  Anrede TEXT,"
                + "  Titel TEXT,"
                + "  Nachname TEXT NOT NULL,"
                + "  Vorname TEXT,"
                + "  Straße TEXT,"
                + "  Postleitzahl TEXT NOT NULL,"
                + "  Ort TEXT NOT NULL,"
                + "  Telefon TEXT,"
                + "  Email TEXT,"
                + "  Hinweise TEXT"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Tabelle erstellen, falls nicht vorhanden
            stmt.execute(sql);
            System.out.println("Datenbank erfolgreich initialisiert.");

        } catch (SQLException e) {
            System.err.println("Fehler bei der Datenbank-Initialisierung: " + e.getMessage());
        }
    }
}