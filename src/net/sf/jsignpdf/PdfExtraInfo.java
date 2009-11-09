package net.sf.jsignpdf;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;

/**
 * Provides additional information for selected input PDF file.
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
	 * Returns number of pages in PDF document. If error occures (file not found or sth. similar)
	 * -1 is returned.
	 * @return number of pages (or -1 if error occures)
	 */
	public int getNumberOfPages() {
		int tmpResult = 0;
		PdfReader reader = null;
		try {
			try {
				//try to read without password
				reader = new PdfReader(options.getInFile());
			} catch (Exception e) {
				try {
					reader = new PdfReader(options.getInFile(),
							new byte[0]);
				} catch (Exception e2) {
					reader = new PdfReader(options.getInFile(),
							options.getPdfOwnerPwdStr().getBytes());
				}
			}
			tmpResult = reader.getNumberOfPages();
		} catch (Exception e) {
			tmpResult = -1;
		} finally {
			if (reader!=null) {
				try {
					reader.close();
				} catch (Exception e) {}
			}
		}

		return tmpResult;
	}

	/**
	 * Returns page size identified by upper rigth corner position.
	 * If no error occures float array of size 2 is returned with page hight (top)
	 * and width (right) in this order, otherwise null.
	 * @param aPage number of page for which size should be returned
	 * @return float[2] or null
	 */
	public float[] getUpperRightCorner(int aPage) {
		float tmpResult[] = null;
		PdfReader reader = null;
		try {
			try {
				//try to read without password
				reader = new PdfReader(options.getInFile());
			} catch (Exception e) {
				try {
					reader = new PdfReader(options.getInFile(),
							new byte[0]);
				} catch (Exception e2) {
					reader = new PdfReader(options.getInFile(),
							options.getPdfOwnerPwdStr().getBytes());
				}
			}
			final Rectangle tmpRect = reader.getPageSize(aPage);
			if (tmpRect != null) {
				tmpResult = new float[] {tmpRect.getTop(), tmpRect.getRight()};
			}
		} catch (Exception e) {
			// nothing to do
		} finally {
			if (reader!=null) {
				try {
					reader.close();
				} catch (Exception e) {}
			}
		}

		return tmpResult;
	}
}
