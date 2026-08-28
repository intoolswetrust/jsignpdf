package net.sf.jsignpdf.preview;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.AppConfig;
import net.sf.jsignpdf.utils.PdfUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;
import org.openpdf.renderer.PDFFile;
import org.openpdf.renderer.PDFPage;
import org.openpdf.renderer.PDFParseException;
import org.openpdf.renderer.decrypt.PDFPassword;
import org.openpdf.text.pdf.PdfReader;

/** Converts PDF pages to images for the JavaFX preview. */
public class Pdf2Image {
    private static final int JPEDAL_MAX_IMAGE_RENDER_SIZE = 2000 * 2000;

    private final BasicSignerOptions options;
    private double lastRenderDpi = PreviewRenderSettings.PDF_POINTS_PER_INCH;

    public Pdf2Image(BasicSignerOptions options) {
        if (options == null) {
            throw new NullPointerException("Options have to be not-null");
        }
        this.options = options;
    }

    /** DPI actually used for the most recent successful render (may be below the target for large pages). */
    public double getLastRenderDpi() {
        return lastRenderDpi;
    }

    /**
     * Renders the page with the libraries named in {@code pdf2image.libraries}, in the configured
     * order, returning the first one that succeeds. The bundled default lists {@code pdfbox} first
     * because placement accuracy depends on preserving page geometry; the remaining entries act as
     * fallbacks.
     */
    public BufferedImage getImageForPage(final int page) {
        for (String library : AppConfig.pdf2imageLibraries().split("\\s*,\\s*")) {
            BufferedImage image = switch (library) {
                case Constants.PDF2IMAGE_PDFBOX -> getImageUsingPdfBox(page);
                case Constants.PDF2IMAGE_JPEDAL -> getImageUsingJPedal(page);
                case Constants.PDF2IMAGE_OPENPDF -> getImageUsingOpenPdfRenderer(page);
                default -> {
                    Constants.LOGGER.fine("Unknown pdf2image library: " + library);
                    yield null;
                }
            };
            if (image != null) {
                return image;
            }
        }
        return null;
    }

    public BufferedImage getImageUsingJPedal(final int page) {
        BufferedImage result = null;
        PdfReader reader = null;
        PdfDecoder decoder = null;
        try {
            reader = PdfUtils.getPdfReader(options.getInFile(), options.getPdfOwnerPwdStrX().getBytes());
            if (JPEDAL_MAX_IMAGE_RENDER_SIZE > reader.getPageSize(page).getWidth() * reader.getPageSize(page).getHeight()) {
                decoder = new PdfDecoder();
                try {
                    decoder.openPdfFile(options.getInFile(), options.getPdfOwnerPwdStrX());
                } catch (PdfException e) {
                    try {
                        decoder.openPdfFile(options.getInFile(), "");
                    } catch (PdfException e1) {
                        decoder.openPdfFile(options.getInFile());
                    }
                }
                double dpi = PreviewRenderSettings.effectiveRenderDpi(
                        reader.getPageSize(page).getWidth(), reader.getPageSize(page).getHeight());
                lastRenderDpi = dpi;
                decoder.setPageParameters((float) (dpi / PreviewRenderSettings.PDF_POINTS_PER_INCH), page);
                result = decoder.getPageAsImage(page);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) reader.close();
            if (decoder != null) decoder.closePdfFile();
        }
        return result;
    }

    public BufferedImage getImageUsingOpenPdfRenderer(final int pageNumber) {
        BufferedImage result = null;
        RandomAccessFile raf = null;
        try {
            File file = new File(options.getInFile());
            raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            PDFFile pdfFile;
            try {
                pdfFile = new PDFFile(buffer, new PDFPassword(options.getPdfOwnerPwdStrX()));
            } catch (PDFParseException e) {
                try {
                    pdfFile = new PDFFile(buffer, new PDFPassword(""));
                } catch (PDFParseException e2) {
                    pdfFile = new PDFFile(buffer);
                }
            }
            PDFPage page = pdfFile.getPage(pageNumber);
            Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
            double dpi = PreviewRenderSettings.effectiveRenderDpi(rect.width, rect.height);
            lastRenderDpi = dpi;
            double scale = dpi / PreviewRenderSettings.PDF_POINTS_PER_INCH;
            int imgWidth = (int) Math.round(rect.width * scale);
            int imgHeight = (int) Math.round(rect.height * scale);
            result = (BufferedImage) page.getImage(imgWidth, imgHeight, rect, null, true, true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try { raf.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
        return result;
    }

    public BufferedImage getImageUsingPdfBox(final int page) {
        try (PDDocument document = Loader.loadPDF(new File(options.getInFile()), options.getPdfOwnerPwdStrX())) {
            var mediaBox = document.getPage(page - 1).getMediaBox();
            double dpi = PreviewRenderSettings.effectiveRenderDpi(mediaBox.getWidth(), mediaBox.getHeight());
            lastRenderDpi = dpi;
            PDFRenderer renderer = new PDFRenderer(document);
            return renderer.renderImageWithDPI(page - 1, (float) dpi);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
