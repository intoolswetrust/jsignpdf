package net.sf.jsignpdf.types;

import static net.sf.jsignpdf.Constants.RES;

/**
 * Enum of possible printing rights.
 *
 * <p>
 * The integer codes are the PDF permission bit values historically exposed by iText / OpenPDF as
 * {@code PdfWriter.ALLOW_DEGRADED_PRINTING == 4} and {@code PdfWriter.ALLOW_PRINTING == 2052}. They
 * are inlined here so this shared model type carries no signing-backend dependency.
 * </p>
 *
 * @author Josef Cacek
 */
public enum PrintRight {

    DISALLOW_PRINTING("rights.disallowPrinting", 0), ALLOW_DEGRADED_PRINTING("rights.allowDegradedPrinting",
            4), ALLOW_PRINTING("rights.allowPrinting", 2052);

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
        return RES.get(msgKey);
    }

    /**
     * Returns the PDF permission bit value (as historically used by iText).
     *
     * @return the permission bit mask
     */
    public int getRight() {
        return right;
    }

}
