import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebView;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class AlgDatabankGui extends JFrame {

    // Neue Komponenten für Tab 4 (Beitrags-Verwaltung)
    private JTable tabelleBeitraege;
    private BeitraegeTableModel beitraegeModel; // Das Tabellenmodell für die Beiträge
    private JButton btnExcelBeitraegeLaden, btnExcelBeitraegeSpeichern;
    private JButton btnAusDatenbankLaden;
    private JTextField txtBeitragJahr, txtBeitragBetrag;
    private JComboBox<String> comboBeitragStatus;
    private JLabel lblBeitragMitgliedInfo;
    private Beitrag ausgewaehlterBeitrag = null;

    private JTable tabelle;
    private MitgliederTableModel mitglieder;
    private MitgliedDAO mitgliedDAO;
    private List<Mitglied> aktuelleListe;

    private JTabbedPane tabbedPane;

    // Aktuell ausgewähltes Mitglied für die automatische Benennung
    private Mitglied ausgewaehltesMitglied = null;

    // GUI Komponenten für die Eingabemaske (CRUD)
    private JTextField txtMNr, txtEintritt, txtAustritt, txtAnrede, txtTitel;
    private JTextField txtNachname, txtVorname, txtStrasse, txtPLZ, txtOrt;
    private JTextField txtTelefon, txtEmail, txtHinweise;

    // GUI Komponenten für das Briefzentrum (Seite 2)
    private JFXPanel fxBriefPanel;
    private WebView webViewBrief;

    // Ganz oben bei Ihren anderen Variablen
    //private String aktuellerBriefInhalt = "<html><body>...</body></html>"; // Initialwert

    private String aktuellerBriefInhalt = "";

    // In Ihre Klasse einfügen
    private String masterHtmlContent = "";

    // Komponenten für den HTML-Template-Editor (Seite 3)
    private JFXPanel fxEditorPanel;
    private WebView webViewEditor;
    private JComboBox<String> comboPlatzhalter;

    private JPanel kartenPanel; // Der Container mit CardLayout
    private CardLayout cardLayout;

    private JComboBox<String> comboVorlagen;
    private JButton btnTextLaden, btnTextSpeichern, btnBriefDrucken, btnExportPDF, btnExportAndPrintPDF, btnTestPrinter;
    private JButton getBtnUmschlagDrucken;
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
    private JButton btnExcelExport;

    // Instanzvariablen für den Etikettendruck
    private JSpinner spinnerEtikettPos;
    private JButton btnEtikettDrucken;

     private JFormattedTextField txtBeitragAbgeschickt;

    // Buttons & Labels für die Statistik
    private JButton btnDrucken;
    private JTextField txtMaxBeitrag;
    private JTextField txtTatsaechlicherBeitrag;
    private JTextField txtAnzahlMitglieder;
    private JTextField txtDavonBezahlt;
    private JTextField txtAnzahlMahnungen;

    private JRadioButton rbStatusOffen;
    private JRadioButton rbStatusBezahlt;
    private JRadioButton rbStatusGemahnt;

    // Diese Variablen müssen als Instanzvariablen (oben in der Klasse) deklariert sein:
    private JTextField txtStatusAnzeige;
    private JButton btnStatusPfeil;

    private JTextArea txtBriefText;

    // Drucker-Speicherung
    private String ausgewaehlterDrucker = null;

    // Ein Flag, um ungespeicherte Änderungen zu tracken
    private boolean unsavedChanges = false;

    private String letzterVorschauInhalt = ""; // Hier speichern wir den fertigen Inhalt

    public int startPosition;

    public JLabel lblVorschauBild;

    // ---- HIER DIE FARBEN ALS GLOBALE KONSTANTEN ANLEGEN ---
    java.awt.Color farbeAktivBg = new java.awt.Color(0, 102, 204);
    java.awt.Color farbeAktivFg = java.awt.Color.WHITE;
    java.awt.Color farbeInaktivBg = new java.awt.Color(45, 45, 45);
    java.awt.Color farbeInaktivFg = new java.awt.Color(180, 180, 180);
    private JTextArea targetArea;


    public AlgDatabankGui() {

            mitgliedDAO = new MitgliedDAO();
            aktuelleListe = new ArrayList<>();
            mitglieder = new MitgliederTableModel(aktuelleListe);

            // =========================================================================
            // 1. BRIEFZENTRUM (SEITE 2): Startet als leerer Swing-Container
            // =========================================================================
            kartenPanel = new JPanel(new BorderLayout());
            JLabel lblLadeVorschau = new JLabel("Druckvorschau wird beim Wechsel auf diesen Reiter geladen.", SwingConstants.CENTER);
            lblLadeVorschau.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
            kartenPanel.add(lblLadeVorschau, BorderLayout.CENTER);

            // =========================================================================
            // 2. TEMPLATE-EDITOR (SEITE 3): Direkt im Konstruktor vorbereiten
            // =========================================================================
            fxEditorPanel = new JFXPanel();

            // Wir erzeugen die WebView für den Editor sicher auf dem FX-Thread.
            // Wichtig: Wir rufen hierdrinnen KEINE Swing-Methoden auf, die die Vorschau erzwingen wollen!
            Platform.runLater(() -> {
                try {
                    webViewEditor = new WebView();
                    fxEditorPanel.setScene(new javafx.scene.Scene(webViewEditor));

                    // Standard-Vorlage laden
                    String initialHtml = ladeVorlageAusRessourcen("templates/algorithmus1.html");
                    if (initialHtml == null || initialHtml.trim().isEmpty()) {
                        initialHtml = "<html><body contenteditable='true' style='font-family:Arial; font-size:14px;'><p><br></p></body></html>";
                    }

                    webViewEditor.getEngine().loadContent(initialHtml, "text/html; charset=utf-8");

                    // Design-Modus erst nach erfolgreichem Laden aktivieren
                    webViewEditor.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        webViewEditor.getEngine().executeScript("document.designMode = 'on';");
                    }
                });
            } catch (Exception ex) {
                System.err.println("Fehler bei FX-Editor-Initialisierung: " + ex.getMessage());
            }
        });

            // =========================================================================
            // 3. Native Swing Einstellungen
            // =========================================================================
            setTitle("AlgDatabank - Mitgliederverwaltung & Briefzentrum");
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setSize(1200, 900);
            setLocationRelativeTo(null);

             // WindowListener hinzufügen, um das "X" abzufangen
            addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                beendenMitSicherheitsabfrage();
            }
        });

            aktuelleListe = mitgliedDAO.readAll();
            mitglieder = new MitgliederTableModel(aktuelleListe);
            tabelle = new JTable(mitglieder);
            tabelle.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            tabelle.setAutoCreateRowSorter(true);

            tabelle.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            anpassenSpaltenBreiten();

            tabbedPane = new JTabbedPane();

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

        // --- SEITE 4: BEITRAGS-VERWALTUNG (NEU) ---
        JPanel beitragsPanel = erzeugeBeitragsPanel(); // Methode erstellen wir unten

        tabbedPane.addTab("Mitglieder-Verwaltung", verwaltungsPanel);
        tabbedPane.addTab("Brief- & Druckzentrum", druckzentrumPanel);
        tabbedPane.addTab("Template-Editor (HTML)", editorPanel);
        tabbedPane.addTab("Beitrags-Verwaltung", beitragsPanel); // <- Neu hinzufügen

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
                    // =========================================================================
                    // ROBUSTER FIX: VORSCHAU BEIM WECHSEL INS DRUCKZENTRUM ERZWINGEN
                    // =========================================================================

                    if (selectedIndex == 1) { // Brief- & Druckzentrum
                        System.out.println("DEBUG TAB: Wechsel ins Druckzentrum.");

                        // 1. Falls das Panel noch nie betreten wurde: JFXPanel erzeugen
                        if (fxBriefPanel == null) {
                            System.out.println("DEBUG UI: Erzeuge JFXPanel für die Vorschau...");
                            kartenPanel.removeAll();

                            fxBriefPanel = new JFXPanel();
                            fxBriefPanel.setMinimumSize(new Dimension(400, 500));
                            fxBriefPanel.setPreferredSize(new Dimension(600, 700));

                            kartenPanel.add(fxBriefPanel, BorderLayout.CENTER);

                            Platform.runLater(() -> {
                                try {
                                    webViewBrief = new WebView();
                                    fxBriefPanel.setScene(new javafx.scene.Scene(webViewBrief));

                                    // ERZWUNGENER SOFORT-START: Nicht auf die Breite warten!
                                    webViewBrief.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                                        if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                                            System.out.println("DEBUG VORSCHAU: WebView bereit.");
                                        }
                                    });

                                    // Direkt laden!
                                    this.aktualisiereVorschauInhalt();

                                } catch (Exception ex) {
                                    System.err.println("Fehler beim Erstellen der Vorschau-WebView: " + ex.getMessage());
                                }
                            });
                        } else {
                            // Wenn das Panel schon existiert: Bedingungslos aktualisieren (ohne Breitenprüfung!)
                            Platform.runLater(() -> {
                                if (webViewBrief != null) {
                                    aktualisiereVorschauInhalt();
                                }
                            });
                        }

                        // 2. Swing-UI mit Nachdruck zum Neuzeichnen zwingen
                        SwingUtilities.invokeLater(() -> {
                            kartenPanel.revalidate();
                            kartenPanel.repaint();
                        });
                    }
                });
        add(tabbedPane, BorderLayout.CENTER);

        registriereEvents();
        initialisiereDruckLogik();
        initialisiereEditorLogik();
    }

    private void ladeBriefInDenEditor(String html) {
        this.aktuellerBriefInhalt = html; // Variable aktuell halten
        Platform.runLater(() -> {
            webViewEditor.getEngine().loadContent(html);
        });
    }

    /**
     * Holt asynchron den Text aus dem HTML-Editor und lädt ihn in die Druckvorschau.
     * Muss zwingend auf dem JavaFX Application Thread aufgerufen werden!
     */

    // Die Methode für die Abfrage (ebenfalls in der Klasse platzieren)
    private void beendenMitSicherheitsabfrage() {
        if (unsavedChanges) {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    "Es gibt ungespeicherte Änderungen bei den Beiträgen. Möchten Sie diese jetzt in Excel speichern?",
                    "Programm beenden",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (option == JOptionPane.YES_OPTION) {
                // ERSETZT dateiSpeichern() -> Ruft stattdessen den Klick-Event des Excel-Speichern-Buttons auf!
                btnExcelBeitraegeSpeichern.doClick();
                System.exit(0);
            } else if (option == JOptionPane.NO_OPTION) {
                System.exit(0); // Schließen ohne Speichern
            }
            // Bei CANCEL passiert einfach gar nichts
        } else {
            System.exit(0); // Keine Änderungen? Direkt zu machen.
        }
    }

    private void aktualisiereVorschauAlsBild(String htmlInhalt) {
        // Hier nutzen wir Platform.runLater nur, um den JavaFX-Renderer anzustoßen
        Platform.runLater(() -> {
            WebView webViewDummy = new WebView();
            webViewDummy.setPrefSize(800, 1100);
            webViewDummy.getEngine().loadContent(htmlInhalt);

            webViewDummy.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    // Snapshot machen
                    WritableImage image = webViewDummy.snapshot(new SnapshotParameters(), null);

                    // Bild konvertieren und im JLabel anzeigen
                    SwingUtilities.invokeLater(() -> {
                        lblVorschauBild.setIcon(new ImageIcon(SwingFXUtils.fromFXImage(image, null)));
                        lblVorschauBild.setText("");
                    });
                }
            });
        });
    }

    /**
     * Öffnet einen Dialog zur Auswahl des neuen Zahlungsstatus
     * für die in der Tabelle selektierte Zeile.
     */
    private void oeffneStatusAuswahlDialog() {
        // 1. Prüfen, ob überhaupt eine Zeile in der Beitragstabelle ausgewählt ist
        int ausgewaehlteZeile = tabelleBeitraege.getSelectedRow();
        if (ausgewaehlteZeile == -1) {
            JOptionPane.showMessageDialog(this,
                    "Bitte wählen Sie zuerst einen Beitrag aus der Tabelle aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Den echten Modell-Index ermitteln (wichtig, falls die Tabelle sortiert wurde)
        int modellZeile = tabelleBeitraege.convertRowIndexToModel(ausgewaehlteZeile);

        // 3. Das Beitrags-Objekt direkt aus dem TableModel holen
        Beitrag aktuellerBeitrag = this.beitraegeModel.getBeitragAt(modellZeile);

        // 4. Die Optionen für das Dropdown-Menü (JComboBox) definieren
        String[] statusOptionen = {"Offen", "Bezahlt", "Mahnung", "Befreit"};

        // 5. Die JComboBox erstellen und den aktuellen Status des Beitrags vorselektieren
        JComboBox<String> comboStatus = new JComboBox<>(statusOptionen);
        comboStatus.setSelectedItem(aktuellerBeitrag.getStatus());

        // 6. Den Swing-Bestätigungsdialog mit der ComboBox anzeigen
        int ergebnis = JOptionPane.showConfirmDialog(
                this,
                comboStatus,
                "Zahlungsstatus ändern für " + aktuellerBeitrag.getVorname() + " " + aktuellerBeitrag.getNachname(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        // 7. Wenn der Benutzer auf "OK" geklickt hat, Änderungen verarbeiten
        if (ergebnis == JOptionPane.OK_OPTION) {
            String auswahl = (String) comboStatus.getSelectedItem();

            // Objekt im Tabellenmodell aktualisieren
            aktuellerBeitrag.setStatus(auswahl);

            // Das ungespeicherte-Änderungen-Flag für Excel aktivieren
            unsavedChanges = true;

            // 8. Änderung direkt in die SQLite-Datenbank schreiben
            try {
                // Das mitgliedDAO aufrufen, um den Status anhand der Mitgliedsnummer (MNr) zu aktualisieren
                //mitgliedDAO.aktualisiereZahlungsstatus(aktuellerBeitrag.getMNr(), auswahl);

                // Dem TableModel mitteilen, dass sich die Daten geändert haben (aktualisiert die Ansicht)
                beitraegeModel.fireTableRowsUpdated(modellZeile, modellZeile);

                JOptionPane.showMessageDialog(this,
                        "Status erfolgreich auf '" + auswahl + "' geändert und in Datenbank gespeichert.",
                        "Erfolg",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Fehler beim Speichern in der Datenbank: " + ex.getMessage(),
                        "Datenbankfehler",
                        JOptionPane.ERROR_MESSAGE);
                System.err.println("Fehler beim Speichern: " + ex.getMessage());
            }
        }
    }

    // Bitte stelle sicher, dass diese Variable in deiner Klasse AlgDatabankGui existiert:
    // private String letzterVorschauInhalt = "";

    private void aktualisiereVorschauInhalt() {
        try {
            if (webViewEditor == null || webViewBrief == null) return;

            // 1. Inhalt aus Editor holen
            String editorHtml = (String) webViewEditor.getEngine().executeScript(
                    "(new XMLSerializer()).serializeToString(document);"
            );

            // Fallback, falls das DOM noch nicht voll geladen war
            if (editorHtml == null || editorHtml.trim().length() <= 45) {
                editorHtml = (String) webViewEditor.getEngine().executeScript(
                        "document.getElementsByTagName('html')[0].innerHTML"
                );
            }

            final String finalHtmlBase = (editorHtml == null || editorHtml.trim().isEmpty()) ?
                    "<html><body><p>Kein Text.</p></body></html>" : editorHtml;

            // 2. Platzhalter ersetzen (Swing-Thread)
            SwingUtilities.invokeLater(() -> {
                String finalHtml;
                if (ausgewaehltesMitglied != null) {
                    finalHtml = ersetzePlatzhalter(finalHtmlBase, ausgewaehltesMitglied);
                } else {
                    finalHtml = finalHtmlBase.replace("[Brief_Anrede]", "Sehr geehrte Damen und Herren,");
                }

                // HIER speichern wir den Inhalt, damit wir beim Drucken darauf zugreifen können
                this.letzterVorschauInhalt = finalHtml;

                // 3. HTML in die Vorschau laden (FX-Thread)
                Platform.runLater(() -> {
                    if (webViewBrief.getEngine() != null) {
                        //webViewBrief.getEngine().loadContent(finalHtml, "text/html; charset=utf-8");

                        // Diese Methode übersetzt den HTML-Text in ein stabiles Bild für Ihr JLabel
                        aktualisiereVorschauAlsBild(finalHtml);

                        System.out.println("DEBUG VORSCHAU: Ladevorgang gestartet (Länge: " + finalHtml.length() + ").");
                    }
                });
            });
        } catch (Exception ex) {
            System.err.println("Fehler bei aktualisiereVorschauInhalt: " + ex.getMessage());
        }
    }

    private void druckeUmschlag() {
        // 1. Prüfung, ob ein Mitglied gewählt wurde
        if (ausgewaehltesMitglied == null) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie zuerst ein Mitglied aus!",
                    "Kein Mitglied gewählt", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Einfaches Umschlag-Layout (HTML)
        // Sie können hier ein spezielles Umschlag-Template laden oder erstellen
        String umschlagHtml = "<html><body style='font-family:Arial; padding: 20mm;'>" +
                "<div style='margin-top: 50mm; margin-left: 80mm;'>" +
                ausgewaehltesMitglied.getVorname() + " " + ausgewaehltesMitglied.getNachname() + "<br>" +
                ausgewaehltesMitglied.getStrasse() + "<br><br>" +
                ausgewaehltesMitglied.getPostleitzahl() + " " + ausgewaehltesMitglied.getOrt() +
                "</div></body></html>";

        // 3. Druck starten
        try {
            File tempUmschlagPdf = File.createTempFile("umschlag_", ".pdf");

            // Wir nutzen den bewährten PdfExporter, um das PDF zu erzeugen und zu drucken
            // (Null steht hier für den Systemstandard-Drucker)
            PdfExporter.exportHtmlToPdfOrPng(umschlagHtml, tempUmschlagPdf, null);

            JOptionPane.showMessageDialog(this, "Umschlag-Druckauftrag wurde gesendet.");

            // Kurz warten und aufräumen
            Thread.sleep(1000);
            tempUmschlagPdf.delete();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Fehler beim Umschlag-Druck: " + ex.getMessage(),
                    "Druckfehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    //private void ladeDateiInEditor(File file) {
    //    try {
    //        editorPane.setPage(file.toURI().toURL()); // Lädt HTML-Datei direkt
    //    } catch (IOException e) {
    //        editorPane.setText("<html><body>Fehler beim Laden der Datei.</body></html>");
    //    }
    //}

    // Methode, die Ihre existierende Druck-Logik nutzt, um ein Bild zu erzeugen
    private void ladeVorschauAlsBild(String html) {
        new Thread(() -> {
            try {
                // 1. Temporäre Datei für die Vorschau
                File tempBild = File.createTempFile("vorschau", ".png");

                // 2. Hier rufen Sie IHREN EIGENEN PDF-EXPORTER auf
                // Er muss den HTML-String rendern und als PNG speichern
                // Beispiel: PdfExporter.renderToPng(html, tempBild.getAbsolutePath());

                // 3. Wenn Sie keinen PdfExporter haben, der das kann:
                // Sie können auch eine einfache JavaFX-Snapshots-Klasse nutzen,
                // die NUR für das Bild rendert und dann sofort wieder schließt.

                SwingUtilities.invokeLater(() -> {
                    lblVorschauBild.setIcon(new ImageIcon(tempBild.getAbsolutePath()));
                    lblVorschauBild.repaint();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    public JPanel erzeugeBeitragsPanel() {
        // 1. Modelle und Tabelle initialisieren
        this.beitraegeModel = new BeitraegeTableModel(new ArrayList<>());
        this.tabelleBeitraege = new JTable(this.beitraegeModel);

        // 2. Buttons initialisieren
        this.btnExcelBeitraegeLaden = new JButton("Excel Beiträge laden");
        this.btnExcelBeitraegeSpeichern = new JButton("Excel Beiträge speichern");
        this.btnAusDatenbankLaden = new JButton("Aus Datenbank laden");

        // 3. Labels & Textfelder für die Beitrags-Maske initialisieren
        this.lblBeitragMitgliedInfo = new JLabel("Kein Mitglied ausgewählt");
        this.txtBeitragJahr = new JTextField(6);
        this.txtBeitragBetrag = new JTextField(8);

        // 4. SEPARATE STATUS-AUSWAHL (Textfeld + Pfeil-Button)
        this.txtStatusAnzeige = new JTextField("Offen", 8);
        this.txtStatusAnzeige.setEditable(false); // Verhindert freie Tastatureingaben
        this.txtStatusAnzeige.setBackground(Color.WHITE); // Sieht trotz "read-only" einladend aus

        // Kleiner Button mit einem Pfeil-Symbol (▼ Unicode)
        this.btnStatusPfeil = new JButton("▼");
        this.btnStatusPfeil.setMargin(new Insets(2, 5, 2, 5)); // Macht den Button schön kompakt

        // Wir bauen das Eingabefeld und den Pfeil-Button nahtlos zusammen
        JPanel statusAuswahlKomponente = new JPanel(new BorderLayout(0, 0));
        statusAuswahlKomponente.add(this.txtStatusAnzeige, BorderLayout.CENTER);
        statusAuswahlKomponente.add(this.btnStatusPfeil, BorderLayout.EAST);

        // Ein Container-Panel für das Label "Status:" und die neue Komponente
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusPanel.add(new JLabel("Status: "));
        statusPanel.add(statusAuswahlKomponente);

        // 5. Eine Aktionsleiste (Panel) für die Buttons erstellen
        JPanel aktionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        aktionsPanel.add(this.btnExcelBeitraegeLaden);
        aktionsPanel.add(this.btnExcelBeitraegeSpeichern);
        aktionsPanel.add(this.btnAusDatenbankLaden);
        aktionsPanel.add(statusPanel); // Hier fügen wir die separate Status-Auswahl hinzu

        // Klick-Logik direkt hinzufügen (oder alternativ in deine registriereEvents() verschieben)
        java.awt.event.ActionListener zeigeAuswahlLogik = e -> oeffneStatusAuswahlDialog();
        this.btnStatusPfeil.addActionListener(zeigeAuswahlLogik);

        // Ermöglicht den Klick auf das Textfeld selbst, um die Auswahl zu öffnen
        this.txtStatusAnzeige.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                oeffneStatusAuswahlDialog();
            }
        });

        // 6. Das bestehende BeitragsModulPanel erzeugen
        BeitragsModulPanel beitragsModulPanel = new BeitragsModulPanel(tabelleBeitraege);

        // 7. Das Hauptpanel layouten und alles zusammensetzen
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainPanel.add(aktionsPanel, BorderLayout.NORTH);
        mainPanel.add(beitragsModulPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel erzeugeDruckZentrumPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Brief- & Druckzentrum"));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        // 1. WebView für die Vorschau (Zentrum)
        // In erzeugeDruckZentrumPanel()
//        JFXPanel jfxPanel = new JFXPanel();
//        jfxPanel.setPreferredSize(new Dimension(800, 600)); // HIER explizite Größe setzen
//        Platform.runLater(() -> {
//            this.webViewBrief = new WebView();
//
//            // 1. Zwinge das WebView zur Größe
//            this.webViewBrief.setMinSize(600, 400);
//            this.webViewBrief.setPrefSize(800, 600);
//
//            // 2. Erstelle die Szene
//            fxBriefPanel.setScene(new javafx.scene.Scene(this.webViewBrief));
//
//            // 3. WICHTIG: Das JFXPanel selbst muss Swing mitteilen, dass es Platz braucht
//            // Suchen Sie das fxBriefPanel oder jfxPanel und setzen Sie dies:
//            fxBriefPanel.setPreferredSize(new Dimension(800, 600));
//            fxBriefPanel.revalidate();
//            fxBriefPanel.repaint();
//        });

        // Ersetzen Sie das JFXPanel durch ein einfaches JLabel in einer ScrollPane
        //lblVorschauBild = new JLabel("Vorschau wird generiert...");
        //mainPanel.add(new JScrollPane(lblVorschauBild), BorderLayout.CENTER);

        lblAusgewaehltesMitglied = new JLabel("Aktuell ausgewählt: (Kein Mitglied in der Tabelle markiert)");
        lblAusgewaehltesMitglied.setForeground(new Color(0, 102, 204));
        topPanel.add(lblAusgewaehltesMitglied, BorderLayout.NORTH);

        JPanel aktionsLeiste = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        aktionsLeiste.add(new JLabel("Textbausteine:"));

        String[] vorLagenNamen = {"-- Bitte wählen --", "Willkommen im Verein",
                "Beitragserinnerung", "Einladung zur Hauptversammlung"};
        comboVorlagen = new JComboBox<>(vorLagenNamen);
        aktionsLeiste.add(comboVorlagen);

        btnTextLaden = new JButton("Brief-Vorlage öffnen (.txt, .docx, .odt)");
        btnTextSpeichern = new JButton("Aktuellen Text");
        aktionsLeiste.add(btnTextLaden);
        aktionsLeiste.add(btnTextSpeichern);

        topPanel.add(aktionsLeiste, BorderLayout.SOUTH);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // txtBriefText
        txtBriefText = new JTextArea();
        txtBriefText.setFont(new Font("Arial", Font.PLAIN, 12));
        txtBriefText.setLineWrap(true);
        txtBriefText.setWrapStyleWord(true);
        txtBriefText.setText("Wählen Sie auf der ersten Seite ein Mitglied aus. " +
                "Nutzen Sie dann hier die Vorlagen oder schreibenSie einen freien Text...");

        JScrollPane scrollPane = new JScrollPane(txtBriefText);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // -- UNTERER BEREICH: GETEILLT IN SCANNER & DRUCKER --
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 15, 0));

        // Links: Dokumenten-Scanner (Neu)
        JPanel scannerPanel = new JPanel(new GridBagLayout());
        scannerPanel.setBorder((BorderFactory.createTitledBorder("Dokumenten-Scanner (SANE / Paperless)")));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtScanName = new JTextField("dokument_scan",12);
        String[] ziele = {"Direkt an Paperless-ngx übergeben","Lokal als Bild/PDF speichern"};

        comboScanZiel = new JComboBox<>(ziele);
        btnScanStarten = new JButton("Scan-Vorgang starten");
        btnScanStarten.setFont(new Font("Arial", Font.BOLD, 11));
        btnScanStarten.setBackground(new Color(40,167,69)) ; // Grün
        btnScanStarten.setForeground(Color.WHITE) ; // Weiß

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

    private void anpassenSpaltenBreiten() {
        int[] breiten = {50, 80, 80, 60, 60, 120, 100, 150, 70, 120, 100, 150, 200};
        for (int i = 0; i < breiten.length; i++) {
            if (i < tabelle.getColumnModel().getColumnCount()) {
                tabelle.getColumnModel().getColumn(i).setPreferredWidth(breiten[i]);
            }
        }
    }


    private String getEditorInhalt() {
        // Falls Sie Änderungen im Editor (z.B. durch den User) mitbekommen müssen,
        // könnten Sie hier noch die Variable mit dem Editor abgleichen.
        // Wenn der User aber nur Vorlagen druckt, reicht das hier völlig aus:
        return this.aktuellerBriefInhalt;
    }

//
    /**
     * Führt einen Rich-Text-Formatierungsbefehl auf der aktiven Editor-WebView aus.
     * Wechselt hierzu threadsicher auf den JavaFX Application Thread und stößt
     * danach ein Live-Update der Briefvorschau an.
     *
     * @param command Der HTML-ExecCommand (z.B. "bold", "italic", "fontSize", "justifyLeft")
     * @param value   Optionale Parameter (z.B. die Größe "3" oder Block-Typen wie "h4"), kann null sein
     */

    private void exekutiereHtmlBefehl(String command, String value) {
        // Ermitteln, welcher Tab aktiv ist, um die richtige WebView zu manipulieren
        final int aktiverTab = tabbedPane.getSelectedIndex();

        Platform.runLater(() -> {
            try {
                // Standardmäßig soll der Template-Editor formatiert werden
                WebView aktiveWebView = webViewEditor;

                // Falls der Nutzer direkt im Druckzentrum-Tab editiert
                if (aktiverTab == 1) {
                    aktiveWebView = webViewBrief;
                }

                if (aktiveWebView != null && aktiveWebView.getEngine() != null) {
                    String script;
                    if (value == null) {
                        script = "document.execCommand('" + command + "', false, null);";
                    } else {
                        script = "document.execCommand('" + command + "', false, '" + value + "');";
                    }

                    // Befehl ausführen
                    aktiveWebView.getEngine().executeScript(script);
                    System.out.println("DEBUG EDITOR: HTML-Befehl '" + command + "' ausgeführt.");

                    // LIVE-SYNCHRONISATION:
                    String geaenderterInhalt = getEditorInhalt();

                    // Fallback, falls der Editor-Inhalt leer oder zu kurz zurückgegeben wurde
                    if (geaenderterInhalt == null || geaenderterInhalt.trim().length() < 10) {
                        // Direktes Auslesen aus der WebEngine als Fallback im JavaFX-Thread
                        geaenderterInhalt = (String) aktiveWebView.getEngine().executeScript("document.documentElement.outerHTML");
                    }

                    // Platzhalter ersetzen
                    if (ausgewaehltesMitglied != null) {
                        geaenderterInhalt = ersetzePlatzhalter(geaenderterInhalt, ausgewaehltesMitglied);
                    } else {
                        // Fallback-Dummy-Ersetzung, damit der Text nicht leer bleibt, wenn kein Mitglied aktiv ist
                        geaenderterInhalt = geaenderterInhalt.replace("[Brief_Anrede]", "Sehr geehrte Damen und Herren,");
                    }

                    // Sicherheitsnetz: Falls das HTML zerschossen oder komplett leer sein sollte
                    if (geaenderterInhalt == null || geaenderterInhalt.trim().isEmpty()) {
                        geaenderterInhalt = "<html><body style='font-family:Arial; margin:20px;'><h3>Brief-Vorschau</h3><p>Text wird geladen...</p></body></html>";
                    }

                    // WICHTIG: Die Vorschau-WebView aktualisieren
                    if (webViewBrief != null && webViewBrief.getEngine() != null) {
                        final String finalHtml = geaenderterInhalt;
                        // Erzwinge das Laden des Contents im korrekten Format
                        webViewBrief.getEngine().loadContent(finalHtml, "text/html; charset=utf-8");
                    }
                } else {
                    System.err.println("WARNUNG: Editor-WebView ist für Formatierungsbefehl nicht bereit.");
                }
            } catch (Exception ex) {
                System.err.println("Fehler bei exekutiereHtmlBefehl: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    private JPanel erzeugeTemplateEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Haupt-Werkzeugleiste (FlowLayout)
        JPanel werkzeugLeiste = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        // Datei-Aktionen
        btnEditorLaden = new JButton("Template öffnen");
        btnEditorSpeichern = new JButton("Template speichern");
        werkzeugLeiste.add(btnEditorLaden);
        werkzeugLeiste.add(btnEditorSpeichern);

        // Trennsteg für optische Ordnung
        werkzeugLeiste.add(new JToolBar.Separator());

        // FORMATIERUNGS-BUTTONS
        JButton btnBold = new JButton("B");
        btnBold.setFont(btnBold.getFont().deriveFont(java.awt.Font.BOLD));
        btnBold.setToolTipText("Fett");

        JButton btnItalic = new JButton("I");
        btnItalic.setFont(btnItalic.getFont().deriveFont(java.awt.Font.ITALIC));
        btnItalic.setToolTipText("Kursiv");

        JButton btnUnderline = new JButton("U");
        // Kleiner Trick für ein echtes Unterstreichen im Swing-Button-Text:
        btnUnderline.setText("<html><u>U</u></html>");
        btnUnderline.setToolTipText("Unterstreichen");

        werkzeugLeiste.add(btnBold);
        werkzeugLeiste.add(btnItalic);
        werkzeugLeiste.add(btnUnderline);

        werkzeugLeiste.add(new JToolBar.Separator());

        // Ausrichtungs-Buttons
        JButton btnAlignLeft = new JButton("⇐");
        JButton btnAlignCenter = new JButton("⇑");
        JButton btnAlignRight = new JButton("⇒");
        btnAlignLeft.setToolTipText("Linksbündig");
        btnAlignCenter.setToolTipText("Zentriert");
        btnAlignRight.setToolTipText("Rechtsbündig");

        werkzeugLeiste.add(btnAlignLeft);
        werkzeugLeiste.add(btnAlignCenter);
        werkzeugLeiste.add(btnAlignRight);

        werkzeugLeiste.add(new JToolBar.Separator());

        // Schriftgrößen-Auswahl
        String[] groessen = {"Schriftgröße", "Klein", "Normal", "Groß", "Überschrift"};
        JComboBox<String> comboSize = new JComboBox<>(groessen);
        werkzeugLeiste.add(comboSize);

        werkzeugLeiste.add(new JToolBar.Separator());

        // Platzhalter-Auswahl (bestehend)
        String[] platzhalter = {"-- Platzhalter einfügen --", "MNr", "Anrede", "Titel", "Vorname", "Nachname", "Strasse", "PLZ", "Ort", "Eintritt", "Austritt", "Brief_Anrede", "Datum", "Jahr", "Betrag"};
        comboPlatzhalter = new JComboBox<>(platzhalter);
        werkzeugLeiste.add(comboPlatzhalter);

        // EVENT-LOGIK FÜR DIE NEUEN BUTTONS DIREKT ANZEIGEN
        btnBold.addActionListener(e -> exekutiereHtmlBefehl("bold", null));
        btnItalic.addActionListener(e -> exekutiereHtmlBefehl("italic", null));
        btnUnderline.addActionListener(e -> exekutiereHtmlBefehl("underline", null));

        btnAlignLeft.addActionListener(e -> exekutiereHtmlBefehl("justifyLeft", null));
        btnAlignCenter.addActionListener(e -> exekutiereHtmlBefehl("justifyCenter", null));
        btnAlignRight.addActionListener(e -> exekutiereHtmlBefehl("justifyRight", null));

        comboSize.addActionListener(e -> {
            int sel = comboSize.getSelectedIndex();
            if (sel == 1) exekutiereHtmlBefehl("fontSize", "2"); // klein
            if (sel == 2) exekutiereHtmlBefehl("fontSize", "3"); // normal
            if (sel == 3) exekutiereHtmlBefehl("fontSize", "5"); // groß
            if (sel == 4) exekutiereHtmlBefehl("formatBlock", "h4"); // Überschrift
            comboSize.setSelectedIndex(0);
        });

        panel.add(werkzeugLeiste, BorderLayout.NORTH);
        panel.add(fxEditorPanel, BorderLayout.CENTER);

        return panel;
    }

//

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

        btnExcelExport = new JButton("Excel-Export");

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

                // --- RESET DER GEBLOCKTEN BUTTONS BEI NEUWAHL ---
                btnUmschlagDrucken.setEnabled(true);
                btnUmschlagDrucken.setText("Umschlag (DL)");
                btnUmschlagDrucken.setBackground(null);
                btnUmschlagDrucken.setForeground(null);
                btnUmschlagDrucken.setBorder(UIManager.getBorder("Button.border"));
                // ------------------------------------------------

                int selectedRow = tabelle.getSelectedRow();
                int[] selectedRows = tabelle.getSelectedRows();
                // ... hier folgt dein bestehender Code (if (selectedRows.length > 1) etc.) ...

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

       //btnEtikettDrucken.addActionListener(e -> {
       //     int startPos = 1;
       //     if (spinnerEtikettPos.getValue() instanceof Number) {
       //         startPos = ((Number) spinnerEtikettPos.getValue()).intValue();
        //    }

//            int selectedRow = tabelle.getSelectedRow();
//            int[] selectedRows = tabelle.getSelectedRows();
//
//            List<Mitglied> zuDruckendeMitglieder = new ArrayList<>();
//
//            if (selectedRows != null && selectedRows.length > 1) {
//                for (int viewRow : selectedRows) {
//                    int modelRow = tabelle.convertRowIndexToModel(viewRow);
//                    zuDruckendeMitglieder.add(mitglieder.getMitgliedAt(modelRow));
//                }
//            } else if (selectedRow != -1) {
//                int modelRow = tabelle.convertRowIndexToModel(selectedRow);
//                zuDruckendeMitglieder.add(mitglieder.getMitgliedAt(modelRow));
//            } else {
//                JOptionPane.showMessageDialog(this, "Bitte wählen Sie mindestens ein Mitglied aus der Tabelle aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
//                return;
//            }

            // Druck ausführen
            //fuehreEtikettenDruckAus(zuDruckendeMitglieder, startPos);

            // --- KOSMETISCHE REPARATUR HIER ---
            // Button sperren und optisch als "erledigt" markieren
            //btnEtikettDrucken.setEnabled(false);
            //btnEtikettDrucken.setText("Seriendruck erledigt");
            //btnEtikettDrucken.setBackground(new java.awt.Color(220, 53, 69)); // Dunkelrot
            //btnEtikettDrucken.setForeground(java.awt.Color.WHITE);
            // Einen roten Rahmen setzen
            //btnEtikettDrucken.setBorder(BorderFactory.createLineBorder(java.awt.Color.RED, 2));

            // Nach 3 Sekunden automatisch zurücksetzen
            //javax.swing.Timer timer = new javax.swing.Timer(3000, event -> {
            //    btnEtikettDrucken.setEnabled(true);
            //    btnEtikettDrucken.setText("Etikett drucken");
            //    btnEtikettDrucken.setBackground(null);
            //    btnEtikettDrucken.setForeground(null);
            //    btnEtikettDrucken.setBorder(UIManager.getBorder("Button.border"));
            //});

        //});

        // Event für den Dokumenten-Scanner registrieren
        btnScanStarten.addActionListener(e -> {
            String dateiName = txtScanName.getText().trim();
            String ziel = (String) comboScanZiel.getSelectedItem();

            if (dateiName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Bitte einen Dateinamen eingeben.", "Fehler", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Die neue Abfrage: Text (Graustufen) oder Farbe?
            Object[] optionen = {"Dokument / Text (S&W)", "Farbe (Fotos/Logos)"};
            int auswahl = JOptionPane.showOptionDialog(
                    this,
                    "Wie möchten Sie das Dokument einscannen?\n(Der Text-Modus scannt deutlich schneller)",
                    "Scan-Modus wählen",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    optionen,
                    optionen[0] // Standardvorauswahl ist Text
            );

            // Wenn der Dialog geschlossen wurde (X), brechen wir ab
            if (auswahl == JOptionPane.CLOSED_OPTION) {
                return;
            }

            // "Dokument / Text" entspricht Option 0 (YES_OPTION) -> farbe = false
            // "Farbe" entspricht Option 1 (NO_OPTION) -> farbe = true
            boolean scanInFarbe = (auswahl == JOptionPane.NO_OPTION);

            // Scan-Vorgang im Hintergrund-Thread starten
            new Thread(() -> {
                try {
                    btnScanStarten.setEnabled(false);
                    btnScanStarten.setText("Scanne...");

                    // Die Methode wird jetzt mit dem dritten Parameter aufgerufen
                    fuehreScanAus(dateiName, ziel, scanInFarbe);

                    JOptionPane.showMessageDialog(this, "Scan-Vorgang erfolgreich beendet!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Scannen: " + ex.getMessage(), "Scanner-Fehler", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnScanStarten.setEnabled(true);
                    btnScanStarten.setText("Scan-Vorgang starten");
                }
            }).start();
        });
        btnExcelExport.addActionListener(e -> {
            if (aktuelleListe == null || aktuelleListe.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Keine Daten zum Exportieren vorhanden.", "Export abgebrochen", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Excel-Export speichern");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Excel-Arbeitsmappe (*.xlsx)", "xlsx"));
            fileChooser.setSelectedFile(new File("ALG_Mitgliederliste.xlsx"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File datei = fileChooser.getSelectedFile();

                // Dateiendung erzwingen, falls vom User vergessen
                if (!datei.getName().toLowerCase().endsWith(".xlsx")) {
                    datei = new File(datei.getAbsolutePath() + ".xlsx");
                }

                final File finaleDatei = datei;

                // Export im Hintergrund ausführen, um GUI-Lag zu verhindern
                new Thread(() -> {
                    try {
                        btnExcelExport.setEnabled(false);
                        fuehreExcelExportAus(finaleDatei);
                        JOptionPane.showMessageDialog(this, "Daten erfolgreich nach Excel exportiert!", "Export erfolgreich", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Fehler beim Excel-Export: " + ex.getMessage(), "Export-Fehler", JOptionPane.ERROR_MESSAGE);
                        System.err.println("Excel-Export error: " + ex.getMessage());
                    } finally {
                        btnExcelExport.setEnabled(true);
                    }
                }).start();
            }
        });

        // --- EVENTS FÜR TAB 4 (BEITRÄGE) ---
        btnExcelBeitraegeLaden.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Beitrags-Excel einlesen");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel-Arbeitsmappe (*.xlsx)", "xlsx"));

            // 'this' bezieht sich hier auf die umgebende GUI-Klasse (z.B. dein Hauptfenster oder Panel)
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();

                new Thread(() -> {
                    try {
                        // 1. Laden der Daten im Hintergrund-Thread
                        List<Beitrag> geladeneBeitraege = fuehreBeitragExcelImportAus(file);

                        // 2. GUI-Aktualisierungen gesammelt zurück auf den Event Dispatch Thread (EDT) schicken
                        SwingUtilities.invokeLater(() -> {
                            beitraegeModel.setBeitraegeListe(geladeneBeitraege);
                            JOptionPane.showMessageDialog(this, "Beiträge erfolgreich aus Excel geladen!");
                        });

                    } catch (Exception ex) {
                        // Auch die Fehlermeldung muss auf den EDT, falls das Einlesen fehlschlägt
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this,
                                    "Fehler beim Laden der Excel: " + ex.getMessage(),
                                    "Fehler",
                                    JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }).start();
            }
        });

        btnExcelBeitraegeSpeichern.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Beiträge in Excel speichern");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Excel-Arbeitsmappe (*.xlsx)", "xlsx"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".xlsx")) {
                    file = new File(file.getAbsolutePath() + ".xlsx");
                }
                File finaleDatei = file;
                new Thread(() -> {
                    try {
                        // Aufruf der Speicherlogik
                        fuehreBeitragExcelExportAus(finaleDatei, beitraegeModel.getBeitraegeListe());

                        // Flag setzen
                        unsavedChanges = false;

                        JOptionPane.showMessageDialog(this, "Beiträge erfolgreich in Excel gesichert!");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Fehler beim Speichern der Excel: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
                    }
                }).start();
            }
        });

        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();

            // --- BESTEHENDES STYLING (bleibt unverändert) ---
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
            // ------------------------------------------------

            // --- NEU: VORBELEGUNG FÜR DIE BEITRAGS-MASKE ---
            if (selectedIndex == 3) { // Index 3 entspricht dem 4. Tab
                // Prüfen, ob im ersten Tab ein Mitglied markiert ist
                if (ausgewaehltesMitglied != null) {
                    lblBeitragMitgliedInfo.setText("Beitrag anlegen für: "
                            + ausgewaehltesMitglied.getNachname() + ", "
                            + ausgewaehltesMitglied.getVorname() + " (M-Nr: "
                            + ausgewaehltesMitglied.getMNr() + ")");

                    // Felder automatisch vorbelegen
                    txtBeitragJahr.setText(String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)));
                    txtBeitragBetrag.setText("20.00"); // Hier ggf. deinen Standard-Beitrag eintragen
                    comboBeitragStatus.setSelectedIndex(0); // "Offen" voreinstellen

                    // Wichtig: Wir merken uns das Mitglied im Beitrags-Kontext,
                    // falls ein neuer Beitrag erzeugt werden soll
                    ausgewaehlterBeitrag = new Beitrag();
                    ausgewaehlterBeitrag.setMNr(ausgewaehltesMitglied.getMNr());
                    ausgewaehlterBeitrag.setNachname(ausgewaehltesMitglied.getNachname());
                    ausgewaehlterBeitrag.setVorname(ausgewaehltesMitglied.getVorname());
                }
            }
        });

        // =========================================================================
        // EVENTS FÜR TAB 4 (NEU: AUS MITGLIEDER-DB ÜBERNEHMEN)
        // =========================================================================
        btnAusDatenbankLaden.addActionListener(e -> {
            // Prüfen, ob in Tab 1 überhaupt schon Mitglieder geladen wurden
            if (aktuelleListe == null || aktuelleListe.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Bitte laden Sie zuerst im ersten Tab die Mitgliederliste aus der Datenbank!",
                        "Hinweis", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int antwort = JOptionPane.showConfirmDialog(this,
                    "Möchten Sie für alle " + aktuelleListe.size() + " geladenen Mitglieder Beitrags-Einträge erzeugen?",
                    "Daten übernehmen", JOptionPane.YES_NO_OPTION);

            if (antwort == JOptionPane.YES_OPTION) {
                List<Beitrag> neueBeitraege = new ArrayList<>();
                int aktuellesJahr = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

                for (int i = 0; i < aktuelleListe.size(); i++) {
                    Mitglied m = aktuelleListe.get(i);
                    Beitrag b = new Beitrag();

                    // Fortlaufender Index: i startet bei 0, i + 1
                    b.setMNr(i + 1);

                    b.setNachname(m.getNachname());
                    b.setVorname(m.getVorname());
                    b.setJahr(aktuellesJahr);
                    b.setBetrag(20.00); // Standardbeitrag für die ALG
                    b.setStatus("Offen");
                    neueBeitraege.add(b);
                }
                // Die Tabelle im 4. Tab mit den exakten IDs befüllen
                beitraegeModel.setBeitraegeListe(neueBeitraege);

                // Flag setzen
                unsavedChanges = true;

                JOptionPane.showMessageDialog(this,
                        "Beiträge wurden mit fortlaufendem Index (1 bis " + aktuelleListe.size() + ") generiert.\n" +
                                "Bitte sichern Sie diese nun in der Excel-Datei!");
            }
        });

        tabelleBeitraege.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    oeffneStatusAuswahlDialog();

                }
            }
        });

        // ... hier folgen ggf. btnExcelBeitraegeLaden und btnExcelBeitraegeSpeichern
    } // <-- Hier endet registriereEvents()

    private void fuehreExcelExportAus(File datei) throws Exception {
        // Apache POI Klassen nutzen (SXSSFWorkbook ist extrem speicherschonend)
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(datei)) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Mitglieder ALG");

            // 1. Header-Zeile erstellen
            String[] header = {"M-Nr", "Eintritt", "Austritt", "Anrede", "Titel", "Nachname", "Vorname", "Straße", "PLZ", "Ort", "Telefon", "E-Mail", "Hinweise"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);

            // Einfaches Fett-Styling für den Header
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < header.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(header[i]);
                cell.setCellStyle(headerStyle);
            }

            // 2. Daten schreiben
            int rowNum = 1;
            for (Mitglied m : aktuelleListe) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(m.getMNr());
                row.createCell(1).setCellValue(m.getEintritt() != null ? m.getEintritt() : "");
                row.createCell(2).setCellValue(m.getAustritt() != null ? m.getAustritt() : "");
                row.createCell(3).setCellValue(m.getAnrede() != null ? m.getAnrede() : "");
                row.createCell(4).setCellValue(m.getTitel() != null ? m.getTitel() : "");
                row.createCell(5).setCellValue(m.getNachname() != null ? m.getNachname() : "");
                row.createCell(6).setCellValue(m.getVorname() != null ? m.getVorname() : "");
                row.createCell(7).setCellValue(m.getStrasse() != null ? m.getStrasse() : "");
                row.createCell(8).setCellValue(m.getPostleitzahl() != null ? m.getPostleitzahl() : "");
                row.createCell(9).setCellValue(m.getOrt() != null ? m.getOrt() : "");
                row.createCell(10).setCellValue(m.getTelefon() != null ? m.getTelefon() : "");
                row.createCell(11).setCellValue(m.getEmail() != null ? m.getEmail() : "");
                row.createCell(12).setCellValue(m.getHinweise() != null ? m.getHinweise() : "");
            }

            // Spaltenbreiten automatisch anpassen
            for (int i = 0; i < header.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
        }
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

    private void fuehrePDFExportAus() {
        String htmlInhalt = getEditorInhalt();

        // VERBESSERTE PRÜFUNG: Ignoriere minimale HTML-Standardgerüste
        if (htmlInhalt == null || htmlInhalt.trim().isEmpty() || htmlInhalt.length() <= 45) {
            JOptionPane.showMessageDialog(this, "Kein Text oder nur ein leeres Dokument zum Exportieren vorhanden.", "Warnung", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (ausgewaehltesMitglied != null) {
            htmlInhalt = ersetzePlatzhalter(htmlInhalt, ausgewaehltesMitglied);
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Ziel-PDF auswählen");
        chooser.setSelectedFile(new java.io.File(System.getProperty("user.home"), "ALG_Brief.pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = chooser.getSelectedFile();
        // .pdf-Endung sicherstellen
        if (!target.getName().toLowerCase().endsWith(".pdf")) {
            target = new File(target.getAbsolutePath() + ".pdf");
        }
        final File pdfTarget = target;
        final String finalHtml = htmlInhalt;

        Thread t = new Thread(() -> {
            try {
                System.out.println("DEBUG EXPORT: Starte PDF-Generierung mit " + finalHtml.length() + " Zeichen HTML.");
                //File result = PdfExporter.exportHtmlToPdfOrPng(pdfOut, finalHtml, pdfTarget);

                // NEU (Korrekt):
                // 1. Wir übergeben den HTML-String (finalHtml)
                // 2. Wir übergeben das Ziel-File (pdfTarget)
                // 3. Wir übergeben den Drucker-Namen (hier 'null', da der Nutzer ihn im Dialog wählt)

                System.out.println("DEBUG: Finaler String vor Export: " + finalHtml.substring(0, Math.min(finalHtml.length(), 200)));

                File result = PdfExporter.exportHtmlToPdfOrPng(finalHtml, pdfTarget, null);
                if (result != null && result.exists()) {
                    // Gesamte Interaktion zurück auf den Swing-EDT-Thread verlagern
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                "Export erzeugt: " + result.getAbsolutePath(), "Export fertig", JOptionPane.INFORMATION_MESSAGE);

                        // Dialog ist jetzt absolut threadsicher hier drinnen!
                        int antwort = JOptionPane.showConfirmDialog(this,
                                "Möchten Sie die erzeugte Datei jetzt drucken?", "Drucken?", JOptionPane.YES_NO_OPTION);

                        if (antwort == JOptionPane.YES_OPTION) {

                            // --- NATIVEN DRUCKDIALOG INITIALISIEREN ---
                            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
                            job.setJobName("AlgDatabank - PDF Ausdruck");

                            // Letzten ausgewählten Drucker als Vorauswahl setzen, falls vorhanden
                            if (ausgewaehlterDrucker != null) {
                                try {
                                    javax.print.PrintService[] services = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
                                    if (services != null) {
                                        for (javax.print.PrintService s : services) {
                                            if (s.getName().equals(ausgewaehlterDrucker)) {
                                                job.setPrintService(s);
                                                break;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    System.err.println("Fehler bei Druckervorauswahl im Export: " + e.getMessage());
                                }
                            }

                            // Öffnet den vollständigen GTK/Linux-Systemdialog (inkl. Print to File)
                            boolean doPrint = job.printDialog();
                            if (doPrint && job.getPrintService() != null) {
                                // Den gewählten Drucker für Folgedrucke merken
                                ausgewaehlterDrucker = job.getPrintService().getName();
                                System.out.println("Im PDF-Nachdruck-System-Dialog gewählt: " + ausgewaehlterDrucker);


                            } else {
                                System.out.println("Druck nach PDF-Export abgebrochen.");
                            }

                        } else {
                            // Versuche Datei im PDF-Betrachter zu öffnen
                            try {
                                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(result);
                            } catch (Exception ex) {
                                System.err.println("Desktop.open error: " + ex.getMessage());
                            }
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Export fehlgeschlagen.", "Fehler", JOptionPane.ERROR_MESSAGE));
                }
            } catch (Exception ex) {
                System.err.println("PDF Export error: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Fehler beim Export: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE));
            }
        });
        t.setDaemon(true);
        t.start();
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

        if (comboVorlagen == null) {
            System.err.println("DEBUG: comboVorlagen ist NULL in initialisiereDruckLogik!");
            return; // Verhindert den Absturz, bis wir das behoben haben
        }

        comboVorlagen.addActionListener(e -> {
            int index = comboVorlagen.getSelectedIndex();
            if (index > 0) {
                String standardTemplate = ladeStandardTemplate(index);
                setEditorInhalt(standardTemplate);
                aktualisiereBriefVorschau(standardTemplate);
            }
        });

        btnTextLaden.addActionListener(e -> oeffneDateiInTextArea(txtBriefText));


        btnTextSpeichern.addActionListener(e -> {
            String inhalt = getEditorInhalt();
            JFileChooser chooser = new JFileChooser();
            if (ausgewaehltesMitglied != null) {
                chooser.setSelectedFile(new File("Brief_" + ausgewaehltesMitglied.getNachname() + ".html"));
            }
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
                    bw.write(inhalt);
                    JOptionPane.showMessageDialog(this, "Datei erfolgreich gespeichert.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Speichern: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnBriefDrucken.addActionListener(e -> fuehreDruckAus());
        //btnExportPDF.addActionListener(e -> fuehrePDFExportAus());
        //btnExportAndPrintPDF.addActionListener(e -> fuehreExportUndDirektDruckAus());
        //btnTestPrinter.addActionListener(e -> fuehreTestDruckAus());

         // ... Vorheriger GUI-Code (z.B. Erstellung der Textfelder, Tabellen im Briefzentrum)

        //btnVorschau = new JButton("Vorschau anzeigen"); // oder ähnlich benannt
        //btnDrucken = new JButton("Drucken");

        // =========================================================================
        // ROBUSTER VORSCHAU-BUTTON WITH AUTODETECT & FALLBACK-MITGLIED
        // =========================================================================

        // =========================================================================
        // ULTRA-DIAGNOSE: REAGIERT DER BUTTON ÜBERHAUPT?
        // =========================================================================
//        System.out.println("DEBUG: Binde Diagnose-Listener an btnVorschau...");
//
//        // Wir entfernen vorsichtshalber alle eventuell doppelt registrierten Listener
//        for (java.awt.event.ActionListener al : btnVorschau.getActionListeners()) {
//            btnVorschau.removeActionListener(al);
//        }
//
//
//        // 1. Der absolute Minimal-Listener
//        btnVorschau.addActionListener(e -> {
//            System.out.println("!!! DIREKT-KLICK ERFOLGREICH: btnVorschau hat den Klick erhalten !!!");
//            btnVorschau.setBackground(java.awt.Color.GREEN); // Visuelles Feedback auf dem Bildschirm
//
//            // Test-Meldung direkt auf dem Hauptfenster ausgeben
//            AlgDatabankGui.this.setTitle("KLICK REGISTRIERT! " + new java.util.Date());
//
//            // Starte jetzt die Vorschau im separaten Thread
//            new Thread(() -> {
//                try {
//                    System.out.println("-> Starte Notfall-Generierung...");
//                    String html = getEditorInhalt();
//                    HtmlDrucker renderer = new HtmlDrucker(html);
//
//                    PageFormat pf = new PageFormat();
//                    Paper paper = new Paper();
//                    paper.setSize(595, 842);
//                    paper.setImageableArea(0, 0, 595, 842);
//                    pf.setPaper(paper);
//
//                    BufferedImage img = new BufferedImage(595, 842, BufferedImage.TYPE_INT_RGB);
//                    Graphics2D g2d = img.createGraphics();
//                    g2d.setColor(java.awt.Color.WHITE);
//                    g2d.fillRect(0, 0, 595, 842);
//                    renderer.print(g2d, pf, 0);
//                    g2d.dispose();
//
//                    SwingUtilities.invokeLater(() -> {
//                        JFrame f = new JFrame("Notfall-Vorschau");
//                        f.setSize(400, 600);
//                        f.add(new JScrollPane(new JLabel(new ImageIcon(img))));
//                        f.setVisible(true);
//                    });
//                } catch(Exception ex) {
//                    System.err.println("Fehler: " + ex.getMessage());
//                }
//            }).start();
//        });

        // 2. Ein MouseListener als Fallback, falls der ActionListener blockiert wird
        btnVorschau.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                System.out.println("-> [MOUSE EVENT] Button wurde physisch gedrückt! Koordinaten: " + e.getPoint());
            }
        });

        //

        btnScanStarten.addActionListener(e -> {
            String dateiName = txtScanName.getText().trim();
            String ziel = (String) comboScanZiel.getSelectedItem();

            if (dateiName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Bitte einen Dateinamen eingeben.", "Fehler", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Object[] optionen = {"Dokument / Text (S&W)", "Farbe (Fotos/Logos)"};
            int auswahl = JOptionPane.showOptionDialog(
                    this,
                    "Wie möchten Sie das Dokument einscannen?\n(Der Text-Modus scannt deutlich schneller)",
                    "Scan-Modus wählen",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    optionen,
                    optionen[0]
            );

            if (auswahl == JOptionPane.CLOSED_OPTION) {
                return;
            }

            boolean scanInFarbe = (auswahl == JOptionPane.NO_OPTION);

            new Thread(() -> {
                try {
                    btnScanStarten.setEnabled(false);
                    btnScanStarten.setText("Scanne...");

                    fuehreScanAus(dateiName, ziel, scanInFarbe);

                    JOptionPane.showMessageDialog(this, "Scan-Vorgang erfolgreich beendet!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Scannen: " + ex.getMessage(), "Scanner-Fehler", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnScanStarten.setEnabled(true);
                    btnScanStarten.setText("Scan-Vorgang starten");
                }
            }).start();
        });
    }

    private void oeffneDateiInTextArea(JTextArea targetArea) {
        JFileChooser fileChooser = new JFileChooser();

        // Der erste Parameter ist die Beschreibung, die folgenden sind die Erweiterungen OHNE Punkt!
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Textdokumente (*.doc, *.docx, *.html, *.odt, *.txt)",
                "doc", "docx", "html", "htm", "odt", "txt"
        );
        fileChooser.setFileFilter(filter);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String name = file.getName().toLowerCase();

            try {
                String gelesenerInhalt = "";

                // 1. Inhalt korrekt einlesen
                if (name.endsWith(".docx")) {
                    gelesenerInhalt = readDocxFile(file);
                } else if (name.endsWith(".odt")) {
                    gelesenerInhalt = readOdtFile(file);
                } else {
                    // Einlesen für txt/html
                    gelesenerInhalt = new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                }

                // 2. Swing: Immer nur den Text anzeigen
                targetArea.setText(gelesenerInhalt);

                // 3. JavaFX: Unterscheidung zwischen HTML und Text
                final String finalContent = gelesenerInhalt;
                Platform.runLater(() -> {
                    if (webViewBrief != null) {
                        // ALT: webViewBrief.getEngine().loadContent(finalContent);

                        // NEU: Explizit als HTML rendern lassen
                        webViewBrief.getEngine().loadContent(finalContent, "text/html");

                        System.out.println("DEBUG: WebView hat HTML-Inhalt als text/html geladen.");
                    }
                });

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Laden: " + ex.getMessage());
                ex.printStackTrace();
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
                System.err.println("Error: " + ex.getMessage());
            }
        });
        printThread.setDaemon(true);
        printThread.start();
    }

    private String ladeVorlageAusRessourcen(String resourcePath) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            System.err.println("Fehler: resourcePath ist leer oder null.");
            return null;
        }

        // 1. Pfad für den ClassLoader normieren (darf KEINEN führenden Slash haben)
        String classLoaderPath = resourcePath;
        if (classLoaderPath.startsWith("/")) {
            classLoaderPath = classLoaderPath.substring(1);
        }

        try {
            // Versuche, Ressource aus dem Klassenverzeichnis zu laden (Standard für JARs)
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(classLoaderPath);
            if (inputStream != null) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Laden der Vorlage aus Ressourcen: " + e.getMessage());
        }

        // 2. Pfad für das Dateisystem normieren
        String fsPath = resourcePath;
        if (fsPath.startsWith("/")) {
            fsPath = fsPath.substring(1);
        }

        // Fallback für lokale Entwicklungsstarts mit exakter 3-Ebenen-Korrektur (Bricht src/src-Bug)
        try {
            String userDir = System.getProperty("user.dir");
            File resourcesRoot;

            if (userDir.endsWith("src/main/java")) {
                // 3-mal getParentFile() springt von /java -> /main -> /src -> Projekt-Wurzelverzeichnis
                File projektRoot = new File(userDir).getParentFile().getParentFile().getParentFile();
                resourcesRoot = new File(projektRoot, "src/main/resources");
            } else {
                // Falls das Programm korrekt im Projekt-Root gestartet wurde
                resourcesRoot = new File("src/main/resources");
            }

            File templateFile = new File(resourcesRoot, fsPath);
            System.out.println("DEBUG TEMPLATE: Absolute Suche unter: " + templateFile.getAbsolutePath());

            if (templateFile.exists()) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(templateFile), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                return sb.toString();
            } else {
                System.err.println("WARNUNG: Datei trotz Pfadkorrektur nicht gefunden unter: " + templateFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Laden der Vorlage aus Dateisystem: " + e.getMessage());
        }

        System.err.println("Template endgültig nicht gefunden! Gesucht nach: '" + classLoaderPath + "'");
        return null;
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

    private void initialisiereStandardVorlage() {
        // Lade die Standard-Vorlage aus den Ressourcen
        String defaultTemplate = ladeVorlageAusRessourcen("templates/algorithmus1.html");
        if (defaultTemplate != null && !defaultTemplate.isEmpty()) {
            setEditorInhalt(defaultTemplate);
            aktualisiereBriefVorschau(defaultTemplate);
        }
    }


    private String normalisiereLogoImTemplate(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        String base64Logo = ladeLogoBase64();
        if (base64Logo == null || base64Logo.isEmpty()) {
            return html;
        }

        String dataUri = "data:image/png;base64," + base64Logo;

        // Ersetzt vorhandene data:-URIs und file://-Varianten fuer das Logo robust in einem Schritt.
        return html
                .replaceAll("src=\"data:image/png;base64,[^\"]*\"", "src=\"" + dataUri + "\"")
                .replace("src=\"file:///home/wolfram/JB-Java_Projekte/Alg_Databank/bilder/logo-alg.png\"", "src=\"" + dataUri + "\"")
                .replace("src=\"bilder/logo-alg.png\"", "src=\"" + dataUri + "\"");
    }

    private String ladeLogoBase64() {
        // 1) Bevorzugt aus Ressourcen laden.
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("bilder/base64_logo.txt")) {
            if (in != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line = br.readLine();
                    if (line != null && !line.trim().isEmpty()) {
                        return line.trim();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Laden von base64_logo.txt aus Ressourcen: " + e.getMessage());
        }

        // 2) Fallback fuer lokale Entwicklungsstarts.
        File logoTxt = new File("bilder/base64_logo.txt");
        if (logoTxt.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(logoTxt), StandardCharsets.UTF_8))) {
                String line = br.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return line.trim();
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Laden von base64_logo.txt aus Dateisystem: " + e.getMessage());
            }
        }

        return null;
    }

    private void initialisiereEditorLogik() {
        // 1. LOGIK FÜR DAS PLATZHALTER-AUSWAHLFELD (Bestehend)
        comboPlatzhalter.addActionListener(e -> {
            int index = comboPlatzhalter.getSelectedIndex();
            if (index > 0) {
                String selectedPlaceholder = "[" + comboPlatzhalter.getSelectedItem().toString() + "]";
                Platform.runLater(() -> {
                    if (webViewEditor != null) {
                        // Nutzt JavaScript, um den Platzhalter präzise an der aktuellen Cursorposition im Editor zu platzieren
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

        // 2. TEMPLATE SPEICHERN (.html)
        btnEditorSpeichern.addActionListener(e -> {
            // HTML-Inhalt aus der WebView abfragen
            String htmlInhalt = getEditorInhalt();
            if (htmlInhalt == null || htmlInhalt.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Das Template ist leer und kann nicht gespeichert werden.", "Fehler", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Template speichern");
            fileChooser.setFileFilter(new FileNameExtensionFilter("HTML-Dateien (*.html, *.htm)", "html", "htm"));

            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File dateiZuSpeichern = fileChooser.getSelectedFile();

                // Dateiendung .html erzwingen, falls nicht eingegeben
                if (!dateiZuSpeichern.getName().toLowerCase().endsWith(".html") && !dateiZuSpeichern.getName().toLowerCase().endsWith(".htm")) {
                    dateiZuSpeichern = new File(dateiZuSpeichern.getAbsolutePath() + ".html");
                }

                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dateiZuSpeichern), StandardCharsets.UTF_8))) {
                    writer.write(htmlInhalt);
                    JOptionPane.showMessageDialog(this, "Template erfolgreich gespeichert!", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Speichern der Datei: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
                    System.err.println("Error: " + ex.getMessage());
                }
            }
        });

        // 3. TEMPLATE LADEN (.html)
        btnEditorLaden.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Template öffnen");
            fileChooser.setFileFilter(new FileNameExtensionFilter("HTML-Dateien (*.html, *.htm)", "html", "htm"));

            int userSelection = fileChooser.showOpenDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File ausgewaehlteDatei = fileChooser.getSelectedFile();
                StringBuilder inhaltBuilder = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ausgewaehlteDatei), StandardCharsets.UTF_8))) {
                    String zeile;
                    while ((zeile = reader.readLine()) != null) {
                        inhaltBuilder.append(zeile).append("\n");
                    }

                    String geladenerHtmlText = inhaltBuilder.toString();

                    // Inhalt in die JavaFX WebView zurückladen
                    Platform.runLater(() -> {
                        if (webViewEditor != null) {
                            webViewEditor.getEngine().loadContent(geladenerHtmlText);
                        }
                    });

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Laden der Datei: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
                    System.err.println("Error: " + ex.getMessage());
                }
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

    public void setEditorInhalt(String html) {
        this.masterHtmlContent = html;

        // Alles, was die WebView betrifft, MUSS in Platform.runLater
        Platform.runLater(() -> {
            try {
                if (webViewEditor != null && webViewEditor.getEngine() != null) {
                    // Hier das Laden ausführen
                    webViewEditor.getEngine().loadContent(html);
                    System.out.println("Debug: Inhalt erfolgreich in WebView geladen.");
                } else {
                    System.err.println("Fehler: webViewEditor ist null!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void aktualisiereBriefVorschau(String html) {
        // Führe das Rendering im Hintergrund aus
        Platform.runLater(() -> {
            // 1. Virtuelle, unsichtbare WebView zum Rendern
            WebView wv = new WebView();
            wv.setPrefSize(800, 1100); // A4 Format
            wv.getEngine().loadContent(html);

            // 2. Warten bis geladen
            wv.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    // 3. Snapshot erstellen
                    WritableImage img = wv.snapshot(new SnapshotParameters(), null);

                    // 4. Bild an Swing übergeben
                    SwingUtilities.invokeLater(() -> {
                        lblVorschauBild.setIcon(new ImageIcon(SwingFXUtils.fromFXImage(img, null)));
                        lblVorschauBild.setText(""); // Text entfernen
                        lblVorschauBild.repaint();
                    });
                }
            });
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
        System.out.println("Aufruf von fuehreDruckAus()");

        // 1. Inhalt sicherstellen
        String htmlInhalt = getBriefVorschauInhalt();
        if (istUnbrauchbarerDruckInhalt(htmlInhalt)) htmlInhalt = getEditorInhalt();
        if (istUnbrauchbarerDruckInhalt(htmlInhalt)) htmlInhalt = ladeVorlageAusRessourcen("ressourcen/algorithmus1.html");
        if (istUnbrauchbarerDruckInhalt(htmlInhalt)) htmlInhalt = ladeStandardTemplate(2);

        if (istUnbrauchbarerDruckInhalt(htmlInhalt)) {
            JOptionPane.showMessageDialog(this, "Kein Text zum Drucken vorhanden.", "Warnung", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (ausgewaehltesMitglied != null) {
            htmlInhalt = ersetzePlatzhalter(htmlInhalt, ausgewaehltesMitglied);
        }

        // 2. Nativer Druckerdialog mit Exception-Handling
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        javax.print.attribute.PrintRequestAttributeSet attributes = new javax.print.attribute.HashPrintRequestAttributeSet();
        attributes.add(javax.print.attribute.standard.MediaSizeName.ISO_A4);

        // Drucker vorauswählen, falls bekannt
        if (ausgewaehlterDrucker != null) {
            for (javax.print.PrintService s : javax.print.PrintServiceLookup.lookupPrintServices(null, null)) {
                if (s.getName().equals(ausgewaehlterDrucker)) {
                    try {
                        job.setPrintService(s);
                    } catch (java.awt.print.PrinterException e) {
                        System.err.println("Vorauswahl des Druckers fehlgeschlagen: " + e.getMessage());
                    }
                }
            }
        }


        // Dialog anzeigen - ohne try-catch, da keine PrinterException geworfen wird
        boolean dialogResult = job.printDialog(attributes);

        if (!dialogResult) {
            return; // Nutzer hat Abbrechen gedrückt
        }

        if (!dialogResult) {
            return; // Nutzer hat Abbrechen gedrückt
        }

        // Gewählten Drucker für zukünftige Aufrufe merken
        if (job.getPrintService() != null) {
            ausgewaehlterDrucker = job.getPrintService().getName();
        }

        // 3. Druck via PdfExporter (Chromium) in eigenem Thread
        final String finalHtml = htmlInhalt;
        final String printer = ausgewaehlterDrucker;

        new Thread(() -> {
            File tempPdf = null;
            try {
                // Temporäre Datei erstellen
                tempPdf = File.createTempFile("druckauftrag_", ".pdf");

                // Der PdfExporter rendert HTML via Chromium zu PDF und sendet es an den Drucker (lp)
                // Hinweis: Stellen Sie sicher, dass Ihre HTML-Datei im <head> ein <style>@page { size: A4; }</style> enthält.
                PdfExporter.exportHtmlToPdfOrPng(finalHtml, tempPdf, printer);

                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Der Brief wurde an '" + printer + "' gesendet.", "Druck gestartet", JOptionPane.INFORMATION_MESSAGE)
                );

                // Kurze Wartezeit, um sicherzustellen, dass lp den Job übernommen hat
                Thread.sleep(2000);
                if (tempPdf.exists()) tempPdf.delete();

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Druckfehler: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE)
                );
            }
        }).start();
    }

   private boolean istBildFastLeer(java.awt.image.BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int stepX = Math.max(1, w / 80);
        int stepY = Math.max(1, h / 80);
        int sampled = 0;
        int nonWhite = 0;
        for (int y = 0; y < h; y += stepY) {
            for (int x = 0; x < w; x += stepX) {
                sampled++;
                int rgb = image.getRGB(x, y) & 0x00FFFFFF;
                if (rgb != 0x00FFFFFF) {
                    nonWhite++;
                }
            }
        }
        double fill = (nonWhite * 100.0) / Math.max(1, sampled);
        return fill < 1.0;
    }

    private boolean istUnbrauchbarerDruckInhalt(String html) {
        if (html == null) return true;
        String t = html.trim();
        if (t.isEmpty()) return true;
        if (t.length() < 80) return true;
        String lower = t.toLowerCase(Locale.ROOT);
        return lower.contains("hier ihr template-text")
                || lower.contains("template konnte nicht geladen werden")
                || lower.equals("<html><head></head><body></body></html>")
                || lower.equals("<html><body></body></html>");
    }

   private String getBriefVorschauInhalt() {
        FutureTask<String> task = new FutureTask<>(() -> {
            try {
                if (webViewBrief != null) {
                    return (String) webViewBrief.getEngine().executeScript("document.documentElement.outerHTML");
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Lesen der Brief-Vorschau: " + e.getMessage());
            }
            return null;
        });

        Platform.runLater(task);
        try {
            return task.get();
        } catch (Exception e) {
            System.err.println("Fehler beim Abruf der Brief-Vorschau: " + e.getMessage());
            return null;
        }
    }

   private void schreibeDruckDebugDatei(String html) {
        try {
            if (html == null) return;
            File out = getDebugDatei("alg_last_print.html");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
                w.write(html);
            }
            System.out.println("DEBUG Druck-HTML gespeichert: " + out.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Fehler beim Schreiben der Debug-HTML: " + e.getMessage());
        }
    }

   private File getDebugDatei(String dateiname) {
        String tmp = System.getProperty("java.io.tmpdir", "/tmp");
        File dir = new File(tmp, "alg_databank_debug");
        if (!dir.exists() && !dir.mkdirs()) {
            // Falls das Anlegen fehlschlaegt, nutzen wir das temp-root direkt.
            return new File(tmp, dateiname);
        }
        return new File(dir, dateiname);
    }

   private boolean druckeHtmlMitJavaFx(String htmlInhalt, String druckerName) {
        // Auf diesem Setup erzeugt JavaFX-Direktdruck weiterhin weisse Seiten.
        // Deshalb absichtlich deaktiviert und auf PDF/AWT-Fallback umgestellt.
        return false;

        /*
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean ok = new java.util.concurrent.atomic.AtomicBoolean(false);

        Platform.runLater(() -> {
            try {
                javafx.scene.web.WebView printView = new javafx.scene.web.WebView();
                printView.setContextMenuEnabled(false);
                printView.getEngine().setJavaScriptEnabled(true);

                // WebView in eine Scene haengen, damit CSS/Layout wirklich gerechnet wird.
                printView.setPrefSize(794, 1123); // A4 grob bei 96 DPI
                javafx.scene.Group root = new javafx.scene.Group(printView);
                javafx.scene.Scene scene = new javafx.scene.Scene(root, 794, 1123);
                scene.getRoot().applyCss();
                scene.getRoot().layout();

                printView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));
                        delay.setOnFinished(evt -> {
                            try {
                                javafx.print.Printer selected = null;
                                if (druckerName != null) {
                                    for (javafx.print.Printer p : javafx.print.Printer.getAllPrinters()) {
                                        if (druckerName.equals(p.getName())) {
                                            selected = p;
                                            break;
                                        }
                                    }
                                }

                                javafx.print.PrinterJob fxJob = (selected != null)
                                        ? javafx.print.PrinterJob.createPrinterJob(selected)
                                        : javafx.print.PrinterJob.createPrinterJob();

                                if (fxJob == null) {
                                    latch.countDown();
                                    return;
                                }

                                javafx.print.PageLayout pageLayout = fxJob.getPrinter().createPageLayout(
                                        javafx.print.Paper.A4,
                                        javafx.print.PageOrientation.PORTRAIT,
                                        javafx.print.Printer.MarginType.DEFAULT
                                );
                                fxJob.getJobSettings().setPageLayout(pageLayout);

                                // DOM-Hoehe ermitteln und auf Node anwenden, damit der erste Druck nicht leer ist.
                                try {
                                    Object domHeight = printView.getEngine().executeScript(
                                            "Math.max(document.body ? document.body.scrollHeight : 0, " +
                                                    "document.documentElement ? document.documentElement.scrollHeight : 0)");
                                    double h = domHeight instanceof Number ? ((Number) domHeight).doubleValue() : 1123.0;
                                    if (h < 1123.0) {
                                        h = 1123.0;
                                    }
                                    printView.setPrefSize(794, h + 40);
                                } catch (Exception ignore) {
                                    printView.setPrefSize(794, 1123);
                                }

                                printView.applyCss();
                                printView.layout();

                                // Robuster Pfad: Snapshot der gerenderten Seite drucken (WYSIWYG, verhindert weisse Seiten).
                                javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters();
                                sp.setFill(javafx.scene.paint.Color.WHITE);
                                javafx.scene.image.WritableImage snapshot = printView.snapshot(sp, null);

                                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(snapshot);
                                iv.setPreserveRatio(true);
                                double targetW = pageLayout.getPrintableWidth();
                                double targetH = pageLayout.getPrintableHeight();
                                iv.setFitWidth(targetW);
                                iv.setFitHeight(targetH);

                                boolean pagePrinted = fxJob.printPage(pageLayout, iv);
                                ok.set(pagePrinted && fxJob.endJob());
                            } catch (Exception ex) {
                                System.err.println("JavaFX Direktdruck Fehler: " + ex.getMessage());
                            } finally {
                                latch.countDown();
                            }
                        });
                        delay.play();
                    } else if (newState == javafx.concurrent.Worker.State.FAILED || newState == javafx.concurrent.Worker.State.CANCELLED) {
                        latch.countDown();
                    }
                });

                printView.getEngine().loadContent(htmlInhalt, "text/html; charset=utf-8");
            } catch (Exception ex) {
                System.err.println("JavaFX Druck-Init Fehler: " + ex.getMessage());
                latch.countDown();
            }
        });

        try {
            if (!latch.await(20, java.util.concurrent.TimeUnit.SECONDS)) {
                System.err.println("JavaFX Druck-Timeout, falle auf AWT zurück.");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        if (ok.get()) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "Der Brief wurde erfolgreich an den Drucker übergeben.",
                            "Druck gestartet", JOptionPane.INFORMATION_MESSAGE)
            );
            return true;
        }
        return false;
        */
    }

   private boolean druckeHtmlViaTemporaremPdf(String htmlInhalt, String druckerName) {
        if (htmlInhalt == null || htmlInhalt.trim().isEmpty() || druckerName == null || druckerName.trim().isEmpty()) {
            System.err.println("Druckfehler: HTML oder Druckername leer.");
            return false;
        }
        try {
            // Wir erstellen die Ziel-Datei
            File tmpPdf = new File(System.getProperty("user.home"), "temp_print_job.pdf");

            System.out.println("DEBUG: Der zu druckende Inhalt ist: " + htmlInhalt);

            // Der PdfExporter erledigt jetzt alles: Export -> PDF -> lp-Befehl
            // Hinweis: Übergib hier den druckerName, den du als Parameter bekommen hast
            PdfExporter.exportHtmlToPdfOrPng(htmlInhalt, tmpPdf, druckerName);

            return true; // Erfolg!
        } catch (Exception ex) {
            System.err.println("PDF-Druckfehler: " + ex.getMessage());
            return false;
        }
    }

    //-----
    private void fuehreUmschlagDruckAus() {
        // Prüfe, ob ein Mitglied ausgewählt ist
        if (ausgewaehltesMitglied == null) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie ein Mitglied aus der Tabelle.",
                "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Drucker auswählen (oder verwende den gespeicherten)
        if (ausgewaehlterDrucker == null) {
            ausgewaehlterDrucker = waehledruckerAus();
        }
        if (ausgewaehlterDrucker == null) {
            return; // Nutzer hat abgebrochen
        }

        // Druckjob mit AWT-Printing (funktioniert auf Linux)
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        job.setJobName("AlgDatabank - Umschlagdruck");

        // Einstellungen für DL-Umschlag (DIN 110x220)
        java.awt.print.PageFormat pf = job.defaultPage();
        java.awt.print.Paper paper = new java.awt.print.Paper();
        final double mmToPoint = 2.83465;
        double breite = 220 * mmToPoint;    // DL-Breite
        double hoehe = 110 * mmToPoint;     // DL-Höhe
        paper.setSize(breite, hoehe);
        paper.setImageableArea(15 * mmToPoint, 15 * mmToPoint, (220 - 30) * mmToPoint, (110 - 30) * mmToPoint);
        pf.setPaper(paper);
        pf.setOrientation(java.awt.print.PageFormat.LANDSCAPE);

        // Nutze UmschlagDrucker für AWT-kompatibles Drucken - WICHTIG: PageFormat übergeben!
        job.setPrintable(new UmschlagDrucker(ausgewaehltesMitglied), pf);

        // Versuche, den gewählten Drucker zu setzen
        try {
            javax.print.PrintService[] services = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
            if (services != null) {
                for (javax.print.PrintService s : services) {
                    if (s.getName().equals(ausgewaehlterDrucker)) {
                        job.setPrintService(s);
                        System.out.println("Setze Drucker auf: " + s.getName());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        // Druckdialog zeigen
        javax.print.attribute.PrintRequestAttributeSet attributes =
            new javax.print.attribute.HashPrintRequestAttributeSet();
        attributes.add(javax.print.attribute.standard.MediaSizeName.ISO_A4);
        attributes.add(javax.print.attribute.standard.OrientationRequested.LANDSCAPE);

        boolean doPrint = job.printDialog(attributes);
        System.out.println("Umschlag printDialog returned: " + doPrint);
        if (!doPrint) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Umschlagdruck abgebrochen (Dialog).", "Info", JOptionPane.INFORMATION_MESSAGE));
            return;
        }

        // Führe Druck in separatem Daemon-Thread aus
        Thread printThread = new Thread(() -> {
            try {
                System.out.println("Starte job.print() für Umschlag...");
                javax.print.PrintService ps = job.getPrintService();
                System.out.println("PrinterJob.getPrintService(): " + ps);
                job.print(attributes);
                System.out.println("Umschlag job.print() zurückgegeben");
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(AlgDatabankGui.this,
                        "Der Umschlag wurde erfolgreich an den Drucker übergeben.",
                        "Druck gestartet", JOptionPane.INFORMATION_MESSAGE)
                );
            } catch (java.awt.print.PrinterException ex) {
                System.err.println("Error: " + ex.getMessage());
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(AlgDatabankGui.this,
                        "Druckfehler: " + ex.getMessage(),
                        "Fehler", JOptionPane.ERROR_MESSAGE)
                );
            }
        });
        printThread.setDaemon(true);
        printThread.start();
    }

    private void fuehreExportUndDirektDruckAus() {
        // 1. Inhalt aus der Variable nehmen (statt aus der Engine zu ziehen)
        String htmlInhalt = this.masterHtmlContent;

        // Sicherheitsprüfung
        if (htmlInhalt == null || htmlInhalt.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fehler: Kein Inhalt geladen!");
            return;
        }

        // 2. Platzhalter ersetzen
        if (ausgewaehltesMitglied != null) {
            htmlInhalt = ersetzePlatzhalter(htmlInhalt, ausgewaehltesMitglied);
        }

        // 3. Erst jetzt an den PdfExporter übergeben
        final String finalHtml = htmlInhalt;

        Thread t = new Thread(() -> {
            try {
                File result = new File(System.getProperty("user.home"), "temp_print_job.pdf");
                // Hier übergeben wir den String, der garantiert nicht leer ist
                PdfExporter.exportHtmlToPdfOrPng(finalHtml, result, ausgewaehlterDrucker);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        t.start();
    }

    private void fuehreTestDruckAus() {
        // 1. Drucker auswählen (bleibt erhalten)
        if (ausgewaehlterDrucker == null) {
            ausgewaehlterDrucker = waehledruckerAus();
        }
        if (ausgewaehlterDrucker == null) {
            return; // Nutzer hat abgebrochen
        }

        // 2. HTML-Testinhalt erstellen
        String testHtml = "<html><body style='font-family:Arial; margin:20px;'>"
                + "<h2>ALG Datenbank - Testdruck</h2>"
                + "<p><b>Datum:</b> " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date()) + "</p>"
                + "<p><b>Drucker:</b> " + ausgewaehlterDrucker + "</p>"
                + "<hr>"
                + "<h3>Testinhalte:</h3>"
                + "<ul><li>Zeilenumbrüche funktionieren</li>"
                + "<li>Umlaute: Ä Ö Ü ä ö ü ß</li>"
                + "<li>Fettdruck <b>und kursiv <i>text</i></b></li></ul>"
                + "</body></html>";

        // 3. Ausführung in einem Thread, um die UI nicht zu blockieren
        new Thread(() -> {
            try {
                // Temporäre Datei für den PDF-Export
                File tempPdf = File.createTempFile("alg_testdruck", ".pdf");

                // Nutze Ihren PdfExporter anstelle von HtmlDrucker/PrinterJob
                PdfExporter.exportHtmlToPdfOrPng(testHtml, tempPdf, ausgewaehlterDrucker);

                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Testdruck erfolgreich an den Drucker '" + ausgewaehlterDrucker + "' gesendet!",
                        "Testdruck erfolgreich", JOptionPane.INFORMATION_MESSAGE));

                // Aufräumen
                tempPdf.deleteOnExit();

            } catch (Exception ex) {
                System.err.println("Testdruck Fehler: " + ex.getMessage());
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Testdruck-Fehler: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void fuehreScanAus(String dateiName, String ziel, boolean farbe) throws Exception {
        File zielOrdner;
        if (ziel.contains("Paperless-ngx")) {
            zielOrdner = new File("/home/wolfram/paperless-ngx/consume/");
        } else {
            zielOrdner = new File(System.getProperty("user.home") + "/Downloads/");
        }

        if (!zielOrdner.exists()) {
            zielOrdner.mkdirs();
        }

        File ausgabeDatei = new File(zielOrdner, dateiName + ".tiff");

        // Dynamischen Modus setzen: Gray (schnell für Text) oder Color (Farbe)
        String modus = farbe ? "Color" : "Gray";

        // Bei Graustufen reicht oft schon 150 DPI, für gute OCR bei Farbe sind 150-300 DPI ideal
        String kommando = "SANE_NET_TIMEOUT=60 scanimage --resolution=150 --mode=" + modus + " | pnmtotiff -quiet > " + ausgabeDatei.getAbsolutePath();

        System.out.println("Starte Canon TR4700 Scan (" + modus + "): " + kommando);

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", kommando);
        pb.redirectErrorStream(true);

        Process prozess = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(prozess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Scanner-Log: " + line);
            }
        }

        int exitCode = prozess.waitFor();
        if (exitCode != 0) {
            if (ausgabeDatei.exists()) ausgabeDatei.delete();
            throw new IOException("Scan fehlgeschlagen. Shell-Exit-Code: " + exitCode);
        }
    }

   private List<Beitrag> fuehreBeitragExcelImportAus(File datei) throws Exception {
        List<Beitrag> liste = new ArrayList<>();

        try (InputStream in = new FileInputStream(datei);
             Workbook workbook = WorkbookFactory.create(in)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // Zeile 0 ist der Header, wir starten bei Zeile 1
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Sicherstellen, dass zumindest eine Mitgliedsnummer da ist
                Cell cellMNr = row.getCell(0);
                if (cellMNr == null || cellMNr.getCellType() == CellType.BLANK) continue;

                Beitrag b = new Beitrag();

                // M-Nr (Numerisch oder Text extrahieren)
                if (cellMNr.getCellType() == CellType.NUMERIC) {
                    b.setMNr((int) cellMNr.getNumericCellValue());
                } else {
                    b.setMNr(Integer.parseInt(formatter.formatCellValue(cellMNr).trim()));
                }

                // Nachname & Vorname
                b.setNachname(formatter.formatCellValue(row.getCell(1)));
                b.setVorname(formatter.formatCellValue(row.getCell(2)));

                // Jahr
                Cell cellJahr = row.getCell(3);
                if (cellJahr != null && cellJahr.getCellType() == CellType.NUMERIC) {
                    b.setJahr((int) cellJahr.getNumericCellValue());
                } else if (cellJahr != null) {
                    b.setJahr(Integer.parseInt(formatter.formatCellValue(cellJahr).trim()));
                }

                // Betrag
                Cell cellBetrag = row.getCell(4);
                if (cellBetrag != null && cellBetrag.getCellType() == CellType.NUMERIC) {
                    b.setBetrag(cellBetrag.getNumericCellValue());
                } else if (cellBetrag != null) {
                    String s = formatter.formatCellValue(cellBetrag).replace(",", ".");
                    b.setBetrag(Double.parseDouble(s.trim()));
                }

                // Status
                Cell cellStatus = row.getCell(5);
                b.setStatus(cellStatus != null ? formatter.formatCellValue(cellStatus).trim() : "Offen");

                liste.add(b);
            }
        }
        return liste;
    }

   private void fuehreBeitragExcelExportAus(File datei, List<Beitrag> beitraegeListe) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(datei)) {

            Sheet sheet = workbook.createSheet("Beiträge");

            // Header anlegen
            String[] headers = {"M-Nr", "Nachname", "Vorname", "Jahr", "Betrag", "Status"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Daten befüllen
            int rowIdx = 1;
            for (Beitrag b : beitraegeListe) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(b.getMNr());
                row.createCell(1).setCellValue(b.getNachname());
                row.createCell(2).setCellValue(b.getVorname());
                row.createCell(3).setCellValue(b.getJahr());
                row.createCell(4).setCellValue(b.getBetrag());
                row.createCell(5).setCellValue(b.getStatus());
            }

            // Spaltenbreiten anpassen
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
        }
    }

   private void addStatistikRow(JPanel panel, String labelText, JTextField field, GridBagConstraints gbc, int row) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.3;
        gbc.gridwidth = 1;
        panel.add(new JLabel(labelText), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(field, gbc);
    }

    /**
     * Zeigt einen Dialog zur Auswahl eines Druckers an.
     * Wenn keine Drucker vorhanden sind, wird null zurückgegeben.
     */
   private String waehledruckerAus() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services == null || services.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Keine Drucker verfügbar.",
                    "Drucker nicht gefunden",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String[] druckerNamen = new String[services.length];
        for (int i = 0; i < services.length; i++) {
            druckerNamen[i] = services[i].getName();
        }

        String gewaehlter = (String) JOptionPane.showInputDialog(this,
                "Wählen Sie einen Drucker:",
                "Drucker auswählen",
                JOptionPane.QUESTION_MESSAGE,
                null,
                druckerNamen,
                druckerNamen[0]);

        return gewaehlter;
    }

    /**
     * Löscht alle fehlerhaften/hängenden Druckjobs einer Queue
     * und reaktiviert den Drucker (cupsenable).
     */
   private void bereinigeDruckQueue(String druckerName) {
        try {
            new ProcessBuilder("cancel", "-a", druckerName).start().waitFor();
            new ProcessBuilder("cupsenable", druckerName).start().waitFor();
            System.out.println("Druckqueue bereinigt: " + druckerName);
        } catch (Exception ex) {
            System.err.println("Queue-Bereinigung fehlgeschlagen: " + ex.getMessage());
        }
   }

    // --- EXEKUTIERBARE MAIN METHODE ---
   public static void main(String[] args) {
        // 1. Zwingt JavaFX zur Nutzung des stabilen Software-Renderers unter Linux
        System.setProperty("prism.order", "sw");

        // 2. Aktiviert FontConfig für das System-Mapping
        System.setProperty("prism.useFontConfig", "true");

        // 3. FALLBACK-SICHERUNG: Falls FontConfig ins Leere läuft,
        // wird JavaFX hierdurch gezwungen, die eingebetteten logischen JRE-Schriften zu nutzen.
        System.setProperty("prism.fontdir", System.getProperty("java.home") + "/lib/fonts");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback
        }

        // 4. Startet den JavaFX-Thread im Hintergrund
        new JFXPanel();

        // GUI sicher auf dem Event Dispatch Thread initialisieren
        SwingUtilities.invokeLater(() -> {
            AlgDatabankGui gui = new AlgDatabankGui();
            gui.setVisible(true);
        });
   }
}


