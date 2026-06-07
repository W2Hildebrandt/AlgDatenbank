import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.StyledEditorKit;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AlgDatabankGui extends JFrame {

    private JTable tabelle;
    private MitgliederTableModel tableModel;
    private MitgliedDAO mitgliedDAO;
    private List<Mitglied> aktuelleListe;

    // Aktuell ausgewähltes Mitglied für die automatische Benennung
    private Mitglied ausgewaehltesMitglied = null;

    // GUI Komponenten für die Eingabemaske (CRUD)
    private JTextField txtMNr, txtEintritt, txtAustritt, txtAnrede, txtTitel;
    private JTextField txtNachname, txtVorname, txtStrasse, txtPLZ, txtOrt;
    private JTextField txtTelefon, txtEmail, txtHinweise;

    // GUI Komponenten für das Druckzentrum (Seite 2)
    private JTextArea txtBriefText;
    private JComboBox<String> comboVorlagen;
    private JButton btnTextLaden, btnTextSpeichern, btnBriefDrucken;
    private JLabel lblAusgewaehltesMitglied;

    // GUI Komponenten für den Dokumenten-Scanner (Neu auf Seite 2)
    private JTextField txtScanName;
    private JComboBox<String> comboScanZiel;
    private JButton btnScanStarten;

    // GUI Komponenten für den Template-Editor (Seite 3)
    private JTextPane txtEditorText;
    private JComboBox<String> comboPlatzhalter;
    private JButton btnEditorLaden, btnEditorSpeichern;

    private JComboBox<String> comboSchriftart;
    private JComboBox<Integer> comboSchriftgroesse;
    private JButton btnFett, btnKursiv, btnUnterstrichen;

    // CRUD Buttons
    private JButton btnNeu, btnSpeichern, btnLoeschen, btnVorschau;
    private JButton btnNeuAufnehmen;
    private JButton btnUmschlagDrucken;

    // Instanzvariablen für den Etikettendruck
    private JSpinner spinnerEtikettPos;
    private JButton btnEtikettDrucken;

    public AlgDatabankGui() {
        mitgliedDAO = new MitgliedDAO();

        setTitle("AlgDatabank - Mitgliederverwaltung & Briefzentrum");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 900); // Leicht vergrößert für das neue Scanner-Panel
        setLocationRelativeTo(null);

        aktuelleListe = mitgliedDAO.readAll();
        tableModel = new MitgliederTableModel(aktuelleListe);
        tabelle = new JTable(tableModel);
        tabelle.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabelle.setAutoCreateRowSorter(true);

        tabelle.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        anpassenSpaltenBreiten();

        JTabbedPane tabbedPane = new JTabbedPane();

        // --- SEITE 1: VERWALTUNG ---
        JPanel verwaltungsPanel = new JPanel(new BorderLayout());
        JScrollPane tableScrollPane = new JScrollPane(tabelle);
        JPanel formPanel = erzeugeFormularPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, formPanel);
        splitPane.setDividerLocation(320);
        verwaltungsPanel.add(splitPane, BorderLayout.CENTER);

        // --- SEITE 2: BRIEF- & DRUCKZENTRUM (INKL. SCANNER) ---
        JPanel druckzentrumPanel = erzeugeDruckZentrumPanel();

        // --- SEITE 3: TEMPLATE-EDITOR ---
        JPanel editorPanel = erzeugeTemplateEditorPanel();

        tabbedPane.addTab("Mitglieder-Verwaltung", verwaltungsPanel);
        tabbedPane.addTab("Brief- & Druckzentrum", druckzentrumPanel);
        tabbedPane.addTab("Template-Editor", editorPanel);

        // --- HIGHLIGHTING LOGIK FÜR SCHWARZEN HINTERGRUND ---
        Color farbeAktivBg = new Color(0, 102, 204);
        Color farbeAktivFg = Color.WHITE;
        Color farbeInaktivBg = new Color(45, 45, 45);
        Color farbeInaktivFg = new Color(180, 180, 180);

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            JLabel lblTab = new JLabel(tabbedPane.getTitleAt(i), SwingConstants.CENTER);
            lblTab.setOpaque(true);
            lblTab.setPreferredSize(new Dimension(180, 30));
            lblTab.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

            if (i == 0) {
                lblTab.setBackground(farbeAktivBg);
                lblTab.setForeground(farbeAktivFg);
                lblTab.setFont(lblTab.getFont().deriveFont(Font.BOLD));
            } else {
                lblTab.setBackground(farbeInaktivBg);
                lblTab.setForeground(farbeInaktivFg);
                lblTab.setFont(lblTab.getFont().deriveFont(Font.PLAIN));
            }
            tabbedPane.setTabComponentAt(i, lblTab);
        }

        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                JLabel lblTab = (JLabel) tabbedPane.getTabComponentAt(i);
                if (lblTab != null) {
                    if (i == selectedIndex) {
                        lblTab.setBackground(farbeAktivBg);
                        lblTab.setForeground(farbeAktivFg);
                        lblTab.setFont(lblTab.getFont().deriveFont(Font.BOLD));
                    } else {
                        lblTab.setBackground(farbeInaktivBg);
                        lblTab.setForeground(farbeInaktivFg);
                        lblTab.setFont(lblTab.getFont().deriveFont(Font.PLAIN));
                    }
                }
            }
        });

        add(tabbedPane, BorderLayout.CENTER);

        registriereEvents();
        initialisiereDruckLogik();
        initialisiereEditorLogik();
    }

    private void anpassenSpaltenBreiten() {
        int[] breiten = {50, 80, 80, 60, 60, 120, 100, 150, 70, 120, 100, 150, 200};
        for (int i = 0; i < breiten.length; i++) {
            if (i < tabelle.getColumnModel().getColumnCount()) {
                tabelle.getColumnModel().getColumn(i).setPreferredWidth(breiten[i]);
            }
        }
    }

    private JPanel erzeugeDruckZentrumPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        lblAusgewaehltesMitglied = new JLabel("Aktuell ausgewählt: (Kein Mitglied in der Tabelle markiert)");
        lblAusgewaehltesMitglied.setFont(new Font("Arial", Font.BOLD, 13));
        lblAusgewaehltesMitglied.setForeground(new Color(0, 102, 204));
        topPanel.add(lblAusgewaehltesMitglied, BorderLayout.NORTH);

        JPanel aktionsLeiste = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        aktionsLeiste.add(new JLabel("Textbausteine:"));

        String[] vorlagenNamen = {"-- Bitte wählen --", "Willkommen im Verein", "Beitragserinnerung", "Einladung Hauptversammlung"};
        comboVorlagen = new JComboBox<>(vorlagenNamen);
        aktionsLeiste.add(comboVorlagen);

        btnTextLaden = new JButton("Brief-Vorlage öffnen (.txt, .docx, .odt)");
        btnTextSpeichern = new JButton("Aktuellen Text保存");
        aktionsLeiste.add(btnTextLaden);
        aktionsLeiste.add(btnTextSpeichern);

        topPanel.add(aktionsLeiste, BorderLayout.SOUTH);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        txtBriefText = new JTextArea();
        txtBriefText.setFont(new Font("Arial", Font.PLAIN, 12));
        txtBriefText.setLineWrap(true);
        txtBriefText.setWrapStyleWord(true);
        txtBriefText.setText("Wählen Sie auf der ersten Seite ein Mitglied aus. Nutzen Sie dann hier die Vorlagen oder schreiben Sie einen freien Text...");

        JScrollPane textScrollPane = new JScrollPane(txtBriefText);
        mainPanel.add(textScrollPane, BorderLayout.CENTER);

        // --- UNTERER BEREICH: GETEILT IN SCANNER & DRUCKER ---
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 15, 0));

        // Links: Dokumenten-Scanner (Neu)
        JPanel scannerPanel = new JPanel(new GridBagLayout());
        scannerPanel.setBorder(BorderFactory.createTitledBorder("Dokumenten-Scanner (SANE / Paperless)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtScanName = new JTextField("dokument_scan", 12);
        String[] ziele = {"Direkt an Paperless-ngx übergeben", "Lokal als Bild/PDF speichern"};
        comboScanZiel = new JComboBox<>(ziele);
        btnScanStarten = new JButton("Scan-Vorgang starten");
        btnScanStarten.setFont(new Font("Arial", Font.BOLD, 11));
        btnScanStarten.setBackground(new Color(40, 167, 69)); // Grün
        btnScanStarten.setForeground(Color.WHITE);

        gbc.gridx = 0; gbc.gridy = 0; scannerPanel.add(new JLabel("Datei-Name:"), gbc);
        gbc.gridx = 1; scannerPanel.add(txtScanName, gbc);
        gbc.gridx = 0; gbc.gridy = 1; scannerPanel.add(new JLabel("Ziel-Ordner:"), gbc);
        gbc.gridx = 1; scannerPanel.add(comboScanZiel, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; scannerPanel.add(btnScanStarten, gbc);

        // Rechts: Drucken & Etiketten (Bestehend)
        JPanel druckPanel = new JPanel(new BorderLayout(5, 5));
        druckPanel.setBorder(BorderFactory.createTitledBorder("Drucken & Etiketten"));

        JPanel etikettenOben = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        etikettenOben.add(new JLabel("Bogen-Pos (1-24):"));
        spinnerEtikettPos = new JSpinner(new SpinnerNumberModel(1, 1, 24, 1));
        etikettenOben.add(spinnerEtikettPos);
        btnEtikettDrucken = new JButton("Etikett drucken");
        etikettenOben.add(btnEtikettDrucken);

        JPanel briefUnten = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        btnUmschlagDrucken = new JButton("Umschlag (DL)");
        btnVorschau = new JButton("Vorschau...");
        btnBriefDrucken = new JButton("Brief drucken");
        btnBriefDrucken.setFont(new Font("Arial", Font.BOLD, 11));

        briefUnten.add(btnUmschlagDrucken);
        briefUnten.add(btnVorschau);
        briefUnten.add(btnBriefDrucken);

        druckPanel.add(etikettenOben, BorderLayout.NORTH);
        druckPanel.add(briefUnten, BorderLayout.SOUTH);

        bottomPanel.add(scannerPanel);
        bottomPanel.add(druckPanel);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        return mainPanel;
    }

    private JPanel erzeugeTemplateEditorPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.Y_AXIS));
        toolbarPanel.setBorder(BorderFactory.createTitledBorder("Serienbrief-Vorlagen bearbeiten & formatieren"));

        JPanel zeile1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnEditorLaden = new JButton("Vorlage öffnen (.html, .txt, .docx, .odt)");
        btnEditorSpeichern = new JButton("Vorlage speichern (.html)");

        String[] platzhalter = {
                "-- Feld einfügen --",
                "Anrede", "Titel", "Vorname", "Nachname",
                "Strasse", "PLZ", "Ort",
                "MNr", "Eintritt", "Austritt", "Telefon", "Email"
        };
        comboPlatzhalter = new JComboBox<>(platzhalter);

        zeile1.add(btnEditorLaden);
        zeile1.add(btnEditorSpeichern);
        zeile1.add(new JLabel("  |  Platzhalter:"));
        zeile1.add(comboPlatzhalter);
        toolbarPanel.add(zeile1);

        JPanel zeile2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        comboSchriftart = new JComboBox<>(new String[]{"Arial", "Times New Roman", "Courier New", "SansSerif", "Serif"});

        Integer[] groessen = {9, 10, 11, 12, 14, 16, 18, 20, 24, 28};
        comboSchriftgroesse = new JComboBox<>(groessen);
        comboSchriftgroesse.setSelectedItem(12);

        btnFett = new JButton("Fett");
        btnFett.setFont(btnFett.getFont().deriveFont(Font.BOLD));
        btnKursiv = new JButton("Kursiv");
        btnKursiv.setFont(btnKursiv.getFont().deriveFont(Font.ITALIC));
        btnUnterstrichen = new JButton("<html><u>Unterstrichen</u></html>");

        zeile2.add(new JLabel("Schrift:"));
        zeile2.add(comboSchriftart);
        zeile2.add(new JLabel("Größe:"));
        zeile2.add(comboSchriftgroesse);
        zeile2.add(new JToolBar.Separator());
        zeile2.add(btnFett);
        zeile2.add(btnKursiv);
        zeile2.add(btnUnterstrichen);

        toolbarPanel.add(zeile2);
        mainPanel.add(toolbarPanel, BorderLayout.NORTH);

        txtEditorText = new JTextPane();
        txtEditorText.setContentType("text/html");

        txtEditorText.setText("<html><body style='font-family:Arial; font-size:12pt;'>"
                + "[Anrede] [Titel]<br>"
                + "[Vorname] [Nachname]<br>"
                + "[Strasse]<br>"
                + "<br>"
                + "[PLZ] [Ort]<br><br><br>"
                + "Sehr geehrte(r) [Anrede] [Nachname],<br><br>"
                + "schreiben Sie hier Ihren bearbeitbaren Text..."
                + "</body></html>");

        JScrollPane scrollPane = new JScrollPane(txtEditorText);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JLabel lblInfo = new JLabel("Hinweis: Formatierungen bleiben beim Speichern als .html-Datei erhalten.");
        lblInfo.setFont(new Font("Arial", Font.ITALIC, 11));
        mainPanel.add(lblInfo, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel erzeugeFormularPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Mitglieder-Details (Bearbeiten)"));

        JPanel felderPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtMNr = new JTextField(5);
        txtEintritt = new JTextField(10);
        txtAustritt = new JTextField(10);
        txtAnrede = new JTextField(10);
        txtTitel = new JTextField(10);
        txtNachname = new JTextField(15);
        txtVorname = new JTextField(15);
        txtStrasse = new JTextField(20);
        txtPLZ = new JTextField(7);
        txtOrt = new JTextField(15);
        txtTelefon = new JTextField(12);
        txtEmail = new JTextField(15);
        txtHinweise = new JTextField(25);

        addKomponente(felderPanel, new JLabel("Mitglieds-Nr:"), txtMNr, gbc, 0, 0);
        addKomponente(felderPanel, new JLabel("Eintritt:"), txtEintritt, gbc, 2, 0);
        addKomponente(felderPanel, new JLabel("Austritt:"), txtAustritt, gbc, 4, 0);

        addKomponente(felderPanel, new JLabel("Anrede:"), txtAnrede, gbc, 0, 1);
        addKomponente(felderPanel, new JLabel("Titel:"), txtTitel, gbc, 2, 1);

        addKomponente(felderPanel, new JLabel("Nachname:"), txtNachname, gbc, 0, 2);
        addKomponente(felderPanel, new JLabel("Vorname:"), txtVorname, gbc, 2, 2);

        addKomponente(felderPanel, new JLabel("Straße:"), txtStrasse, gbc, 0, 3);
        addKomponente(felderPanel, new JLabel("PLZ:"), txtPLZ, gbc, 2, 3);
        addKomponente(felderPanel, new JLabel("Ort:"), txtOrt, gbc, 4, 3);

        addKomponente(felderPanel, new JLabel("Telefon:"), txtTelefon, gbc, 0, 4);
        addKomponente(felderPanel, new JLabel("E-Mail:"), txtEmail, gbc, 2, 4);

        gbc.gridx = 0; gbc.gridy = 5; felderPanel.add(new JLabel("Hinweise:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 5;
        felderPanel.add(txtHinweise, gbc);
        gbc.gridwidth = 1;

        panel.add(felderPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnNeu = new JButton("Felder leeren");

        btnNeuAufnehmen = new JButton("Neu aufnehmen");
        btnNeuAufnehmen.setFont(new Font("Arial", Font.BOLD, 12));
        btnNeuAufnehmen.setBackground(new Color(40, 167, 69)); // Angenehmes Grün
        btnNeuAufnehmen.setForeground(Color.WHITE);

        btnSpeichern = new JButton("Änderungen Speichern");
        btnLoeschen = new JButton("Mitglied löschen");
        btnLoeschen.setForeground(Color.RED);

        buttonPanel.add(btnNeu);
        buttonPanel.add(btnNeuAufnehmen); // <--- NEU eingefügt
        buttonPanel.add(btnSpeichern);
        buttonPanel.add(btnLoeschen);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void addKomponente(JPanel p, JLabel label, JTextField tf, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x; gbc.gridy = y; p.add(label, gbc);
        gbc.gridx = x + 1; p.add(tf, gbc);
    }

    private void registriereEvents() {
        tabelle.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                int[] selectedRows = tabelle.getSelectedRows();
                if (selectedRows.length == 1) {
                    int modelRow = tabelle.convertRowIndexToModel(selectedRows[0]);
                    ausgewaehltesMitglied = tableModel.getMitgliedAt(modelRow);
                    zeigeMitglied(ausgewaehltesMitglied);

                    lblAusgewaehltesMitglied.setText("Aktuell ausgewählt: " + ausgewaehltesMitglied.getVorname() + " " + ausgewaehltesMitglied.getNachname() + " (MNr: " + ausgewaehltesMitglied.getMNr() + ")");

                    // Schlägt intelligenten Dateinamen für den Scan vor
                    String datum = new SimpleDateFormat("yyyyMMdd").format(new Date());
                    txtScanName.setText("Scan_" + ausgewaehltesMitglied.getNachname() + "_" + datum);
                } else if (selectedRows.length > 1) {
                    ausgewaehltesMitglied = null;
                    lblAusgewaehltesMitglied.setText("Aktuell ausgewählt: " + selectedRows.length + " Mitglieder für den Seriendruck");
                    txtScanName.setText("SerienScan_" + new SimpleDateFormat("yyyyMMdd").format(new Date()));
                } else {
                    ausgewaehltesMitglied = null;
                    lblAusgewaehltesMitglied.setText("Aktuell ausgewählt: (Kein Mitglied in der Tabelle markiert)");
                    txtScanName.setText("dokument_scan");
                }
            }
        });

        btnNeu.addActionListener(e -> leereFormular());
        btnSpeichern.addActionListener(e -> speichereDatensatz());
        btnLoeschen.addActionListener(e -> loescheDatensatz());
        btnNeu.addActionListener(e -> leereFormular());
        btnNeuAufnehmen.addActionListener(e -> neuAufnehmenDatensatz()); // <--- NEU
        btnSpeichern.addActionListener(e -> speichereDatensatz());
        btnLoeschen.addActionListener(e -> loescheDatensatz());

    }

    private void initialisiereDruckLogik() {
        comboVorlagen.addActionListener(e -> {
            int index = comboVorlagen.getSelectedIndex();
            switch (index) {
                case 1:
                    txtBriefText.setText("wir freuen uns sehr, Sie als neues Mitglied in unserem Verein begrüßen zu dürfen.\n\n"
                            + "Ihr Eintrittsdatum wurde auf den [Eintritt] festgelegt.\n"
                            + "Bei Fragen zu Ihrer Mitgliedschaft (Ihre Nummer lautet: [MNr]) stehen wir Ihnen jederzeit zur Verfügung.");
                    break;
                case 2:
                    txtBriefText.setText("hiermit möchten wir Sie höflich an den noch ausstehenden Mitgliedsbeitrag für das aktuelle Quartal erinnern.\n\n"
                            + "Bitte überweisen Sie den Betrag zeitnah auf unser Vereinskonto.\n"
                            + "Sollten Sie den Betrag zwischenzeitlich beglichen haben, betrachten Sie dieses Schreiben als gegenstandslos.");
                    break;
                case 3:
                    txtBriefText.setText("hiermit laden wir Sie herzlich zu unserer diesjährigen Hauptversammlung ein.\n\n"
                            + "Die Versammlung findet im Vereinsheim statt. Anträge zur Tagesordnung können bis zum Ende der Woche schriftlich eingereicht werden.\n\n"
                            + "Wir freuen uns auf Ihr Erscheinen.");
                    break;
                default:
                    break;
            }
        });

        btnTextLaden.addActionListener(e -> oeffneDateiInTextArea(txtBriefText));
        btnTextSpeichern.addActionListener(e -> speichereDateiAusTextArea(txtBriefText));

        btnBriefDrucken.addActionListener(e -> starteBriefDruck());
        btnEtikettDrucken.addActionListener(e -> starteEtikettDruck());
        btnVorschau.addActionListener(e -> zeigeDruckVorschau());
        btnUmschlagDrucken.addActionListener(e -> starteUmschlagDruck());

        // SCANNER EVENT VERKNÜPFUNG
        btnScanStarten.addActionListener(e -> fuehreScanAus());
    }

    private void initialisiereEditorLogik() {
        btnFett.addActionListener(new StyledEditorKit.BoldAction());
        btnKursiv.addActionListener(new StyledEditorKit.ItalicAction());
        btnUnterstrichen.addActionListener(new StyledEditorKit.UnderlineAction());

        comboSchriftart.addActionListener(e -> {
            String fontName = (String) comboSchriftart.getSelectedItem();
            if (fontName != null) {
                new StyledEditorKit.FontFamilyAction(fontName, fontName).actionPerformed(e);
            }
        });

        comboSchriftgroesse.addActionListener(e -> {
            Integer size = (Integer) comboSchriftgroesse.getSelectedItem();
            if (size != null) {
                new StyledEditorKit.FontSizeAction(String.valueOf(size), size).actionPerformed(e);
            }
        });

        comboPlatzhalter.addActionListener(e -> {
            int index = comboPlatzhalter.getSelectedIndex();
            if (index > 0) {
                String feldName = (String) comboPlatzhalter.getSelectedItem();
                String platzhalterTag = "[" + feldName + "]";
                try {
                    int pos = txtEditorText.getCaretPosition();
                    txtEditorText.getDocument().insertString(pos, platzhalterTag, null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                comboPlatzhalter.setSelectedIndex(0);
                txtEditorText.requestFocus();
            }
        });

        btnEditorLaden.addActionListener(e -> oeffneDateiInEditorPane());
        btnEditorSpeichern.addActionListener(e -> speichereDateiAusEditorPane());
    }

    // --- NEU: DOKUMENTEN SCANNER LOGIK (SANE-BACKEND) ---
    private void fuehreScanAus() {
        String dateiName = txtScanName.getText().trim().replaceAll("[^a-zA-Z0-9__-]", "");
        if (dateiName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte einen gültigen Dateinamen eingeben.", "Eingabefehler", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int zielIndex = comboScanZiel.getSelectedIndex();
        String zielPfad = "";

        if (zielIndex == 0) {
            // ANPASSEN: Pfad zu deinem Paperless-ngx Einzugsordner (Consume)
            zielPfad = "/home/user/paperless/paperless-ngx/consume/" + dateiName + ".pdf";
        } else {
            // Lokaler Speicherordner innerhalb des Projekts
            File lokalerOrdner = new File("eingescannte_dokumente");
            if (!lokalerOrdner.exists()) lokalerOrdner.mkdir();
            zielPfad = lokalerOrdner.getAbsolutePath() + "/" + dateiName + ".pdf";
        }

        final String finalPfad = zielPfad;

        // Scan in separatem Thread starten, damit die GUI nicht einfriert
        new Thread(() -> {
            try {
                btnScanStarten.setEnabled(false);
                btnScanStarten.setText("Scanne... Bitte warten...");

                // Linux SANE-Kommando wandelt Scan direkt über 'convert' (ImageMagick) in komprimiertes PDF um
                // Auflösung 300 dpi ist optimal für Texterkennungen (OCR)
                String kommando = "scanimage --format=png --resolution 300 | convert - " + finalPfad;

                Process prozess = Runtime.getRuntime().exec(new String[]{"bash", "-c", kommando});
                int exitCode = prozess.waitFor();

                if (exitCode == 0) {
                    JOptionPane.showMessageDialog(this,
                            "Dokument erfolgreich eingelesen!\nGespeichert unter:\n" + finalPfad,
                            "Scan beendet", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Der Scanner-Prozess lieferte einen Fehler.\nPrüfe, ob der Scanner eingeschaltet und über SANE erreichbar ist.",
                            "Scanner-Fehler", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Systemfehler beim Scannen: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } finally {
                btnScanStarten.setEnabled(true);
                btnScanStarten.setText("Scan-Vorgang starten");
            }
        }).start();
    }

    private void oeffneDateiInTextArea(JTextArea targetArea) {
        JFileChooser fileChooser = new JFileChooser();
        javax.swing.filechooser.FileNameExtensionFilter filter =
                new javax.swing.filechooser.FileNameExtensionFilter(
                        "Textdokumente (*.txt, *.docx, *.odt)", "txt", "docx", "odt");
        fileChooser.setFileFilter(filter);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String name = file.getName().toLowerCase();

            try {
                if (name.endsWith(".docx")) {
                    targetArea.setText(readDocxFile(file));
                } else if (name.endsWith(".odt")) {
                    targetArea.setText(readOdtFile(file));
                } else {
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        targetArea.read(br, null);
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Laden der Datei: " + ex.getMessage(),
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void speichereDateiAusTextArea(JTextArea targetArea) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                targetArea.write(bw);
                JOptionPane.showMessageDialog(this, "Datei erfolgreich gespeichert!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Speichern: " + ex.getMessage());
            }
        }
    }

    private void oeffneDateiInEditorPane() {
        JFileChooser fileChooser = new JFileChooser();
        javax.swing.filechooser.FileNameExtensionFilter filter =
                new javax.swing.filechooser.FileNameExtensionFilter(
                        "Unterstützte Vorlagen (*.html, *.txt, *.docx, *.odt)",
                        "html", "htm", "txt", "docx", "odt");
        fileChooser.setFileFilter(filter);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String name = file.getName().toLowerCase();

            try {
                String geladenerText = "";

                if (name.endsWith(".docx")) {
                    String rohText = readDocxFile(file);
                    geladenerText = "<html><body style='font-family:Arial; font-size:12pt;'>"
                            + rohText.replace("\n", "<br>")
                            + "</body></html>";
                } else if (name.endsWith(".odt")) {
                    String rohText = readOdtFile(file);
                    geladenerText = "<html><body style='font-family:Arial; font-size:12pt;'>"
                            + rohText.replace("\n", "<br>")
                            + "</body></html>";
                } else if (name.endsWith(".txt")) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line).append("<br>");
                        }
                    }
                    geladenerText = "<html><body style='font-family:Arial; font-size:12pt;'>"
                            + sb.toString()
                            + "</body></html>";
                } else {
                    try (FileReader fr = new FileReader(file)) {
                        txtEditorText.setText("");
                        txtEditorText.read(fr, null);
                        return;
                    }
                }

                txtEditorText.setText(geladenerText);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Laden der Format-Vorlage: " + ex.getMessage(),
                        "Ladefehler", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void speichereDateiAusEditorPane() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".html")) {
                file = new File(file.getAbsolutePath() + ".html");
            }
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(txtEditorText.getText());
                JOptionPane.showMessageDialog(this, "Vorlage erfolgreich als HTML gespeichert!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Speichern: " + ex.getMessage());
            }
        }
    }

    private String readDocxFile(File file) throws Exception {
        StringBuilder text = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(fis)) {

            for (org.apache.poi.xwpf.usermodel.XWPFParagraph para : document.getParagraphs()) {
                text.append(para.getText()).append("\n");
            }
        }
        return text.toString();
    }

    private String readOdtFile(File file) throws Exception {
        org.odftoolkit.odfdom.doc.OdfTextDocument odfDoc = org.odftoolkit.odfdom.doc.OdfTextDocument.loadDocument(file);
        return odfDoc.getContentRoot().getTextContent();
    }

    private void zeigeMitglied(Mitglied m) {
        txtMNr.setText(String.valueOf(m.getMNr()));
        txtMNr.setEditable(false);
        txtEintritt.setText(m.getEintritt());
        txtAustritt.setText(m.getAustritt() != null ? m.getAustritt() : "");
        txtAnrede.setText(m.getAnrede());
        txtTitel.setText(m.getTitel() != null ? m.getTitel() : "");
        txtNachname.setText(m.getNachname());
        txtVorname.setText(m.getVorname());
        txtStrasse.setText(m.getStrasse());
        txtPLZ.setText(m.getPostleitzahl());
        txtOrt.setText(m.getOrt());
        txtTelefon.setText(m.getTelefon() != null ? m.getTelefon() : "");
        txtEmail.setText(m.getEmail() != null ? m.getEmail() : "");
        txtHinweise.setText(m.getHinweise() != null ? m.getHinweise() : "");
    }

    private void leereFormular() {
        tabelle.clearSelection();
        lblAusgewaehltesMitglied.setText("Aktuell ausgewählt: (Kein Mitglied in der Tabelle markiert)");
        txtMNr.setText("(wird automatisch vergeben)");
        txtMNr.setEditable(false);

        txtEintritt.setText("");
        txtAustritt.setText("");
        txtAnrede.setText("");
        txtTitel.setText("");
        txtNachname.setText("");
        txtVorname.setText("");
        txtStrasse.setText("");
        txtPLZ.setText("");
        txtOrt.setText("");
        txtTelefon.setText("");
        txtEmail.setText("");
        txtHinweise.setText("");
    }

    private void speichereDatensatz() {
        try {
            int mNr = Integer.parseInt(txtMNr.getText().trim());
            String nachname = txtNachname.getText().trim();
            String plz = txtPLZ.getText().trim();
            String ort = txtOrt.getText().trim();

            if (nachname.isEmpty() || plz.isEmpty() || ort.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nachname, PLZ und Ort dürfen nicht leer sein!", "Eingabefehler", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Mitglied m = new Mitglied(
                    mNr, txtEintritt.getText().trim(), txtAustritt.getText().trim(),
                    txtAnrede.getText().trim(), txtTitel.getText().trim(), nachname,
                    txtVorname.getText().trim(), txtStrasse.getText().trim(), plz, ort,
                    txtTelefon.getText().trim(), txtEmail.getText().trim(), txtHinweise.getText().trim()
            );

            mitgliedDAO.save(m);
            aktualisiereTabelle();
            JOptionPane.showMessageDialog(this, "Mitglied erfolgreich gespeichert.");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Bitte eine gültige Mitgliedsnummer eingeben.", "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loescheDatensatz() {
        if (ausgewaehltesMitglied == null) {
            JOptionPane.showMessageDialog(this,
                    "Bitte wählen Sie zuerst ein Mitglied aus der Tabelle aus, das gelöscht werden soll.",
                    "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. Daten für die Meldungen sichern, bevor gelöscht wird
        String vorname = ausgewaehltesMitglied.getVorname();
        String nachname = ausgewaehltesMitglied.getNachname();
        int mNr = ausgewaehltesMitglied.getMNr();

        // 2. Sicherheitsabfrage, damit nichts aus Versehen passiert
        int antwort = JOptionPane.showConfirmDialog(this,
                "Möchten Sie das Mitglied " + vorname + " " + nachname + " (MNr: " + mNr + ") wirklich löschen?",
                "Löschen bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (antwort == JOptionPane.YES_OPTION) {
            try {
                // Aus der Datenbank entfernen
                mitgliedDAO.delete(mNr);

                // 3. Liste neu laden und an das TableModel übergeben
                // In AlgDatabankGui.java innerhalb von loescheDatensatz() nach mitgliedDAO.delete(mNr);
                // mitgliedDAO.partitioniereMitgliedsnummernNeu(); // <--- Schließt die Lücke in der DB
                aktuelleListe = mitgliedDAO.readAll();
                tableModel.setListe(aktuelleListe);

                // Formular und Auswahl zurücksetzen
                leereFormular();

                // 4. Die gewünschte Erfolgsmeldung anzeigen
                JOptionPane.showMessageDialog(this,
                        "Mitglied " + vorname + " " + nachname + " gelöscht.",
                        "Erfolg", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Fehler beim Löschen: " + ex.getMessage(),
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void starteBriefDruck() {
        int[] selectedRows = tabelle.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie zuerst auf der ersten Seite mindestens ein Mitglied aus der Tabelle aus.", "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.ArrayList<Mitglied> ausgewaehlteMitglieder = new java.util.ArrayList<>();
        for (int row : selectedRows) {
            int modelRow = tabelle.convertRowIndexToModel(row);
            Mitglied m = tableModel.getMitgliedAt(modelRow);
            ausgewaehlteMitglieder.add(m);
        }

        String roherBriefText = txtBriefText.getText();

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new SerienBriefDrucker(ausgewaehlteMitglieder, roherBriefText));

        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "Serienbrief-Druck erfolgreich gestartet!");
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Drucken: " + ex.getMessage(), "Druckfehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void aktualisiereTabelle() {
        aktuelleListe.clear();
        aktuelleListe.addAll(mitgliedDAO.readAll());
        tableModel.fireTableDataChanged();
    }

    private void starteEtikettDruck() {
        int[] selectedRows = tabelle.getSelectedRows();

        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie zuerst auf der ersten Seite mindestens ein Mitglied aus der Tabelle aus.", "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int kopien = 1;
        String eingabe = JOptionPane.showInputDialog(this,
                "Wie viele Exemplare möchten Sie pro ausgewähltem Mitglied drucken?",
                "Etiketten-Anzahl",
                JOptionPane.QUESTION_MESSAGE);

        if (eingabe == null) {
            return;
        }

        try {
            kopien = Integer.parseInt(eingabe.trim());
            if (kopien <= 0) {
                JOptionPane.showMessageDialog(this, "Die Anzahl muss mindestens 1 betragen.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Bitte geben Sie eine gültige Zahl ein.", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        java.util.ArrayList<Mitglied> ausgewaehlteMitglieder = new java.util.ArrayList<>();
        for (int row : selectedRows) {
            int modelRow = tabelle.convertRowIndexToModel(row);
            Mitglied m = tableModel.getMitgliedAt(modelRow);

            for (int i = 0; i < kopien; i++) {
                ausgewaehlteMitglieder.add(m);
            }
        }

        int startPos = (Integer) spinnerEtikettPos.getValue();

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new MehrfachEtikettenDrucker(ausgewaehlteMitglieder, startPos));

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Etikettendruck: " + ex.getMessage(), "Druckfehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void zeigeDruckVorschau() {
        int[] selectedRows = tabelle.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie zuerst auf der ersten Seite mindestens ein Mitglied aus der Tabelle aus.", "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.ArrayList<Mitglied> ausgewaehlteMitglieder = new java.util.ArrayList<>();
        for (int row : selectedRows) {
            int modelRow = tabelle.convertRowIndexToModel(row);
            ausgewaehlteMitglieder.add(tableModel.getMitgliedAt(modelRow));
        }

        SerienBriefDrucker drucker = new SerienBriefDrucker(ausgewaehlteMitglieder, txtBriefText.getText());
        DruckVorschauDialog vorschauDialog = new DruckVorschauDialog(this, drucker, ausgewaehlteMitglieder.size());
        vorschauDialog.setVisible(true);
    }

    private void starteUmschlagDruck() {
        int selectedRow = tabelle.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie zuerst auf der ersten Seite ein Mitglied aus der Tabelle aus.", "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tabelle.convertRowIndexToModel(selectedRow);
        Mitglied m = tableModel.getMitgliedAt(modelRow);

        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();

        double width = 220 * 2.834;
        double height = 110 * 2.834;
        paper.setSize(width, height);
        paper.setImageableArea(15 * 2.834, 15 * 2.834, (220 - 30) * 2.834, (110 - 30) * 2.834);

        pf.setPaper(paper);
        pf.setOrientation(PageFormat.LANDSCAPE);

        job.setPrintable(new UmschlagDrucker(m), pf);

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Umschlagdruck: " + ex.getMessage(), "Druckfehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void neuAufnehmenDatensatz() {
        // Optionale Validierung: Pflichtfelder prüfen
        if (txtNachname.getText().trim().isEmpty() || txtVorname.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Bitte mindestens Vorname und Nachname ausfüllen.",
                    "Eingabefehler", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Mitglied m = new Mitglied();
            // Falls die Mitgliedsnummer von der DB (Auto-Increment) vergeben wird,
            // kann txtMNr ignoriert werden. Falls manuell:
            if (!txtMNr.getText().trim().isEmpty()) {
                m.setMNr(Integer.parseInt(txtMNr.getText().trim()));
            }

            m.setEintritt(txtEintritt.getText().trim());
            m.setAustritt(txtAustritt.getText().trim().isEmpty() ? null : txtAustritt.getText().trim());
            m.setAnrede(txtAnrede.getText().trim());
            m.setTitel(txtTitel.getText().trim().isEmpty() ? null : txtTitel.getText().trim());
            m.setNachname(txtNachname.getText().trim());
            m.setVorname(txtVorname.getText().trim());
            m.setStrasse(txtStrasse.getText().trim());
            m.setPostleitzahl(txtPLZ.getText().trim());
            m.setOrt(txtOrt.getText().trim());
            m.setTelefon(txtTelefon.getText().trim().isEmpty() ? null : txtTelefon.getText().trim());
            m.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
            m.setHinweise(txtHinweise.getText().trim().isEmpty() ? null : txtHinweise.getText().trim());

            // In DB schreiben
            mitgliedDAO.save(m); // Vermutlich heißt die Methode 'create' oder 'insert' in deinem DAO

            // GUI und Tabelle aktualisieren
            aktuelleListe = mitgliedDAO.readAll();
            tableModel.setListe(aktuelleListe); // Setzt die neue Liste im TableModel
            tableModel.fireTableDataChanged();

            leereFormular(); // Felder für die nächste Eingabe frei machen

            JOptionPane.showMessageDialog(this, "Mitglied erfolgreich aufgenommen!", "Erfolg", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Die Mitgliedsnummer muss eine Zahl sein.", "Fehler", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Fehler beim Speichern: " + ex.getMessage(), "Datenbankfehler", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new AlgDatabankGui().setVisible(true);
        });
    }
}