package net.sf.jsignpdf.types;

import static net.sf.jsignpdf.Constants.RES;

/**
 * Enum for visible sign rendering configuration.
 *
 * <p>
 * The integer codes are the appearance render flags historically exposed by iText / OpenPDF as
 * {@code PdfSignatureAppearance.SignatureRenderDescription == 0},
 * {@code SignatureRenderNameAndDescription == 1} and
 * {@code SignatureRenderGraphicAndDescription == 2}. They are inlined here so this shared model type
 * carries no signing-backend dependency.
 * </p>
 *
 * @author Josef Cacek
 */
public enum RenderMode {

    DESCRIPTION_ONLY("render.descriptionOnly", 0), GRAPHIC_AND_DESCRIPTION("render.graphicAndDescription",
            2), SIGNAME_AND_DESCRIPTION("render.signameAndDescription", 1);

    private String msgKey;
    private int render;

    RenderMode(final String aMsgKey, final int aLevel) {
        msgKey = aMsgKey;
        render = aLevel;
    }

    /**
     * Returns internationalized description of a right.
     */
    @Override
    public String toString() {
        return RES.get(msgKey);
    }

    /**
     * Returns the visible-signature render flag (as historically used by iText).
     *
     * @return integer flag
     */
    public int getRender() {
        return render;
    }

}
