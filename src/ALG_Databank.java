import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
//import org.apache.poi.xwpf.usermodel.*;
import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class ALG_Databank extends JFrame {

    // Datenliste für die Adressen
    private List<Address> addressList;
    private DefaultListModel<Address> listModel;
    private static final String DATABASE_FILE = "adressen.dat";

    // GUI Komponenten
    private JList<Address> jAddressList;
    private JTextField tfFirstName, tfLastName, tfStreet, tfZip, tfCity, tfEmail;
    private JTextField tfSubject, tfDate;
    private JTextArea taLetterBody, taPreview;
    private JTextField tfWordPath; // Speichert den Pfad zur ausgewählten Word-Vorlage

    public ALG_Databank() {
        addressList = new ArrayList<>();
        listModel = new DefaultListModel<>();

        setTitle("ALG_Databank - Adressen & Briefe");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // GUI Struktur aufbauen
        initUI();

        // Testdaten hinzufügen
        loadAddressesFromFile();
    }

    private void initUI() {
        // Haupt-Layout: SplitPane teilt Links (Suche/Liste) und Rechts (Details/Briefe)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(300);

        // =========================================================================
        // --- LINKE SEITE: Suche, Adressliste & Verwaltung ---
        // =========================================================================
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));

        // 1. Das Suchfeld ganz oben links platzieren
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        JTextField tfSearch = new JTextField();
        searchPanel.add(new JLabel("Suchen:"), BorderLayout.WEST);
        searchPanel.add(tfSearch, BorderLayout.CENTER);
        leftPanel.add(searchPanel, BorderLayout.NORTH);

        // 2. Die Adressliste in der Mitte links platzieren
        jAddressList = new JList<>(listModel);
        leftPanel.add(new JScrollPane(jAddressList), BorderLayout.CENTER);

        // 3. Die Aktions-Buttons "Neu" und "Löschen" ganz unten links
        JPanel leftButtonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton btnAdd = new JButton("Neu");
        JButton btnDelete = new JButton("Löschen");

        btnAdd.addActionListener(e -> addNewAddress());
        btnDelete.addActionListener(e -> deleteSelectedAddress());

        leftButtonPanel.add(btnAdd);
        leftButtonPanel.add(btnDelete);
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);

        // =========================================================================
        // --- RECHTE SEITE: Registerkarten für Bearbeitung & Briefe ---
        // =========================================================================
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Formular zum Bearbeiten bestehender Kontakte
        tabbedPane.addTab("Adresse bearbeiten", createAddressForm());

        // Tab 2: Bereich zum Erstellen, Vorschauen und Drucken von Briefen
        tabbedPane.addTab("Brief erstellen", createLetterForm());

        // Beide Hälften in das Hauptfenster-SplitPane einhängen
        mainSplit.setLeftComponent(leftPanel);
        mainSplit.setRightComponent(tabbedPane);
        add(mainSplit);

        // =========================================================================
        // --- LISTENERS: Verknüpfung der Interaktionen ---
        // =========================================================================

        // Listener A: Wenn links ein Name angeklickt wird, Daten rechts in die Felder laden
        jAddressList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFieldsFromSelectedAddress();
            }
        });

        // Listener B: Echtzeit-Filterung bei der Eingabe im Suchfeld
        tfSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { triggerFilter(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { triggerFilter(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { triggerFilter(); }

            private void triggerFilter() {
                // Thread-Sicher im Swing Event-Dispatch-Thread ausführen
                SwingUtilities.invokeLater(() -> filterAddresses(tfSearch.getText()));
            }
        });
    }

    private JPanel createAddressForm() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new GridLayout(6, 2, 5, 5));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tfFirstName = new JTextField();
        tfLastName = new JTextField();
        tfStreet = new JTextField();
        tfZip = new JTextField();
        tfCity = new JTextField();
        tfEmail = new JTextField();

        form.add(new JLabel("Vorname:")); form.add(tfFirstName);
        form.add(new JLabel("Nachname:")); form.add(tfLastName);
        form.add(new JLabel("Straße:")); form.add(tfStreet);
        form.add(new JLabel("PLZ:")); form.add(tfZip);
        form.add(new JLabel("Stadt:")); form.add(tfCity);
        form.add(new JLabel("E-Mail:")); form.add(tfEmail);

        JButton btnSave = new JButton("Änderungen speichern");
        btnSave.addActionListener(e -> saveAddressChanges());

        panel.add(form, BorderLayout.CENTER);
        panel.add(btnSave, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createLetterForm() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ==========================================
        // 1. OBERER BEREICH: Metadaten & Modus-Weiche
        // ==========================================
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        // Grid für Betreff und Datum (2 Zeilen, 2 Spalten)
        JPanel gridFields = new JPanel(new GridLayout(2, 2, 5, 5));
        tfSubject = new JTextField("In Bezug auf Ihre Anfrage");
        tfDate = new JTextField("01.06.2026");

        gridFields.add(new JLabel("Betreff:"));
        gridFields.add(tfSubject);
        gridFields.add(new JLabel("Datum:"));
        gridFields.add(tfDate);

        // RadioButtons für die drei Modi erstellen
        JRadioButton rbTextMode = new JRadioButton("Direkttext (TXT / Drucken)", true);
        JRadioButton rbWordMode = new JRadioButton("Word-Vorlage (.docx)", false);
        JRadioButton rbOdtMode = new JRadioButton("LibreOffice-Vorlage (.odt)", false);

        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(rbTextMode);
        modeGroup.add(rbWordMode);
        modeGroup.add(rbOdtMode);

        // Die Radio-Zeile bekommt ein eigenes FlowLayout, damit nichts gestaucht wird
        JPanel radioRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        radioRow.add(new JLabel("Modus wählen: "));
        radioRow.add(rbTextMode);
        radioRow.add(rbWordMode);
        radioRow.add(rbOdtMode);

        // Zusammenbau des oberen Bereichs
        topPanel.add(gridFields, BorderLayout.CENTER);
        topPanel.add(radioRow, BorderLayout.SOUTH);

        // ==========================================
        // 2. MITTLERER BEREICH: Karten-Layout (Wechselmaske)
        // ==========================================
        JPanel centerCardPanel = new JPanel(new CardLayout());

        // Karte A: Text-Modus (Eingabefeld oben, Vorschau unten)
        JPanel textCard = new JPanel(new GridLayout(2, 1, 5, 5));
        taLetterBody = new JTextArea("vielen Dank für das Gespräch in {STADT}.\n\nMit freundlichen Grüßen\nMax Mustermann");
        taPreview = new JTextArea();
        taPreview.setEditable(false);
        taPreview.setBackground(new Color(245, 245, 245));
        textCard.add(new JScrollPane(taLetterBody));
        textCard.add(new JScrollPane(taPreview));

        // Karte B: Datei-Modus (Wird von Word UND ODT gemeinsam genutzt)
        JPanel officeCard = new JPanel(new FlowLayout(FlowLayout.LEFT));
        officeCard.setBorder(BorderFactory.createTitledBorder("Dokument-Einstellungen"));
        tfWordPath = new JTextField(30);
        tfWordPath.setEditable(false);
        JButton btnBrowse = new JButton("Vorlage suchen...");
        officeCard.add(new JLabel("Vorlage:"));
        officeCard.add(tfWordPath);
        officeCard.add(btnBrowse);

        centerCardPanel.add(textCard, "TEXT");
        centerCardPanel.add(officeCard, "OFFICE"); // Name auf "OFFICE" vereinheitlicht

        // ==========================================
        // 3. UNTERER BEREICH: Die Aktions-Buttons
        // ==========================================
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton btnGenerate = new JButton("Brief erstellen / vorschauen");
        JButton btnPrint = new JButton("Direkt Drucken");  // (Nur für Text)");
        buttonPanel.add(btnGenerate);
        buttonPanel.add(btnPrint);

        // Hauptkomponenten in das Master-Panel setzen
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(centerCardPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // ==========================================
        // 4. LOGIK & EVENT-LISTENER
        // ==========================================
        CardLayout cl = (CardLayout) centerCardPanel.getLayout();

        // Umschalt-Logik für Radio-Buttons
        rbTextMode.addActionListener(e -> {
            cl.show(centerCardPanel, "TEXT");
            btnPrint.setEnabled(true);
        });

        rbWordMode.addActionListener(e -> {
            cl.show(centerCardPanel, "OFFICE");
            // Ebenfalls nur aktiv, wenn bereits eine Datei ausgewählt wurde
            btnPrint.setEnabled(!tfWordPath.getText().trim().isEmpty());
        });

        rbOdtMode.addActionListener(e -> {
            cl.show(centerCardPanel, "OFFICE");
            // Ebenfalls nur aktiv, wenn bereits eine Datei ausgewählt wurde
            btnPrint.setEnabled(!tfWordPath.getText().trim().isEmpty());
        });

        // Und im Datei-Browser aktivieren wir den Button, sobald der Nutzer eine Datei wählt:
        btnBrowse.addActionListener(e -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Office-Dokumente (.docx, .odt)", "docx", "odt"));
                    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                        tfWordPath.setText(chooser.getSelectedFile().getAbsolutePath());
                        btnPrint.setEnabled(true); // <-- HIER: Sobald die Datei da ist, Knopfdruck erlauben!
                    }
        });

        // Zentraler Start-Button (3-Wege-Weiche)
        btnGenerate.addActionListener(e -> {
            Address selected = jAddressList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Bitte wähle zuerst links eine Adresse aus!", "Keine Adresse", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (rbTextMode.isSelected()) {
                // Text-Vorschau generieren
                generateLetterPreview();
            } else if (rbWordMode.isSelected()) {
                // MS Word-Generierung anstoßen
                executeWordGeneration(selected);
            } else if (rbOdtMode.isSelected()) {
                // LibreOffice-Generierung anstoßen
                executeOdtGeneration(selected);
            }
        });

        btnPrint.addActionListener(e -> printLetter());

        return panel;
    }
    private void fillFieldsFromSelectedAddress() {
        Address selected = jAddressList.getSelectedValue();
        if (selected != null) {
            tfFirstName.setText(selected.getFirstName());
            tfLastName.setText(selected.getLastName());
            tfStreet.setText(selected.getStreet());
            tfZip.setText(selected.getZipCode());
            tfCity.setText(selected.getCity());
            tfEmail.setText(selected.getEmail());
        }
    }

    private void saveAddressChanges() {
        Address selected = jAddressList.getSelectedValue();
        if (selected != null) {
            selected.setFirstName(tfFirstName.getText());
            selected.setLastName(tfLastName.getText());
            selected.setStreet(tfStreet.getText());
            selected.setZipCode(tfZip.getText());
            selected.setCity(tfCity.getText());
            selected.setEmail(tfEmail.getText());

            jAddressList.repaint(); // Aktualisiert die Anzeige in der JList

            saveAddressesToFile(); // Änderungen sofort speichern
        }
    }

    private void generateLetterPreview() {
        Address selected = jAddressList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Bitte wähle zuerst links eine Adresse aus!", "Keine Adresse", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String previewText = LetterGenerator.generateTextLetter(
                selected,
                tfDate.getText(),
                tfSubject.getText(),
                taLetterBody.getText()
        );
        taPreview.setText(previewText);
    }

    private void loadSampleData() {
        Address a1 = new Address("1", "Erika", "Mustermann", "Heidestraße 17", "30419", "Hannover", "erika@example.com");
        Address a2 = new Address("2", "Max", "Mustermann", "Hauptstraße 1", "10115", "Berlin", "max@example.com");
        addressList.add(a1);
        addressList.add(a2);
        listModel.addElement(a1);
        listModel.addElement(a2);
    }


    /**
     * Speichert die gesamte Adressliste in eine Binärdatei.
     */
    private void saveAddressesToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATABASE_FILE))) {
            // Wir serialisieren die gesamte ArrayList auf einmal
            oos.writeObject(addressList);
            System.out.println("Adressen erfolgreich gespeichert.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Speichern der Daten: " + e.getMessage(),
                    "Speicherfehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Lädt die Adressliste aus der Binärdatei, falls diese existiert.
     */
    @SuppressWarnings("unchecked")
    private void loadAddressesFromFile() {
        File file = new File(DATABASE_FILE);

        // Wenn noch keine Datei existiert, brechen wir ab (beim allerersten Start)
        if (!file.exists()) {
            loadSampleData(); // Fallback: Erzeuge deine Testdaten
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            // Liste einlesen und casten
            addressList = (List<Address>) ois.readObject();

            // Die JList-Anzeige leeren und mit den geladenen Daten füllen
            listModel.clear();
            for (Address addr : addressList) {
                listModel.addElement(addr);
            }
            System.out.println("Adressen erfolgreich geladen. Anzahl: " + addressList.size());
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Laden der Daten: " + e.getMessage(),
                    "Ladefehler", JOptionPane.ERROR_MESSAGE);
            // Falls die Datei beschädigt ist, laden wir zur Sicherheit die Testdaten
            loadSampleData();
        }
    }

    /**
     * Öffnet ein modales Eingabefenster, um eine neue Adresse anzulegen.
     */
    private void addNewAddress() {
        // Ein einfaches Panel mit Eingabefeldern für den Dialog bauen
        JPanel panel = new JPanel(new GridLayout(6, 2, 5, 5));
        JTextField inputFirst = new JTextField();
        JTextField inputLast = new JTextField();
        JTextField inputStreet = new JTextField();
        JTextField inputZip = new JTextField();
        JTextField inputCity = new JTextField();
        JTextField inputEmail = new JTextField();

        panel.add(new JLabel("Vorname:")); panel.add(inputFirst);
        panel.add(new JLabel("Nachname:")); panel.add(inputLast);
        panel.add(new JLabel("Straße:")); panel.add(inputStreet);
        panel.add(new JLabel("PLZ:")); panel.add(inputZip);
        panel.add(new JLabel("Stadt:")); panel.add(inputCity);
        panel.add(new JLabel("E-Mail:")); panel.add(inputEmail);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Neue Adresse hinzufügen", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // Wenn der Benutzer auf OK klickt und zumindest ein Nachname eingetragen ist
        if (result == JOptionPane.OK_OPTION) {
            String lastName = inputLast.getText().trim();
            if (lastName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ein Nachname ist zwingend erforderlich!", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Eine eindeutige ID generieren (z.B. über die aktuelle Systemzeit)
            String uniqueID = String.valueOf(System.currentTimeMillis());

            Address newAddr = new Address(
                    uniqueID,
                    inputFirst.getText().trim(),
                    lastName,
                    inputStreet.getText().trim(),
                    inputZip.getText().trim(),
                    inputCity.getText().trim(),
                    inputEmail.getText().trim()
            );

            // Daten im Speicher und in der GUI-Liste ablegen
            addressList.add(newAddr);
            listModel.addElement(newAddr);

            // Sofort auf die Festplatte sichern
            saveAddressesToFile();

            // Den neu erstellten Eintrag in der Liste automatisch auswählen
            jAddressList.setSelectedValue(newAddr, true);
        }
    }

    /**
     * Löscht die aktuell ausgewählte Adresse nach einer Sicherheitsabfrage.
     */
    private void deleteSelectedAddress() {
        Address selected = jAddressList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Bitte wähle zuerst eine Adresse zum Löschen aus!", "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Sicherheitsabfrage, um versehentliches Löschen zu verhindern
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Möchtest du den Kontakt '" + selected.getFirstName() + " " + selected.getLastName() + "' wirklich unwiderruflich löschen?",
                "Adresse löschen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            // Aus beiden Listen entfernen
            addressList.remove(selected);
            listModel.removeElement(selected);

            // Formularfelder leeren, da die Adresse nicht mehr existiert
            clearAddressFields();

            // Datei auf der Festplatte aktualisieren
            saveAddressesToFile();
        }
    }

    /**
     * Hilfsmethode, um die Detailfelder zu leeren (z. B. nach einem Löschvorgang)
     */
    private void clearAddressFields() {
        tfFirstName.setText("");
        tfLastName.setText("");
        tfStreet.setText("");
        tfZip.setText("");
        tfCity.setText("");
        tfEmail.setText("");
    }

    private void printLetter() {
        // 1. Fall: Text-Modus (Nutzt das klassische Java-Druckfenster)
        if (taPreview.getText().isEmpty() && (tfWordPath.getText().isEmpty())) {
            JOptionPane.showMessageDialog(this, "Es gibt nichts zu drucken! Bitte erstelle zuerst einen Brief.", "Fehler", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Wenn der Text-Modus aktiv ist, drucken wir wie gehabt den Inhalt der JTextArea
        if (taPreview.isShowing() && !taPreview.getText().isEmpty()) {
            try {
                boolean complete = taPreview.print();
                if (complete) {
                    JOptionPane.showMessageDialog(this, "Der Text-Brief wurde an den Drucker übergeben.", "Drucken", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Fehler beim Text-Drucken:\n" + e.getMessage(), "Druckfehler", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        // 2. Fall: Office-Modus (.docx oder .odt)
        // Wir nehmen den Pfad der fertig generierten Datei (nicht der Vorlage!)
        // Hier nutzen wir einen JFileChooser, um die bereits erstellte Datei auszuwählen, falls nötig,
        // oder greifen direkt auf die zuletzt gespeicherte Datei zu.
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Zu druckende Office-Datei auswählen");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Office-Dateien (.docx, .odt)", "docx", "odt"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToPrint = chooser.getSelectedFile();

            try {
                // Linux-Systembefehl zusammenbauen: libreoffice --headless --print-to-file oder -p
                // -p druckt direkt auf dem Standard-Systemdrucker aus
                String[] cmd = {
                        "libreoffice",
                        "--headless",
                        "-p",
                        fileToPrint.getAbsolutePath()
                };

                // Prozess im Linux-System starten
                Process process = Runtime.getRuntime().exec(cmd);
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    JOptionPane.showMessageDialog(this,
                            "Die Datei '" + fileToPrint.getName() + "' wurde erfolgreich an den LibreOffice-Druckdienst übergeben.",
                            "Drucken erfolgreich", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "LibreOffice meldet einen Fehlercode: " + exitCode,
                            "Druckfehler", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Konnte LibreOffice-Druckbefehl nicht ausführen:\n" + ex.getMessage(),
                        "Systemfehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Filtert die Adressliste in Echtzeit basierend auf dem Suchbegriff.
     */
    private void filterAddresses(String query) {
        String lowerCaseQuery = query.toLowerCase().trim();

        // Die Anzeige in der JList komplett leeren
        listModel.clear();

        for (Address addr : addressList) {
            // Wir prüfen, ob der Suchbegriff im Vor-, Nachnamen oder der Stadt vorkommt
            boolean matches = addr.getFirstName().toLowerCase().contains(lowerCaseQuery) ||
                    addr.getLastName().toLowerCase().contains(lowerCaseQuery) ||
                    addr.getCity().toLowerCase().contains(lowerCaseQuery);

            // Wenn es passt, fügen wir es der Anzeige wieder hinzu
            if (matches) {
                listModel.addElement(addr);
            }
        }
    }

    private void executeWordGeneration(Address selected) {
        String templatePath = tfWordPath.getText();
        if (templatePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte wähle zuerst eine Word-Vorlage (.docx) aus!", "Keine Vorlage", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Pfad für die fertige Datei bestimmen (speichert es im gleichen Verzeichnis ab)
        JFileChooser saver = new JFileChooser();
        saver.setSelectedFile(new File("Brief_" + selected.getLastName() + ".docx"));
        if (saver.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String outputPath = saver.getSelectedFile().getAbsolutePath();

        // Map für die Apache POI Ersetzungen füllen
        java.util.Map<String, String> replacements = new HashMap<>();
        replacements.put("{VORNAME}", selected.getFirstName());
        replacements.put("{NACHNAME}", selected.getLastName());
        replacements.put("{STRASSE}", selected.getStreet());
        replacements.put("{PLZ}", selected.getZipCode());
        replacements.put("{STADT}", selected.getCity());
        replacements.put("{DATUM}", tfDate.getText());
        replacements.put("{BETREFF}", tfSubject.getText());

        try {
            LetterGenerator.generateWordLetter(templatePath, outputPath, replacements);
            JOptionPane.showMessageDialog(this, "Word-Brief erfolgreich erstellt unter:\n" + outputPath, "Erfolg", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Fehler beim Erstellen der Word-Datei:\n" + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ALG_Databank().setVisible(true);
        });
    }

    private void executeOdtGeneration(Address selected) {
        String templatePath = tfWordPath.getText(); // Nutzen wir einfach für beide Dateitypen
        if (templatePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte wähle zuerst eine ODT-Vorlage aus!", "Keine Vorlage", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser saver = new JFileChooser();
        saver.setSelectedFile(new File("Brief_" + selected.getLastName() + ".odt"));
        if (saver.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String outputPath = saver.getSelectedFile().getAbsolutePath();

        // Map befüllen (wie gehabt)
        Map<String, String> replacements = new HashMap<>();
        replacements.put("{VORNAME}", selected.getFirstName());
        replacements.put("{NACHNAME}", selected.getLastName());
        replacements.put("{STRASSE}", selected.getStreet());
        replacements.put("{PLZ}", selected.getZipCode());
        replacements.put("{STADT}", selected.getCity());
        replacements.put("{DATUM}", tfDate.getText());
        replacements.put("{BETREFF}", tfSubject.getText());

        try {
            LetterGenerator.generateOdtLetter(templatePath, outputPath, replacements);
            JOptionPane.showMessageDialog(this, "LibreOffice-Brief erfolgreich erstellt unter:\n" + outputPath, "Erfolg", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Fehler beim Erstellen der ODT-Datei:\n" + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }
}