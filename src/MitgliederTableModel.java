import javax.swing.table.AbstractTableModel;
import java.util.List;

public class MitgliederTableModel extends AbstractTableModel {

    private final List<Mitglied> mitglieder;

    // 1. Alle 13 Felder als Spaltenüberschriften definieren
    private final String[] spaltenNamen = {
            "MNr", "Eintritt", "Austritt", "Anrede", "Titel",
            "Nachname", "Vorname", "Straße", "PLZ", "Ort",
            "Telefon", "E-Mail", "Hinweise"
    };

    public MitgliederTableModel(List<Mitglied> mitglieder) {
        this.mitglieder = mitglieder;
    }

    @Override
    public int getRowCount() {
        return mitglieder.size();
    }

    @Override
    public int getColumnCount() {
        return spaltenNamen.length;
    }

    @Override
    public String getColumnName(int column) {
        return spaltenNamen[column];
    }

    // 2. Alle 13 Felder den entsprechenden Spalten-Indizes (0 bis 12) zuordnen
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Mitglied m = mitglieder.get(rowIndex);
        switch (columnIndex) {
            case 0:
                // Hier wird jetzt einfach die Tabellenzeile (0-basiert) + 1 zurückgegeben
                return (rowIndex + 1);
                //return m.getMNr();
            case 1:  return m.getEintritt();
            case 2:  return m.getAustritt();
            case 3:  return m.getAnrede();
            case 4:  return m.getTitel();
            case 5:  return m.getNachname();
            case 6:  return m.getVorname();
            case 7:  return m.getStrasse();
            case 8:  return m.getPostleitzahl();
            case 9:  return m.getOrt();
            case 10: return m.getTelefon();
            case 11: return m.getEmail();
            case 12: return m.getHinweise();
            default: return null;
        }
    }

    public Mitglied getMitgliedAt(int rowIndex) {
        return mitglieder.get(rowIndex);
    }

    public void setListe(List<Mitglied> neueListe) {
        this.mitglieder.clear();          // 1. Die alte Liste komplett leeren
        if (neueListe != null) {
            this.mitglieder.addAll(neueListe); // 2. Alle neuen Mitglieder hinzufügen
        }
        fireTableDataChanged();           // 3. JTable Bescheid geben, dass sich die Daten geändert haben
    }
}