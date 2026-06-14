package net.sf.jsignpdf.types;

import static net.sf.jsignpdf.Constants.RES;

/**
 * Enum of possible certification levels used to Sign PDF.
 *
 * <p>
 * The integer codes are the DocMDP certification-level values defined by the PDF specification and
 * historically exposed by iText / OpenPDF as {@code PdfSignatureAppearance.NOT_CERTIFIED == -1},
 * {@code CERTIFIED_NO_CHANGES_ALLOWED == 1}, {@code CERTIFIED_FORM_FILLING == 2} and
 * {@code CERTIFIED_FORM_FILLING_AND_ANNOTATIONS == 3}. They are inlined here so this shared model
 * type carries no signing-backend dependency; the active engine maps the code onto its own library.
 * </p>
 *
 * @author Josef Cacek
 */
public enum CertificationLevel {

    NOT_CERTIFIED("certificationLevel.notCertified", -1), CERTIFIED_NO_CHANGES_ALLOWED("certificationLevel.noChanges",
            1), CERTIFIED_FORM_FILLING("certificationLevel.formFill", 2), CERTIFIED_FORM_FILLING_AND_ANNOTATIONS(
                    "certificationLevel.formFillAnnot", 3);

    private String msgKey;
    private int level;

    CertificationLevel(final String aMsgKey, final int aLevel) {
        msgKey = aMsgKey;
        level = aLevel;
    }

    /**
     * Returns internationalized description of a level.
     */
    public String toString() {
        return RES.get(msgKey);
    }

    /**
     * Returns the DocMDP certification-level code (PDF-spec value, as historically used by iText).
     *
     * @return the certification-level code
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns {@link CertificationLevel} instance for given code. If the code is not found,
     * {@link CertificationLevel#NOT_CERTIFIED} is returned.
     * 
     * @param certLevelCode level code
     * @return not-null CertificationLevel instance
     */
    public static CertificationLevel findCertificationLevel(int certLevelCode) {
        for (CertificationLevel certLevel : values()) {
            if (certLevelCode == certLevel.getLevel()) {
                return certLevel;
            }
        }
        return NOT_CERTIFIED;
    }
}
