import java.awt.print.*;
public class TestHtmlDrucker {
    public static void main(String[] args) throws Exception {
        String html = "<html><body><h2>Test ALG</h2>" +
                     "<p>Dies ist ein <b>Test</b> mit Umlauten: Ä Ö Ü ä ö ü ß</p>" +
                     "<p>Zweiter Absatz</p>" +
                     "</body></html>";
        HtmlDrucker drucker = new HtmlDrucker(html);
        PageFormat pf = new PageFormat();
        Paper paper = new Paper();
        final double mmToPoint = 2.83465;
        double w = 210 * mmToPoint;
        double h = 297 * mmToPoint;
        paper.setSize(w, h);
        paper.setImageableArea(10 * mmToPoint, 10 * mmToPoint, (210-20) * mmToPoint, (297-20) * mmToPoint);
        pf.setPaper(paper);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
            (int)w, (int)h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, (int)w, (int)h);
        drucker.print(g, pf, 0);
        g.dispose();
        System.out.println("Test erfolgreich - Vorschau unter /tmp/alg_brief_preview.png");
    }
}
