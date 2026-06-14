package net.sf.jsignpdf.types;

/**
 * Enum of PDF versions.
 *
 * <p>
 * The string codes are the PDF version numbers ("1.2".."1.7") as returned by {@code PdfReader} and
 * accepted by {@code PdfWriter} since OpenPDF 3.x (historically {@code PdfWriter.VERSION_1_x}).
 * They are inlined here so this shared model type carries no signing-backend dependency.
 * </p>
 *
 * @author Josef Cacek
 */
public enum PdfVersion {
    PDF_1_2("PDF-1.2", "1.2"), PDF_1_3("PDF-1.3", "1.3"), PDF_1_4("PDF-1.4", "1.4"), PDF_1_5("PDF-1.5",
            "1.5"), PDF_1_6("PDF-1.6", "1.6"), PDF_1_7("PDF-1.7", "1.7");

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
