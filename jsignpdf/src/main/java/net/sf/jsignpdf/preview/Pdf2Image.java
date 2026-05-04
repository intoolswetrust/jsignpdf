/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is 'JSignPdf, a free application for PDF signing'.
 *
 * The Initial Developer of the Original Code is Josef Cacek.
 * Portions created by Josef Cacek are Copyright (C) Josef Cacek. All Rights Reserved.
 *
 * Contributor(s): Josef Cacek.
 *
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU Lesser General Public License, version 2.1 (the  "LGPL License"), in which case the
 * provisions of LGPL License are applicable instead of those
 * above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the LGPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the LGPL License.
 */
package net.sf.jsignpdf.preview;

import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Toolkit;
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
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;

import org.openpdf.renderer.PDFFile;
import org.openpdf.renderer.PDFPage;
import org.openpdf.renderer.PDFParseException;
import org.openpdf.renderer.decrypt.PDFPassword;
import org.openpdf.text.pdf.PdfReader;

/**
 * Helper class for converting a page in PDF to a {@link BufferedImage} object.
 *
 * @author Josef Cacek
 */
public class Pdf2Image {

    private static final int JPEDAL_MAX_IMAGE_RENDER_SIZE = 2000 * 2000;

    private BasicSignerOptions options;

    /**
     * Constructor - gets an options object with configured input PDF and possibly decoding (owner) password.
     *
     * @param anOpts
     */
    public Pdf2Image(BasicSignerOptions anOpts) {
        if (anOpts == null)
            throw new NullPointerException("Options have to be not-null");
        options = anOpts;
    }

    /**
     * Returns an image preview of given page.
     *
     * @param aPage Page to preview (counted from 1)
     * @return image or null if error occures.
     */
    public BufferedImage getImageForPage(final int aPage) {
        BufferedImage tmpResult = null;
        for (String libname : AppConfig.pdf2imageLibraries().split("\\s*,\\s*")) {
            tmpResult = switch (libname) {
                case Constants.PDF2IMAGE_JPEDAL -> getImageUsingJPedal(aPage);
                case Constants.PDF2IMAGE_PDFBOX -> getImageUsingPdfBox(aPage);
                case Constants.PDF2IMAGE_OPENPDF -> getImageUsingOpenPdfRenderer(aPage);
                default -> {
                    Constants.LOGGER.fine("Unknown pdf2image library: " + libname);
                    yield null;
                }
            };
            if (tmpResult != null)
                break;
        }
        return tmpResult;
    }

    /**
     * Returns image (or null if failed) generated from given page in PDF using JPedal LGPL.
     *
     * @param aPage page in PDF (1 based)
     * @return image or null
     */
    public BufferedImage getImageUsingJPedal(final int aPage) {
        BufferedImage tmpResult = null;
        PdfReader reader = null;
        PdfDecoder pdfDecoder = null;
        try {

            reader = PdfUtils.getPdfReader(options.getInFile(), options.getPdfOwnerPwdStrX().getBytes());
            if (JPEDAL_MAX_IMAGE_RENDER_SIZE > reader.getPageSize(aPage).getWidth() * reader.getPageSize(aPage).getHeight()) {
                pdfDecoder = new PdfDecoder();
                try {
                    pdfDecoder.openPdfFile(options.getInFile(), options.getPdfOwnerPwdStrX());
                } catch (PdfException e) {
                    try {
                        // try to read PDF with empty password
                        pdfDecoder.openPdfFile(options.getInFile(), "");
                    } catch (PdfException e1) {
                        // try to read PDF without password
                        pdfDecoder.openPdfFile(options.getInFile());
                    }
                }
                tmpResult = pdfDecoder.getPageAsImage(aPage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (pdfDecoder != null) {
                pdfDecoder.closePdfFile();
            }
        }
        return tmpResult;
    }

    /**
     * Returns image (or null if failed) generated from given page in PDF using the OpenPDF renderer
     * (actively-maintained descendant of the Sun Labs PDFRenderer).
     *
     * @param aPage page in PDF (1 based)
     * @return image or null
     */
    public BufferedImage getImageUsingOpenPdfRenderer(final int aPage) {
        BufferedImage tmpResult = null;
        RandomAccessFile raf = null;
        try {
            // load a pdf from a byte buffer
            File file = new File(options.getInFile());
            raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            PDFFile pdffile = null;
            try {
                // try to read PDF with owner password
                pdffile = new PDFFile(buf, new PDFPassword(options.getPdfOwnerPwdStrX()));
            } catch (PDFParseException ppe) {
                try {
                    // try to read PDF with empty password
                    pdffile = new PDFFile(buf, new PDFPassword(""));
                } catch (PDFParseException ppe2) {
                    // try to read PDF without password
                    pdffile = new PDFFile(buf);
                }
            }

            // draw the page to an image
            PDFPage page = pdffile.getPage(aPage);

            // get the width and height for the doc at the default zoom
            Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());

            // generate the image
            tmpResult = (BufferedImage) page.getImage(rect.width, rect.height, rect, // clip
                    // rect
                    null, // null for the ImageObserver
                    true, // fill background with white
                    true // block until drawing is done
            );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return tmpResult;
    }

    /**
     * Returns image (or null if failed) generated from given page in PDF using PDFBox tool.
     *
     * @param aPage page in PDF (1 based)
     * @return image or null
     */
    public BufferedImage getImageUsingPdfBox(final int aPage) {
        BufferedImage tmpResult = null;
        PDDocument tmpDoc = null;

        try {
            File tmpFile = new File(options.getInFile());
            tmpDoc = Loader.loadPDF(tmpFile, options.getPdfOwnerPwdStrX());
            int resolution;
            try {
                resolution = Toolkit.getDefaultToolkit().getScreenResolution();
            } catch (HeadlessException e) {
                resolution = 96;
            }

            PDFRenderer rendedrer = new PDFRenderer(tmpDoc);
            tmpResult = rendedrer.renderImageWithDPI(aPage - 1, resolution);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tmpDoc != null) {
                try {
                    tmpDoc.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return tmpResult;
    }
}
