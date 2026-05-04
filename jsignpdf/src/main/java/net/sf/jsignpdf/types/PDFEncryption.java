package net.sf.jsignpdf.types;

import static net.sf.jsignpdf.Constants.RES;

/**
 * PDF encryption type.
 * 
 * @author Josef Cacek
 */
public enum PDFEncryption {
    NONE("pdfEncryption.notEncrypted"), PASSWORD("pdfEncryption.password"), CERTIFICATE("pdfEncryption.certificate");

    private String msgKey;

    PDFEncryption(final String aMsgKey) {
        msgKey = aMsgKey;
    }

    /**
     * Returns internationalized description of a level.
     */
    @Override
    public String toString() {
        return RES.get(msgKey);
    }

}
