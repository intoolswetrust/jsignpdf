package net.sf.jsignpdf;

import net.sf.jsignpdf.types.PageInfo;
import net.sf.jsignpdf.utils.PdfUtils;

import org.openpdf.text.Rectangle;
import org.openpdf.text.exceptions.BadPasswordException;
import org.openpdf.text.pdf.PdfReader;

/**
 * Provides additional information for selected input PDF file.
 * 
 * @author Josef Cacek
 */
public class PdfExtraInfo {

    private BasicSignerOptions options;

    /**
     * @param anOptions
     */
    public PdfExtraInfo(BasicSignerOptions anOptions) {
        options = anOptions;
    }

    /**
     * Returns number of pages in PDF document. If error occures (file not found or sth. similar) -1 is returned.
     *
     * @return number of pages (or -1 if error occures)
     * @throws BadPasswordException if the PDF is password-protected and the configured password is wrong or missing
     */
    public int getNumberOfPages() throws BadPasswordException {
        int tmpResult = 0;
        PdfReader reader = null;
        try {
            try {
                reader = new PdfReader(options.getInFile(), options.getPdfOwnerPwdStrX().getBytes());
            } catch (BadPasswordException e) {
                try {
                    reader = new PdfReader(options.getInFile(), new byte[0]);
                } catch (Exception e2) {
                    reader = new PdfReader(options.getInFile());
                }
            } catch (Exception e) {
                try {
                    reader = new PdfReader(options.getInFile(), new byte[0]);
                } catch (Exception e2) {
                    // try to read without password
                    reader = new PdfReader(options.getInFile());
                }
            }
            tmpResult = reader.getNumberOfPages();
        } catch (BadPasswordException e) {
            throw e;
        } catch (Exception e) {
            tmpResult = -1;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }

        return tmpResult;
    }

    /**
     * Returns page info.
     * 
     * @param aPage number of page for which size should be returned
     * @return FloatPoint or null
     */
    public PageInfo getPageInfo(int aPage) {
        PageInfo tmpResult = null;
        PdfReader reader = null;
        try {
            reader = PdfUtils.getPdfReader(options.getInFile(), options.getPdfOwnerPwdStrX().getBytes());
            final Rectangle tmpRect = reader.getPageSizeWithRotation(aPage);
            if (tmpRect != null) {
                tmpResult = new PageInfo(tmpRect.getRight(), tmpRect.getTop());
            }
        } catch (Exception e) {
            // nothing to do
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }

        return tmpResult;
    }
}
