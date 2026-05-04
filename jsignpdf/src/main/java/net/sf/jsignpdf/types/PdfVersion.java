package net.sf.jsignpdf.types;

import org.openpdf.text.pdf.PdfWriter;

/**
 * Enum of PDF versions
 * 
 * @author Josef Cacek
 */
public enum PdfVersion {
    PDF_1_2("PDF-1.2", PdfWriter.VERSION_1_2), PDF_1_3("PDF-1.3", PdfWriter.VERSION_1_3), PDF_1_4("PDF-1.4",
            PdfWriter.VERSION_1_4), PDF_1_5("PDF-1.5", PdfWriter.VERSION_1_5), PDF_1_6("PDF-1.6",
                    PdfWriter.VERSION_1_6), PDF_1_7("PDF-1.7", PdfWriter.VERSION_1_7);

    private final String name;
    private final String stringVersion;

    private PdfVersion(final String aName, String aVersion) {
        name = aName;
        stringVersion = aVersion;
    }

    /**
     * Gets version name.
     */
    public String getVersionName() {
        return name;
    }

    /**
     * Gets version as String (representation in PdfReader and PdfWriter since OpenPDF 3.x).
     */
    public String getStringVersion() {
        return stringVersion;
    }

    public static PdfVersion fromStringVersion(String ver) {
        if (ver == null) {
            return null;
        }
        for (PdfVersion pdfVer: PdfVersion.values()) {
            if (pdfVer.getStringVersion().equals(ver)) {
                return pdfVer;
            }
        }
        return null;
    }
}
