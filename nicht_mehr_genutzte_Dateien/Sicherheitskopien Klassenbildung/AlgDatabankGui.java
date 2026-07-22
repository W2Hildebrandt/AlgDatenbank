import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.StyledEditorKit;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// für JavaFx
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;

import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import java.util.stream.Collectors;

import java.util.logging.Logger;

public class AlgDatabankGui extends JFrame {

    private static final Logger logger = Logger.getLogger(AlgDatabankGui.class.getName());

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

    // GUI Komponenten für das Druckzentrum (Seite 2)
    //private JTextArea txtBriefText;
    //Ersatz JFXPanel
    private JFXPanel jfxDruckZentrumPanel;
    private WebEngine druckZentrumEngine; // Zum Steuern des Inhalts

    private JComboBox<String> comboVorlagen;
    private JButton btnTextLaden, btnTextSpeichern, btnBriefDrucken;
    private JLabel lblAusgewaehltesMitglied;

    // GUI Komponenten für den Dokumenten-Scanner (Neu auf Seite 2)
    private JTextField txtScanName;
    private JComboBox<String> comboScanZiel;
    private JButton btnScanStarten;

    // GUI Komponenten für den Template-Editor (Seite 3)
    private JComboBox<String> comboPlatzhalter;
    private JButton btnEditorLaden, btnEditorSpeichern;

    private JComboBox<String> comboSchriftart;
    private JComboBox<Integer> comboSchriftgroesse;
    private JButton btnFett, btnKursiv, btnUnterstrichen;

    // CRUD Buttons
    private JButton btnNeu, btnSpeichern, btnLoeschen; // btnVorschau;
    private JButton btnNeuAufnehmen;
    private JButton btnUmschlagDrucken;

    // Instanzvariablen für den Etikettendruck
    private JSpinner spinnerEtikettPos;
    private JButton btnEtikettDrucken;

    // Instanzvariablen für JavaFX
    private JFXPanel jfxEditorPanel;
    private WebEngine webEngine; // für den Template-Editor (editorWebView)

    // Schalter für zum Leeren des Formulars
    private boolean isUpdating = false;

    // für die neue Vorschau (Preview) und für den Template-Editor (Editor)
    private WebView vorschauWebView; // Vorschau-WebView (Brief- & Druckzentrum)
    private WebView editorWebView;   // separater WebView für den Template-Editor

    private JButton btnPlatzhalterEinfuegen;

    private String aktuellGeladenesTemplate = null; // Hält den Inhalt nach dem JFileChooser-Import

    // slider für Druckzentrum
    private JSlider zoomSlider; // NEU
    private JSpinner spinnerSeitenwahl;

    private int maxSeiten; // Interne Verwendung für die maximale Anzahl der Seiten

    private boolean isResizing = false;
    private JLabel lblSeitenInfo;

    private javafx.scene.control.ScrollPane fxScrollPane;

    // Globale Klassenvariable im DruckzentrumPanel definieren:
    private javafx.scene.layout.Pane fxWrapperPane;

    public AlgDatabankGui() {
        mitgliedDAO = new MitgliedDAO();

        this.btnPlatzhalterEinfuegen = new JButton("Platzhalter einfügen"); // Hier erfolgt die Erzeugung!

        // 1. Initialisiere die Buttons HIER, bevor sie verwendet werden
        btnNeu = new JButton("Neu");
        btnNeuAufnehmen = new JButton("Neu Aufnehmen");
        btnSpeichern = new JButton("Speichern");
        btnLoeschen = new JButton("Löschen");

        setTitle("AlgDatabank - Mitgliederverwaltung & Briefzentrum");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 900); // Leicht vergrößert für das neue Scanner-Panel
        setLocationRelativeTo(null);

        // Maximierter Modus
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Datum erstellen (wird bei Bedarf lokal ermittelt)
        //String personalisiertesHtml = DruckVorbereiter.ersetzePlatzhalter(template, m, heute);

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
        JPanel formPanel = erzeugeMitgliederFormular();

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


        initialisiereDruckLogik();
        initialisiereEditorLogik();

        registriereEvents();
    }

    // Beispiel innerhalb der Methode, die den Inhalt lädt:
    private void aktualisiereSeitenAnzahl(int neueAnzahl) {
        this.maxSeiten = neueAnzahl;

        // Spinner-Modell ebenfalls aktualisieren, damit die UI korrekt reagiert
        if (spinnerSeitenwahl != null) {
            spinnerSeitenwahl.setModel(new SpinnerNumberModel(1, 1, Math.max(1, this.maxSeiten), 1));
        }

        // NEU: Label aktualisieren
        if (lblSeitenInfo != null) {
            lblSeitenInfo.setText(" " + maxSeiten);
        }
    }



// ... innerhalb deiner Klasse AlgDatabankGui ...

    /**
     * Gibt das aktuelle Datum im Format TT.MM.JJJJ zurück.
     */
    public String heutigesDatumToString() {
        // 1. Das aktuelle Datum holen
        LocalDate heute = LocalDate.now();

        // 2. Formatierer definieren
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        // 3. Als String formatieren und zurückgeben
        return heute.format(formatter);
    }

    private JPanel erzeugeMitgliederFormular() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Mitglieder-Daten bearbeiten"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Initialisierung der Felder (falls nicht bereits im Konstruktor geschehen)
        txtMNr = new JTextField(5);
        txtEintritt = new JTextField(10);
        txtAustritt = new JTextField(10);
        txtAnrede = new JTextField(10);
        txtTitel = new JTextField(10);
        txtNachname = new JTextField(15);
        txtVorname = new JTextField(15);
        txtStrasse = new JTextField(20);
        txtPLZ = new JTextField(5);
        txtOrt = new JTextField(15);
        txtTelefon = new JTextField(15);
        txtEmail = new JTextField(15);
        txtHinweise = new JTextField(20);

        // Layout hinzufügen
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("MNr:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtMNr, gbc);
        gbc.gridx = 2;
        formPanel.add(new JLabel("Eintritt:"), gbc);
        gbc.gridx = 3;
        formPanel.add(txtEintritt, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Anrede:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtAnrede, gbc);
        gbc.gridx = 2;
        formPanel.add(new JLabel("Titel:"), gbc);
        gbc.gridx = 3;
        formPanel.add(txtTitel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Vorname:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtVorname, gbc);
        gbc.gridx = 2;
        formPanel.add(new JLabel("Nachname:"), gbc);
        gbc.gridx = 3;
        formPanel.add(txtNachname, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Strasse:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtStrasse, gbc);
        gbc.gridx = 2;
        formPanel.add(new JLabel("PLZ / Ort:"), gbc);
        gbc.gridx = 3;
        JPanel ortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ortPanel.add(txtPLZ);
        ortPanel.add(new JLabel(" "));
        ortPanel.add(txtOrt);
        formPanel.add(ortPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Telefon:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtTelefon, gbc);
        gbc.gridx = 2;
        formPanel.add(new JLabel("E-Mail:"), gbc);
        gbc.gridx = 3;
        formPanel.add(txtEmail, gbc);

        // CRUD Buttons hinzufügen
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(btnNeu);
        buttonPanel.add(btnNeuAufnehmen);
        buttonPanel.add(btnSpeichern);
        buttonPanel.add(btnLoeschen);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 4;
        formPanel.add(buttonPanel, gbc);

        return formPanel;
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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- 1. SIDEBAR (WEST) ---
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(320, 0)); // Feste Breite für die Sidebar

        // Infobereich
        lblAusgewaehltesMitglied = new JLabel("<html><b>Ausgewählt:</b><br/>Gunnar Bargholz</html>");
        lblAusgewaehltesMitglied.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        sidebar.add(lblAusgewaehltesMitglied);

        // Textbaustein-Panel
        JPanel textPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        textPanel.setBorder(BorderFactory.createTitledBorder("Textbausteine"));
        String[] vorlagenNamen = {"-- Bitte wählen --", "Willkommen im Verein", "Beitragserinnerung", "Einladung HV"};
        comboVorlagen = new JComboBox<>(vorlagenNamen);
        btnTextLaden = new JButton("Brief-Vorlage öffnen");
        btnTextSpeichern = new JButton("Aktuellen Text speichern");
        textPanel.add(comboVorlagen);
        textPanel.add(btnTextLaden);
        textPanel.add(btnTextSpeichern);
        sidebar.add(textPanel);
        sidebar.add(Box.createVerticalStrut(15)); // Abstand

        // Scanner-Panel
        JPanel scannerPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        scannerPanel.setBorder(BorderFactory.createTitledBorder("Dokumenten-Scanner"));
        txtScanName = new JTextField("dokument_scan");
        comboScanZiel = new JComboBox<>(new String[]{"An Paperless-ngx", "Lokal speichern"});
        btnScanStarten = new JButton("Scan-Vorgang starten");
        scannerPanel.add(new JLabel("Name:"));
        scannerPanel.add(txtScanName);
        scannerPanel.add(new JLabel("Ziel:"));
        scannerPanel.add(comboScanZiel);
        scannerPanel.add(btnScanStarten);
        sidebar.add(scannerPanel);
        sidebar.add(Box.createVerticalStrut(15));

        // Druck-Panel
        JPanel druckPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        druckPanel.setBorder(BorderFactory.createTitledBorder("Drucken & Etiketten"));
        spinnerEtikettPos = new JSpinner(new SpinnerNumberModel(1, 1, 24, 1));
        btnEtikettDrucken = new JButton("Etikett drucken (Pos: " + spinnerEtikettPos.getValue() + ")");
        btnUmschlagDrucken = new JButton("Umschlag (DL) drucken");
        btnBriefDrucken = new JButton("Brief drucken");
        druckPanel.add(new JLabel("Etiketten-Position:"));
        druckPanel.add(spinnerEtikettPos);
        druckPanel.add(btnEtikettDrucken);
        druckPanel.add(btnUmschlagDrucken);
        druckPanel.add(btnBriefDrucken);
        sidebar.add(druckPanel);

        mainPanel.add(sidebar, BorderLayout.WEST);

        // --- 2. CENTER: Vorschau & Zoom ---
        JPanel centerWrapper = new JPanel(new BorderLayout());

        // Zoom/Blätter-Leiste
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        zoomSlider = new JSlider(50, 150, 100);
        JButton btnFit = new JButton("Ganze Seite");
        spinnerSeitenwahl = new JSpinner(new SpinnerNumberModel(1, 1, Math.max(1, this.maxSeiten), 1));
        JButton btnVorherige = new JButton("<");
        JButton btnNaechste = new JButton(">");

        controlPanel.add(new JLabel("Zoom:"));
        controlPanel.add(zoomSlider);
        controlPanel.add(btnFit);
        controlPanel.add(new JSeparator(SwingConstants.VERTICAL));

        controlPanel.add(new JLabel("Seite:"));
        controlPanel.add(btnVorherige);
        controlPanel.add(spinnerSeitenwahl);
        controlPanel.add(btnNaechste);

        // NEU: Label für die maximale Seitenzahl
        lblSeitenInfo = new JLabel(" " + maxSeiten);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(lblSeitenInfo);

        centerWrapper.add(controlPanel, BorderLayout.NORTH);

        // JFXPanel
        jfxDruckZentrumPanel = new JFXPanel();
        centerWrapper.add(jfxDruckZentrumPanel, BorderLayout.CENTER);

        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        jfxDruckZentrumPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (isResizing) return;
                isResizing = true;

                // Kleine Verzögerung oder InvokeLater, um den Event-Stack abzuarbeiten
                SwingUtilities.invokeLater(() -> {
                    setzeZoomAufGanzeSeite();
                    isResizing = false;
                });
            }
        });

        // Initialisierung FX (Logik bleibt identisch)
        // Globale Klassenvariable im DruckzentrumPanel definieren:


        // Innerhalb von erzeugeDruckZentrumPanel():
        Platform.runLater(() -> {
            vorschauWebView = new WebView();
            druckZentrumEngine = vorschauWebView.getEngine();

            druckZentrumEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    // Ermittelt die exakte Höhe des geladenen HTML-Inhalts in Pixeln
                    Object heightObj = druckZentrumEngine.executeScript("document.documentElement.scrollHeight");
                    if (heightObj instanceof Number) {
                        double docHeight = ((Number) heightObj).doubleValue();
                        // WebView dehnt sich voll aus -> Alle Mitglieder werden gerendert!
                        vorschauWebView.setPrefHeight(docHeight);
                        vorschauWebView.setMinHeight(docHeight);
                    }
                }
            });

            // WICHTIG: Keine feste Maximalhöhe für die WebView vorschreiben,
            // damit sie im Hintergrund alle geladenen Mitglieder-Seiten rendert!
            vorschauWebView.setPrefWidth(595);
            vorschauWebView.setMinWidth(595);

            // NEU: Scrollbalken der WebView über den eingebetteten Skin komplett abschalten,
            // da wir die Anzeige nativ verschieben und keine Browser-Scrollbars wollen.
            vorschauWebView.getChildrenUnmodifiable().addListener((javafx.collections.ListChangeListener.Change<? extends javafx.scene.Node> c) -> {
                for (javafx.scene.Node node : vorschauWebView.lookupAll(".scroll-bar")) {
                    if (node instanceof javafx.scene.control.ScrollBar) {
                        node.setVisible(false);
                        node.setManaged(false);
                    }
                }
            });

            // Wrapper-Pane als feste Maske (Suchfenster für genau eine A4-Seite)
            fxWrapperPane = new javafx.scene.layout.Pane(vorschauWebView);
            fxWrapperPane.setPrefSize(595, 842);
            fxWrapperPane.setMinSize(595, 842);

            // Alles abschneiden, was über die aktuelle A4-Ansicht hinausragt
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(595, 842);
            fxWrapperPane.setClip(clip);

            javafx.scene.Scene scene = new javafx.scene.Scene(fxWrapperPane, 595, 842, javafx.scene.paint.Color.WHITE);
            jfxDruckZentrumPanel.setScene(scene);

            String empty = prepareHtmlForPreview("<html><body></body></html>");
            druckZentrumEngine.loadContent(empty);
        });

        // Event-Listener hier wieder einfügen (btnVorherige, btnNaechste, zoomSlider etc.)
        // ... (deine existierenden Listener können hier unverändert übernommen werden)
        // Zoom-Logik
        zoomSlider.addChangeListener(e -> {
            double scale = zoomSlider.getValue() / 100.0;
            Platform.runLater(() -> {
                if (vorschauWebView != null) {
                    vorschauWebView.setZoom(scale);
                }
            });
        });

        // Und binden Sie den Button an die Funktion: passe Zoom so an, dass die ganze A4-Seite sichtbar ist
        btnFit.addActionListener(e -> {
            if (vorschauWebView != null) {
                setzeZoomAufGanzeSeite();
            }
        });

        // Action-Listener für vorherige Seite
        btnVorherige.addActionListener(e -> {
            int aktuelleSeite = (int) spinnerSeitenwahl.getValue();
            if (aktuelleSeite > 1) {
                int zielSeite = aktuelleSeite - 1;
                spinnerSeitenwahl.setValue(zielSeite);

                Platform.runLater(() -> {
                    // Seite 1 -> 0px, Seite 2 -> -842px, Seite 3 -> -1684px usw.
                    double yVerschiebung = -(zielSeite - 1) * 842;
                    vorschauWebView.setTranslateY(yVerschiebung);
                });
            }
        });

        // Action-Listener für nächste Seite
        btnNaechste.addActionListener(e -> {
            int aktuelleSeite = (int) spinnerSeitenwahl.getValue();
            SpinnerNumberModel model = (SpinnerNumberModel) spinnerSeitenwahl.getModel();
            int max = ((Number) model.getMaximum()).intValue();

            if (aktuelleSeite < max) {
                int zielSeite = aktuelleSeite + 1;
                spinnerSeitenwahl.setValue(zielSeite);

                Platform.runLater(() -> {
                    double yVerschiebung = -(zielSeite - 1) * 842;
                    vorschauWebView.setTranslateY(yVerschiebung);
                });
            }
        });

        Platform.runLater(() -> {
            Object result = druckZentrumEngine.executeScript("document.getElementsByClassName('din-a4-blatt').length");
            System.out.println("Tatsächliche Anzahl Blätter im HTML: " + result);
        });

        Platform.runLater(() -> {
            String content = (String) druckZentrumEngine.executeScript("document.documentElement.innerHTML");
            System.out.println("Geladenes HTML: " + content);
        });

//        druckZentrumEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
//            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
//                Object result = druckZentrumEngine.executeScript("document.getElementsByClassName('din-a4-blatt').length");
//                System.out.println("Tatsächliche Anzahl Blätter: " + result);
//            }
//        });

        return mainPanel;
    }

    public void componentResized(java.awt.event.ComponentEvent e) {
        if (jfxDruckZentrumPanel.getWidth() > 100) { // Nur wenn Fenster echte Größe hat
            setzeZoomAufGanzeSeite();
        }
    }

    private void setzeZoomAufGanzeSeite() {
        Platform.runLater(() -> {
            if (vorschauWebView == null) return;

            double availableWidth = jfxDruckZentrumPanel.getWidth();
            double availableHeight = jfxDruckZentrumPanel.getHeight();

            // 1. Skalierungsfaktor
            double scale = Math.min(availableWidth / 595.0, availableHeight / 842.0);

            // Apply zoom to the WebView (keeps layout bounds intact and avoids inner scrollbars)
            vorschauWebView.setZoom(scale);

            // Da die Skalierung die optische Größe ändert, aber nicht die "belegte" Größe
            // im Layout, sorgt dies dafür, dass keine Scrollbars entstehen.
        });
    }

    private JPanel erzeugeTemplateEditorPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- TOOLBAR ---
        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.Y_AXIS));
        toolbarPanel.setBorder(BorderFactory.createTitledBorder("Serienbrief-Vorlagen bearbeiten (JavaFX WebView)"));

        JPanel zeile1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnEditorLaden = new JButton("Vorlage öffnen (.html)");
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
        btnFett = new JButton("Fett");
        btnKursiv = new JButton("Kursiv");
        btnUnterstrichen = new JButton("Unterstrichen");

        comboSchriftart = new JComboBox<>(new String[]{"Arial", "Times New Roman", "Courier New"});
        comboSchriftgroesse = new JComboBox<>(new Integer[]{10, 12, 14, 16, 18, 20});

        zeile2.add(btnFett);
        zeile2.add(btnKursiv);
        zeile2.add(btnUnterstrichen);
        zeile2.add(new JLabel("  |  Schriftart:"));
        zeile2.add(comboSchriftart);
        zeile2.add(new JLabel("  |  Schriftgröße:"));
        zeile2.add(comboSchriftgroesse);

        // JavaFX-Aktionen statt StyledEditorKit
        btnFett.addActionListener(e -> executeJs("document.execCommand('bold', false, null);"));
        btnKursiv.addActionListener(e -> executeJs("document.execCommand('italic', false, null);"));
        btnUnterstrichen.addActionListener(e -> executeJs("document.execCommand('underline', false, null);"));

        zeile2.add(btnFett);
        zeile2.add(btnKursiv);
        toolbarPanel.add(zeile2);

        mainPanel.add(toolbarPanel, BorderLayout.NORTH);

        // --- JAVAFX INTEGRATION ---
        // Ersetze den entsprechenden Block in erzeugeTemplateEditorPanel():
        jfxEditorPanel = new JFXPanel();
        Platform.runLater(() -> {
            // Erzeuge eine eigene WebView für den Editor, damit sie unabhängig von der Vorschau ist
            editorWebView = new WebView();
            webEngine = editorWebView.getEngine();
            editorWebView.setPrefSize(595, 842);

            String initial = "<html><head><meta charset='utf-8'></head><body contenteditable='true' style='margin:0;padding:0;overflow:hidden;width:595px;font-family:Arial;font-size:12pt;'>"
                    + "Hier klicken und Text bearbeiten...</body></html>";

            webEngine.loadContent(initial);
            // sicherstellen, dass designMode erst nach Laden aktiviert wird
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    try {
                        webEngine.executeScript("document.designMode = 'on';");
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            });

            jfxEditorPanel.setScene(new Scene(editorWebView));
        });

        mainPanel.add(jfxEditorPanel, BorderLayout.CENTER);

        JLabel lblInfo = new JLabel("Hinweis: Formatierung erfolgt via HTML5/JavaScript.");
        lblInfo.setFont(new Font("Arial", Font.ITALIC, 11));
        mainPanel.add(lblInfo, BorderLayout.SOUTH);

        return mainPanel;
    }

    // Hilfsmethode für JavaScript-Befehle
    private void executeJs(String script) {
        if (webEngine != null) {
            Platform.runLater(() -> webEngine.executeScript(script));
        }
    }

    private void addKomponente(JPanel p, JLabel label, JTextField tf, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x;
        gbc.gridy = y;
        p.add(label, gbc);
        gbc.gridx = x + 1;
        p.add(tf, gbc);
    }

    // vor jedem .loadContent
    private void ladeInhaltInVorschau(String html) {
        Platform.runLater(() -> {
            if (druckZentrumEngine != null) {
                druckZentrumEngine.loadContent(html);
            } else {
                logger.warning("Engine noch nicht bereit!");
            }
        });
    }

    private void aktualisiereVorschauAnzeige() {
        int[] selectedRows = tabelle.getSelectedRows();
        if (selectedRows.length == 0 || aktuellGeladenesTemplate == null) return;

        List<Mitglied> liste = new java.util.ArrayList<>();
        for (int row : selectedRows) {
            liste.add(mitglieder.getMitgliedAt(tabelle.convertRowIndexToModel(row)));
        }

        String datum = heutigesDatumToString();
        StringBuilder sb = new StringBuilder("<html><body>");
        for (Mitglied m : liste) {
            sb.append(DruckVorbereiter.ersetzePlatzhalter(aktuellGeladenesTemplate, m, datum));
            sb.append("<div style='page-break-after: always;'></div>");
        }
        sb.append("</body></html>");
        String finalHtml = sb.toString();

        Platform.runLater(() -> {
            if (druckZentrumEngine != null) {
                // Nur EIN Aufruf, kein leeres Laden vorher
                String prepared = prepareHtmlForPreview(finalHtml);
                druckZentrumEngine.loadContent(prepared);
            }
        });
    }

    private void reloadTable() {
        aktuelleListe = mitgliedDAO.readAll();
        mitglieder = new MitgliederTableModel(aktuelleListe);
        tabelle.setModel(mitglieder);
        anpassenSpaltenBreiten();
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

    private String ladeTemplateAlsString(String dateiName) {
        try {
            File file = new File(dateiName);
            if (!file.exists()) {
                return "<html><body>Vorlage " + dateiName + " nicht gefunden.</body></html>";
            }
            return new String(java.nio.file.Files.readAllBytes(file.toPath()));
        } catch (IOException ex) {
            logger.severe("Fehler beim Laden der Vorlage: " + ex.getMessage());
            // Hier muss ein Rückgabewert hin, damit die Methode in jedem Fall etwas zurückgibt
            return "<html><body>Fehler beim Laden der Vorlage.</body></html>";
        }
    }

    private void aktualisiereDruckZentrumMitGeladenemTemplate(Mitglied m) {
        if (aktuellGeladenesTemplate == null) return;

        Platform.runLater(() -> {
            // Hier wird das dynamisch geladene Template verarbeitet
            String finalerHtmlText = DruckVorbereiter.ersetzePlatzhalter(aktuellGeladenesTemplate, m, heutigesDatumToString());
            String prepared = prepareHtmlForPreview(finalerHtmlText);
            druckZentrumEngine.loadContent(prepared);
        });
    }

    private void registriereEvents() {
        // Innerhalb von registriereEvents()
        tabelle.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            // 1. WICHTIG: Prüfen, ob das Event gerade erst verarbeitet wird
            if (e.getValueIsAdjusting()) return;

            int[] selectedRows = tabelle.getSelectedRows();

            this.maxSeiten = selectedRows[selectedRows.length - 1];

            aktualisiereSeitenAnzahl(this.maxSeiten + 1);

            // 2. Den GUI-Update-Teil in ein Try-Catch packen, um Blockaden zu verhindern
            try {
                if (selectedRows.length > 0) {
                    // Logik nur ausführen, wenn wir sicher sind, dass Daten vorhanden sind
                    SwingUtilities.invokeLater(() -> {
                        if (selectedRows.length == 1) {
                            int modelRow = tabelle.convertRowIndexToModel(selectedRows[0]);
                            ausgewaehltesMitglied = mitglieder.getMitgliedAt(modelRow);
                            zeigeMitglied(ausgewaehltesMitglied);

                            lblAusgewaehltesMitglied.setText("Aktuell ausgewählt: " + ausgewaehltesMitglied.getVorname() + " " + ausgewaehltesMitglied.getNachname());

                            // Aktualisierung der Vorschau (nur bei einer Auswahl)
                            aktualisiereVorschauAnzeige();
                        } else {
                            lblAusgewaehltesMitglied.setText("Aktuell ausgewählt: " + selectedRows.length + " Mitglieder");
                            aktualisiereVorschauAnzeige();
                        }
                    });
                }
             } catch (Exception ex) {
                 logger.warning("Fehler bei Tabellenauswahl: " + ex.getMessage());
             }
        });

        // Beispiel im SelectionListener der Tabelle:
        if (ausgewaehltesMitglied != null) {
            Platform.runLater(() -> {
                // 1. Template laden (oder aus Variable holen)
                String template = ladeTemplateAlsString("algorithmus1.html");

                // 2. Platzhalter ersetzen
                String finalerHtmlText = DruckVorbereiter.ersetzePlatzhalter(template, ausgewaehltesMitglied,heutigesDatumToString());

                // 3. Erst DANN in die WebView laden
                druckZentrumEngine.loadContent(finalerHtmlText);
            });
        }

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
        // <--- NEU
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

    // Hilfsmethode, um die Vorlage zu wählen (statt alles im Switch zu haben)
    private String waehleVorlage(int index) {
        switch (index) {
            case 1: return "<html><body>...begrüßen zu dürfen... [Eintritt]... [MNr]</body></html>";
            // ... weitere Fälle
            default: return "<html><body>Vorlage nicht gefunden.</body></html>";
        }
    }

    private void initialisiereDruckLogik() {
        // 1. ActionListener für Vorlagen (nur einmalig definiert!)
        comboVorlagen.addActionListener(e -> {
            int index = comboVorlagen.getSelectedIndex();
            if (index <= 0) return;

            String htmlTemplate = waehleVorlage(index);

            if (ausgewaehltesMitglied != null) {
                htmlTemplate = DruckVorbereiter.ersetzePlatzhalter(htmlTemplate, ausgewaehltesMitglied, heutigesDatumToString());
            }

            final String finalHtml = htmlTemplate;
            Platform.runLater(() -> {
                druckZentrumEngine.loadContent(finalHtml);
            });
        });

        // 2. Button zum Einfügen des Platzhalters
        btnPlatzhalterEinfuegen.addActionListener(e -> {
            // Hinweis: Hier sollte idealerweise ein Wert aus einer anderen ComboBox kommen
            String platzhalter = "[Name]";
            String jsCode = "document.execCommand('insertText', false, '" + platzhalter + "');";

            Platform.runLater(() -> {
                druckZentrumEngine.executeScript(jsCode);
            });
        });

        // 3. Button zum Laden externer Dateien
        btnTextLaden.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    // Erzwinge UTF-8 Encoding beim Einlesen
                    java.nio.charset.Charset charset = java.nio.charset.StandardCharsets.UTF_8;
                    aktuellGeladenesTemplate = new String(java.nio.file.Files.readAllBytes(file.toPath()), charset);

                    if (!aktuellGeladenesTemplate.trim().toLowerCase().startsWith("<!doctype html>") &&
                            !aktuellGeladenesTemplate.trim().toLowerCase().contains("<html>")) {
                        JOptionPane.showMessageDialog(this, "Warnung: Die geladene Datei ist kein valides HTML!");
                    }

                    // Debug-Ausgabe zur Kontrolle
                    System.out.println("Geladene Datei: " + file.getName());
                    System.out.println("Erste 100 Zeichen: " + aktuellGeladenesTemplate.substring(0, Math.min(100, aktuellGeladenesTemplate.length())));

                    if (ausgewaehltesMitglied != null) {
                        aktualisiereDruckZentrumMitGeladenemTemplate(ausgewaehltesMitglied);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Laden (Encoding Problem?): " + ex.getMessage());
                }
            }
        });

        // 4. Druck- und Aktionsbuttons
        btnBriefDrucken.addActionListener(e -> starteDruckMitSnapshots());
        btnEtikettDrucken.addActionListener(e -> starteEtikettDruck());
        //.addActionListener(e -> zeigeDruckVorschau());
        btnUmschlagDrucken.addActionListener(e -> starteUmschlagDruck());
        btnScanStarten.addActionListener(e -> fuehreScanAus());
    }

    private void starteDruckMitSnapshots() {
        int[] selectedRows = tabelle.getSelectedRows();
        if (selectedRows.length == 0) return;

        List<BufferedImage> alleBilder = new ArrayList<>();
        String template = aktuellGeladenesTemplate;

        // Wir gehen durch die Liste der ausgewählten Mitglieder
        for (int row : selectedRows) {
            Mitglied m = mitglieder.getMitgliedAt(tabelle.convertRowIndexToModel(row));

            try {
                // Hier passiert die Magie: Pro Mitglied ein eigener Render-Vorgang
                WritableImage fxImg = DruckVorbereiter.erstelleSnapshotFuerMitglied(
                        m, template, vorschauWebView, heutigesDatumToString()).get();

                if (fxImg != null) {
                    alleBilder.add(SwingFXUtils.fromFXImage(fxImg, null));
                }
            } catch (Exception e) {
                logger.severe("Fehler bei Snapshot für Mitglied " + m.getNachname());
            }
        }

        // Jetzt erst den Druckauftrag starten
        SwingUtilities.invokeLater(() -> {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(new SerienBriefDrucker(alleBilder));
            if (job.printDialog()) {
                try { job.print(); } catch (Exception e) { e.printStackTrace(); }
            }
        });
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
                // Hier ist die Variable definiert und korrekt im Scope:
                String platzhalterTag = "[" + feldName + "]";

                String jsCode = "document.execCommand('insertText', false, '" + platzhalterTag + "');";
                executeJs(jsCode); // Dies nutzt die vorhandene Hilfsmethode

                comboPlatzhalter.setSelectedIndex(0);

            }
        });

        btnEditorLaden.addActionListener(e -> oeffneDateiInEditorPane());
        btnEditorSpeichern.addActionListener(e -> speichereDateiAusEditorPane());
    }

    // --- NEU: DOKUMENTEN SCANNER LOGIK (SANE-BACKEND) ---
    private void fuehreScanAus() {
        String dateiName = txtScanName.getText().trim().replaceAll("[^a-zA-Z0-9_-]", "");
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
                        "Textdokumente (*.docx, *.html, *.odt, *.rtf, *.txt)", "docx", "html", "odt", "rtf", "txt");
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
                    // Fallback: lade die Datei als Roh-HTML/Text und zeige sie im Editor-WebView
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line).append("\n");
                    }
                    geladenerText = "<html><body style='font-family:Arial; font-size:12pt;'>" + sb.toString().replace("\n", "<br>") + "</body></html>";
                }

                // Zeige die geladene Vorlage im Editor (JavaFX Thread)
                final String toShow = geladenerText;
                Platform.runLater(() -> {
                    if (webEngine != null) webEngine.loadContent(toShow);
                });

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Laden der Format-Vorlage: " + ex.getMessage(),
                        "Ladefehler", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void speichereDateiAusEditorPane() {
        // Hole den Inhalt aus der WebEngine (JavaScript-Callback)
        Platform.runLater(() -> {
            String htmlContent = (String) webEngine.executeScript("document.documentElement.outerHTML");

            // Hier folgt nun dein FileChooser-Code zum Speichern
            SwingUtilities.invokeLater(() -> {
                JFileChooser fileChooser = new JFileChooser();
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    try (FileWriter fw = new FileWriter(fileChooser.getSelectedFile())) {
                        fw.write(htmlContent);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        });
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

    private void leereFormularFelder() {
        //isUpdating = true;

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

        ausgewaehltesMitglied = null;
        lblAusgewaehltesMitglied.setText("Aktuell ausgewählt: (Kein Mitglied in der Tabelle markiert)");
      //isUpdating = false;
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
                mitglieder.setListe(aktuelleListe);

                // Formular und Auswahl zurücksetzen
                leereFormularFelder();

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

    //private void starteBriefDruck() {
        //starteDruckMitSnapshots();
        /*int[] selectedRows = tabelle.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie zuerst auf der ersten Seite mindestens ein Mitglied aus der Tabelle aus.", "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.ArrayList<Mitglied> ausgewaehlteMitglieder = new java.util.ArrayList<>();
        for (int row : selectedRows) {
            int modelRow = tabelle.convertRowIndexToModel(row);
            Mitglied m = mitglieder.getMitgliedAt(modelRow);
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
        }*/
    //}

    private void aktualisiereTabelle() {
        aktuelleListe.clear();
        aktuelleListe.addAll(mitgliedDAO.readAll());
        mitglieder.fireTableDataChanged();
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
            Mitglied m = mitglieder.getMitgliedAt(modelRow);

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

    /*private void zeigeDruckVorschau() {
        int[] selectedRows = tabelle.getSelectedRows();
        if (selectedRows.length == 0) return;

        List<Mitglied> liste = new java.util.ArrayList<>();
        for (int row : selectedRows) {
            liste.add(mitglieder.getMitgliedAt(tabelle.convertRowIndexToModel(row)));
        }

        // Hole das aktuelle Template aus der WebEngine
        Platform.runLater(() -> {
            String htmlTemplate = (String) webEngine.executeScript("document.documentElement.outerHTML");

            SwingUtilities.invokeLater(() -> {
                // Ruf hier den neuen Konstruktor auf
                DruckVorschauDialog vorschauDialog = new DruckVorschauDialog(this, liste, htmlTemplate);
                vorschauDialog.setVisible(true);
            });
        });
    }*/

    private void starteUmschlagDruck() {
        int selectedRow = tabelle.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie zuerst auf der ersten Seite ein Mitglied aus der Tabelle aus.", "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tabelle.convertRowIndexToModel(selectedRow);
        Mitglied m = mitglieder.getMitgliedAt(modelRow);

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
            mitglieder.setListe(aktuelleListe); // Setzt die neue Liste im TableModel
            mitglieder.fireTableDataChanged();

            leereFormularFelder(); // Felder für die nächste Eingabe frei machen

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

    // Hilfsmethode: sorgt dafür, dass geladene Templates eine feste A4-Box (595x842) bekommen
    private String prepareHtmlForPreview(String html) {
        if (html == null) return "";
        String lower = html.toLowerCase();
        try {
            // KEIN JavaScript mehr, reines, sauberes A4-CSS
            String css = "<style>"
                    + "body {"
                    + "  background-color: #E0E0E0;" // Grauer Hintergrund wie in Word/Acrobat Reader
                    + "  margin: 0 !important;"
                    + "  padding: 20px 0 !important;" // Etwas Abstand oben/unten
                    + "  display: flex !important;"
                    + "  flex-direction: column !important;"
                    + "  align-items: center !important;"
                    + "}"
                    + ".alg-page {"
                    + "  width: 595px !important;"
                    + "  height: 842px !important;"
                    + "  background-color: white !important;"
                    + "  margin-bottom: 20px !important;" // Sichtbarer Abstand zwischen den Seiten!
                    + "  box-shadow: 0 4px 8px rgba(0,0,0,0.2) !important;" // Schicke Schatten
                    + "  box-sizing: border-box !important;"
                    + "  position: relative !important;"
                    + "  overflow: hidden !important;"
                    + "}"
                    + "</style>";

            if (lower.contains("<head")) {
                return html.replaceFirst("(?i)</head>", css + "</head>");
            } else if (lower.contains("<html")) {
                String withHead = html.replaceFirst("(?i)<html([^>]*)>", "<html$1><head><meta charset='utf-8'>" + css + "</head>");
                if (withHead.toLowerCase().contains("<body")) {
                    return withHead;
                } else {
                    return withHead.replaceFirst("(?i)</html>", "<body>" + html + "</body></html>");
                }
            } else {
                return "<html><head><meta charset='utf-8'>" + css + "</head><body>" + html + "</body></html>";
            }
        } catch (Exception ex) {
            return html;
        }
    }
}

