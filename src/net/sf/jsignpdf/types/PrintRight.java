package net.sf.jsignpdf.types;

import net.sf.jsignpdf.utils.ResourceProvider;

import com.lowagie.text.pdf.PdfWriter;

/**
 * Enum of possible printing rights
 * @author Josef Cacek
 */
public enum PrintRight {

	DISALLOW_PRINTING ("rights.disallowPrinting", 0),
	ALLOW_DEGRADED_PRINTING ("rights.allowDegradedPrinting", PdfWriter.ALLOW_DEGRADED_PRINTING),
	ALLOW_PRINTING ("rights.allowPrinting", PdfWriter.ALLOW_PRINTING);

	private String msgKey;
	private int right;

	PrintRight(final String aMsgKey, final int aLevel) {
		msgKey = aMsgKey;
		right = aLevel;
	}

	/**
	 * Returns internationalized description of a right.
	 */
	public String toString() {
		return ResourceProvider.getInstance().get(msgKey);
	}

	/**
	 * Returns right (bit mask) as defined in iText.
	 * @return
	 * @see PdfWriter#ALLOW_PRINTING
	 */
	public int getRight() {
		return right;
	}

}
