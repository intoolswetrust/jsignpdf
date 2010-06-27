package net.sf.jsignpdf;

import net.sf.jsignpdf.types.FloatPoint;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;

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
	 * Returns number of pages in PDF document. If error occures (file not found
	 * or sth. similar) -1 is returned.
	 * 
	 * @return number of pages (or -1 if error occures)
	 */
	public int getNumberOfPages() {
		int tmpResult = 0;
		PdfReader reader = null;
		try {
			try {
				// try to read without password
				reader = new PdfReader(options.getInFile());
			} catch (Exception e) {
				try {
					reader = new PdfReader(options.getInFile(), new byte[0]);
				} catch (Exception e2) {
					reader = new PdfReader(options.getInFile(), options.getPdfOwnerPwdStr().getBytes());
				}
			}
			tmpResult = reader.getNumberOfPages();
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
	 * Returns page size identified by upper rigth corner position. If no error
	 * occures FloatPoint with x as a width and y as a height is returned, null
	 * otherwise.
	 * 
	 * @param aPage
	 *            number of page for which size should be returned
	 * @return FloatPoint or null
	 */
	public FloatPoint getPageSize(int aPage) {
		FloatPoint tmpResult = null;
		PdfReader reader = null;
		try {
			try {
				// try to read without password
				reader = new PdfReader(options.getInFile());
			} catch (Exception e) {
				try {
					reader = new PdfReader(options.getInFile(), new byte[0]);
				} catch (Exception e2) {
					reader = new PdfReader(options.getInFile(), options.getPdfOwnerPwdStr().getBytes());
				}
			}
			final Rectangle tmpRect = reader.getPageSize(aPage);
			if (tmpRect != null) {
				tmpResult = new FloatPoint(tmpRect.getRight(), tmpRect.getTop());
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
