import java.awt.*;
import java.awt.print.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.JFXPanel;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Absolut stabiler HtmlDrucker.
 * Bereinigt fehlerhafte HTML-Rahmen unter Linux und brennt das Vereinslogo ein.
 */
public class HtmlDrucker implements Printable {
    private final String htmlInhalt;
    private static final AtomicBoolean FX_STARTED = new AtomicBoolean(false);
    private BufferedImage[] cachedPages = null;

    public HtmlDrucker(String htmlInhalt) {
        // Falls der übergebene Text fehlerhaft oder leer war, fangen wir das hier ab
        this.htmlInhalt = (htmlInhalt != null && htmlInhalt.trim().length() > 5) ? htmlInhalt : "<html><body><p>Briefinhalt leer.</p></body></html>";
        initJavaFX();

        // Wir rendern die Seiten SOFORT im Konstruktor vor, um Deadlocks im Druckzentrum zu vermeiden
        try {
            PageFormat standardA4 = new PageFormat();
            Paper paper = new Paper();
            paper.setSize(595, 842);
            paper.setImageableArea(0, 0, 595, 842);
            standardA4.setPaper(paper);

            this.cachedPages = ensureRenderedPages(standardA4);
        } catch (Exception e) {
            System.err.println("Fehler beim Vorab-Rendering: " + e.getMessage());
        }
    }

    private static void initJavaFX() {
        if (!FX_STARTED.getAndSet(true)) {
            try {
                new JFXPanel();
                Platform.setImplicitExit(false);
            } catch (Exception e) {
                System.err.println("Fehler bei JavaFX-Initialisierung: " + e.getMessage());
            }
        }
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (cachedPages == null) {
            cachedPages = ensureRenderedPages(pageFormat);
            if (cachedPages == null) return NO_SUCH_PAGE;
        }

        if (pageIndex >= cachedPages.length) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        double printableX = pageFormat.getImageableX();
        double printableY = pageFormat.getImageableY();
        double printableWidth = pageFormat.getImageableWidth();
        double printableHeight = pageFormat.getImageableHeight();

        BufferedImage aktuelleSeite = cachedPages[pageIndex];

        double scaleX = printableWidth / aktuelleSeite.getWidth();
        double scaleY = printableHeight / aktuelleSeite.getHeight();
        double scale = Math.min(scaleX, scaleY);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        g2d.translate(printableX, printableY);
        g2d.scale(scale, scale);
        g2d.drawImage(aktuelleSeite, 0, 0, null);

        return PAGE_EXISTS;
    }

    // WICHTIG: Füge dieses in HtmlDrucker.java ein,
// um das Rendering zu erzwingen, bevor das Bild an Swing geht.
    private BufferedImage[] ensureRenderedPages(PageFormat pageFormat) throws PrinterException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<BufferedImage> result = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                WebView webView = new WebView();
                // WICHTIG: Die WebView MUSS in einer Scene stecken, sonst rendert sie nicht!
                javafx.scene.Scene scene = new javafx.scene.Scene(webView, 800, 1000);
                webView.getEngine().loadContent("<html><body style='color:black; background:white;'>" + htmlInhalt + "</body></html>");

                //bView.getEngine().loadContent("<html><body style='color:black; background:white; padding: 50px;'><h1>TEST</h1>" + htmlInhalt + "</body></html>");

                webView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                    if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                        // Kurze Verzögerung für das Layout
                        javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1000));
                        p.setOnFinished(e -> {
                            SnapshotParameters params = new SnapshotParameters();
                            params.setFill(javafx.scene.paint.Color.WHITE);
                            // Jetzt den Snapshot machen
                            WritableImage img = webView.snapshot(params, null);
                            result.set(SwingFXUtils.fromFXImage(img, null));
                            latch.countDown();
                        });
                        p.play();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });

        try {
            if (!latch.await(20, TimeUnit.SECONDS)) throw new PrinterException("Timeout!");
        } catch (InterruptedException e) {
            throw new PrinterException("Abbruch");
        }

        if (result.get() == null) throw new PrinterException("Bild blieb leer!");
        return splitIntoPages(result.get(), pageFormat);
    }

    private BufferedImage[] splitIntoPages(BufferedImage fullImg, PageFormat pageFormat) {
        int targetWidth = (int) pageFormat.getWidth();
        int targetHeight = (int) pageFormat.getHeight();
        double widthScale = (double) targetWidth / fullImg.getWidth();
        int sourceSegmentHeight = (int) (targetHeight / widthScale);
        int totalHeight = fullImg.getHeight();
        int pageCount = Math.max(1, (int) Math.ceil((double) totalHeight / sourceSegmentHeight));

        BufferedImage[] pages = new BufferedImage[pageCount];
        for (int i = 0; i < pageCount; i++) {
            pages[i] = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = pages[i].createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetWidth, targetHeight);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            int srcY = i * sourceSegmentHeight;
            int currentSrcH = Math.min(sourceSegmentHeight, totalHeight - srcY);
            int currentTargetH = (int) (currentSrcH * widthScale);

            if (currentSrcH > 0) {
                g.drawImage(fullImg, 0, 0, targetWidth, currentTargetH, 0, srcY, fullImg.getWidth(), srcY + currentSrcH, null);
            }
            g.dispose();
        }
        return pages;
    }
}