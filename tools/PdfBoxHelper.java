import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
public class PdfBoxHelper {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: PdfBoxHelper <png> <pdf>");
            System.exit(2);
        }
        File in = new File(args[0]);
        File out = new File(args[1]);
        BufferedImage img = ImageIO.read(in);
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(new PDRectangle(img.getWidth(), img.getHeight()));
        doc.addPage(page);
        org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage = LosslessFactory.createFromImage(doc, img);
        PDPageContentStream contents = new PDPageContentStream(doc, page);
        contents.drawImage(pdImage, 0, 0, img.getWidth(), img.getHeight());
        contents.close();
        doc.save(out.getAbsolutePath());
        doc.close();
    }
}
