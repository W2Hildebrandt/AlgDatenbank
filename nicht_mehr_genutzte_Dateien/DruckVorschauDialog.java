import javax.swing.*;
import java.awt.*;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;

/**
 * Ein modaler Vorschau-Dialog für die Vereinsbriefe der AlgDatabank.
 * Behebt das Problem der "weißen Fläche" durch verzögertes Rendering.
 */
public class DruckVorschauDialog extends JDialog {

    private JFXPanel fxPanel;
    private WebView webView;
    private String htmlInhalt;

    public DruckVorschauDialog(Frame owner, String titel, String htmlInhalt) {
        super(owner, titel, true);
        this.htmlInhalt = htmlInhalt;

        setSize(900, 850);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // 1. JavaFX-Panel initialisieren
        fxPanel = new JFXPanel();
        add(fxPanel, BorderLayout.CENTER);

        // 2. Buttons unten
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton btnDrucken = new JButton("Direkt Drucken");
        btnDrucken.setFont(new Font("Arial", Font.BOLD, 12));
        JButton btnSchliessen = new JButton("Schließen");
        buttonPanel.add(btnDrucken);
        buttonPanel.add(btnSchliessen);
        add(buttonPanel, BorderLayout.SOUTH);

        // 3. FX-Threadsicherheit & Load-Sicherung
        Platform.runLater(() -> {
            webView = new WebView();
            fxPanel.setScene(new Scene(webView));

            // Ermittelt den Ressourcen-Pfad für das Logo
            java.net.URL resUrl = getClass().getResource("/alg_logo.png");
            String baseUrl = (resUrl != null) ? resUrl.toExternalForm().replaceAll("alg_logo.png$", "") : null;

            // Sicherer Ladevorgang: Erzwinge das Laden im korrekten Zustand
            webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.FAILED) {
                    System.err.println("WebView konnte den Inhalt nicht rendern.");
                }
            });

            // Inhalt laden
            webView.getEngine().loadContent(htmlInhalt, "text/html; charset=utf-8");
        });

        // 4. Swing Actions
        btnSchliessen.addActionListener(e -> dispose());

        btnDrucken.addActionListener(e -> {
            Platform.runLater(() -> {
                javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
                if (job != null && job.showPrintDialog(webView.getScene().getWindow())) {
                    webView.getEngine().print(job);
                    job.endJob();
                    SwingUtilities.invokeLater(() -> dispose());
                }
            });
        });
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        // Falls die Engine beim Öffnen asynchron hängen bleibt,
        // stößt dies das Neuzeichnen der Swing/FX-Schnittstelle an.
        if (b && fxPanel != null) {
            Platform.runLater(() -> {
                if (webView != null && htmlInhalt != null) {
                    webView.getEngine().loadContent(htmlInhalt, "text/html; charset=utf-8");
                }
            });
        }
    }
}