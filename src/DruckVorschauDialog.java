import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;

public class DruckVorschauDialog extends JDialog {

    private SerienBriefDrucker drucker;
    private int gesamtSeiten;
    private int aktuelleSeite = 0;

    private JPanel blattPanel;
    private JLabel lblSeitenAnzeige;
    private JButton btnZurueck, btnWeiter;

    public DruckVorschauDialog(JFrame parent, SerienBriefDrucker drucker, int gesamtSeiten) {
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

        // Mitte: Das weiße Briefpapier-Panel (DIN A4 skaliert)
        blattPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Antialiasing für schöne Schrift in der Vorschau aktivieren
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Weißer Hintergrund für das "Papier"
                g2d.setColor(Color.WHITE);
                g2d.fillRect(10, 10, getWidth() - 20, getHeight() - 20);

                // Grauer Schatten/Rahmen um das Papier
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawRect(10, 10, getWidth() - 20, getHeight() - 20);

                // Standard DIN A4 Format für den Drucker simulieren
                PageFormat pf = new PageFormat();
                Paper paper = new Paper();
                paper.setSize(595, 842); // A4 Punkte
                paper.setImageableArea(0, 0, 595, 842);
                pf.setPaper(paper);

                try {
                    // Wir rufen direkt die print()-Methode deines Seriendruckers auf,
                    // leiten das Zeichnen aber auf unser GUI-Panel um!
                    g2d.translate(10, 10); // Abstand zum Fensterrand
                    drucker.print(g2d, pf, aktuelleSeite);
                } catch (Exception ex) {
                    g2d.drawString("Fehler bei der Vorschau: " + ex.getMessage(), 30, 30);
                }
            }
        };
        blattPanel.setBackground(new Color(240, 240, 240)); // Grauer Hintergrund außerhalb des Blattes
        add(blattPanel, BorderLayout.CENTER);

        // Navigation-Logik
        btnZurueck.setEnabled(false);
        if (gesamtSeiten <= 1) btnWeiter.setEnabled(false);

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
    }

    private void aktualisiereAnsicht() {
        lblSeitenAnzeige.setText("Brief " + (aktuelleSeite + 1) + " von " + gesamtSeiten);
        btnZurueck.setEnabled(aktuelleSeite > 0);
        btnWeiter.setEnabled(aktuelleSeite < gesamtSeiten - 1);
        blattPanel.repaint(); // Erzwingt das Neuzeichnen des Briefes
    }
}