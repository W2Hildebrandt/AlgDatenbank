import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement; // oben hinzufügen falls noch nicht vorhanden

public class MitgliedDAO {

    public void save(Mitglied mitglied) {
        String sql = "INSERT OR REPLACE INTO mitglieder (MNr, Eintritt, Austritt, Anrede, Titel, "
                + "Nachname, Vorname, Straße, Postleitzahl, Ort, Telefon, Email, Hinweise) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Parameter an das SQL-Statement übergeben
            pstmt.setInt(1, mitglied.getMNr());
            pstmt.setString(2, mitglied.getEintritt());
            pstmt.setString(3, mitglied.getAustritt());
            pstmt.setString(4, mitglied.getAnrede());
            pstmt.setString(5, mitglied.getTitel());
            pstmt.setString(6, mitglied.getNachname());
            pstmt.setString(7, mitglied.getVorname());
            pstmt.setString(8, mitglied.getStrasse());
            pstmt.setString(9, mitglied.getPostleitzahl());
            pstmt.setString(10, mitglied.getOrt());
            pstmt.setString(11, mitglied.getTelefon());
            pstmt.setString(12, mitglied.getEmail());
            pstmt.setString(13, mitglied.getHinweise());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Fehler beim Speichern des Mitglieds (MNr: "
                    + mitglied.getMNr() + "): " + e.getMessage());
        }
    }

    public List<Mitglied> readAll() {
        List<Mitglied> liste = new ArrayList<>();
        String sql = "SELECT * FROM mitglieder ORDER BY Nachname, Vorname";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Mitglied m = new Mitglied(
                        rs.getInt("MNr"),
                        rs.getString("Eintritt"),
                        rs.getString("Austritt"),
                        rs.getString("Anrede"),
                        rs.getString("Titel"),
                        rs.getString("Nachname"),
                        rs.getString("Vorname"),
                        rs.getString("Straße"),
                        rs.getString("Postleitzahl"),
                        rs.getString("Ort"),
                        rs.getString("Telefon"),
                        rs.getString("Email"),
                        rs.getString("Hinweise")
                );
                liste.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der Mitglieder: " + e.getMessage());
        }
        return liste;
    }



// ... (deine bestehenden Methoden save und readAll) ...

    public void delete(int mNr) {
        String sql = "DELETE FROM mitglieder WHERE MNr = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, mNr);
            pstmt.executeUpdate();
            System.out.println("Mitglied mit MNr " + mNr + " gelöscht.");

        } catch (SQLException e) {
            System.err.println("Fehler beim Löschen des Mitglieds: " + e.getMessage());
        }
    }

    public void partitioniereMitgliedsnummernNeu() {
        // 1. Alle Mitglieder holen (sind bereits nach Name sortiert)
        List<Mitglied> alle = readAll();

        String sqlUpdate = "UPDATE mitglieder SET MNr = ? WHERE MNr = ?";
        String sqlDeleteOld = "DELETE FROM mitglieder WHERE MNr = ?";

        // Da wir Primärschlüssel ändern, nutzen wir eine Transaktion, damit nichts verloren geht
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            int neueNummer = 1;
            for (Mitglied m : alle) {
                int alteNummer = m.getMNr();

                // Wenn die Nummer schon stimmt, müssen wir nichts tun
                if (alteNummer == neueNummer) {
                    neueNummer++;
                    continue;
                }

                // Da MNr ein Primärschlüssel (INSERT OR REPLACE) ist, löschen wir sicherheitshalber
                // den alten Eintrag, nachdem wir den neuen mit der fortlaufenden Nummer eingefügt haben.
                // Alternativ kopieren wir das Mitglied mit save() unter der neuen Nummer:
                m.setMNr(neueNummer);

                // Alten Eintrag löschen
                try (PreparedStatement delStmt = conn.prepareStatement(sqlDeleteOld)) {
                    delStmt.setInt(1, alteNummer);
                    delStmt.executeUpdate();
                }

                conn.commit(); // Löschen bestätigen

                // Neuen Eintrag schreiben via bestehender save-Methode
                save(m);

                neueNummer++;
            }
            System.out.println("Mitgliedsnummern wurden erfolgreich von 1 bis " + (neueNummer - 1) + " neu vergeben.");
        } catch (SQLException e) {
            System.err.println("Fehler bei der Neunummerierung: " + e.getMessage());
        }
    }


}