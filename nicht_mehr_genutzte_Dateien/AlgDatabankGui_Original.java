import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.web.WebView;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class AlgDatabankGui extends JFrame {

    private JTable tabelle;
    private MitgliederTableModel mitglieder;
    private MitgliedDAO mitgliedDAO;
    private List<Mitglied> aktuelleListe;

    // Aktuell ausgewähltes Mitglied für die automatische Benennung
    private Mitglied ausgewaehltesMitglied = null;

    // GUI Komponenten für die Eingabemaske (CRUD)
    private JTextField txtMNr, txtEintritt, txtAustritt, txtAnrede, txtTitel;
    private JTextField txtNachname, txtVorname, txtStrasse, txtPLZ, txtOrt;
    private JTextField txtTelefon, txtEmail, txtHinweise;

    // GUI Komponenten für das Briefzentrum (Seite 2)
    private JFXPanel fxBriefPanel;
    private WebView webViewBrief;

    // Komponenten für den HTML-Template-Editor (Seite 3)
    private JFXPanel fxEditorPanel;
    private WebView webViewEditor;
    private JComboBox<String> comboPlatzhalter;

    private JPanel kartenPanel; // Der Container mit CardLayout
    private CardLayout cardLayout;

    private JComboBox<String> comboVorlagen;
    private JButton btnTextLaden, btnTextSpeichern, btnBriefDrucken;
    private JLabel lblAusgewaehltesMitglied;

    // GUI Komponenten für den Dokumenten-Scanner
    private JTextField txtScanName;
    private JComboBox<String> comboScanZiel;
    private JButton btnScanStarten;

    private JButton btnEditorLaden, btnEditorSpeichern;

    // CRUD Buttons
    private JButton btnNeu, btnSpeichern, btnLoeschen, btnVorschau;
    private JButton btnNeuAufnehmen;
    private JButton btnUmschlagDrucken;

    // Instanzvariablen für den Etikettendruck
    private JSpinner spinnerEtikettPos;
    private JButton btnEtikettDrucken;

    public AlgDatabankGui() {
        mitgliedDAO = new MitgliedDAO();
        aktuelleListe = new ArrayList<>();
        mitglieder = new MitgliederTableModel(aktuelleListe);

        // 1. Initialisierung für das Briefzentrum (Seite 2)
        fxBriefPanel = new JFXPanel();
        Platform.runLater(() -> {
            webViewBrief = new WebView();
            fxBriefPanel.setScene(new javafx.scene.Scene(webViewBrief));
            webViewBrief.getEngine().loadContent("<html><body style='font-family:Arial; margin:10px;'><h3>Brief-Vorschau</h3></body></html>");
        });

        // 2. Initialisierung für den Template-Editor (Seite 3)
        fxEditorPanel = new JFXPanel();
        Platform.runLater(() -> {
            webViewEditor = new WebView();
            fxEditorPanel.setScene(new javafx.scene.Scene(webViewEditor));
            webViewEditor.getEngine().loadContent("<html><body contenteditable='true' style='font-family:Arial; margin:10px;'><h3>HTML-Template-Editor</h3></body></html>");
        });

        // CardLayout sauber initialisieren OHNE FlowLayout-Überschreibung
        cardLayout = new CardLayout();
        kartenPanel = new JPanel(cardLayout);
        kartenPanel.add(fxBriefPanel, "VORSCHAU");

        setTitle("AlgDatabank - Mitgliederverwaltung & Briefzentrum");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 900);
        setLocationRelativeTo(null);

        aktuelleListe = mitgliedDAO.readAll();
        mitglieder = new MitgliederTableModel(aktuelleListe);
        tabelle = new JTable(mitglieder);
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

        // --- SEITE 2: BRIEF- & DRUCKZENTRUM ---
        JPanel druckzentrumPanel = erzeugeDruckZentrumPanel();

        // --- SEITE 3: TEMPLATE-EDITOR ---
        JPanel editorPanel = erzeugeTemplateEditorPanel();

        tabbedPane.addTab("Mitglieder-Verwaltung", verwaltungsPanel);
        tabbedPane.addTab("Brief- & Druckzentrum", druckzentrumPanel);
        tabbedPane.addTab("Template-Editor (HTML)", editorPanel);

        // Styling der Tabs
        java.awt.Color farbeAktivBg = new java.awt.Color(0, 102, 204);
        java.awt.Color farbeAktivFg = java.awt.Color.WHITE;
        java.awt.Color farbeInaktivBg = new java.awt.Color(45, 45, 45);
        java.awt.Color farbeInaktivFg = new java.awt.Color(180, 180, 180);

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            JLabel lblTab = new JLabel(tabbedPane.getTitleAt(i), SwingConstants.CENTER);
            lblTab.setOpaque(true);
            lblTab.setPreferredSize(new Dimension(180, 30));
            lblTab.setBorder(BorderFactory.createLineBorder(java.awt.Color.BLACK, 1));

            if (i == 0) {
                lblTab.setBackground(farbeAktivBg);
                lblTab.setForeground(farbeAktivFg);
                lblTab.setFont(lblTab.getFont().deriveFont(java.awt.Font.BOLD));
            } else {
                lblTab.setBackground(farbeInaktivBg);
                lblTab.setForeground(farbeInaktivFg);
                lblTab.setFont(lblTab.getFont().deriveFont(java.awt.Font.PLAIN));
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
                        lblTab.setFont(lblTab.getFont().deriveFont(java.awt.Font.BOLD));
                    } else {
                        lblTab.setBackground(farbeInaktivBg);
                        lblTab.setForeground(farbeInaktivFg);
                        lblTab.setFont(lblTab.getFont().deriveFont(java.awt.Font.PLAIN));
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
        lblAusgewaehltesMitglied.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 13));
        lblAusgewaehltesMitglied.setForeground(new java.awt.Color(0, 102, 204));
        topPanel.add(lblAusgewaehltesMitglied, BorderLayout.NORTH);

        JPanel aktionsLeiste = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        aktionsLeiste.add(new JLabel("Textbausteine:"));

        String[] vorlagenNamen = {"-- Bitte wählen --", "Willkommen im Verein", "Beitragserinnerung", "Einladung Hauptversammlung"};
        comboVorlagen = new JComboBox<>(vorlagenNamen);
        aktionsLeiste.add(comboVorlagen);

        btnTextLaden = new JButton("Brief-Vorlage öffnen (.docx, .html, .odt, .rtf, .txt)");
        btnTextSpeichern = new JButton("Aktuellen Text speichern");
        aktionsLeiste.add(btnTextLaden);
        aktionsLeiste.add(btnTextSpeichern);

        topPanel.add(aktionsLeiste, BorderLayout.SOUTH);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        mainPanel.add(kartenPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 15, 0));

        JPanel scannerPanel = new JPanel(new GridBagLayout());
        scannerPanel.setBorder(BorderFactory.createTitledBorder("Dokumenten-Scanner (SANE / Paperless)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtScanName = new JTextField("dokument_scan", 12);
        String[] ziele = {"Direkt an Paperless-ngx übergeben", "Lokal als Bild/PDF speichern"};
        comboScanZiel = new JComboBox<>(ziele);
        btnScanStarten = new JButton("Scan-Vorgang starten");
        btnScanStarten.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 11));
        btnScanStarten.setBackground(new java.awt.Color(40, 167, 69));
        btnScanStarten.setForeground(java.awt.Color.WHITE);

        gbc.gridx = 0;
        gbc.gridy = 0;
        scannerPanel.add(new JLabel("Datei-Name:"), gbc);
        gbc.gridx = 1;
        scannerPanel.add(txtScanName, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        scannerPanel.add(new JLabel("Ziel-Ordner:"), gbc);
        gbc.gridx = 1;
        scannerPanel.add(comboScanZiel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        scannerPanel.add(btnScanStarten, gbc);

        JPanel druckPanel = new JPanel(new BorderLayout(5, 5));
        druckPanel.setBorder(BorderFactory.createTitledBorder("Drucken & Etiketten"));

        JPanel etikettenOben = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        etikettenOben.add(new JLabel("Bogen-Pos (1-21):"));
        spinnerEtikettPos = new JSpinner(new SpinnerNumberModel(1, 1, 21, 1));
        etikettenOben.add(spinnerEtikettPos);
        btnEtikettDrucken = new JButton("Etikett drucken");
        etikettenOben.add(btnEtikettDrucken);

        JPanel briefUnten = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        btnUmschlagDrucken = new JButton("Umschlag (DL)");
        btnVorschau = new JButton("Vorschau...");
        btnBriefDrucken = new JButton("Brief drucken");
        btnBriefDrucken.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 11));

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
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel werkzeugLeiste = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        btnEditorLaden = new JButton("Template öffnen");
        btnEditorSpeichern = new JButton("Template speichern");
        werkzeugLeiste.add(btnEditorLaden);
        werkzeugLeiste.add(btnEditorSpeichern);

        String[] platzhalter = {"-- Platzhalter einfügen --", "MNr", "Anrede", "Titel", "Vorname", "Nachname", "Strasse", "PLZ", "Ort", "Eintritt", "Austritt", "Brief_Anrede", "Datum", "Jahr", "Betrag"};
        comboPlatzhalter = new JComboBox<>(platzhalter);
        werkzeugLeiste.add(comboPlatzhalter);

        panel.add(werkzeugLeiste, BorderLayout.NORTH);
        panel.add(fxEditorPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel erzeugeFormularPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Mitglieder-Details (Bearbeiten)"));

        JPanel felderPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtMNr = new JTextField(5);
        txtMNr.setEditable(false);
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

        gbc.gridx = 0;
        gbc.gridy = 5;
        felderPanel.add(new JLabel("Hinweise:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 5;
        felderPanel.add(txtHinweise, gbc);
        gbc.gridwidth = 1;

        panel.add(felderPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnNeu = new JButton("Felder leeren");
        btnNeuAufnehmen = new JButton("Neu aufnehmen");
        btnNeuAufnehmen.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
        btnNeuAufnehmen.setBackground(new java.awt.Color(40, 167, 69));
        btnNeuAufnehmen.setForeground(java.awt.Color.WHITE);

        btnSpeichern = new JButton("Änderungen Speichern");
        btnLoeschen = new JButton("Mitglied löschen");
        btnLoeschen.setForeground(java.awt.Color.RED);

        JButton btnExcelExport = new JButton("Excel-Export");

        buttonPanel.add(btnNeu);
        buttonPanel.add(btnNeuAufnehmen);
        buttonPanel.add(btnSpeichern);
        buttonPanel.add(btnLoeschen);
        buttonPanel.add(btnExcelExport);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void addKomponente(JPanel p, JLabel label, JTextField tf, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x;
        gbc.gridy = y;
        p.add(label, gbc);
        gbc.gridx = x + 1;
        p.add(tf, gbc);
    }

    private void registriereEvents() {
        tabelle.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tabelle.getSelectedRow();
                int[] selectedRows = tabelle.getSelectedRows();

                if (selectedRows.length > 1) {
                    lblAusgewaehltesMitglied.setText("Aktuell ausgewählt: " + selectedRows.length + " Mitglieder für Seriendruck.");
                    ausgewaehltesMitglied = null;
                } else if (selectedRow != -1) {
                    int modelRow = tabelle.convertRowIndexToModel(selectedRow);
                    Mitglied m = mitglieder.getMitgliedAt(modelRow);
                    ausgewaehltesMitglied = m;

                    lblAusgewaehltesMitglied.setText("Aktuell ausgewählt: " + m.getVorname() + " " + m.getNachname() + " (M-Nr: " + m.getMNr() + ")");

                    txtMNr.setText(String.valueOf(m.getMNr()));
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

                    String aktuellerInhalt = getEditorInhalt();
                    if (aktuellerInhalt != null && !aktuellerInhalt.trim().isEmpty() && !aktuellerInhalt.contains("<h3>HTML-Template-Editor</h3>")) {
                        aktualisiereBriefVorschau(aktuellerInhalt);
                    }
                }
            }
        });

        btnNeu.addActionListener(e -> leereFormularFelder());

        btnNeuAufnehmen.addActionListener(e -> {
            try {
                if (txtNachname.getText().trim().isEmpty() || txtVorname.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Nachname und Vorname dürfen nicht leer sein.", "Eingabefehler", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Mitglied m = erzeugeMitgliedAusFormular();
                int naechsteNummer = 1;
                for (Mitglied ex : aktuelleListe) {
                    if (ex.getMNr() >= naechsteNummer) {
                        naechsteNummer = ex.getMNr() + 1;
                    }
                }
                m.setMNr(naechsteNummer);

                mitgliedDAO.save(m);
                reloadTable();
                leereFormularFelder();
                JOptionPane.showMessageDialog(this, "Mitglied erfolgreich unter M-Nr " + naechsteNummer + " neu aufgenommen.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Speichern: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSpeichern.addActionListener(e -> {
            if (txtMNr.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Kein Mitglied zum Aktualisieren ausgewählt.", "Fehler", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                Mitglied m = erzeugeMitgliedAusFormular();
                m.setMNr(Integer.parseInt(txtMNr.getText()));

                mitgliedDAO.save(m);
                reloadTable();
                JOptionPane.showMessageDialog(this, "Änderungen erfolgreich gespeichert.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Aktualisieren: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnLoeschen.addActionListener(e -> {
            if (txtMNr.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Kein Mitglied zum Löschen ausgewählt.", "Fehler", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int mNr = Integer.parseInt(txtMNr.getText());
            int bestaetigung = JOptionPane.showConfirmDialog(this, "Soll das Mitglied Nr. " + mNr + " wirklich unwiderruflich gelöscht werden?", "Löschen bestätigen", JOptionPane.YES_NO_OPTION);
            if (bestaetigung == JOptionPane.YES_OPTION) {
                mitgliedDAO.delete(mNr);
                reloadTable();
                leereFormularFelder();
                JOptionPane.showMessageDialog(this, "Mitglied gelöscht.");
            }
        });
    }

    private void leereFormularFelder() {
        txtMNr.setText("");
        txtEintritt.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        txtAustritt.setText("");
        txtAnrede.setText("Herr");
        txtTitel.setText("");
        txtNachname.setText("");
        txtVorname.setText("");
        txtStrasse.setText("");
        txtPLZ.setText("");
        txtOrt.setText("");
        txtTelefon.setText("");
        txtEmail.setText("");
        txtHinweise.setText("");
        tabelle.clearSelection();
        ausgewaehltesMitglied = null;
        lblAusgewaehltesMitglied.setText("Aktuell ausgewählt: (Kein Mitglied in der Tabelle markiert)");
    }

    private Mitglied erzeugeMitgliedAusFormular() {
        Mitglied m = new Mitglied();
        m.setEintritt(txtEintritt.getText());
        m.setAustritt(txtAustritt.getText().trim().isEmpty() ? null : txtAustritt.getText());
        m.setAnrede(txtAnrede.getText());
        m.setTitel(txtTitel.getText().trim().isEmpty() ? null : txtTitel.getText());
        m.setNachname(txtNachname.getText());
        m.setVorname(txtVorname.getText());
        m.setStrasse(txtStrasse.getText());
        m.setPostleitzahl(txtPLZ.getText());
        m.setOrt(txtOrt.getText());
        m.setTelefon(txtTelefon.getText().trim().isEmpty() ? null : txtTelefon.getText());
        m.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText());
        m.setHinweise(txtHinweise.getText().trim().isEmpty() ? null : txtHinweise.getText());
        return m;
    }

    private void initialisiereDruckLogik() {
        comboVorlagen.addActionListener(e -> {
            int index = comboVorlagen.getSelectedIndex();
            if (index > 0) {
                String standardTemplate = ladeStandardTemplate(index);
                setEditorInhalt(standardTemplate);
                aktualisiereBriefVorschau(standardTemplate);
            }
        });

        btnTextLaden.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Text- & HTML-Dateien", "html", "htm", "txt", "docx", "odt", "rtf"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    String inhalt = sb.toString();
                    setEditorInhalt(inhalt);
                    aktualisiereBriefVorschau(inhalt);
                    JOptionPane.showMessageDialog(this, "Vorlage erfolgreich geladen.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Laden: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnTextSpeichern.addActionListener(e -> {
            String inhalt = getEditorInhalt();
            JFileChooser chooser = new JFileChooser();
            if (ausgewaehltesMitglied != null) {
                chooser.setSelectedFile(new File("Brief_" + ausgewaehltesMitglied.getNachname() + ".html"));
            }
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"))) {
                    bw.write(inhalt);
                    JOptionPane.showMessageDialog(this, "Datei erfolgreich gespeichert.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Speichern: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnBriefDrucken.addActionListener(e -> fuehreDruckAus());

        java.awt.Color farbeAktivBg = new java.awt.Color(0, 102, 204);
        java.awt.Color farbeAktivFg = java.awt.Color.WHITE;

        btnVorschau.addActionListener(e -> {
            java.awt.Color originalBg = btnVorschau.getBackground();
            java.awt.Color originalFg = btnVorschau.getForeground();

            btnVorschau.setBackground(farbeAktivBg);
            btnVorschau.setForeground(farbeAktivFg);
            btnVorschau.setContentAreaFilled(true);

            String aktuellerInhalt = getEditorInhalt();

            if (aktuellerInhalt == null || aktuellerInhalt.trim().isEmpty() || aktuellerInhalt.contains("<h3>HTML-Template-Editor</h3>")) {
                JOptionPane.showMessageDialog(this, "Kein Text oder Template im Editor vorhanden.", "Vorschau nicht möglich", JOptionPane.WARNING_MESSAGE);
                btnVorschau.setBackground(originalBg);
                btnVorschau.setForeground(originalFg);
                return;
            }

            String aufbereitetesHtml;
            if (ausgewaehltesMitglied != null) {
                aufbereitetesHtml = ersetzePlatzhalter(aktuellerInhalt, ausgewaehltesMitglied);
            } else {
                aufbereitetesHtml = aktuellerInhalt.replace("[Brief_Anrede]", "Sehr geehrte Damen und Herren,");
                aufbereitetesHtml = aufbereitetesHtml.replaceAll("\\[(.*?)\\]", "<span style='color:red; font-weight:bold;'>[$1]</span>");
            }

            DruckVorschauDialog dialog = new DruckVorschauDialog(this, "Druckvorschau - Briefzentrum", aufbereitetesHtml);
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    btnVorschau.setBackground(originalBg);
                    btnVorschau.setForeground(originalFg);
                }
            });
            dialog.setVisible(true);
        });

        btnEtikettDrucken.addActionListener(e -> {
            int startPos = 1;
            if (spinnerEtikettPos.getValue() instanceof Number) {
                startPos = ((Number) spinnerEtikettPos.getValue()).intValue();
            }

            int selectedRow = tabelle.getSelectedRow();
            int[] selectedRows = tabelle.getSelectedRows();

            List<Mitglied> zuDruckendeMitglieder = new ArrayList<>();

            if (selectedRows != null && selectedRows.length > 1) {
                for (int viewRow : selectedRows) {
                    int modelRow = tabelle.convertRowIndexToModel(viewRow);
                    zuDruckendeMitglieder.add(mitglieder.getMitgliedAt(modelRow));
                }
            } else if (selectedRow != -1) {
                int modelRow = tabelle.convertRowIndexToModel(selectedRow);
                zuDruckendeMitglieder.add(mitglieder.getMitgliedAt(modelRow));
            } else {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie mindestens ein Mitglied aus der Tabelle aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
                return;
            }

            java.awt.Color origBg = btnEtikettDrucken.getBackground();
            java.awt.Color origFg = btnEtikettDrucken.getForeground();
            btnEtikettDrucken.setBackground(farbeAktivBg);
            btnEtikettDrucken.setForeground(farbeAktivFg);

            fuehreEtikettenDruckAus(zuDruckendeMitglieder, startPos);

            btnEtikettDrucken.setBackground(origBg);
            btnEtikettDrucken.setForeground(origFg);
        });
    }

    private void reloadTable() {
        aktuelleListe = mitgliedDAO.readAll();
        mitglieder = new MitgliederTableModel(aktuelleListe);
        tabelle.setModel(mitglieder);
        anpassenSpaltenBreiten();
    }

    // --- LINUX-ETIKETTEN-DRUCK (AVERY 7161) ---
    private void fuehreEtikettenDruckAus(List<Mitglied> mitgliederListe, int startPosition) {
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        job.setJobName("Alg_Databank - Etikettendruck Avery 7161");

        final double mmToPoint = 2.83465;

        javax.print.attribute.PrintRequestAttributeSet attributes = new javax.print.attribute.HashPrintRequestAttributeSet();
        attributes.add(javax.print.attribute.standard.MediaSizeName.ISO_A4);
        attributes.add(javax.print.attribute.standard.OrientationRequested.PORTRAIT);

        java.awt.print.PageFormat pf = job.defaultPage();
        java.awt.print.Paper paper = new java.awt.print.Paper();
        double breiteA4 = 210 * mmToPoint;
        double hoeheA4 = 297 * mmToPoint;
        paper.setSize(breiteA4, hoeheA4);

        double hardwareRand = 4.0 * mmToPoint;
        paper.setImageableArea(hardwareRand, hardwareRand, breiteA4 - (hardwareRand * 2), hoeheA4 - (hardwareRand * 2));
        pf.setPaper(paper);
        pf.setOrientation(java.awt.print.PageFormat.PORTRAIT);

        final double labelWidth = 63.5 * mmToPoint;
        final double labelHeight = 46.6 * mmToPoint;
        final double marginLeft = 7.2 * mmToPoint;
        final double marginTop = 15.1 * mmToPoint;
        final double columnGap = 2.5 * mmToPoint;

        job.setPrintable(new java.awt.print.Printable() {
            @Override
            public int print(java.awt.Graphics graphics, java.awt.print.PageFormat pageFormat, int pageIndex) throws java.awt.print.PrinterException {
                int labelsPerPage = 21;
                int totalNeededLabels = mitgliederListe.size() + (startPosition - 1);
                int maxPages = (int) Math.ceil((double) totalNeededLabels / labelsPerPage);

                if (pageIndex >= maxPages) {
                    return java.awt.print.Printable.NO_SUCH_PAGE;
                }

                java.awt.Graphics2D g2d = (java.awt.Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX() - hardwareRand, pageFormat.getImageableY() - hardwareRand);
                g2d.setColor(java.awt.Color.BLACK);
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                for (int i = 0; i < labelsPerPage; i++) {
                    int currentLabelPosOnSheet = i + 1;
                    int absoluteLabelIndex = (pageIndex * labelsPerPage) + currentLabelPosOnSheet;

                    if (absoluteLabelIndex < startPosition) {
                        continue;
                    }

                    int mitgliedIndex = absoluteLabelIndex - startPosition;
                    if (mitgliedIndex >= mitgliederListe.size()) {
                        break;
                    }

                    Mitglied m = mitgliederListe.get(mitgliedIndex);

                    int col = i % 3;
                    int row = i / 3;

                    double x = marginLeft + (col * (labelWidth + columnGap));
                    double y = marginTop + (row * labelHeight);

                    float textX = (float) (x + 5 * mmToPoint);
                    float textY = (float) (y + 12 * mmToPoint);

                    g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
                    String zeile1 = m.getAnrede();
                    if (m.getTitel() != null && !m.getTitel().isEmpty()) {
                        zeile1 += " " + m.getTitel();
                    }
                    g2d.drawString(zeile1, textX, textY);

                    textY += 14;
                    g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 10));
                    g2d.drawString(m.getVorname() + " " + m.getNachname(), textX, textY);

                    textY += 14;
                    g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
                    g2d.drawString(m.getStrasse(), textX, textY);

                    textY += 24;
                    g2d.drawString(m.getPostleitzahl() + " " + m.getOrt(), textX, textY);
                }
                return java.awt.print.Printable.PAGE_EXISTS;
            }
        }, pf);

        boolean doPrint = job.printDialog(attributes);
        if (!doPrint) {
            return;
        }

        Thread printThread = new Thread(() -> {
            try {
                job.print(attributes);
                javafx.application.Platform.runLater(() -> zeigeErfolgsMeldung());
            } catch (java.awt.print.PrinterException ex) {
                ex.printStackTrace();
            }
        });
        printThread.setDaemon(true);
        printThread.start();
    }

    private void zeigeErfolgsMeldung() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Drucken erfolgreich");
        alert.setHeaderText(null);
        alert.setContentText("Der Etikettendruck (Avery 7161) wurde erfolgreich an den Drucker übergeben.");

        javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #ffffff;");
        dialogPane.lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: #ffffff;"));
        alert.showAndWait();
    }

    // --- VOLLSTÄNDIG AUSPROGRAMMIERTE HILFSMETHODEN FÜR BRIEFZENTRUM & HTML-EDITOR ---

    private void initialisiereEditorLogik() {
        comboPlatzhalter.addActionListener(e -> {
            int index = comboPlatzhalter.getSelectedIndex();
            if (index > 0) {
                String selectedPlaceholder = "[" + comboPlatzhalter.getSelectedItem().toString() + "]";
                Platform.runLater(() -> {
                    if (webViewEditor != null) {
                        // Nutzt JavaScript, um den Platzhalter präzise an der aktuellen Cursorposition im Editor zu platzieren
                        String js = "insertAtCursor('" + selectedPlaceholder + "');";
                        webViewEditor.getEngine().executeScript(
                                "function insertAtCursor(text) {" +
                                        "  var sel = window.getSelection();" +
                                        "  if (sel.getRangeAt && sel.rangeCount) {" +
                                        "    var range = sel.getRangeAt(0);" +
                                        "    range.deleteContents();" +
                                        "    range.insertNode(document.createTextNode(text));" +
                                        "  }" +
                                        "}"
                        );
                        webViewEditor.getEngine().executeScript("insertAtCursor('" + selectedPlaceholder + "');");
                    }
                });
                comboPlatzhalter.setSelectedIndex(0); // Wieder auf Standard zurücksetzen
            }
        });
    }

    private String ladeStandardTemplate(int index) {
        String datum = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:Arial, sans-serif; margin:40px; line-height:1.5;'>");

        // Briefkopf-Struktur
        sb.append("<div style='text-align:right; font-size:10px; color:#666;'>Schutzverein Biotop e.V. | Postfach 1234 | Hannover</div><br><br>");
        sb.append("<div style='font-size:11px;'>[Anrede] [Titel] [Vorname] [Nachname]<br>[Strasse]<br><b>[PLZ] [Ort]</b></div><br><br>");
        sb.append("<div style='text-align:right;'>Hannover, den ").append(datum).append("</div><br>");

        switch (index) {
            case 1: // Willkommen
                sb.append("<h4><b>Betreff: Herzlich willkommen im Naturschutzverein!</b></h4><br>");
                sb.append("<p>[Brief_Anrede]</p>");
                sb.append("<p>wir freuen uns außerordentlich, Sie unter der Mitgliedsnummer <b>[MNr]</b> seit dem <b>[Eintritt]</b> in unseren Reihen begrüßen zu dürfen.</p>");
                sb.append("<p>Gemeinsam werden wir uns wirksam für den Erhalt der lokalen Naturräume einsetzen. Ihre Mitgliedskarte geht Ihnen in den nächsten Tagen zu.</p>");
                break;
            case 2: // Beitragserinnerung
                sb.append("<h4><b>Betreff: Erinnerung an den ausstehenden Jahresbeitrag [Jahr]</b></h4><br>");
                sb.append("<p>[Brief_Anrede]</p>");
                sb.append("<p>Sicherlich ist es im Alltagstrubel untergegangen: Ihr jährlicher Mitgliedsbeitrag für das laufende Jahr in Höhe von <b>[Betrag] €</b> steht aktuell noch aus.</p>");
                sb.append("<p>Um unsere laufenden Pflegeprojekte stabil fortführen zu können, bitten wir um Überweisung des Betrags auf unser Vereinskonto bis zum Ende dieses Monats.</p>");
                break;
            case 3: // Einladung HV
                sb.append("<h4><b>Betreff: Einladung zur ordentlichen Hauptversammlung</b></h4><br>");
                sb.append("<p>[Brief_Anrede]</p>");
                sb.append("<p>hiermit laden wir Sie recht herzlich zu unserer diesjährigen Hauptversammlung im Vereinshaus ein.</p>");
                sb.append("<p><b>Tagesordnungspunkte:</b><br>1. Bericht des Vorstands<br>2. Entlastung des Schatzmeisters<br>3. Zukünftige Projekte im Biotopschutz</p>");
                break;
            default:
                return "<html><body></body></html>";
        }

        sb.append("<br><p>Mit freundlichen Grüßen,<br><br><b>Der Vorstand</b></p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void setEditorInhalt(String html) {
        Platform.runLater(() -> {
            if (webViewEditor != null) {
                // Verhindert Skript-Abstürze bei Zeilenumbrüchen im String
                String escapedHtml = html.replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
                webViewEditor.getEngine().executeScript("document.body.innerHTML = '" + escapedHtml + "';");
            }
        });
    }

    private String getEditorInhalt() {
        try {
            final String[] ergebnis = { "" };
            // Da WebView asynchron auf dem JavaFX-Thread läuft, müssen wir auf die Antwort warten
            java.util.concurrent.FutureTask<String> task = new java.util.concurrent.FutureTask<>(() -> {
                if (webViewEditor != null) {
                    return (String) webViewEditor.getEngine().executeScript("document.documentElement.outerHTML;");
                }
                return "<html><body><h3>HTML-Template-Editor</h3></body></html>";
            });
            Platform.runLater(task);
            return task.get();
        } catch (Exception ex) {
            return "<html><body><h3>HTML-Template-Editor</h3></body></html>";
        }
    }

    private void aktualisiereBriefVorschau(String html) {
        Platform.runLater(() -> {
            if (webViewBrief != null) {
                String gerendertesHtml;
                if (ausgewaehltesMitglied != null) {
                    gerendertesHtml = ersetzePlatzhalter(html, ausgewaehltesMitglied);
                } else {
                    gerendertesHtml = html.replace("[Brief_Anrede]", "Sehr geehrte Damen und Herren,");
                }
                webViewBrief.getEngine().loadContent(gerendertesHtml);
            }
        });
    }

    private String ersetzePlatzhalter(String html, Mitglied m) {
        if (html == null) return "";
        String briefAnrede = "Herr".equalsIgnoreCase(m.getAnrede()) ? "Sehr geehrter Herr " + m.getNachname() + "," : "Sehr geehrte Frau " + m.getNachname() + ",";
        if (m.getTitel() != null && !m.getTitel().isEmpty()) {
            briefAnrede = "Herr".equalsIgnoreCase(m.getAnrede()) ? "Sehr geehrter Herr " + m.getTitel() + " " + m.getNachname() + "," : "Sehr geehrte Frau " + m.getTitel() + " " + m.getNachname() + ",";
        }

        String aktJahr = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        String aktDatum = new SimpleDateFormat("dd.MM.yyyy").format(new Date());

        return html
                .replace("[MNr]", String.valueOf(m.getMNr()))
                .replace("[Anrede]", m.getAnrede() != null ? m.getAnrede() : "")
                .replace("[Titel]", m.getTitel() != null ? m.getTitel() : "")
                .replace("[Vorname]", m.getVorname() != null ? m.getVorname() : "")
                .replace("[Nachname]", m.getNachname() != null ? m.getNachname() : "")
                .replace("[Strasse]", m.getStrasse() != null ? m.getStrasse() : "")
                .replace("[PLZ]", m.getPostleitzahl() != null ? m.getPostleitzahl() : "")
                .replace("[Ort]", m.getOrt() != null ? m.getOrt() : "")
                .replace("[Eintritt]", m.getEintritt() != null ? m.getEintritt() : "")
                .replace("[Austritt]", m.getAustritt() != null ? m.getAustritt() : "")
                .replace("[Brief_Anrede]", briefAnrede)
                .replace("[Datum]", aktDatum)
                .replace("[Jahr]", aktJahr)
                .replace("[Betrag]", "45,00"); // Standard-Vereinsbeitrag als Fallback
    }

    private void fuehreDruckAus() {
        String htmlInhalt = getEditorInhalt();
        if (htmlInhalt == null || htmlInhalt.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kein Text zum Drucken vorhanden.", "Fehler", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Alg_Databank - Briefdruck");

        if (ausgewaehltesMitglied != null) {
            htmlInhalt = ersetzePlatzhalter(htmlInhalt, ausgewaehltesMitglied);
        }

        final String finalHtml = htmlInhalt;

        // Nutzt die Standard Swing JEditorPane Druckschnittstelle für stabile Linux CUPS-Ausgabe
        JEditorPane printPane = new JEditorPane("text/html", finalHtml);
        printPane.setSize(595, 842); // Standard A4 Punktmaße für das korrekte Layout

        job.setPrintable(printPane.getPrintable(null, null));

        if (job.printDialog()) {
            try {
                job.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Druckfehler: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- EXEKUTIERBARE MAIN METHODE ---
    public static void main(String[] args) {
        // Schickes System-Look-and-Feel für eine native Anwendungsoptik unter Ubuntu/Linux Mint
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback auf Standard Swing Look-and-Feel falls unauffindbar
        }

        // GUI sicher auf dem Event Dispatch Thread initialisieren
        SwingUtilities.invokeLater(() -> {
            AlgDatabankGui gui = new AlgDatabankGui();
            gui.setVisible(true);
        });
    }
}