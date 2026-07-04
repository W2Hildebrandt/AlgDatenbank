import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
public class test_html_render {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JEditorPane pane = new JEditorPane();
            pane.setContentType("text/html; charset=UTF-8");
            pane.setBackground(Color.WHITE);
            pane.setForeground(Color.BLACK);
            pane.setOpaque(true);
            String html = "<html><body style='background:white;color:black;'>" +
                    "<h2>Test</h2>" +
                    "<p>Dies ist ein Test</p>" +
                    "</body></html>";
            pane.setText(html);
            pane.setSize(800, 600);
            pane.validate();
            BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 800, 600);
            pane.paint(g);
            g.dispose();
            try {
                ImageIO.write(img, "png", new File("/tmp/test_render.png"));
                System.out.println("Test PNG geschrieben");
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.exit(0);
        });
    }
}
