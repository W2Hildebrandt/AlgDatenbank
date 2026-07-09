import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;

public class DruckVorschauDialog1 extends JDialog {

    private SerienBriefDrucker drucker;
    private int gesamtSeiten;
    private int aktuelleSeite = 0;

    private JPanel blattPanel;
    private JLabel lblSeitenAnzeige;
    private JButton btnZurueck, btnWeiter;

    // 1. Das Logo sauber als Klassenvariable hinterlegen (Pfad ggf. anpassen)
    private Image logo = new ImageIcon("logos/alg-logo.png").getImage();

    public DruckVorschauDialog1(JFrame parent, SerienBriefDrucker drucker, int gesamtSeiten) {
        super(parent, "Druckvorschau - Serienbrief", true);
        this.drucker = drucker;
        this.gesamtSeiten = gesamtSeiten;

        setSize(650, 850);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // Oben: Navigationsleiste
        JPanel naviPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnZurueck = new JButton("<- Vorheriger Brief");
        btnWeiter = new JButton("Nächster Brief ->");
        lblSeitenAnzeige = new JLabel("Brief 1 von " + gesamtSeiten);
        lblSeitenAnzeige.setFont(new Font("Arial", Font.BOLD, 12));

        naviPanel.add(btnZurueck);
        naviPanel.add(lblSeitenAnzeige);
        naviPanel.add(btnWeiter);
        add(naviPanel, BorderLayout.NORTH);

        // Mitte: Das weiße Briefpapier-Panel (Reines Zeichnen, keine Logik hier!)
        blattPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Antialiasing für schöne Schrift in der Vorschau aktivieren
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // 1. ZUERST: Weißer Hintergrund für das "Papier" zeichnen
                g2d.setColor(Color.WHITE);
                g2d.fillRect(10, 10, getWidth() - 20, getHeight() - 20);

                // 2. Grauer Schatten/Rahmen um das Papier
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawRect(10, 10, getWidth() - 20, getHeight() - 20);

                // 3. Erst jetzt das Logo AUF das weiße Papier zeichnen
                if (logo != null) {
                    // Composite für Transparenz sichern, um den Text nicht permanent zu beeinflussen
                    java.awt.Composite originalComposite = g2d.getComposite();

                    g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.3f));
                    int x = getWidth() - logo.getWidth(this) - 30; // 30px Abstand von rechts wegen Papierrand
                    int y = 20; // 20px von oben
                    g2d.drawImage(logo, x, y, this);

                    // Composite wieder zurücksetzen für den nachfolgenden Textdruck
                    g2d.setComposite(originalComposite);
                }

                // Standard DIN A4 Format für den Drucker simulieren
                PageFormat pf = new PageFormat();
                Paper paper = new Paper();
                paper.setSize(595, 842); // A4 Punkte
                paper.setImageableArea(0, 0, 595, 842);
                pf.setPaper(paper);

                try {
                    // Inhalt des Briefes zeichnen
                    g2d.translate(20, 20); // Margins / Randabstand für den Textfluss
                    drucker.print(g2d, pf, aktuelleSeite);
                } catch (Exception ex) {
                    g2d.setColor(Color.RED);
                    g2d.drawString("Fehler bei der Vorschau: " + ex.getMessage(), 30, 30);
                }
            }
        };

        // Hintergrund außerhalb des Blattes auf grau setzen
        blattPanel.setBackground(new Color(240, 240, 240));
        add(blattPanel, BorderLayout.CENTER);

        // --- HIER GEHÖRT DIE LOGIK HIN (Im Konstruktor) ---
        // Startzustand der Buttons ermitteln
        btnZurueck.setEnabled(false);
        if (gesamtSeiten <= 1) {
            btnWeiter.setEnabled(false);
        }

        // Action-Listener einmalig registrieren
        btnZurueck.addActionListener(e -> {
            if (aktuelleSeite > 0) {
                aktuelleSeite--;
                aktualisiereAnsicht();
            }
        });

        btnWeiter.addActionListener(e -> {
            if (aktuelleSeite < gesamtSeiten - 1) {
                aktuelleSeite++;
                aktualisiereAnsicht();
            }
        });
    } // <-- Ende des Konstruktors

    private void aktualisiereAnsicht() {
        lblSeitenAnzeige.setText("Brief " + (aktuelleSeite + 1) + " von " + gesamtSeiten);
        btnZurueck.setEnabled(aktuelleSeite > 0);
        btnWeiter.setEnabled(aktuelleSeite < gesamtSeiten - 1);
        blattPanel.repaint(); // Erzwingt das saubere Neuzeichnen des aktuellen Briefes
    }
}